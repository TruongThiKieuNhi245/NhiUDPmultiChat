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
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Vai tro: Cua so hai man hinh, ba cot chat; UI chi goi ClientCore tren luong nen. Ai goi: ClientMain, UiPreview, Listener. Goi: ClientCore, cac panel Swing, HopThoai, Theme. */
@SuppressWarnings("serial")
public final class ChatMulticastUI extends JFrame implements Listener {
    private final ClientCore client=new ClientCore(this);
    private final ExecutorService nen=Executors.newSingleThreadExecutor(v -> { Thread t=new Thread(v,"ui-client-task"); t.setDaemon(true); return t; });
    private final CardLayout cards=new CardLayout();
    private final JPanel goc=new JPanel(cards),chat=new JPanel(new BorderLayout()),giua=new JPanel(new BorderLayout());
    private final CardLayout cacPhong=new CardLayout();
    private final JPanel noiDungPhong=new JPanel(cacPhong);
    private String phongDangXem="";
    private final Map<String,KhungChatPanel> khung=new LinkedHashMap<>();
    private final Map<String,Integer> chuaDoc=new LinkedHashMap<>();
    private final Map<String,List<Phong.ThanhVien>> thanhVien=new LinkedHashMap<>();
    private final DanhSachPhongPanel phongPanel;
    private final ThanhVienPanel thanhVienPanel=new ThanhVienPanel();
    private final SoanTinPanel soan=new SoanTinPanel(this::guiTin);
    private final DangNhapPanel dangNhap=new DangNhapPanel(this::taiKhoan,this::thuServer);
    private final ThongBao toast=new ThongBao(getLayeredPane());
    private final JLabel tenPhong=Theme.nhan("Chọn phòng để trò chuyện",true),chiTiet=Theme.nhan(" ",false),ketNoi=Theme.nhan(" ",false);
    private final JButton avatar=Theme.nutNhe("Tài khoản",""),copy=Theme.nutNhe("Sao chép mã",""),thanhVienNut=Theme.nutNhe("Thành viên (0)",""),menuPhong=Theme.nutNhe("","more");
    private final JLabel tenTaiKhoan=Theme.nhan("",true),vaiTaiKhoan=Theme.nhan("",false),anhTaiKhoan=Theme.nhan("",false);
    private NguoiDung toi;
    private boolean dong,dangGui,online=true;
    private List<Phong> danhSach=List.of();
    private Timer anDai;

