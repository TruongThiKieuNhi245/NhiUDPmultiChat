package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/** Vai tro: diem chay/giam sat Server. Ai goi: VS Code, Maven, NetBeans, ServerCore.Listener. Goi: ServerCore, MangUtil, Theme. */
@SuppressWarnings("serial")
public final class ServerMain extends JFrame implements ServerCore.Listener {
    private final JTextPane log=new JTextPane();
    private final JLabel diaChi=Theme.nhan("IP LAN: đang xác định… · UDP "+Protocol.CONG_SERVER,false);
    private final JLabel trangThai=Theme.nhan("Chưa chạy Server",false);
    private final JLabel[] soLieu={Theme.nhan("0",true),Theme.nhan("0",true),Theme.nhan("0",true),Theme.nhan("0",true),Theme.nhan("0",true),Theme.nhan("0",true)};
    private final JButton batDau=Theme.nut("Bắt đầu",true),dung=Theme.nut("Dừng",false),copy=Theme.nutNhe("Sao chép IP","");
    private final ExecutorService viecNen=Executors.newSingleThreadExecutor(v -> { Thread t=new Thread(v,"ui-server-task"); t.setDaemon(true); return t; });
    private final Path thuMucDuLieu=Path.of(System.getProperty("udpmultichat.dataDir","data"));
    private final ServerCore server=new ServerCore(thuMucDuLieu,this);
    private final com.mycompany.udpmultichat.ui.components.ThongBao toast=new com.mycompany.udpmultichat.ui.components.ThongBao(getLayeredPane());
    private Path baoCao;
    private final javax.swing.Timer capNhat=new javax.swing.Timer(4000,e -> viecNen.execute(() -> { int[] n=server.phienVaTep(); SwingUtilities.invokeLater(() -> { soLieu[4].setText(""+n[0]); soLieu[5].setText(""+n[1]); }); }));
    private boolean dangDong;
    private String ipLan="";

