package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Vai tro: hai nhom phong. Ai goi: ChatMulticastUI. Goi: Theme, callback UI. */
@SuppressWarnings("serial")
final class DanhSachPhongPanel extends JPanel {
    private final JPanel ds=new JPanel() {
        // Sidebar luon bam be rong viewport, khong co thanh cuon ngang.
        @Override public Dimension getPreferredSize() { return new Dimension(1,super.getPreferredSize().height); }
    };
    private final JScrollPane cuon=new JScrollPane(ds);
    private final JTextField loc=new JTextField();
    private final JButton tao=Theme.nut("+ Tạo phòng",true),ma=Theme.nut("Nhập mã",false);
    private final Supplier<NguoiDung> toi;
    private final Predicate<String> daMo;
    private final Function<String,Integer> chuaDoc;
    private final Consumer<Phong> vao;
    private final BiConsumer<String,Phong> hanhDong;
    private final Map<String,TinNhan> tinCuoi=new LinkedHashMap<>();
    private final Map<String,JPanel> cacHang=new LinkedHashMap<>();
    private List<Phong> danhSach=List.of();
    private String dangChon="";
    private boolean choPhep;
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    // ===== DUNG SIDEBAR =====
    // Danh sach chi goi callback; ClientCore duoc goi tai cua so chinh.
    DanhSachPhongPanel(Supplier<NguoiDung> toi,Predicate<String> daMo,Function<String,Integer> chuaDoc,
                       Consumer<Phong> vao,BiConsumer<String,Phong> hanhDong) {
        super(new BorderLayout(0,Theme.S12)); this.toi=toi; this.daMo=daMo; this.chuaDoc=chuaDoc; this.vao=vao; this.hanhDong=hanhDong;
        setBackground(Theme.NEN); setBorder(Theme.le(Theme.S12));
        JPanel dau=new JPanel(new BorderLayout(0,Theme.S16)); dau.setOpaque(false);
        JLabel ten=Theme.nhan("UDP Chat",true); ten.setFont(Theme.DAM.deriveFont(20f)); dau.add(ten,BorderLayout.NORTH);
        tao.setMargin(new java.awt.Insets(6,8,6,8)); ma.setMargin(new java.awt.Insets(6,8,6,8));
        loc.putClientProperty("JTextField.placeholderText","Lọc phòng…"); dau.add(loc,BorderLayout.CENTER);
        JPanel nut=new JPanel(new GridLayout(1,2,Theme.S8,0)); nut.setOpaque(false); nut.add(tao); nut.add(ma); dau.add(nut,BorderLayout.SOUTH); add(dau,BorderLayout.NORTH);
        ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS)); ds.setBackground(Theme.NEN); cuon.setBorder(null);
        cuon.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); add(cuon,BorderLayout.CENTER);
        tao.addActionListener(e -> hanhDong.accept("tao",null)); ma.addActionListener(e -> hanhDong.accept("ma",null));
        loc.getDocument().addDocumentListener(new DocumentListener() {
            // Cap nhat khi them noi dung vao o nhap.
            public void insertUpdate(DocumentEvent e) { loc(); }
            // Cap nhat khi xoa noi dung khoi o nhap.
            public void removeUpdate(DocumentEvent e) { loc(); }
            // Cap nhat khi dinh dang van ban thay doi.
            public void changedUpdate(DocumentEvent e) { loc(); }
        });
    }
    // Bat tat thao tac khi chua dang nhap hoac mat ket noi.
    void choPhep(boolean bat) { choPhep=bat; tao.setEnabled(bat); ma.setEnabled(bat); batNut(ds,bat); }
    // Bat tat ca nut con ma khong thay doi du lieu.
    private void batNut(java.awt.Container cha,boolean bat) {
        for (var c:cha.getComponents()) {
            if (c instanceof JButton b) { b.setEnabled(bat); }
            if (c instanceof java.awt.Container n) { batNut(n,bat); }
        }
    }
    // Nhan snapshot rieng cua tai khoan, giu bo loc dang nhap.
    void capNhat(List<Phong> moi) { danhSach=List.copyOf(moi); loc(); }
    // Tin lich su cu khong duoc ghi de preview cua tin moi.
    void tinCuoi(TinNhan t) {
        TinNhan cu=tinCuoi.get(t.roomId);
        if (cu==null || t.msgId>=cu.msgId) { tinCuoi.put(t.roomId,t); loc(); }
    }
    // Preview tin cuoi khong duoc giu noi dung da thu hoi.
    void thuHoi(String room,long id) { TinNhan t=tinCuoi.get(room); if (t!=null && t.msgId==id) { tinCuoi.put(room,t.daThuHoi()); loc(); } }
    // Nhan nguoi gui dung id, khong so sanh ten hien thi.
    private String xemTruoc(TinNhan tin) {
        var n=toi.get(); String ten=n!=null && n.id.equals(tin.userId)?"Bạn":tin.ten;
        String nd=tin.thuHoi?"Tin nhắn đã thu hồi":tin.loai.equals("S")?"[Sticker] "+StickerKho.ten(tin.noiDung):tin.loai.equals("F")?"[Tệp] "+com.mycompany.udpmultichat.common.TepTin.doc(tin.roomId,tin.userId,tin.noiDung,tin.thoiGian).ten():tin.noiDung.replace('\n',' ');
        return ten+": "+nd;
    }
    // Don danh sach sau dang xuat.
    void xoa() { tinCuoi.clear(); danhSach=List.of(); dangChon=""; loc(); }
    // Chon phong tu sidebar, khong tao them tab.
    void chon(String id) {
        dangChon=id;
        cacHang.forEach((k,h) -> {
            h.setBackground(k.equals(id)?Theme.CHON:Theme.NEN);
            if (h.getClientProperty("menuPhong") instanceof JButton b) { b.setVisible(k.equals(id)); }
            if (h.getClientProperty("badge") instanceof JLabel l) { int so=chuaDoc.apply(k); l.setText(so>0?String.valueOf(so):""); }
        });
    }
    // Loc tren ca hai nhom, giu vi tri cuon.
    private void loc() {
        int viTri=cuon.getVerticalScrollBar().getValue(); ds.removeAll(); cacHang.clear();
        String tim=loc.getText().strip().toLowerCase(java.util.Locale.ROOT);
        List<Phong> ketQua=danhSach.stream().filter(p -> p.ten.toLowerCase(java.util.Locale.ROOT).contains(tim)).toList();
        themNhom("PHÒNG CỦA TÔI",ketQua.stream().filter(this::cuaToi).toList(),true);
        themNhom("PHÒNG CÔNG KHAI",ketQua.stream().filter(p -> !cuaToi(p)).toList(),false);
        ds.add(javax.swing.Box.createVerticalGlue()); ds.revalidate(); ds.repaint(); cuon.getVerticalScrollBar().setValue(viTri);
    }
    // Membership do Server gui, khong suy ra tu ten hien thi.
    private boolean cuaToi(Phong p) {
        return p.daThamGia || daMo.test(p.id) || chu(p) || (!p.congKhai && quanLy(p));
    }
    // Moi nhom co tieu de xam can trai; phong rieng admin giam sat o nhom cua toi.
    private void themNhom(String ten,List<Phong> phong,boolean thamGia) {
        if (!thamGia && phong.isEmpty()) { return; }
        JLabel t=Theme.nhan(ten,true); t.setForeground(Theme.PHU); t.setFont(Theme.NHO); t.setBorder(Theme.le(Theme.S8));
        t.setAlignmentX(LEFT_ALIGNMENT); t.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); ds.add(t);
        for (Phong p:phong) { ds.add(hang(p,thamGia)); }
    }
    // Dong phong hai hang co dai gradient bao hieu phong dang chon.
    private JPanel hang(Phong p,boolean thamGia) {
        JPanel h=new JPanel(new BorderLayout(Theme.S4,0)) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (p.id.equals(dangChon)) {
                    java.awt.Graphics2D g2=(java.awt.Graphics2D)g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new java.awt.GradientPaint(0,0,Theme.GRADIENT_START,0,getHeight(),Theme.GRADIENT_END));
                    g2.fillRoundRect(2,10,4,getHeight()-20,4,4);
                    g2.dispose();
                }
            }
        };
        h.setBorder(Theme.le(Theme.S8)); h.setName("phong-"+p.id);
        h.setAlignmentX(LEFT_ALIGNMENT); h.setMaximumSize(new Dimension(Integer.MAX_VALUE,76)); h.setPreferredSize(new Dimension(250,76));
        h.setBackground(p.id.equals(dangChon)?Theme.CHON:Theme.NEN); cacHang.put(p.id,h);
        TinNhan tin=tinCuoi.get(p.id); JPanel text=new JPanel(new GridLayout(2,1,0,Theme.S4)); text.setOpaque(false);
        JPanel tren=new JPanel(new BorderLayout(Theme.S4,0)); tren.setOpaque(false);
        JLabel ten=Theme.nhan(p.ten,true),gio=Theme.nhan(tin==null?"":GIO.format(Instant.ofEpochMilli(tin.thoiGian)),false);
        ten.setIcon(p.congKhai?null:Theme.icon("lock")); gio.setFont(Theme.NHO); gio.setForeground(Theme.PHU); tren.add(ten); tren.add(gio,BorderLayout.EAST);
        JLabel nd=Theme.nhan(tin==null?(p.dangMo?p.moTa:"Phòng đang đóng"):xemTruoc(tin),false); nd.setForeground(Theme.PHU); nd.setFont(Theme.NHO);
        text.add(tren); text.add(nd); h.add(text); h.add(nutPhong(p,thamGia),BorderLayout.EAST);
        h.setToolTipText(p.moTa+" · Mã: "+p.maPhong+" · Giới hạn "+p.gioiHan);
        ganClick(h,() -> { if (choPhep) { vao.accept(p); } }); h.setComponentPopupMenu(menu(p));
        if (h.getClientProperty("menuPhong") instanceof JButton b) { ganHover(h,h,b,p.id); } return h;
    }
    // Badge gradient bao tin chua doc; phong chua tham gia co nut rieng.
    private JPanel nutPhong(Phong p,boolean thamGia) {
        JPanel ben=new JPanel(new BorderLayout()); ben.setOpaque(false);
        if (!thamGia) {
            JButton b=Theme.nut("Tham gia",false); b.setFont(Theme.NHO); b.setMargin(new java.awt.Insets(4,8,4,8));
            b.setBackground(Theme.CHON); b.setEnabled(choPhep); b.addActionListener(e -> vao.accept(p));
            JPanel giua=new JPanel(new java.awt.GridBagLayout()); giua.setOpaque(false); giua.add(b); ben.add(giua); return ben;
        }
        JButton menu=Theme.nutNhe("","more"); menu.setEnabled(choPhep); menu.setVisible(p.id.equals(dangChon));
        menu.addActionListener(e -> menu(p).show(menu,0,menu.getHeight())); ben.add(menu,BorderLayout.NORTH);
        int so=chuaDoc.apply(p.id);
        JLabel badge=new JLabel(so>0?String.valueOf(so):"") {
            @Override protected void paintComponent(java.awt.Graphics g) {
                if (!getText().isEmpty()) {
                    java.awt.Graphics2D g2=(java.awt.Graphics2D)g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new java.awt.GradientPaint(0,0,Theme.GRADIENT_DO_START,getWidth(),getHeight(),Theme.GRADIENT_DO_END));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        badge.setName("badge-"+p.id); badge.setFont(Theme.NHO.deriveFont(java.awt.Font.BOLD,11f));
        badge.setForeground(Theme.THE); badge.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        badge.setBorder(new javax.swing.border.EmptyBorder(2,6,2,6)); ben.add(badge,BorderLayout.SOUTH);
        JPanel hang=cacHang.get(p.id); hang.putClientProperty("menuPhong",menu); hang.putClientProperty("badge",badge);
        return ben;
    }
    // Hover duoc theo doi tren ca nhan con, khong chi khoang trong cua dong.
    private void ganHover(JComponent c,JPanel hang,JButton menu,String id) {
        c.addMouseListener(new MouseAdapter() {
            // Hien menu khi hover ca nhan con.
            public void mouseEntered(MouseEvent e) { menu.setVisible(true); }
            // An menu khi chuot roi han dong phong.
            public void mouseExited(MouseEvent e) {
                var p=javax.swing.SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),hang);
                if (!hang.contains(p)) { menu.setVisible(id.equals(dangChon)); }
            }
        });
        for (var con:c.getComponents()) { if (con instanceof JComponent j) { ganHover(j,hang,menu,id); } }
    }
    // Chuyen click/chuot phai tu nhan con ve dong phong, khong can renderer gia nut.
    private void ganClick(JComponent c,Runnable vaoPhong) {
        c.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON1) { vaoPhong.run(); } } });
        for (var con:c.getComponents()) {
            if (con instanceof JComponent j && !(j instanceof JButton)) { j.setInheritsPopupMenu(true); ganClick(j,vaoPhong); }
        }
    }
    // ID chu phong la ID bat bien cua tai khoan da xac thuc.
    boolean chu(Phong p) { return toi.get()!=null && toi.get().id.equals(p.chuId); }
    // Quyen he thong tu Server, khong suy ra tu chu Admin trong ten.
    boolean quanLy(Phong p) { return toi.get()!=null && (chu(p) || toi.get().vaiTro==VaiTro.ADMIN); }
    // ===== MENU PHONG =====
    // Khong hien thao tac roi cho chu phong hay cho phong chua tham gia.
    JPopupMenu menu(Phong p) {
        JPopupMenu m=new JPopupMenu(); them(m,"Thông tin phòng","thongtin",p);
        JMenuItem copy=new JMenuItem("Sao chép mã phòng"); copy.addActionListener(e -> saoChep(p.maPhong)); m.add(copy);
        them(m,"Tìm kiếm (Ctrl+F)","tim",p); them(m,"Xuất lịch sử chat (.txt)","xuat_lichsu",p);
        if (Protocol.SANH_ID.equals(p.id)) { return m; }
        if (quanLy(p)) {
            m.addSeparator(); them(m,"Sửa phòng","sua",p); them(m,p.dangMo?"Đóng phòng":"Mở phòng","trangthai",p);
            them(m,"Chuyển quyền chủ phòng","chuyen",p); them(m,"Xóa phòng","xoa",p);
        }
        if (!chu(p) && (p.daThamGia || daMo.test(p.id))) { them(m,"Rời phòng","roi",p); } return m;
    }
    // Sao chep van ban thuong vao clipboard cua he dieu hanh.
    static void saoChep(String text) { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),null); }
    // Them lenh vao menu qua callback UI.
    private void them(JPopupMenu m,String ten,String lenh,Phong p) { JMenuItem i=new JMenuItem(ten); i.addActionListener(e -> hanhDong.accept(lenh,p)); m.add(i); }
}
