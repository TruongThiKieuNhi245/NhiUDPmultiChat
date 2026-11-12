package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Cua so hai man hinh, ba cot chat; UI chi goi ClientCore tren luong nen. */
@SuppressWarnings("serial")
public final class ChatMulticastUI extends JFrame implements Listener {
    private final ClientCore client=new ClientCore(this);
    private final ExecutorService nen=Executors.newSingleThreadExecutor(v -> { Thread t=new Thread(v,"ui-client-task"); t.setDaemon(true); return t; });
    private final CardLayout cards=new CardLayout();
    private final JPanel goc=new JPanel(cards),chat=new JPanel(new BorderLayout()),giua=new JPanel(new BorderLayout());
    private final JTabbedPane tab=new JTabbedPane();
    private final Map<String,KhungChatPanel> khung=new LinkedHashMap<>();
    private final Map<String,Integer> chuaDoc=new LinkedHashMap<>();
    private final Map<String,List<Phong.ThanhVien>> thanhVien=new LinkedHashMap<>();
    private final DanhSachPhongPanel phongPanel;
    private final ThanhVienPanel thanhVienPanel=new ThanhVienPanel();
    private final SoanTinPanel soan=new SoanTinPanel(this::guiTin);
    private final DangNhapPanel dangNhap=new DangNhapPanel(this::taiKhoan,this::thuServer);
    private final ThongBao toast=new ThongBao(getLayeredPane());
    private final JLabel tenPhong=Theme.nhan("Chọn phòng để trò chuyện",true),chiTiet=Theme.nhan(" ",false),ketNoi=Theme.nhan(" ",false);
    private final JButton avatar=Theme.nut("Tài khoản",false),copy=Theme.nut("Sao chép mã",false),thanhVienNut=Theme.nut("Thành viên",false),menuPhong=Theme.nut("",false);
    private NguoiDung toi;
    private boolean dong,dangGui,online=true;
    private List<Phong> danhSach=List.of();
    private Timer anDai;