    // ===== DUNG CUA SO =====
    // Cua so chi nhan Listener; khong tao hay xu ly DatagramSocket.
    public ServerMain() {
        setTitle("UDP Chat — Máy chủ điều phối"); setIconImage(Theme.iconUngDung()); setSize(1100,700);
        setMinimumSize(new Dimension(720,460)); setLocationByPlatform(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel noiDung=new JPanel(new BorderLayout(0,Theme.S16)); noiDung.setBorder(Theme.le(Theme.S16)); setContentPane(noiDung);
        JPanel dau=new JPanel(new BorderLayout(0,Theme.S12)); dau.add(thanhCongCu(),BorderLayout.NORTH); dau.add(theThongKe(),BorderLayout.CENTER); noiDung.add(dau,BorderLayout.NORTH);
        log.setEditable(false); log.setFont(Theme.DON_CACH); log.setBackground(Theme.THE); log.setBorder(Theme.le(Theme.S12)); noiDung.add(new JScrollPane(log),BorderLayout.CENTER);
        batDau.addActionListener(e -> doiTrangThai(true)); dung.addActionListener(e -> doiTrangThai(false));
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ipLan),null));
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dongCuaSo(); } });
        capNhat.start(); viecNen.execute(this::napDiaChi); onLog("Bấm Bắt đầu. Dữ liệu: "+thuMucDuLieu.toAbsolutePath().normalize());
    }
    // Thanh dia chi, sao chep, bat/dung va xoa log dung cung Theme.
    private JPanel thanhCongCu() {
        JPanel thanh=new JPanel(new BorderLayout(Theme.S8,Theme.S8));
        JPanel ten=new JPanel(new BorderLayout()); JLabel t=Theme.nhan("Máy chủ UDP Chat",true); t.setFont(Theme.DAM.deriveFont(22f));
        ten.add(t,BorderLayout.NORTH); ten.add(diaChi,BorderLayout.SOUTH); thanh.add(ten,BorderLayout.CENTER);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S8,0));
        JButton xoa=Theme.nut("Xóa log",false); xoa.addActionListener(e -> log.setText(""));
        nut.add(copy); nut.add(batDau); nut.add(dung); nut.add(xoa); dung.setEnabled(false); copy.setEnabled(false); thanh.add(nut,BorderLayout.EAST);
        JPanel duoi=new JPanel(new BorderLayout()); duoi.add(trangThai,BorderLayout.WEST);
        JPanel bao=new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton xuat=Theme.nut("Xuất báo cáo",true),folder=Theme.nut("Mở thư mục",false),html=Theme.nut("Mở HTML",false); bao.add(xuat); bao.add(folder); bao.add(html); duoi.add(bao,BorderLayout.EAST); thanh.add(duoi,BorderLayout.SOUTH);
        xuat.addActionListener(e -> { xuat.setEnabled(false); viecNen.execute(() -> {
            try { server.xuatBaoCao().whenComplete((p,loi) -> SwingUtilities.invokeLater(() -> { xuat.setEnabled(true); if (loi!=null) { onLog("Lỗi xuất báo cáo: "+loi.getMessage()); } else { baoCao=p; trangThai.setText("Đã xuất báo cáo: "+p); onLog("Đã xuất báo cáo: "+p); toast.hien(com.mycompany.udpmultichat.ui.components.ThongBao.Muc.THANH_CONG,"Đã xuất báo cáo.","Mở thư mục",() -> moBaoCao(false)); } })); }
            catch (java.io.IOException loi) { SwingUtilities.invokeLater(() -> { xuat.setEnabled(true); onLog("Lỗi: "+loi.getMessage()); }); }
        }); });
        folder.addActionListener(e -> moBaoCao(false)); html.addActionListener(e -> moBaoCao(true)); return thanh;
    }
    // Mo HTML/thu muc tren worker, khong chan EDT.
    private void moBaoCao(boolean html) { if (baoCao!=null) { viecNen.execute(() -> { try { java.awt.Desktop.getDesktop().open((html?baoCao.resolve("tong_hop.html"):baoCao).toFile()); } catch (java.io.IOException e) { onLog("Lỗi mở báo cáo."); } }); } }
    // Sau the co cung kich thuoc va trinh bay so ro rang.
    private JPanel theThongKe() {
        JPanel hang=new JPanel(new GridLayout(2,3,Theme.S12,0));
        String[] ten={"Tài khoản","Đang online","Phòng","Tin nhắn","Phiên đang hoạt động","Tệp đang truyền"};
        for (int i=0;i<ten.length;i++) {
            JPanel the=new JPanel(new BorderLayout(0,Theme.S8)); the.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(Theme.VIEN),Theme.le(Theme.S16)));
            the.add(Theme.nhan(ten[i],false),BorderLayout.NORTH); soLieu[i].setFont(Theme.DAM.deriveFont(26f)); the.add(soLieu[i]); hang.add(the);
        }
        return hang;
    }
    // ===== BAT DUNG VA DIA CHI =====
    // Doc card tren luong nen, IP nay cung card ma MulticastSocket se su dung.
    private void napDiaChi() {
        try { String ip=MangUtil.diaChiLan(); SwingUtilities.invokeLater(() -> ganDiaChi(ip)); }
        catch (java.io.IOException e) { onLog("CẢNH BÁO: Không xác định được IP LAN: "+e.getMessage()); }
    }
    // Gan dia chi tren EDT; nut sao chep chi sao chep IP de nhap o Client.
    private void ganDiaChi(String ip) { ipLan=ip; diaChi.setText("IP LAN: "+ip+" · UDP "+Protocol.CONG_SERVER); copy.setEnabled(true); }
    // Bat/dung tren luong nen, khong khoa cua so.
    private void doiTrangThai(boolean bat) {
        batDau.setEnabled(false); dung.setEnabled(false);
        viecNen.execute(() -> {
            try { if (bat) { napDiaChi(); server.batDau(Protocol.CONG_SERVER); } else { server.close(); } }
            catch (Exception e) { onLog("Lỗi: Không khởi động được: "+e.getMessage()); }
            SwingUtilities.invokeLater(() -> {
                batDau.setEnabled(!server.dangChay()); dung.setEnabled(server.dangChay());
                trangThai.setText(server.dangChay()?"Đang chạy · UDP "+Protocol.CONG_SERVER:"Đã dừng");
            });
        });
    }
    // Doi Server dong tai nguyen xong moi dispose.
    private void dongCuaSo() {
        if (dangDong) { return; } dangDong=true;
        viecNen.execute(() -> { server.close(); viecNen.shutdown(); SwingUtilities.invokeLater(this::dispose); });
    }
    // ===== LOG VA THONG KE QUA LISTENER =====
    // Su kien Server khong Swing; mau tung dong duoc gan tren EDT.
    @Override public void onLog(String noiDung) {
        String gio=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        SwingUtilities.invokeLater(() -> themLog("["+gio+"] "+noiDung+"\n"));
    }
    // Gioi han bo nho log va dung font don cach, nen trang.
    private void themLog(String noiDung) {
        var vanBan=log.getStyledDocument(); String thuong=noiDung.toLowerCase(java.util.Locale.ROOT);
        var mau=Theme.CHU;
        if (thuong.contains("cảnh báo") || thuong.contains("mất kết nối")) { mau=Theme.CANH_BAO; }
        else if (thuong.contains("lỗi") || thuong.contains("không khởi động")) { mau=Theme.LOI; }
        SimpleAttributeSet style=new SimpleAttributeSet(); StyleConstants.setForeground(style,mau);
        try {
            vanBan.insertString(vanBan.getLength(),noiDung,style);
            if (vanBan.getLength()>120000) { vanBan.remove(0,30000); }
            log.setCaretPosition(vanBan.getLength());
        } catch (javax.swing.text.BadLocationException e) { trangThai.setText("Không cập nhật được log."); }
    }
    // Listener cu duoc giu cho cac bai SelfTest M1.
    @Override public void onThongKe(int tongUser,int online,long soTin) { onThongKe(tongUser,online,0,soTin); }
    // So phong do Core cung cap, khong do UI tu doc file.
    @Override public void onThongKe(int tongUser,int online,int soPhong,long soTin) {
        SwingUtilities.invokeLater(() -> { soLieu[0].setText(""+tongUser); soLieu[1].setText(""+online); soLieu[2].setText(""+soPhong); soLieu[3].setText(""+soTin); });
    }
    // Du lieu mau truc tiep tren EDT cho UiPreview, khong bat Server.
    public void duLieuMau() {
        ganDiaChi("192.168.1.25"); soLieu[0].setText("3"); soLieu[1].setText("2"); soLieu[2].setText("2"); soLieu[3].setText("128");
        log.setText(""); themLog("[12:30:00] Server UDP đang hoạt động.\n");
        themLog("[12:30:01] Kiều Nhi tạo phòng Bài tập mạng.\n"); themLog("[12:30:02] CẢNH BÁO: Client mất kết nối.\n"); themLog("[12:30:03] Lỗi: Không lưu được tệp.\n");
    }
    // Dong luong UI khi preview dispose ma khong mo Server.
    @Override public void dispose() { toast.close(); capNhat.stop(); viecNen.shutdown(); super.dispose(); }
    // Diem chay Server, cai IPv4 va LAF truoc khi tao giao dien.
    public static void main(String[] args) { MangUtil.dungIPv4(); Theme.caiDat(); SwingUtilities.invokeLater(() -> new ServerMain().setVisible(true)); }
}
