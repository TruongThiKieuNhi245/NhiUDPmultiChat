package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Vai tro: Chon/luu tep va dieu phoi hanh dong the tep. Ai goi: ChatMulticastUI. Goi: TepClient, JFileChooser, ThongBao. */
final class TepGiaoDien {
    private final JFrame cuaSo; private final ClientCore core; private final Supplier<Phong> phong;
    private final ThongBao toast; private final Map<String,TepTienDo> tienDo=new LinkedHashMap<>();
    // Tat ca hop thoai chay EDT; viec truyen do TepClient worker dam nhan.
    TepGiaoDien(JFrame cuaSo,ClientCore core,Supplier<Phong> phong,ThongBao toast) { this.cuaSo=cuaSo; this.core=core; this.phong=phong; this.toast=toast; }
    // ===== CHON TEP VA KEO THA =====
    void menu(JComponent nut) { JPopupMenu m=menuMau(this::chon); m.show(nut,0,-m.getPreferredSize().height); }
    // Menu dung chung cho UiPreview va ung dung that.
    static JPopupMenu menuMau(Consumer<Boolean> chon) {
        JPopupMenu m=new JPopupMenu(); JMenuItem tep=new JMenuItem("Gửi tệp"),anh=new JMenuItem("Gửi ảnh");
        tep.addActionListener(e -> chon.accept(false)); anh.addActionListener(e -> chon.accept(true)); m.add(tep); m.add(anh); return m;
    }
    // Cho phep chon nhieu tep; pool Core tu xep hang sau hai luot.
    private void chon(boolean anh) {
        JFileChooser f=new JFileChooser(); f.setMultiSelectionEnabled(true);
        if (anh) { f.setFileFilter(new FileNameExtensionFilter("Ảnh PNG/JPG/GIF","png","jpg","jpeg","gif")); f.setAcceptAllFileFilterUsed(false); }
        if (f.showOpenDialog(cuaSo)==JFileChooser.APPROVE_OPTION) { for (var p:f.getSelectedFiles()) { gui(p.toPath()); } }
    }
    // Kiem tra phong ngay luc chon; Server van kiem tra lai khi INIT va DONE.
    private void gui(Path p) {
        Phong r=phong.get(); if (r==null || !r.dangMo || !core.sanSang()) { loi("Phòng đang đóng hoặc mất kết nối."); return; }
        core.tep().gui(r.id,p).exceptionally(e -> { SwingUtilities.invokeLater(() -> loi("Không thể gửi tệp: "+ClientCore.loiDeHieu(e))); return null; });
    }
    // Drop handler chi lay danh sach File, khong doc du lieu tep tren EDT.
    void datKeoTha(JComponent vung) {
        vung.setTransferHandler(new TransferHandler() {
            // Chi chap nhan tep tu he dieu hanh khi dang online.
            public boolean canImport(TransferSupport s) { return core.sanSang() && s.isDataFlavorSupported(DataFlavor.javaFileListFlavor); }
            // Chuyen danh sach tep sang hang doi Core.
            public boolean importData(TransferSupport s) {
                if (!canImport(s)) { return false; }
                try { for (Object p:(java.util.List<?>)s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)) { gui(((java.io.File)p).toPath()); } return true; }
                catch (Exception e) { loi("Không đọc được danh sách tệp kéo thả."); return false; }
            }
        });
    }
    // ===== CAC NUT THE TEP =====
    void hanhDong(String lenh,TinNhan tin) {
        TepTin t=TepTin.doc(tin.roomId,tin.userId,tin.noiDung,tin.thoiGian); TepTienDo td=tienDo.get(t.id());
        switch (lenh) {
            case "Hủy": if (td!=null) { core.tep().huy(td.maClient()); } break;
            case "Thử lại": if (td!=null && td.gui()) { core.tep().thuLai(td.maClient()); } else { tai(t,false); } break;
            case "Mở thư mục": if (td!=null && td.duong()!=null) { moThuMuc(td.duong()); } break;
            case "Mở ảnh": tai(t,true); break;
            default: tai(t,false);
        }
    }
    // Hoi vi tri luu; Core kiem tra trung ten lan nua ngay truoc khi doi ten tep tam.
    private void tai(TepTin t,boolean moAnh) {
        JFileChooser f=hopLuu(t.ten()); if (f.showSaveDialog(cuaSo)!=JFileChooser.APPROVE_OPTION) { return; }
        Path p=f.getSelectedFile().toPath();
        core.tep().tai(t,p).whenComplete((d,e) -> SwingUtilities.invokeLater(() -> {
            if (e!=null) { loi("Không thể tải tệp: "+ClientCore.loiDeHieu(e)); return; }
            toast.hien(ThongBao.Muc.THANH_CONG,"Đã tải xong "+t.ten(),"Mở thư mục",() -> moThuMuc(d));
            if (moAnh && d.getFileName().toString().toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpe?g|gif)")) { nen(() -> Desktop.getDesktop().open(d.toFile())); }
        }));
    }
    // UiPreview dung cung hop thoai luu, khong tu tao ban sao giao dien.
    static JFileChooser hopLuu(String ten) {
        Path downloads=Path.of(System.getProperty("user.home"),"Downloads"); JFileChooser f=new JFileChooser(downloads.toFile()); f.setSelectedFile(downloads.resolve(TepTin.tenAnToan(ten)).toFile()); return f;
    }
    // Ghi nho luot dang hien thi de nut Huy/Thu lai/Mo thu muc dung tep.
    void tienDo(TepTienDo td) { tienDo.put(td.tep().id(),td); if (tienDo.size()>200) { tienDo.remove(tienDo.keySet().iterator().next()); } }
    // Xoa thong tin khi dang xuat, khong giu duong dan tep cua phien cu.
    void xoa() { tienDo.clear(); }
    // Mo thu muc bang Desktop tren worker, khong bao gio thuc thi tep vua tai.
    private void moThuMuc(Path p) { nen(() -> Desktop.getDesktop().open(p.toAbsolutePath().getParent().toFile())); }
    // Tac vu he dieu hanh co the cham, khong chan EDT.
    private void nen(Viec viec) { java.util.concurrent.CompletableFuture.runAsync(() -> { try { viec.chay(); } catch (Exception e) { SwingUtilities.invokeLater(() -> loi("Không mở được thư mục hoặc ảnh.")); } }); }
    // Loi hien toast de khong chan hoi thoai khac.
    private void loi(String s) { toast.hien(ThongBao.Muc.LOI,s); }
    // Hop dong nho cho Desktop co IOException.
    private interface Viec { void chay() throws Exception; }
}