    public ChatMulticastUI() {
        phongPanel=new DanhSachPhongPanel(() -> toi,id -> chuaDoc.getOrDefault(id,0),p -> vaoPhong(p.id,""),this::hanhDong);
        setTitle("UDP Chat"); setSize(1240,800); setMinimumSize(new Dimension(1000,720)); setLocationByPlatform(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel trai=new JPanel(new BorderLayout()); trai.setPreferredSize(new Dimension(290,0)); trai.setBackground(Theme.NEN);
        trai.add(phongPanel,BorderLayout.CENTER); avatar.setPreferredSize(new Dimension(100,68)); trai.add(avatar,BorderLayout.SOUTH);
        avatar.addActionListener(e -> menuAvatar().show(avatar,0,-160));
        chat.add(trai,BorderLayout.WEST); chat.add(giua,BorderLayout.CENTER); chat.add(thanhVienPanel,BorderLayout.EAST);
        giua.add(tieuDePhong(),BorderLayout.NORTH); giua.add(tab,BorderLayout.CENTER); giua.add(soan,BorderLayout.SOUTH);
        JPanel chatVaCanhBao=new JPanel(new BorderLayout()); ketNoi.setOpaque(true); ketNoi.setBorder(Theme.le(Theme.S8)); ketNoi.setVisible(false);
        chatVaCanhBao.add(ketNoi,BorderLayout.NORTH); chatVaCanhBao.add(chat,BorderLayout.CENTER);
        goc.add(dangNhap,"login"); goc.add(chatVaCanhBao,"chat"); setContentPane(goc); cards.show(goc,"login");
        tab.addChangeListener(e -> chonTab());
        copy.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { DanhSachPhongPanel.saoChep(p.maPhong); toast(ThongBao.Muc.THANH_CONG,"Đã sao chép mã phòng."); } });
        thanhVienNut.addActionListener(e -> { thanhVienPanel.setVisible(!thanhVienPanel.isVisible()); chat.revalidate(); });
        menuPhong.setIcon(Theme.icon("more"));
        menuPhong.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { phongPanel.menu(p).show(menuPhong,0,menuPhong.getHeight()); } });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dongCuaSo(); } }); capNhat();
    }
    private JPanel tieuDePhong() {
        JPanel dau=new JPanel(new BorderLayout(Theme.S16,0)); dau.setBorder(Theme.le(Theme.S16));
        JPanel text=new JPanel(new GridLayout(2,1,0,Theme.S4)); tenPhong.setFont(Theme.DAM.deriveFont(18f)); chiTiet.setFont(Theme.NHO); chiTiet.setForeground(Theme.PHU);
        text.add(tenPhong); text.add(chiTiet); dau.add(text,BorderLayout.CENTER);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S4,0)); nut.add(copy); nut.add(thanhVienNut); nut.add(menuPhong); dau.add(nut,BorderLayout.EAST); return dau;
    }

    // ===== TAI KHOAN VA FORM =====
    private void taiKhoan(DangNhapPanel.ThongTin t) {
        dangNhap.ban(true);
        if (t.dangKy()) {
            chay(() -> client.dangKy(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),v -> { dangNhap.ban(false); dangNhap.dangKyXong(); },e -> dangNhap.loiServer(e));
        } else {
            chay(() -> client.dangNhap(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),n -> {
                toi=n; online=true; dangNhap.ban(false); dangNhap.xoaMatKhau(); cards.show(goc,"chat");
                avatar.setIcon(Theme.avatar(n.tenHienThi)); avatar.setText(n.tenHienThi+(n.vaiTro==VaiTro.ADMIN?"  · Admin":""));
                setTitle("UDP Chat — "+n.tenHienThi); chonPhong(Protocol.SANH_ID); capNhat();
            },e -> dangNhap.loiServer(e));
        }
    }
    private void thuServer(String ip) {
        dangNhap.ban(true);
        chay(() -> client.kiemTraServer(ip,Protocol.CONG_SERVER),v -> dangNhap.ketQuaThu("Server đang hoạt động.",true),e -> dangNhap.ketQuaThu(e,false));
    }
    JPopupMenu menuAvatar() {
        JPopupMenu m=new JPopupMenu();
        for (String ten:new String[]{"Hồ sơ của tôi (M3)","Đổi mật khẩu (M3)","Giao diện Sáng/Tối (M6)"}) { JMenuItem i=new JMenuItem(ten); i.setEnabled(false); m.add(i); }
        m.addSeparator(); JMenuItem thoat=new JMenuItem("Đăng xuất");
        thoat.addActionListener(e -> chay(client::dangXuat,v -> veDangNhap("Đã đăng xuất."),this::loi)); m.add(thoat); return m;
    }
    private void veDangNhap(String lyDo) {
        toi=null; tab.removeAll(); khung.clear(); thanhVien.clear(); chuaDoc.clear(); danhSach=List.of(); phongPanel.xoa();
        cards.show(goc,"login"); dangNhap.ban(false); setTitle("UDP Chat"); ketNoi.setVisible(false); capNhat(); toast(ThongBao.Muc.THONG_TIN,lyDo);
    }
    private void vaoPhong(String khoa,String mk) {
        if (!online) { return; }
        chay(() -> client.vaoPhong(khoa,mk),p -> chonPhong(p.id),e -> {
            if (e.contains("Mật khẩu")) { HopThoai f=new HopThoai(null,true);
                danhSach.stream().filter(p -> p.id.equals(khoa) || p.maPhong.equalsIgnoreCase(khoa)).findFirst().ifPresent(p -> f.datMa(p.maPhong));
                f.coMatKhau(); f.hien(this,h -> guiVaoForm(h)); }
            else { loi(e); }
        });
    }
    private void guiVaoForm(HopThoai f) {
        f.ban(true); chay(() -> client.vaoPhong(f.ma(),f.matKhau()),p -> { f.dong(); chonPhong(p.id); },f::loiServer);
    }

    // ===== CRUD, XAC NHAN VA CHUYEN CHU =====
    private void hanhDong(String lenh,Phong p) {
        if (!online) { return; }
        switch (lenh) {
            case "ma": new HopThoai(null,true).hien(this,this::guiVaoForm); break;
            case "tao": case "sua":
                HopThoai f=new HopThoai(p,false);
                f.hien(this,h -> {
                    var t=h.duLieu(); h.ban(true);
                    Supplier<CompletableFuture<Phong>> viec=p==null?
                        () -> client.taoPhong(t.ten(),t.moTa(),t.congKhai(),t.matKhau(),t.gioiHan()).thenCompose(r -> client.vaoPhong(r.id,"")):
                        () -> client.suaPhong(p.id,t.ten(),t.moTa(),t.congKhai(),t.matKhau(),t.gioiHan());
                    chay(viec,r -> { h.dong(); chonPhong(r.id); toast(ThongBao.Muc.THANH_CONG,p==null?"Đã tạo phòng.":"Đã lưu phòng."); },h::loiServer);
                }); break;
            case "trangthai": chay(() -> client.dongMoPhong(p.id,!p.dangMo),v -> toast(ThongBao.Muc.THONG_TIN,"Đã đổi trạng thái phòng."),this::loi); break;
            case "xoa": case "roi":
                boolean xoa=lenh.equals("xoa");
                if (JOptionPane.showConfirmDialog(this,(xoa?"Xóa":"Rời")+" phòng «"+p.ten+"»?",xoa?"Xóa phòng":"Rời phòng",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                    chay(() -> xoa?client.xoaPhong(p.id):client.roiPhong(p.id),v -> toast(ThongBao.Muc.THANH_CONG,xoa?"Đã xóa phòng.":"Đã rời phòng."),this::loi);
                } break;
            case "chuyen": chuyenChu(p); break;
            case "thongtin": JOptionPane.showMessageDialog(this,p.ten+"\n"+p.moTa+"\nMã: "+p.maPhong+"\n"+(p.congKhai?"Công khai":"Riêng tư")+" · Giới hạn "+p.gioiHan,"Thông tin phòng",JOptionPane.INFORMATION_MESSAGE); break;
            default: break;
        }
    }
    private void chuyenChu(Phong p) {
        var ds=thanhVien.getOrDefault(p.id,List.of()).stream().filter(n -> !n.id().equals(p.chuId)).toList();
        if (ds.isEmpty()) { loi("Cần có thành viên khác trong phòng để chuyển quyền."); return; }
        Object chon=JOptionPane.showInputDialog(this,"Chọn chủ phòng mới:","Chuyển quyền",JOptionPane.PLAIN_MESSAGE,null,ds.stream().map(Phong.ThanhVien::ten).toArray(),ds.get(0).ten());
        if (chon!=null) { String id=ds.stream().filter(n -> n.ten().equals(chon)).findFirst().orElseThrow().id(); chay(() -> client.chuyenChu(p.id,id),v -> toast(ThongBao.Muc.THANH_CONG,"Đã chuyển quyền chủ phòng."),this::loi); }
    }
    private void guiTin(String text) {
        Phong p=phongChon(); if (p==null || dangGui) { return; }
        dangGui=true; capNhat();
        chay(() -> client.guiTin(p.id,text),v -> { dangGui=false; soan.xoaNeuGiong(text); capNhat(); },e -> { dangGui=false; capNhat(); loi(e); });
    }
    private <T> void chay(Supplier<CompletableFuture<T>> viec,Consumer<T> xong,Consumer<String> loi) {
        nen.execute(() -> {
            CompletableFuture<T> f;
            try { f=viec.get(); } catch (RuntimeException e) { f=CompletableFuture.failedFuture(e); }
            f.whenComplete((v,e) -> trenEdt(() -> { if (!dong) { if (e==null) { xong.accept(v); } else { loi.accept(ClientCore.loiDeHieu(e)); } } }));
        });
    }
    private void loi(String text) { toast(ThongBao.Muc.LOI,text); }
    void toast(ThongBao.Muc muc,String text) { toast.hien(muc,text); }

    // ===== TAB VA TRANG THAI =====
    private Phong phongChon() { return tab.getSelectedComponent() instanceof KhungChatPanel k?k.phong():null; }
    private void chonPhong(String id) { KhungChatPanel k=khung.get(id); if (k!=null) { tab.setSelectedComponent(k); } }
    private void chonTab() {
        Phong p=phongChon();
        if (p!=null) {
            chuaDoc.put(p.id,0); phongPanel.chon(p.id); tenPhong.setText(p.ten);
            var ds=thanhVien.getOrDefault(p.id,List.of()); long on=ds.stream().filter(Phong.ThanhVien::online).count();
            chiTiet.setText("Mã: "+p.maPhong+"  ·  "+ds.size()+" thành viên · "+on+" online"+(p.dangMo?"":" · Đang đóng"));
            chiTiet.setToolTipText(p.nhom+":"+p.cong); thanhVienPanel.capNhat(ds,toi==null?"":toi.id,phongPanel.quanLy(p));
        } else { tenPhong.setText("Chọn phòng để trò chuyện"); chiTiet.setText(" "); thanhVienPanel.capNhat(List.of(),"",false); }
        tieuDeTab(); capNhat(); phongPanel.repaint();
    }
    private void tieuDeTab() {
        khung.forEach((id,k) -> { int i=tab.indexOfComponent(k),n=chuaDoc.getOrDefault(id,0); if (i>=0) { tab.setTitleAt(i,k.phong().ten+(n>0?" ["+n+"]":"")); } });
    }
    private void capNhat() {
        Phong p=phongChon(); soan.choPhep(toi!=null && online && p!=null && p.dangMo,dangGui);
        phongPanel.choPhep(toi!=null && online); copy.setEnabled(p!=null); menuPhong.setEnabled(p!=null && online);
    }
    private void dongCuaSo() {
        if (dong) { return; } dong=true;
        nen.execute(() -> client.dangXuat().whenComplete((v,e) -> { client.close(); nen.shutdown(); trenEdt(this::dispose); }));
    }
    @Override public void dispose() { toast.close(); if (anDai!=null) { anDai.stop(); } nen.shutdown(); super.dispose(); }
    private static void trenEdt(Runnable r) { if (SwingUtilities.isEventDispatchThread()) { r.run(); } else { SwingUtilities.invokeLater(r); } }

    // ===== SU KIEN CLIENT: CHI CAP NHAT SWING TREN EDT =====
    @Override public void onVaoPhong(Phong p) { trenEdt(() -> { if (!khung.containsKey(p.id)) { KhungChatPanel k=new KhungChatPanel(p); khung.put(p.id,k); tab.addTab(p.ten,k); } }); }
    @Override public void onRoiPhong(String id,String lyDo) { trenEdt(() -> { KhungChatPanel k=khung.remove(id); if (k!=null) { tab.remove(k); } thanhVien.remove(id); chuaDoc.remove(id); chonTab(); if (toi!=null) { toast(ThongBao.Muc.THONG_TIN,lyDo); } }); }
    @Override public void onDanhSachPhong(List<Phong> ds) { trenEdt(() -> { danhSach=List.copyOf(ds); ds.forEach(p -> { if (khung.containsKey(p.id)) { khung.get(p.id).capNhat(p); } }); phongPanel.capNhat(ds); chonTab(); }); }
    @Override public void onTinNhan(TinNhan t,boolean laToi) { onTinNhan(t,laToi,false); }
    @Override public void onTinNhan(TinNhan t,boolean laToi,boolean lichSu) { trenEdt(() -> {
        KhungChatPanel k=khung.get(t.roomId); if (k==null) { return; } k.themTin(t,laToi); phongPanel.tinCuoi(t);
        if (!lichSu && !laToi && tab.getSelectedComponent()!=k) { chuaDoc.merge(t.roomId,1,Integer::sum); } tieuDeTab(); phongPanel.repaint();
    }); }
    @Override public void onThanhVienChiTiet(String id,List<Phong.ThanhVien> ds) { trenEdt(() -> { thanhVien.put(id,List.copyOf(ds)); if (phongChon()!=null && phongChon().id.equals(id)) { chonTab(); } }); }
    @Override public void onHeThong(String id,String text) { trenEdt(() -> { if (khung.containsKey(id)) { khung.get(id).heThong(text); } }); }
    @Override public void onHeThong(String text) { trenEdt(() -> toast(ThongBao.Muc.THONG_TIN,text)); }
    @Override public void onDanhSachOnline(List<String> ds) { }
    @Override public void onLoi(String text) { trenEdt(() -> loi(text)); }
    @Override public void onMatKetNoi(String text) { trenEdt(() -> veDangNhap(text)); }
    @Override public void onKetNoi(boolean sanSang,String text) { trenEdt(() -> {
        online=sanSang; ketNoi.setText(text); ketNoi.setBackground(sanSang?Theme.NEN_XANH:Theme.NEN_VANG); ketNoi.setForeground(sanSang?Theme.THANH_CONG:Theme.CANH_BAO); ketNoi.setVisible(true);
        if (anDai!=null) { anDai.stop(); }
        if (sanSang) { anDai=new Timer(3800,e -> ketNoi.setVisible(false)); anDai.setRepeats(false); anDai.start(); } capNhat();
    }); }
    // UiPreview dung dung giao dien that, khong tao ket noi mang.
    void duLieuMau(NguoiDung n,List<Phong> ds) {
        toi=n; online=true; avatar.setIcon(Theme.avatar(n.tenHienThi)); avatar.setText(n.tenHienThi+"  · Admin"); cards.show(goc,"chat"); onDanhSachPhong(ds); ds.forEach(this::onVaoPhong); chonPhong(ds.get(0).id); capNhat();
    }
}
