package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import static com.mycompany.udpmultichat.ui.ThanhPhan.*;

/** Cua so nhieu tab phong; chi Swing, moi lenh mang chay nen qua ClientCore. */
@SuppressWarnings("serial")
public final class ChatMulticastUI extends JFrame implements Listener {
    private static final Color CHINH=new Color(0x4F6BED);
    private final ClientCore client=new ClientCore(this);
    private final ExecutorService viecNen=Executors.newSingleThreadExecutor(viec -> {
        Thread t=new Thread(viec,"ui-client-task"); t.setDaemon(true); return t;
    });
    private final JTabbedPane tab=new JTabbedPane();
    private final Map<String,KhungChatPanel> cacKhung=new LinkedHashMap<>();
    private final Map<String,Integer> chuaDoc=new LinkedHashMap<>();
    private final Map<String,List<String>> cacThanhVien=new LinkedHashMap<>();
    private final DefaultListModel<String> dsOnline=new DefaultListModel<>();
    private final JTextField nhap=new JTextField();
    private final NutBo gui=new NutBo("Gửi",CHINH,new Color(0x3B54C9));
    private final NutBo dangXuat=new NutBo("Đăng xuất",new Color(0xEF6C6C),new Color(0xD65454));
    private final javax.swing.JButton roiPhong=new javax.swing.JButton("Rời phòng");
    private final JLabel trangThai=new JLabel("Chưa đăng nhập"), soOnline=new JLabel("Thành viên: 0");
    private final JLabel phanHoi=new JLabel("Bật Server, đăng ký tài khoản rồi đăng nhập.");
    private final DangNhapPanel dangNhap=new DangNhapPanel(this::taiKhoan);
    private final DanhSachPhongPanel phongPanel;
    private List<Phong> danhSach=List.of();
    private String tenToi="";
    private boolean dangGui,dangDong,daVaoTaiKhoan;
    private int soViec;

