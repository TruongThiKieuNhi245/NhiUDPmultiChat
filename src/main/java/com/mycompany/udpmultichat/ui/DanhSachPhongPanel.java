package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Sidebar chat quen thuoc: loc, tao/nhap ma, dong hai hang, preview/badge va menu phong. */
@SuppressWarnings("serial")
final class DanhSachPhongPanel extends JPanel {
    private final DefaultListModel<Phong> model=new DefaultListModel<>();
    private final JList<Phong> ds=new JList<>(model);
    private final JTextField loc=new JTextField();
    private final JButton tao=Theme.nut("+ Tạo phòng",true),ma=Theme.nut("Nhập mã",false);
    private final Supplier<NguoiDung> toi;
    private final Function<String,Integer> chuaDoc;
    private final Consumer<Phong> vao;
    private final java.util.function.BiConsumer<String,Phong> hanhDong;
    private final Map<String,TinNhan> tinCuoi=new LinkedHashMap<>();
    private List<Phong> danhSach=List.of();
    private int hover=-1;
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    DanhSachPhongPanel(Supplier<NguoiDung> toi,Function<String,Integer> chuaDoc,Consumer<Phong> vao,
                       java.util.function.BiConsumer<String,Phong> hanhDong) {
        super(new BorderLayout(0,Theme.S12)); this.toi=toi; this.chuaDoc=chuaDoc; this.vao=vao; this.hanhDong=hanhDong;
        setBackground(Theme.NEN); setBorder(Theme.le(Theme.S12));
        JPanel dau=new JPanel(new BorderLayout(0,Theme.S16)); dau.setOpaque(false);
        JLabel ten=Theme.nhan("UDP Chat",true); ten.setFont(Theme.DAM.deriveFont(20f)); dau.add(ten,BorderLayout.NORTH);
        loc.putClientProperty("JTextField.placeholderText","Lọc phòng…"); dau.add(loc,BorderLayout.CENTER);
        JPanel nut=new JPanel(new GridLayout(1,2,Theme.S8,0)); nut.setOpaque(false); nut.add(tao); nut.add(ma); dau.add(nut,BorderLayout.SOUTH); add(dau,BorderLayout.NORTH);
        ds.setBackground(Theme.NEN); ds.setCellRenderer(new DongPhong()); ds.setFixedCellHeight(72);
        JScrollPane cuon=new JScrollPane(ds); cuon.setBorder(null); add(cuon,BorderLayout.CENTER);
        tao.addActionListener(e -> hanhDong.accept("tao",null)); ma.addActionListener(e -> hanhDong.accept("ma",null));
        loc.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loc(); } public void removeUpdate(DocumentEvent e) { loc(); }
            public void changedUpdate(DocumentEvent e) { loc(); }
        });
        MouseAdapter chuot=new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover=ds.locationToIndex(e.getPoint()); ds.repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover=-1; ds.repaint(); }
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) { moMenu(e); } }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) { moMenu(e); } }
            @Override public void mouseClicked(MouseEvent e) {
                if (!ds.isEnabled() || e.getButton()!=MouseEvent.BUTTON1) { return; }
                Phong p=taiDiem(e); if (p==null) { return; }
                if (e.getX()>ds.getWidth()-36) { menu(p).show(ds,e.getX(),e.getY()); } else { vao.accept(p); }
            }
        };
        ds.addMouseListener(chuot); ds.addMouseMotionListener(chuot);
    }
    void choPhep(boolean bat) { tao.setEnabled(bat); ma.setEnabled(bat); ds.setEnabled(bat); }
    void capNhat(List<Phong> dsMoi) { danhSach=List.copyOf(dsMoi); loc(); }
    void tinCuoi(TinNhan t) {
        TinNhan cu=tinCuoi.get(t.roomId);
        if (cu==null || t.msgId>cu.msgId) { tinCuoi.put(t.roomId,t); ds.repaint(); }
    }
    void xoa() { tinCuoi.clear(); danhSach=List.of(); loc(); }
    void chon(String id) { for (int i=0;i<model.size();i++) { if (model.get(i).id.equals(id)) { ds.setSelectedIndex(i); break; } } }
    private void loc() {
        String id=ds.getSelectedValue()==null?"":ds.getSelectedValue().id;
        model.clear(); String tim=loc.getText().strip().toLowerCase(java.util.Locale.ROOT);
        danhSach.stream().filter(p -> p.ten.toLowerCase(java.util.Locale.ROOT).contains(tim)).forEach(model::addElement); chon(id);
    }
    boolean chu(Phong p) { return toi.get()!=null && toi.get().id.equals(p.chuId); }
    boolean quanLy(Phong p) { return toi.get()!=null && (chu(p) || toi.get().vaiTro==VaiTro.ADMIN); }
    JPopupMenu menu(Phong p) {
        JPopupMenu m=new JPopupMenu(); them(m,"Thông tin phòng","thongtin",p);
        JMenuItem copy=new JMenuItem("Sao chép mã phòng"); copy.addActionListener(e -> saoChep(p.maPhong)); m.add(copy);
        if (Protocol.SANH_ID.equals(p.id)) { return m; }
        m.addSeparator();
        if (quanLy(p)) {
            them(m,"Sửa phòng","sua",p); them(m,p.dangMo?"Đóng phòng":"Mở phòng","trangthai",p);
            them(m,"Chuyển quyền chủ phòng","chuyen",p); them(m,"Xóa phòng","xoa",p);
        }
        if (!chu(p)) { them(m,"Rời phòng","roi",p); }
        return m;
    }
    static void saoChep(String text) { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),null); }
    private void them(JPopupMenu m,String ten,String lenh,Phong p) { JMenuItem i=new JMenuItem(ten); i.addActionListener(e -> hanhDong.accept(lenh,p)); m.add(i); }
    private Phong taiDiem(MouseEvent e) { int i=ds.locationToIndex(e.getPoint()); return i>=0 && ds.getCellBounds(i,i).contains(e.getPoint())?model.get(i):null; }
    private void moMenu(MouseEvent e) { Phong p=taiDiem(e); if (p!=null && ds.isEnabled()) { ds.setSelectedValue(p,true); menu(p).show(ds,e.getX(),e.getY()); } }

    private static void boCuc(java.awt.Container c) {
        c.doLayout(); for (var con:c.getComponents()) { if (con instanceof java.awt.Container n) { boCuc(n); } }
    }

    /** Renderer khong nhan click truc tiep; nut ba cham duoc hit-test tren JList. */
    private final class DongPhong extends JPanel implements ListCellRenderer<Phong> {
        private final JLabel ten=Theme.nhan("",true),gio=Theme.nhan("",false),preview=Theme.nhan("",false),badge=Theme.nhan("",true),menu=Theme.nhan("",true);
        DongPhong() {
            super(new BorderLayout(Theme.S8,0)); setBorder(Theme.le(Theme.S8));
            JPanel text=new JPanel(new GridLayout(2,1,0,Theme.S4)); text.setOpaque(false);
            JPanel tren=new JPanel(new BorderLayout()); tren.setOpaque(false); tren.add(ten,BorderLayout.CENTER); tren.add(gio,BorderLayout.EAST);
            text.add(tren); text.add(preview); add(text,BorderLayout.CENTER);
            JPanel ben=new JPanel(new GridLayout(2,1)); ben.setOpaque(false); ben.setPreferredSize(new Dimension(24,44));
            ben.add(menu); ben.add(badge); add(ben,BorderLayout.EAST); gio.setFont(Theme.NHO); gio.setForeground(Theme.PHU); preview.setForeground(Theme.PHU); badge.setForeground(Theme.CHINH);
        }
        @Override public Component getListCellRendererComponent(JList<? extends Phong> list,Phong p,int i,boolean chon,boolean focus) {
            TinNhan t=tinCuoi.get(p.id); int so=chuaDoc.apply(p.id);
            ten.setIcon(p.congKhai?null:Theme.icon("lock")); ten.setText((p.congKhai?"# ":"")+p.ten); gio.setText(t==null?"":GIO.format(Instant.ofEpochMilli(t.thoiGian)));
            String nd=t==null?(p.dangMo?(p.congKhai?"Phòng công khai":"Phòng riêng tư"):"Phòng đang đóng"):t.ten+": "+t.noiDung.replace('\n',' ');
            preview.setText(nd.length()>30?nd.substring(0,29)+"…":nd); badge.setText(so>0?String.valueOf(so):"");
            menu.setIcon(Theme.icon("more")); menu.setVisible(chon || hover==i); setBackground(chon?Theme.CHON:Theme.NEN);
            setToolTipText(p.moTa+" · Mã: "+p.maPhong+" · "+p.soThanhVien+" đang online / tối đa "+p.gioiHan);
            setSize(Math.max(240,list.getWidth()),72); boCuc(this); return this;
        }
    }
}