    // Khoi tao lop va noi cac thanh phan can thiet.
    public ChatMulticastUI() {
        thanhVienPanel.datAnh(client.hoSo()::anh);
        phongPanel=new DanhSachPhongPanel(() -> toi,khung::containsKey,id -> chuaDoc.getOrDefault(id,0),p -> vaoPhong(p.id,""),this::hanhDong);
        setIconImage(Theme.iconUngDung()); setTitle("UDP Chat"); setSize(1240,800); setMinimumSize(new Dimension(960,600)); setLocationByPlatform(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel trai=new JPanel(new BorderLayout()); trai.setPreferredSize(new Dimension(280,0)); trai.setBackground(Theme.NEN);
        trai.add(phongPanel,BorderLayout.CENTER); trai.add(theTaiKhoan(),BorderLayout.SOUTH);
        avatar.addActionListener(e -> { JPopupMenu m=menuAvatar(); m.show(avatar,0,-m.getPreferredSize().height); });
        chat.add(trai,BorderLayout.WEST); chat.add(giua,BorderLayout.CENTER); chat.add(thanhVienPanel,BorderLayout.EAST);
        giua.add(tieuDePhong(),BorderLayout.NORTH); giua.add(noiDungPhong,BorderLayout.CENTER); giua.add(soan,BorderLayout.SOUTH);
        JPanel chatVaCanhBao=new JPanel(new BorderLayout()); ketNoi.setOpaque(true); ketNoi.setBorder(Theme.le(Theme.S8)); ketNoi.setVisible(false);
        chatVaCanhBao.add(ketNoi,BorderLayout.NORTH); chatVaCanhBao.add(chat,BorderLayout.CENTER);
        javax.swing.JScrollPane loginCuon=new javax.swing.JScrollPane(dangNhap); loginCuon.setBorder(null); loginCuon.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        goc.add(loginCuon,"login"); goc.add(chatVaCanhBao,"chat"); setContentPane(goc); cards.show(goc,"login");
        toast.datVung(noiDungPhong);
        copy.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { DanhSachPhongPanel.saoChep(p.maPhong); toast(ThongBao.Muc.THANH_CONG,"Đã sao chép mã phòng."); } });
        thanhVienNut.addActionListener(e -> { thanhVienPanel.setVisible(!thanhVienPanel.isVisible()); chat.revalidate(); });
        menuPhong.setIcon(Theme.icon("more"));
        menuPhong.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { phongPanel.menu(p).show(menuPhong,0,menuPhong.getHeight()); } });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dongCuaSo(); } }); capNhat();
    }
    // Tieu de khong lap thanh tab; ma va nut co du cho o cua so toi thieu.
    private JPanel tieuDePhong() {
        JPanel dau=new JPanel(new BorderLayout(Theme.S8,0)); dau.setBorder(Theme.le(Theme.S16));
        tenPhong.setFont(Theme.DAM.deriveFont(18f)); chiTiet.setFont(Theme.NHO); chiTiet.setForeground(Theme.PHU);
        JPanel ten=new JPanel(new GridLayout(2,1,0,Theme.S4)); ten.add(tenPhong); ten.add(chiTiet); dau.add(ten,BorderLayout.CENTER);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S4,0)); nut.add(copy); nut.add(thanhVienNut); nut.add(menuPhong);
        dau.add(nut,BorderLayout.EAST); return dau;
    }
    // The tai khoan co le, hai dong va mui ten de nhan biet menu.
    private JPanel theTaiKhoan() {
        JPanel boc=new JPanel(new BorderLayout()); boc.setBackground(Theme.NEN); boc.setBorder(Theme.le(Theme.S12));
        avatar.setHorizontalAlignment(JButton.LEFT); avatar.setPreferredSize(new Dimension(220,60));
        avatar.setLayout(new BorderLayout()); JLabel muiTen=Theme.nhan("",false); muiTen.setIcon(Theme.icon("up"));
        muiTen.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { avatar.doClick(); } });
        vaiTaiKhoan.setFont(Theme.NHO); vaiTaiKhoan.setForeground(Theme.PHU);
        JPanel chu=new JPanel(new GridLayout(2,1)); chu.setOpaque(false); chu.add(tenTaiKhoan); chu.add(vaiTaiKhoan);
        JPanel the=new JPanel(new BorderLayout(Theme.S8,0)); the.setOpaque(false); the.add(anhTaiKhoan,BorderLayout.WEST); the.add(chu);
        avatar.setText(""); avatar.add(the); avatar.add(muiTen,BorderLayout.EAST);
        for (var c:new java.awt.Component[]{the,chu,anhTaiKhoan,tenTaiKhoan,vaiTaiKhoan}) {
            c.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { avatar.doClick(); } });
        }
        boc.add(avatar); return boc;
    }
    // Danh tinh duoc nhan truoc kenh phong, va ve lai moi noi phu thuoc vao minh.
    private void ganNguoiDung(NguoiDung n) {
        toi=n;
        if (n!=null) {
            anhTaiKhoan.setIcon(Theme.avatar(n.tenHienThi,client.hoSo().anh(n.id),32)); tenTaiKhoan.setText(n.tenHienThi);
            vaiTaiKhoan.setText(n.vaiTro==VaiTro.ADMIN?"Quản trị viên":"Thành viên");
            setTitle("UDP Chat — "+n.tenHienThi);
        }
        phongPanel.capNhat(danhSach); capNhatPhongDangXem();
    }

    // ===== TAI KHOAN VA FORM =====
    private void taiKhoan(DangNhapPanel.ThongTin t) {
        dangNhap.ban(true);
        if (t.dangKy()) {
            chay(() -> client.dangKy(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),v -> { dangNhap.ban(false); dangNhap.dangKyXong(); },e -> dangNhap.loiServer(e));
        } else {
            chay(() -> client.dangNhap(t.ip(),Protocol.CONG_SERVER,t.ten(),t.matKhau()),n -> {
                ganNguoiDung(n); online=true; dangNhap.ban(false); dangNhap.xoaMatKhau(); cards.show(goc,"chat");
                toast.datVung(noiDungPhong); chonPhong(Protocol.SANH_ID); capNhat();
                if (n.buocDoiMatKhau) { moMatKhau(true); }
            },e -> dangNhap.loiServer(e));
        }
    }
    // Thu server.
    private void thuServer(String ip) {
        dangNhap.ban(true);
        chay(() -> client.kiemTraServer(ip,Protocol.CONG_SERVER),v -> dangNhap.ketQuaThu("Server đang hoạt động.",true),e -> dangNhap.ketQuaThu(e,false));
    }
    // Menu tai khoan chi hien cac chuc nang da hoat dong.
    JPopupMenu menuAvatar() {
        JPopupMenu m=new JPopupMenu();
        JMenuItem hoSo=new JMenuItem("Hồ sơ của tôi"),doi=new JMenuItem("Đổi mật khẩu");
        hoSo.addActionListener(e -> { if (toi!=null) { new HoSoDialog(client,toi,false,false).hien(this,n -> toast(ThongBao.Muc.THANH_CONG,"Đã lưu hồ sơ.")); } });
        doi.addActionListener(e -> moMatKhau(false)); m.add(hoSo); m.add(doi); m.addSeparator();
        JMenuItem thoat=new JMenuItem("Đăng xuất");
        thoat.addActionListener(e -> chay(client::dangXuat,v -> veDangNhap("Đã đăng xuất."),this::loi)); m.add(thoat); return m;
    }
    // Chi mo Sanh sau khi Server xac nhan mat khau mac dinh da duoc doi.
    private void moMatKhau(boolean batBuoc) {
        if (toi==null) { return; }
        new HoSoDialog(client,toi,true,batBuoc).hien(this,n -> {
            toast(ThongBao.Muc.THANH_CONG,"Đã đổi mật khẩu.");
            if (batBuoc) { chay(client::vaoSanh,p -> chonPhong(p.id),this::loi); }
        });
    }
    // Ve dang nhap.
    private void veDangNhap(String lyDo) {
        toi=null; noiDungPhong.removeAll(); phongDangXem=""; khung.clear(); thanhVien.clear(); chuaDoc.clear(); danhSach=List.of(); phongPanel.xoa();
        cards.show(goc,"login"); toast.datVung(null); dangNhap.ban(false); setTitle("UDP Chat"); ketNoi.setVisible(false); capNhat(); toast(ThongBao.Muc.THONG_TIN,lyDo);
    }
    // Vao phong.
    private void vaoPhong(String khoa,String mk) {
        if (khung.containsKey(khoa)) { chonPhong(khoa); return; }
        if (!online) { return; }
        chay(() -> client.vaoPhong(khoa,mk),p -> chonPhong(p.id),e -> {
            if (e.contains("Mật khẩu")) { HopThoai f=new HopThoai(null,true);
                danhSach.stream().filter(p -> p.id.equals(khoa) || p.maPhong.equalsIgnoreCase(khoa)).findFirst().ifPresent(p -> f.datMa(p.maPhong));
                f.coMatKhau(); f.hien(this,h -> guiVaoForm(h)); }
            else { loi(e); }
        });
    }
    // Gui vao form.
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
            case "thongtin": HopThoai.thongTin(p,thanhVien.getOrDefault(p.id,List.of())).hien(this,h -> h.dong()); break;
            default: break;
        }
    }
    // Chuyen chu.
    private void chuyenChu(Phong p) {
        var ds=thanhVien.getOrDefault(p.id,List.of()).stream().filter(n -> !n.id().equals(p.chuId)).toList();
        if (ds.isEmpty()) { loi("Cần có thành viên khác trong phòng để chuyển quyền."); return; }
        Object chon=JOptionPane.showInputDialog(this,"Chọn chủ phòng mới:","Chuyển quyền",JOptionPane.PLAIN_MESSAGE,null,
            ds.stream().map(n -> n.ten()+" (@"+n.tenDangNhap()+")").toArray(),ds.get(0).ten()+" (@"+ds.get(0).tenDangNhap()+")");
        if (chon!=null) {
            String id=ds.stream().filter(n -> (n.ten()+" (@"+n.tenDangNhap()+")").equals(chon)).findFirst().orElseThrow().id();
            chay(() -> client.chuyenChu(p.id,id),v -> toast(ThongBao.Muc.THANH_CONG,"Đã chuyển quyền chủ phòng."),this::loi);
        }
    }
    // Gui tin.
    private void guiTin(String text) {
        Phong p=phongChon(); if (p==null || dangGui) { return; }
        dangGui=true; capNhat();
        chay(() -> client.guiTin(p.id,text),v -> { dangGui=false; soan.xoaNeuGiong(text); capNhat(); },e -> { dangGui=false; capNhat(); if (e.equals(Protocol.LOI_TIN_DAI)) { soan.baoLoi(e); } else { loi(e); } });
    }
    private <T> void chay(Supplier<CompletableFuture<T>> viec,Consumer<T> xong,Consumer<String> loi) {
        nen.execute(() -> {
            CompletableFuture<T> f;
            try { f=viec.get(); } catch (RuntimeException e) { f=CompletableFuture.failedFuture(e); }
            f.whenComplete((v,e) -> trenEdt(() -> { if (!dong) { if (e==null) { xong.accept(v); } else { loi.accept(ClientCore.loiDeHieu(e)); } } }));
        });
    }
    // Loi.
    private void loi(String text) { toast(ThongBao.Muc.LOI,text); }
    // Toast.
    void toast(ThongBao.Muc muc,String text) { toast.hien(muc,text); }

    // ===== PHONG DANG XEM VA TRANG THAI =====
    // Cac khung van o nen trong CardLayout, giu nguyen vi tri cuon.
    private Phong phongChon() { KhungChatPanel k=khung.get(phongDangXem); return k==null?null:k.phong(); }
    // Chi sidebar chuyen phong; badge phong duoc mo ve 0.
    private void chonPhong(String id) {
        if (!khung.containsKey(id)) { return; }
        phongDangXem=id; cacPhong.show(noiDungPhong,id); chuaDoc.put(id,0); capNhatPhongDangXem();
    }
    // Ve lai header, quyen va nhan Ban khi nhan thanh vien hoac danh tinh moi.
    private void capNhatPhongDangXem() {
        Phong p=phongChon();
        if (p!=null) {
            chuaDoc.put(p.id,0); phongPanel.chon(p.id); tenPhong.setText(p.ten);
            var ds=thanhVien.getOrDefault(p.id,List.of()); long on=ds.stream().filter(Phong.ThanhVien::online).count();
            chiTiet.setText("Mã: "+p.maPhong+(p.dangMo?"":" · Đang đóng")); thanhVienNut.setText("Thành viên ("+ds.size()+")");
            chiTiet.setToolTipText(p.nhom+":"+p.cong+" · "+on+" đang online");
            thanhVienPanel.capNhat(ds,toi==null?"":toi.tenDangNhap,phongPanel.quanLy(p));
        } else {
            tenPhong.setText("Chọn phòng để trò chuyện"); chiTiet.setText(" "); thanhVienNut.setText("Thành viên (0)");
            thanhVienPanel.capNhat(List.of(),"",false);
        }
        capNhat(); phongPanel.repaint();
    }
    // Cap nhat.
    private void capNhat() {
        Phong p=phongChon(); soan.choPhep(toi!=null && online && p!=null && p.dangMo,dangGui);
        phongPanel.choPhep(toi!=null && online); copy.setEnabled(p!=null); menuPhong.setEnabled(p!=null && online);
    }
    // Dong cua so.
    private void dongCuaSo() {
        if (dong) { return; } dong=true;
        nen.execute(() -> client.dangXuat().whenComplete((v,e) -> { client.close(); nen.shutdown(); trenEdt(this::dispose); }));
    }
    // Don tai nguyen UI khi cua so ket thuc.
    @Override public void dispose() { toast.close(); if (anDai!=null) { anDai.stop(); } nen.shutdown(); super.dispose(); }
    // Tren edt.
    private static void trenEdt(Runnable r) { if (SwingUtilities.isEventDispatchThread()) { r.run(); } else { SwingUtilities.invokeLater(r); } }

    // ===== SU KIEN CLIENT: CHI CAP NHAT SWING TREN EDT =====
    // Profile luon vao EDT truoc cac su kien phong do ClientCore phat tiep sau do.
    @Override public void onNguoiDung(NguoiDung n) { trenEdt(() -> ganNguoiDung(n)); }
    // Ho so chung phong thay doi; laToi luon dua vao ten dang nhap/id, khong theo ten moi.
    @Override public void onHoSo(NguoiDung n) { trenEdt(() -> {
        if (toi!=null && toi.id.equals(n.id)) { ganNguoiDung(client.nguoiDung()==null?n:client.nguoiDung()); }
        thanhVien.replaceAll((id,ds) -> ds.stream().map(v -> v.id().equals(n.id)?
            new Phong.ThanhVien(v.id(),v.tenDangNhap(),n.tenHienThi,v.vaiTro(),v.online()):v).toList());
        khung.values().forEach(k -> k.hoSoMoi(n)); capNhatPhongDangXem();
    }); }
    // Tao khung o nen, khong tao tab hien thi.
    @Override public void onVaoPhong(Phong p) { trenEdt(() -> {
        if (!khung.containsKey(p.id)) { KhungChatPanel k=new KhungChatPanel(p); k.datHoSo(client.hoSo()::anh,client.hoSo()::lay); khung.put(p.id,k); noiDungPhong.add(k,p.id); }
        if (phongDangXem.isEmpty()) { chonPhong(p.id); }
        phongPanel.capNhat(danhSach);
    }); }
    // Roi phong dong rieng khung do va chuyen ve Sanh neu con.
    @Override public void onRoiPhong(String id,String lyDo) { trenEdt(() -> {
        KhungChatPanel k=khung.remove(id); if (k!=null) { noiDungPhong.remove(k); }
        thanhVien.remove(id); chuaDoc.remove(id);
        if (phongDangXem.equals(id)) { phongDangXem=""; chonPhong(Protocol.SANH_ID); }
        capNhatPhongDangXem(); if (toi!=null) { toast(ThongBao.Muc.THONG_TIN,lyDo); }
    }); }
    // Snapshot co membership rieng tung user de chia hai nhom sidebar.
    @Override public void onDanhSachPhong(List<Phong> ds) { trenEdt(() -> {
        danhSach=List.copyOf(ds); ds.forEach(p -> { if (khung.containsKey(p.id)) { khung.get(p.id).capNhat(p); } });
        phongPanel.capNhat(ds); capNhatPhongDangXem();
    }); }
    // Ham Listener cu chuyen ve cung mot duong ve tin.
    @Override public void onTinNhan(TinNhan t,boolean laToi) { onTinNhan(t,laToi,false); }
    // laToi do Core so ID bat bien tu tai khoan xac thuc, khong so ten hien thi.
    @Override public void onTinNhan(TinNhan t,boolean laToi,boolean lichSu) { trenEdt(() -> {
        KhungChatPanel k=khung.get(t.roomId); if (k==null) { return; } if (client.nguoiDung()!=null) { client.hoSo().gap(t.userId); } k.themTin(t,laToi);
        if (!lichSu && !laToi && !phongDangXem.equals(t.roomId)) { chuaDoc.merge(t.roomId,1,Integer::sum); }
        phongPanel.tinCuoi(t);
    }); }
    // Moi snapshot deu ve lai nhan Ban theo ten dang nhap hien tai.
    @Override public void onThanhVienChiTiet(String id,List<Phong.ThanhVien> ds) { trenEdt(() -> {
        thanhVien.put(id,List.copyOf(ds)); if (phongDangXem.equals(id)) { capNhatPhongDangXem(); }
    }); }
    // Nhan thong bao he thong qua Listener.
    @Override public void onHeThong(String id,String text) { trenEdt(() -> { if (khung.containsKey(id)) { khung.get(id).heThong(text); } }); }
    // Nhan thong bao he thong qua Listener.
    @Override public void onHeThong(String text) { trenEdt(() -> toast(ThongBao.Muc.THONG_TIN,text)); }
    // Nhan danh sach online theo Listener.
    @Override public void onDanhSachOnline(List<String> ds) { }
    // Nhan loi de hien thi hoac ghi nhan.
    @Override public void onLoi(String text) { trenEdt(() -> loi(text)); }
    // Nhan trang thai phien da ngat.
    @Override public void onMatKetNoi(String text) { trenEdt(() -> veDangNhap(text)); }
    // On ket noi.
    @Override public void onKetNoi(boolean sanSang,String text) { trenEdt(() -> {
        online=sanSang; ketNoi.setText(text); ketNoi.setBackground(sanSang?Theme.NEN_XANH:Theme.NEN_VANG); ketNoi.setForeground(sanSang?Theme.THANH_CONG:Theme.CANH_BAO); ketNoi.setVisible(true);
        if (anDai!=null) { anDai.stop(); }
        if (sanSang) { anDai=new Timer(3800,e -> ketNoi.setVisible(false)); anDai.setRepeats(false); anDai.start(); } capNhat();
    }); }
    // UiPreview dung dung giao dien that, khong tao ket noi mang.
    void duLieuMau(NguoiDung n,List<Phong> ds) {
        ganNguoiDung(n); online=true; cards.show(goc,"chat"); toast.datVung(noiDungPhong); onDanhSachPhong(ds); ds.stream().filter(p -> p.daThamGia).forEach(this::onVaoPhong); chonPhong(ds.get(0).id); capNhat();
    }
}
