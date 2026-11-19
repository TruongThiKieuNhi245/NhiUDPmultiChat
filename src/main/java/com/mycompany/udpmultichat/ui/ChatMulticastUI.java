package com.mycompany.udpmultichat.ui;
import com.mycompany.udpmultichat.client.CauHinhClient;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
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
    private final TepGiaoDien tepUI=new TepGiaoDien(this,client,this::phongChon,toast);
    private final JLabel tenPhong=Theme.nhan("Chọn phòng để trò chuyện",true),chiTiet=Theme.nhan(" ",false),ketNoi=Theme.nhan(" ",false);
    private final JButton avatar=Theme.nutNhe("Tài khoản",""),copy=Theme.nutNhe("Sao chép mã","copy"),thanhVienNut=Theme.nutNhe("Thành viên (0)",""),menuPhong=Theme.nutNhe("","more"),timNut=Theme.nutNhe("","search"),nenNut=Theme.nutNhe("","palette");
    private final JLabel tenTaiKhoan=Theme.nhan("",true),vaiTaiKhoan=Theme.nhan("",false),anhTaiKhoan=Theme.nhan("",false);
    private QuanTriFrame admin;
    private final java.util.Map<String,Long> mute=new java.util.HashMap<>();
    private final JLabel camChat=Theme.nhan("",false);
    private NguoiDung toi;
    private boolean dong,dangGui,online=true;
    private List<Phong> danhSach=List.of();
    private Timer anDai;
    private final JLabel dangGo=Theme.nhan("",false);
    private final Map<String,Map<String,NguoiGo>> nguoiGo=new LinkedHashMap<>();
    private final Timer dongHoGo=new Timer(500,e -> capNhatDangGo());
    // Khoi tao lop va noi cac thanh phan can thiet.
    public ChatMulticastUI() {
        soan.datTep(tepUI::menu); tepUI.datKeoTha(noiDungPhong);
        thanhVienPanel.datKiemDuyet((l,n) -> { Phong p=phongChon(); if (p!=null) { KiemDuyetUI.thaoTac(this,client,p.id,n.id(),n.ten(),l,this::loi); } });
        thanhVienPanel.datAnh(client.hoSo()::anh);
        thanhVienPanel.datHanhDongMoi(() -> {
            Phong p=phongChon();
            if (p!=null) { new MoiThanhVienDialog(this,client,p.id,p.ten,thanhVien.getOrDefault(p.id,List.of())).setVisible(true); }
        });
        thanhVienPanel.datHanhDongBietDanh((nick,n) -> {
            Phong p=phongChon();
            if (p!=null) {
                client.datBietDanh(p.id,n.id(),nick).thenAccept(moi -> trenEdt(() -> {
                    toast(ThongBao.Muc.THANH_CONG,moi.isEmpty()?"Đã xóa biệt danh của @"+n.tenDangNhap():"Đã đặt biệt danh thành «"+moi+"»");
                })).exceptionally(ex -> {
                    trenEdt(() -> loi(ClientCore.loiDeHieu(ex)));
                    return null;
                });
            }
        });
        phongPanel=new DanhSachPhongPanel(() -> toi,khung::containsKey,id -> chuaDoc.getOrDefault(id,0),p -> vaoPhong(p.id,""),this::hanhDong);
        phongPanel.datKiemTraTatThongBao(client::daTatThongBao);
        setIconImage(Theme.iconUngDung()); setTitle("UDP Chat"); setSize(1240,800); setMinimumSize(new Dimension(960,600)); setLocationByPlatform(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel trai=new JPanel(new BorderLayout()); trai.setPreferredSize(new Dimension(280,0)); trai.setBackground(Theme.NEN);
        trai.add(phongPanel,BorderLayout.CENTER); trai.add(theTaiKhoan(),BorderLayout.SOUTH);
        avatar.addActionListener(e -> { JPopupMenu m=menuAvatar(); m.show(avatar,0,-m.getPreferredSize().height); });
        chat.add(trai,BorderLayout.WEST); chat.add(giua,BorderLayout.CENTER); chat.add(thanhVienPanel,BorderLayout.EAST);
        giua.add(tieuDePhong(),BorderLayout.NORTH); giua.add(noiDungPhong,BorderLayout.CENTER); giua.add(khungSoan(),BorderLayout.SOUTH);
        soan.datChucNang(this::guiSticker,text -> client.troChuyen().dangGo(phongDangXem,text)); dongHoGo.start();
        JPanel chatVaCanhBao=new JPanel(new BorderLayout()); ketNoi.setOpaque(true); ketNoi.setBorder(Theme.le(Theme.S8)); ketNoi.setVisible(false);
        chatVaCanhBao.add(ketNoi,BorderLayout.NORTH); chatVaCanhBao.add(chat,BorderLayout.CENTER);
        javax.swing.JScrollPane loginCuon=new javax.swing.JScrollPane(dangNhap); loginCuon.setBorder(null); loginCuon.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        goc.add(loginCuon,"login"); goc.add(chatVaCanhBao,"chat"); setContentPane(goc); cards.show(goc,"login");
        toast.datVung(noiDungPhong);
        copy.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { DanhSachPhongPanel.saoChep(p.maPhong); toast(ThongBao.Muc.THANH_CONG,"Đã sao chép mã phòng."); } });
        thanhVienNut.addActionListener(e -> { thanhVienPanel.setVisible(!thanhVienPanel.isVisible()); chat.revalidate(); });
        timNut.setToolTipText("Tìm kiếm tin nhắn (Ctrl+F)");
        timNut.addActionListener(e -> { KhungChatPanel k=khung.get(phongDangXem); if (k!=null) k.batTimKiem(); });
        nenNut.setToolTipText("Đổi màu nền phòng chat (Pastel)");
        nenNut.addActionListener(e -> moMenuDoiMauNen(nenNut));
        copy.setToolTipText("Sao chép mã phòng");
        menuPhong.setIcon(Theme.icon("more"));
        menuPhong.addActionListener(e -> { Phong p=phongChon(); if (p!=null) { phongPanel.menu(p).show(menuPhong,0,menuPhong.getHeight()); } });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dongCuaSo(); } }); capNhat();
    }
    // Dong typing nam ngoai khung nhap, khong day noi dung trong textarea.
    private JPanel khungSoan() {
        JPanel p=new JPanel(new BorderLayout()); dangGo.setName("dang-go"); dangGo.setFont(Theme.NHO); dangGo.setForeground(Theme.PHU);
        dangGo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,Theme.S16,0,Theme.S16)); dangGo.setVisible(false);
        p.add(dangGo,BorderLayout.NORTH); p.add(soan); camChat.setFont(Theme.NHO); camChat.setForeground(Theme.CANH_BAO); camChat.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,Theme.S16,Theme.S8,Theme.S16)); camChat.setVisible(false); p.add(camChat,BorderLayout.SOUTH); return p;
    }
    private void capNhatDangGo() { capNhatDangGo(System.nanoTime()); }
    // Dong ho truyen vao giup UiPreview kiem tra het han ma khong ngu EDT.
    void capNhatDangGo(long luc) {
        nguoiGo.values().forEach(ds -> ds.values().removeIf(n -> luc-n.luc()>Protocol.TYPING_HIEN_MS*1000000L));
        var ds=nguoiGo.getOrDefault(phongDangXem,Map.of()).values().stream().toList();
        dangGo.setText(ds.isEmpty()?"":ds.get(0).ten()+(ds.size()==1?" đang nhập...":" và "+(ds.size()-1)+" người khác đang nhập..."));
        dangGo.setVisible(!ds.isEmpty()); giua.revalidate();
    }
    // Gui sticker bang ma, khong gui byte anh.
    private void guiSticker(String ma) {
        Phong p=phongChon(); if (p!=null && online) { chay(() -> client.troChuyen().gui(p.id,ma,"S"),v -> { },this::loi); }
    }
    // Chuot phai tin de tra loi, thu hoi, gui lai dung ma hoac xoa hang loi cuc bo.
    private void hanhDongTin(String lenh,TinNhan t) {
        if (t.loai.equals("F") && !lenh.equals("Thu hồi") && !lenh.equals("Trả lời")) { tepUI.hanhDong(lenh,t); return; }
        switch (lenh) {
            case "Trả lời": soan.datTrichDan(t); break;
            case "Thu hồi": chay(() -> client.troChuyen().thuHoi(t.roomId,t.msgId),v -> { },this::loi); break;
            case "Gửi lại": chay(() -> client.troChuyen().guiLai(t.roomId,t.maClient),v -> { },this::loi); break;
            case "Xóa": client.troChuyen().xoaTinLoi(t.roomId,t.maClient); break;
            default: break;
        }
    }
    // Tieu de khong lap thanh tab; ma va nut co du cho o cua so toi thieu.
    private JPanel tieuDePhong() {
        JPanel dau=new JPanel(new BorderLayout(Theme.S8,0)); dau.setBorder(Theme.le(Theme.S16));
        tenPhong.setFont(Theme.DAM.deriveFont(18f)); chiTiet.setFont(Theme.NHO); chiTiet.setForeground(Theme.PHU);
        JPanel ten=new JPanel(new GridLayout(2,1,0,Theme.S4)); ten.add(tenPhong); ten.add(chiTiet); dau.add(ten,BorderLayout.CENTER);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S4,0)); nut.add(timNut); nut.add(nenNut); nut.add(copy); nut.add(thanhVienNut); nut.add(menuPhong);
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
        doi.addActionListener(e -> moMatKhau(false)); m.add(hoSo); m.add(doi);
        JMenuItem loiMoi=new JMenuItem("Lời mời tham gia phòng...");
        loiMoi.addActionListener(e -> new LoiMoiPhongDialog(this,client,this::vaoPhongDaMoi).setVisible(true));
        m.add(loiMoi);
        JMenuItem gioiThieu=new JMenuItem("Giới thiệu"); gioiThieu.addActionListener(e -> javax.swing.JOptionPane.showMessageDialog(this,
            "UDP Chat\nSticker: Noto Emoji 2D — Google và cộng tác viên\nGiấy phép ảnh: Apache License 2.0\nhttps://github.com/googlefonts/noto-emoji\nGiấy phép kèm trong resources/stickers/LICENSE.","Giới thiệu",javax.swing.JOptionPane.INFORMATION_MESSAGE));
        m.add(gioiThieu); m.addSeparator();
        if (toi!=null && toi.vaiTro==VaiTro.ADMIN) {
            JMenuItem q=new JMenuItem("Quản trị"); q.addActionListener(e -> { if (admin==null || !admin.isDisplayable()) { admin=new QuanTriFrame(client,false); } admin.setVisible(true); admin.toFront(); admin.nap(); }); m.add(q);
        }
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
        if (admin!=null) { admin.dispose(); admin=null; }
        toi=null; noiDungPhong.removeAll(); phongDangXem=""; khung.clear(); mute.clear(); tepUI.xoa(); nguoiGo.clear(); thanhVien.clear(); chuaDoc.clear(); danhSach=List.of(); phongPanel.xoa();
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
            case "tim": { KhungChatPanel k=khung.get(p.id); if (k!=null) k.batTimKiem(); } break;
            case "xuat_lichsu": {
                KhungChatPanel k=khung.get(p.id);
                if (k!=null) {
                    nen.execute(() -> {
                        try {
                            var fileTxt=XuatLichSuUtil.xuat(this,p,k.danhSachTin());
                            if (fileTxt!=null) { toast.hien(ThongBao.Muc.THANH_CONG,"Đã xuất lịch sử chat.","Mở",() -> XuatLichSuUtil.mo(fileTxt)); }
                        } catch (Exception ex) { toast.hien(ThongBao.Muc.LOI,"Lỗi xuất lịch sử."); }
                    });
                }
            } break;
            case "bat_tat_thong_bao": {
                boolean hienTai = client.daTatThongBao(p.id);
                client.batTatThongBao(p.id, !hienTai);
                phongPanel.capNhat(danhSach);
                toast(ThongBao.Muc.THONG_TIN, !hienTai ? "Đã tắt thông báo phòng «" + p.ten + "»." : "Đã bật lại thông báo phòng «" + p.ten + "».");
            } break;
            case "doimaunen": moMenuDoiMauNen(menuPhong); break;
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
    private void loi(String text) { toast.hien(ThongBao.Muc.LOI,text); }
    void toast(ThongBao.Muc muc,String text) { toast.hien(muc,text); }
    // ===== PHONG DANG XEM VA TRANG THAI =====
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
            capNhatDangGo(); thanhVienPanel.capNhat(ds,toi==null?"":toi.tenDangNhap,phongPanel.quanLy(p));
        } else {
            tenPhong.setText("Chọn phòng để trò chuyện"); chiTiet.setText(" "); thanhVienNut.setText("Thành viên (0)");
            thanhVienPanel.capNhat(List.of(),"",false);
        }
        capNhat(); phongPanel.repaint();
    }
    // Cap nhat.
    private void capNhat() {
        Phong p=phongChon(); long den=p==null?0:mute.getOrDefault(p.id,0L); if (den<=System.currentTimeMillis()) { den=0; }
        camChat.setText(den==Long.MAX_VALUE?"Bạn bị cấm chat vĩnh viễn.":den>0?"Bạn bị cấm chat đến "+java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM").format(java.time.Instant.ofEpochMilli(den).atZone(java.time.ZoneId.systemDefault())):""); camChat.setVisible(den>0);
        soan.choPhep(toi!=null && online && p!=null && p.dangMo && den==0,dangGui);
        phongPanel.choPhep(toi!=null && online); copy.setEnabled(p!=null); menuPhong.setEnabled(p!=null && online);
        timNut.setEnabled(p!=null); nenNut.setEnabled(p!=null);
    }
    // Dong cua so.
    private void dongCuaSo() {
        if (dong) { return; } dong=true;
        nen.execute(() -> client.dangXuat().whenComplete((v,e) -> { client.close(); nen.shutdown(); trenEdt(this::dispose); }));
    }
    // Don tai nguyen UI khi cua so ket thuc.
    @Override public void dispose() { if (admin!=null) { admin.dispose(); } dongHoGo.stop(); toast.close(); if (anDai!=null) { anDai.stop(); } nen.shutdown(); super.dispose(); }
    // Tren edt.
    private static void trenEdt(Runnable r) { if (SwingUtilities.isEventDispatchThread()) { r.run(); } else { SwingUtilities.invokeLater(r); } }
    // ===== SU KIEN CLIENT: CHI CAP NHAT SWING TREN EDT =====
    // Profile luon vao EDT truoc cac su kien phong do ClientCore phat tiep sau do.
    @Override public void onNguoiDung(NguoiDung n) { trenEdt(() -> ganNguoiDung(n)); }
    // Core phat trang thai theo ma Client; UI chi ve vao dung khung phong.
    @Override public void onTrangThaiGui(TinNhan t,String ma,String loi) { trenEdt(() -> {
        var k=khung.get(t.roomId); if (k!=null) { k.trangThai(t,ma,loi); }
        if (ma.equals("WAIT") && t.loai.equals("T") && t.roomId.equals(phongDangXem)) { soan.xoaNeuGiong(t.noiDung); }
    }); }
    // Thu hoi xoa ca preview sidebar, khong de lo noi dung cu.
    @Override public void onThuHoi(String room,long id) { trenEdt(() -> { var k=khung.get(room); if (k!=null) { k.thuHoi(id); } phongPanel.thuHoi(room,id); }); }
    // Moi su kien typing gia han bon giay tren dong ho don dieu.
    @Override public void onDangGo(String room,String user,String ten) { trenEdt(() -> {
        if (!khung.containsKey(room)) { return; } nguoiGo.computeIfAbsent(room,k -> new LinkedHashMap<>()).put(user,new NguoiGo(ten,System.nanoTime())); capNhatDangGo();
    }); }
    /** Ho so nho chi phuc vu hien thi typing tren EDT. */
    private record NguoiGo(String ten,long luc) { }
    // Ho so chung phong thay doi; laToi luon dua vao ten dang nhap/id, khong theo ten moi.
    @Override public void onHoSo(NguoiDung n) { trenEdt(() -> {
        if (toi!=null && toi.id.equals(n.id)) { ganNguoiDung(client.nguoiDung()==null?n:client.nguoiDung()); }
        thanhVien.replaceAll((id,ds) -> ds.stream().map(v -> v.id().equals(n.id)?
            new Phong.ThanhVien(v.id(),v.tenDangNhap(),n.tenHienThi,v.vaiTro(),v.online()):v).toList());
        khung.values().forEach(k -> k.hoSoMoi(n)); capNhatPhongDangXem();
    }); }
    // Tao khung o nen, khong tao tab hien thi.
    @Override public void onVaoPhong(Phong p) { trenEdt(() -> {
        if (!khung.containsKey(p.id)) {
            KhungChatPanel k=new KhungChatPanel(p);
            String mauLuu=CauHinhClient.layMauNen(p.id);
            if (!mauLuu.isEmpty()) {
                try { k.datMauNen(Color.decode(mauLuu)); } catch (Exception ignored) {}
            }
            k.datHoSo(client.hoSo()::anh,client.hoSo()::lay);
            k.datAnhTep(client.tep()::anh);
            k.datHanhDong(() -> phongPanel.quanLy(k.phong()),this::hanhDongTin);
            k.datBietDanh((rId,uId) -> thanhVien.getOrDefault(rId,List.of()).stream()
                .filter(tv -> tv.id().equals(uId)).map(Phong.ThanhVien::bietDanh).findFirst().orElse(""));
            khung.put(p.id,k); noiDungPhong.add(k,p.id);
        }
        if (phongDangXem.isEmpty()) { chonPhong(p.id); }
        phongPanel.capNhat(danhSach);
    }); }

    // Mo menu popup de nguoi dung lua chon mau nen pastel cho phong chat.
    private void moMenuDoiMauNen(java.awt.Component anchor) {
        if (phongDangXem.isEmpty()) return;
        JPopupMenu menu=new JPopupMenu();
        JLabel header=new JLabel("  MÀU NỀN PHÒNG CHAT (PASTEL)");
        header.setFont(Theme.NHO); header.setForeground(Theme.PHU);
        menu.add(header);
        menu.addSeparator();

        KhungChatPanel k=khung.get(phongDangXem);
        Color mauHienTai=k!=null?k.layMauNen():Theme.THE;

        for (Theme.MauPastel mp:Theme.DANH_SACH_PASTEL) {
            boolean dangChon=mauHienTai.equals(mp.mau());
            JMenuItem item=new JMenuItem(mp.ten()+(dangChon?"  ✓":""));
            item.setIcon(new javax.swing.Icon() {
                public int getIconWidth() { return 18; }
                public int getIconHeight() { return 18; }
                public void paintIcon(Component c,Graphics g,int x,int y) {
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(mp.mau());
                    g2.fillRoundRect(x+2,y+2,14,14,6,6);
                    g2.setColor(Theme.VIEN);
                    g2.drawRoundRect(x+2,y+2,14,14,6,6);
                    g2.dispose();
                }
            });
            item.addActionListener(e -> {
                if (k!=null) {
                    k.datMauNen(mp.mau());
                    String hex=String.format("#%02x%02x%02x",mp.mau().getRed(),mp.mau().getGreen(),mp.mau().getBlue());
                    CauHinhClient.luuMauNen(phongDangXem,hex);
                    toast(ThongBao.Muc.THANH_CONG,"Đã đổi nền phòng sang "+mp.ten());
                }
            });
            menu.add(item);
        }
        menu.show(anchor,0,anchor.getHeight());
    }
    private void vaoPhongDaMoi(String roomId) { vaoPhong(roomId,""); }
    // Roi phong dong rieng khung do va chuyen ve Sanh neu con.
    @Override public void onRoiPhong(String id,String lyDo) { trenEdt(() -> {
        KhungChatPanel k=khung.remove(id); if (k!=null) { noiDungPhong.remove(k); }
        thanhVien.remove(id); nguoiGo.remove(id); chuaDoc.remove(id);
        if (phongDangXem.equals(id)) { phongDangXem=""; chonPhong(Protocol.SANH_ID); }
        capNhatPhongDangXem(); if (toi!=null) { toast(ThongBao.Muc.THONG_TIN,lyDo); }
    }); }
    // Snapshot co membership rieng tung user de chia hai nhom sidebar.
    @Override public void onDanhSachPhong(List<Phong> ds) { trenEdt(() -> {
        danhSach=List.copyOf(ds); ds.forEach(p -> { if (khung.containsKey(p.id)) { khung.get(p.id).capNhat(p); } });
        phongPanel.capNhat(ds); capNhatPhongDangXem();
    }); }
    // Mute duoc kiem tra tren Server, giao dien khoa o soan theo snapshot moi.
    @Override public void onCamChat(String room,long den) { trenEdt(() -> { mute.put(room,den); capNhat(); }); }
    // Broadcast noi bat cho tat ca Client, dong thoi co tin Y trong lich su phong.
    @Override public void onThongBaoQuanTri(String text) { trenEdt(() -> toast(ThongBao.Muc.THONG_TIN,"Thông báo từ quản trị viên: "+text)); }
    // Tien do tep duoc chuyen ve EDT va ghep dung hang theo ma Client/fileId.
    @Override public void onTep(com.mycompany.udpmultichat.common.TepTienDo td) { trenEdt(() -> {
        tepUI.tienDo(td); KhungChatPanel k=khung.get(td.tep().room()); if (k!=null) { k.tienDoTep(td,toi==null?"":toi.tenHienThi); }
    }); }
    // Anh xem truoc da tai nen, chi ve lai dong lien quan.
    @Override public void onAnhTep(String room,String id) { trenEdt(() -> { KhungChatPanel k=khung.get(room); if (k!=null) { k.anhTepMoi(id); } }); }
    // Ham Listener cu chuyen ve cung mot duong ve tin.
    @Override public void onTinNhan(TinNhan t,boolean laToi) { onTinNhan(t,laToi,false); }
    // laToi do Core so ID bat bien tu tai khoan xac thuc, khong so ten hien thi.
    @Override public void onTinNhan(TinNhan t,boolean laToi,boolean lichSu) { trenEdt(() -> {
        KhungChatPanel k=khung.get(t.roomId); if (k==null) { return; } if (client.nguoiDung()!=null) { client.hoSo().gap(t.userId); } TinNhan hien=k.locTin(t); k.themTin(hien,laToi);
        if (!lichSu && !laToi && !phongDangXem.equals(t.roomId)) {
            if (!client.daTatThongBao(t.roomId)) { chuaDoc.merge(t.roomId,1,Integer::sum); }
        }
        if (hien.loai.equals("F") && client.sanSang()) { client.tep().napAnh(com.mycompany.udpmultichat.common.TepTin.doc(t.roomId,t.userId,t.noiDung,t.thoiGian)); }
        phongPanel.tinCuoi(hien);
    }); }
    @Override public void onThanhVienChiTiet(String id,List<Phong.ThanhVien> ds) { trenEdt(() -> {
        thanhVien.put(id,List.copyOf(ds));
        KhungChatPanel k=khung.get(id); if (k!=null) { k.veLaiTatCa(); }
        if (phongDangXem.equals(id)) { capNhatPhongDangXem(); }
    }); }
    @Override public void onLoiMoi(String inviteId,String roomId,String tenPhong,String nguoiMoi) {
        trenEdt(() -> {
            toast.hien(ThongBao.Muc.THONG_TIN,
                "Lời mời: @"+nguoiMoi+" mời bạn vào phòng «"+tenPhong+"»",
                "Xem",
                () -> new LoiMoiPhongDialog(this,client,this::vaoPhongDaMoi).setVisible(true)
            );
        });
    }
    @Override public void onHeThong(String id,String text) { trenEdt(() -> { if (khung.containsKey(id)) { khung.get(id).heThong(text); } }); }
    @Override public void onHeThong(String text) { trenEdt(() -> toast.hien(ThongBao.Muc.THONG_TIN,text)); }
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
        ganNguoiDung(n); online=true; cards.show(goc,"chat"); toast.datVung(noiDungPhong); onDanhSachPhong(ds); ds.stream().filter(p -> p.daThamGia).forEach(this::onVaoPhong); chonPhong(ds.get(0).id); capNhat();
    }
}