    public ChatMulticastUI() {
        phongPanel=new DanhSachPhongPanel(client::nguoiDung,id -> chuaDoc.getOrDefault(id,0),new DanhSachPhongPanel.SuKien() {
            @Override public void vao(String khoa,String matKhau) { vaoPhong(khoa,matKhau); }
            @Override public void tao(DanhSachPhongPanel.ThongTin t) {
                thucHien(() -> client.taoPhong(t.ten(),t.moTa(),t.congKhai(),t.matKhau(),t.gioiHan())
                    .thenCompose(p -> client.vaoPhong(p.id,"")),p -> { chonPhong(p.id); thongBao("Đã tạo phòng. Mã: "+p.maPhong); });
            }
            @Override public void sua(Phong p,DanhSachPhongPanel.ThongTin t) {
                thucHien(() -> client.suaPhong(p.id,t.ten(),t.moTa(),t.congKhai(),t.matKhau(),t.gioiHan()),v -> thongBao("Đã cập nhật phòng."));
            }
            @Override public void xoa(Phong p) { thucHien(() -> client.xoaPhong(p.id),v -> thongBao("Đã xóa phòng «"+p.ten+"».")); }
            @Override public void dongMo(Phong p) { thucHien(() -> client.dongMoPhong(p.id,!p.dangMo),v -> thongBao("Đã đổi trạng thái phòng.")); }
        });
        setTitle("UDP Multi-room Chat"); setSize(1200,740); setMinimumSize(new Dimension(1050,600));
        setLocationByPlatform(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); setLayout(new BorderLayout());
        JPanel dau=new JPanel(new BorderLayout()); dau.add(taoTieuDe(),BorderLayout.NORTH); dau.add(dangNhap,BorderLayout.CENTER);
        add(dau,BorderLayout.NORTH); add(taoCotTrai(),BorderLayout.WEST);
        add(tab,BorderLayout.CENTER); add(taoThanhVien(),BorderLayout.EAST); add(taoThanhNhap(),BorderLayout.SOUTH);
        gui.addActionListener(e -> guiTin()); nhap.addActionListener(e -> guiTin());
        tab.addChangeListener(e -> capNhatDangChon());
        dangXuat.addActionListener(e -> thucHien(client::dangXuat,v -> daDangXuat("Đã đăng xuất.")));
        roiPhong.addActionListener(e -> {
            KhungChatPanel k=dangChon();
            if (k!=null) { thucHien(() -> client.roiPhong(k.phong().id),v -> thongBao("Đã rời phòng.")); }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dongCuaSo(); } });
        capNhat();
    }

    // ===== BO CUC =====
    private JPanel taoTieuDe() {
        JPanel dau=new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,CHINH,getWidth(),0,new Color(0x7A5CF0)));
                g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
        dau.setBorder(new EmptyBorder(16,20,16,20));
        JLabel ten=new JLabel("UDP Multi-room Chat"); ten.setFont(new Font("Segoe UI",Font.BOLD,22)); ten.setForeground(Color.WHITE);
        trangThai.setForeground(Color.WHITE); dau.add(ten,BorderLayout.WEST); dau.add(trangThai,BorderLayout.EAST); return dau;
    }
    private JPanel taoCotTrai() {
        JPanel trai=new JPanel(new BorderLayout()); trai.setPreferredSize(new Dimension(275,0));
        trai.add(phongPanel,BorderLayout.CENTER);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.CENTER,6,8)); nut.setBackground(Color.WHITE);
        nut.add(roiPhong); nut.add(dangXuat); trai.add(nut,BorderLayout.SOUTH); return trai;
    }
    private JPanel taoThanhVien() {
        JList<String> online=new JList<>(dsOnline); online.setCellRenderer(new OnlineRenderer(() -> tenToi));
        JPanel phai=new JPanel(new BorderLayout()); phai.setBackground(Color.WHITE); phai.setPreferredSize(new Dimension(205,0));
        soOnline.setBorder(new EmptyBorder(16,12,12,12)); phai.add(soOnline,BorderLayout.NORTH);
        JScrollPane cuon=new JScrollPane(online); cuon.setBorder(null); phai.add(cuon,BorderLayout.CENTER); return phai;
    }
    private JPanel taoThanhNhap() {
        JPanel thanh=new JPanel(new BorderLayout(10,8)); thanh.setBackground(Color.WHITE); thanh.setBorder(new EmptyBorder(8,16,12,16));
        nhap.setFont(new Font("Segoe UI",Font.PLAIN,14));
        nhap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xE3E6EF)),new EmptyBorder(8,12,8,12)));
        phanHoi.setFont(new Font("Segoe UI",Font.PLAIN,12)); phanHoi.setForeground(new Color(0x747B91));
        thanh.add(phanHoi,BorderLayout.NORTH); thanh.add(nhap,BorderLayout.CENTER); thanh.add(gui,BorderLayout.EAST); return thanh;
    }

    // ===== TAI KHOAN VA LENH NEN =====
    private void taiKhoan(DangNhapPanel.ThongTin t) {
        if (t.dangKy()) {
            thucHien(() -> client.dangKy(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),v -> thongBao("Đăng ký thành công. Bấm Đăng nhập."));
        } else {
            thucHien(() -> client.dangNhap(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),this::daDangNhap);
        }
    }
    private void daDangNhap(NguoiDung toi) {
        daVaoTaiKhoan=true; tenToi=toi.tenHienThi; dangNhap.xoaMatKhau(); dangNhap.setVisible(false);
        String vai=toi.vaiTro==VaiTro.ADMIN?"ADMIN":"Thành viên";
        setTitle("UDP Chat — "+tenToi+" ("+vai+")"); trangThai.setText(tenToi+" • "+vai);
        chonPhong(Protocol.SANH_ID); thongBao("Đã vào Sảnh chung."); capNhat();
    }
    private void vaoPhong(String khoa,String matKhau) {
        thucHien(() -> client.vaoPhong(khoa,matKhau),p -> { chonPhong(p.id); thongBao("Đã vào «"+p.ten+"». Mã: "+p.maPhong); });
    }
    private void guiTin() {
        KhungChatPanel khung=dangChon(); String noiDung=nhap.getText();
        if (khung==null || dangGui || noiDung.isBlank() || !daVaoTaiKhoan) { return; }
        String id=khung.phong().id; dangGui=true;
        thucHien(() -> client.guiTin(id,noiDung),v -> {
            if (dangChon()==khung && nhap.getText().equals(noiDung)) { nhap.setText(""); }
            dangGui=false; thongBao("Đã gửi vào «"+khung.phong().ten+"».");
        });
    }
    private <T> void thucHien(Supplier<CompletableFuture<T>> viec,Consumer<T> xong) {
        soViec++; capNhat(); thongBao("Đang xử lý…");
        viecNen.execute(() -> {
            CompletableFuture<T> ketQua;
            try { ketQua=viec.get(); } catch (RuntimeException e) { ketQua=CompletableFuture.failedFuture(e); }
            ketQua.whenComplete((v,e) -> SwingUtilities.invokeLater(() -> {
                soViec=Math.max(0,soViec-1); if (dangDong) { return; }
                if (e==null) { xong.accept(v); }
                else { dangGui=false; thongBao("Lỗi: "+ClientCore.loiDeHieu(e)); }
                capNhat();
            }));
        });
    }
    private void daDangXuat(String lyDo) {
        daVaoTaiKhoan=false; dangGui=false; tenToi="";
        tab.removeAll(); cacKhung.clear(); chuaDoc.clear(); cacThanhVien.clear(); danhSach=List.of();
        dangNhap.setVisible(true); dsOnline.clear(); soOnline.setText("Thành viên: 0");
        trangThai.setText("Chưa đăng nhập"); setTitle("UDP Multi-room Chat");
        lamMoiDanhSach(); thongBao(lyDo); capNhat();
    }
    private void dongCuaSo() {
        if (dangDong) { return; }
        dangDong=true; setTitle("Đang thoát…");
        viecNen.execute(() -> client.dangXuat().whenComplete((v,e) -> {
            client.close(); viecNen.shutdown(); SwingUtilities.invokeLater(this::dispose);
        }));
    }

    // ===== TAB, THANH VIEN VA BADGE =====
    private KhungChatPanel dangChon() { return tab.getSelectedComponent() instanceof KhungChatPanel k?k:null; }
    private void chonPhong(String id) {
        KhungChatPanel k=cacKhung.get(id); if (k!=null) { tab.setSelectedComponent(k); phongPanel.chon(id); }
    }
    private void capNhatDangChon() {
        KhungChatPanel k=dangChon(); dsOnline.clear();
        if (k!=null) {
            String id=k.phong().id; chuaDoc.put(id,0);
            cacThanhVien.getOrDefault(id,List.of()).forEach(dsOnline::addElement); phongPanel.chon(id);
        }
        soOnline.setText("Thành viên: "+dsOnline.size()); capNhatTieuDeTab(); phongPanel.repaint(); capNhat();
    }
    private void capNhatTieuDeTab() {
        cacKhung.forEach((id,k) -> {
            int i=tab.indexOfComponent(k), so=chuaDoc.getOrDefault(id,0);
            if (i>=0) { tab.setTitleAt(i,k.phong().ten+(so>0?" ["+so+"]":"")); }
        });
    }
    private void lamMoiDanhSach() {
        Set<String> daVao=client.phongDaVao().stream().map(p -> p.id).collect(Collectors.toSet());
        phongPanel.capNhat(danhSach,daVao);
    }
    private void capNhat() {
        boolean ranh=soViec==0;
        dangNhap.choPhep(!daVaoTaiKhoan && ranh); phongPanel.choPhep(daVaoTaiKhoan && ranh);
        dangXuat.setEnabled(daVaoTaiKhoan && ranh); roiPhong.setEnabled(daVaoTaiKhoan && ranh && dangChon()!=null);
        boolean duocGui=daVaoTaiKhoan && dangChon()!=null && dangChon().phong().dangMo && !dangGui;
        nhap.setEnabled(duocGui); gui.setEnabled(duocGui && ranh);
    }
    private void thongBao(String noiDung) { phanHoi.setText(noiDung); phanHoi.setToolTipText(noiDung); }

    // ===== SU KIEN TU CLIENTCORE: LUON VE EDT =====
    @Override public void onVaoPhong(Phong p) {
        SwingUtilities.invokeLater(() -> {
            if (dangDong || cacKhung.containsKey(p.id)) { return; }
            KhungChatPanel k=new KhungChatPanel(p); cacKhung.put(p.id,k); tab.addTab(p.ten,k);
            capNhatDangChon(); lamMoiDanhSach();
        });
    }
    @Override public void onRoiPhong(String id,String lyDo) {
        SwingUtilities.invokeLater(() -> {
            KhungChatPanel k=cacKhung.remove(id); if (k!=null) { tab.remove(k); }
            chuaDoc.remove(id); cacThanhVien.remove(id); capNhatDangChon(); lamMoiDanhSach(); thongBao(lyDo);
        });
    }
    @Override public void onDanhSachPhong(List<Phong> ds) {
        SwingUtilities.invokeLater(() -> {
            danhSach=List.copyOf(ds);
            ds.forEach(p -> { KhungChatPanel k=cacKhung.get(p.id); if (k!=null) { k.capNhat(p); } });
            lamMoiDanhSach(); capNhatTieuDeTab(); capNhat();
        });
    }
    @Override public void onTinNhan(TinNhan tin,boolean laToi) { onTinNhan(tin,laToi,false); }
    @Override public void onTinNhan(TinNhan tin,boolean laToi,boolean lichSu) {
        SwingUtilities.invokeLater(() -> {
            KhungChatPanel k=cacKhung.get(tin.roomId); if (k==null) { return; }
            k.themTin(tin,laToi);
            if (!lichSu && !laToi && dangChon()!=k) { chuaDoc.merge(tin.roomId,1,Integer::sum); }
            capNhatTieuDeTab(); phongPanel.repaint();
        });
    }
    @Override public void onHeThong(String id,String noiDung) {
        SwingUtilities.invokeLater(() -> { KhungChatPanel k=cacKhung.get(id); if (k!=null) { k.heThong(noiDung); } });
    }
    @Override public void onHeThong(String noiDung) { SwingUtilities.invokeLater(() -> thongBao(noiDung)); }
    @Override public void onLoi(String loi) { SwingUtilities.invokeLater(() -> thongBao("Lỗi: "+loi)); }
    @Override public void onMatKetNoi(String lyDo) { SwingUtilities.invokeLater(() -> daDangXuat(lyDo)); }
    @Override public void onDanhSachOnline(List<String> ds) { onThanhVien(Protocol.SANH_ID,ds); }
    @Override public void onThanhVien(String id,List<String> ds) {
        SwingUtilities.invokeLater(() -> {
            cacThanhVien.put(id,List.copyOf(ds));
            if (dangChon()!=null && dangChon().phong().id.equals(id)) { capNhatDangChon(); }
        });
    }
}
