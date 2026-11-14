package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Vai tro: o soan tin. Ai goi: ChatMulticastUI, UiPreview. Goi: Theme, Protocol, callback gui. */
@SuppressWarnings("serial")
final class SoanTinPanel extends JPanel {
    private final JTextArea o=new JTextArea(1,30) {
        // Placeholder khong tham gia vao noi dung gui.
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) { g.setColor(Theme.PHU); g.setFont(Theme.FONT); g.drawString("Nhập tin nhắn…",4,20); }
        }
    };
    private java.util.function.Consumer<String> chonSticker=ma -> { },dangGo=t -> { };
    private final JButton sticker=Theme.nutNhe("","smile"),file=Theme.nutNhe("","plus");
    private Consumer<javax.swing.JComponent> dinhKem=b -> { };
    // Gan menu tep khi cua so da duoc tao.
    void datTep(Consumer<javax.swing.JComponent> menu) { dinhKem=menu; }
    // Gan thao tac sau khi cua so chinh da tao xong Client.
    void datChucNang(Consumer<String> sticker,Consumer<String> dangGo) { chonSticker=sticker; this.dangGo=dangGo; capNhat(); }
    private final JButton gui=Theme.nutGui();
    private final JLabel dem=Theme.nhan("",false);
    private final JTextArea loi=new JTextArea();
    private final JScrollPane cuon=new JScrollPane(o);
    private final JPanel vien=new JPanel(new BorderLayout(0,Theme.S4)) {
        // Nen khoa cung bo tron voi vien, khong to day cac goc vuong.
        protected void paintComponent(Graphics g) { Theme.boTron(g,getWidth(),getHeight(),choPhep?Theme.THE:Theme.NEN_KHOA,10); }
    };
    private boolean choPhep,dangGui;
    private String loiGui="";

    // ===== DUNG O SOAN =====
    // Cac nut nam o SOUTH cua cot rieng nen khong cao theo textarea.
    SoanTinPanel(Consumer<String> khiGui) {
        super(new BorderLayout(0,Theme.S4)); setBorder(Theme.le(Theme.S16)); setBackground(Theme.THE);
        vien.setOpaque(false); o.setFont(Theme.FONT); o.setLineWrap(true); o.setWrapStyleWord(true);
        vien.setName("khung-soan"); cuon.setName("cuon-soan");
        o.setMargin(new Insets(4,4,4,4)); cuon.setBorder(null); cuon.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vien.add(cuon,BorderLayout.CENTER);
        JPanel congCu=new JPanel(new BorderLayout()); congCu.setOpaque(false); congCu.add(nutDinhKem(),BorderLayout.WEST);
        congCu.add(gui,BorderLayout.EAST); vien.add(congCu,BorderLayout.SOUTH);
        add(vien,BorderLayout.CENTER); JPanel duoi=new JPanel(new BorderLayout()); duoi.setOpaque(false);
        dem.setFont(Theme.NHO); duoi.add(dem,BorderLayout.NORTH);
        loi.setMargin(new Insets(0,0,0,0)); loi.setBorder(null); loi.setFont(Theme.NHO); loi.setForeground(Theme.LOI); loi.setOpaque(false); loi.setEditable(false); loi.setLineWrap(true); loi.setWrapStyleWord(true);
        duoi.add(loi,BorderLayout.CENTER); add(duoi,BorderLayout.SOUTH);
        theoDoiNhap(); ganPhim(khiGui); capNhat();
    }
    // Nut emoji va dinh kem nhe, can day; chua co chuc nang thi an.
    private JPanel nutDinhKem() {
        JPanel nut=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,0,0)); nut.setOpaque(false);
        JButton emoji=sticker; file.addActionListener(e -> dinhKem.accept(file));
        emoji.setToolTipText("Emoji và sticker"); file.setToolTipText("Đính kèm"); file.setEnabled(false);
        emoji.addActionListener(e -> { var m=StickerKho.bangChon(chonSticker); m.show(emoji,0,-m.getPreferredSize().height); });
        nut.add(emoji); nut.add(file); JPanel boc=new JPanel(new BorderLayout()); boc.setOpaque(false); boc.add(nut,BorderLayout.SOUTH); return boc;
    }
    // Thay noi dung xoa loi cu; tinh lai khi be rong o soan thay doi.
    private void theoDoiNhap() {
        o.getDocument().addDocumentListener(new DocumentListener() {
            // Cap nhat khi them noi dung vao o nhap.
            public void insertUpdate(DocumentEvent e) { loiGui=""; capNhat(); dangGo.accept(o.getText()); }
            // Cap nhat khi xoa noi dung khoi o nhap.
            public void removeUpdate(DocumentEvent e) { loiGui=""; capNhat(); dangGo.accept(o.getText()); }
            // Cap nhat khi dinh dang van ban thay doi.
            public void changedUpdate(DocumentEvent e) { capNhat(); }
        });
        o.addFocusListener(new FocusAdapter() {
            // Cap nhat khi o nhap nhan focus.
            public void focusGained(FocusEvent e) { vien(); }
            // Cap nhat khi o nhap mat focus.
            public void focusLost(FocusEvent e) { vien(); }
        });
        o.addComponentListener(new java.awt.event.ComponentAdapter() { public void componentResized(java.awt.event.ComponentEvent e) { capNhat(); } });
    }
    // Enter gui, Shift+Enter xuong dong, nut trong thi khong gui.
    private void ganPhim(Consumer<String> khiGui) {
        Runnable guiTin=() -> { if (gui.isEnabled()) { khiGui.accept(o.getText()); } };
        gui.addActionListener(e -> guiTin.run()); o.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"gui");
        o.getActionMap().put("gui",new AbstractAction() { public void actionPerformed(ActionEvent e) { guiTin.run(); } });
        o.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"),"insert-break");
    }
    // Du lieu mau cho cong cu xem truoc, khong gui len Server.
    void xemTruoc(String text) { choPhep(true,false); o.setText(text); }
    // Chi xoa noi dung neu nguoi dung chua thay the bang tin khac.
    void xoaNeuGiong(String cu) { if (cu.equals(o.getText())) { o.setText(""); } }
    // Khoa khung khi offline, phong dong hoac dang cho ket qua gui.
    void choPhep(boolean bat,boolean dangGui) { this.choPhep=bat; this.dangGui=dangGui; capNhat(); }
    // Loi do header/escape cung hien ngay o soan thay vi mat tin am tham.
    void baoLoi(String text) { loiGui=text; capNhat(); }
    // Vien 1px, khi focus chuyen thanh 2px cung mau chinh.
    private void vien() {
        boolean focus=o.hasFocus();
        vien.setBorder(new javax.swing.border.AbstractBorder() {
            // Paint border.
            public void paintBorder(java.awt.Component c,Graphics g,int x,int y,int w,int h) {
                var a=(java.awt.Graphics2D)g.create(); a.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                a.setColor(focus?Theme.CHINH:Theme.VIEN_SOAN); a.setStroke(new java.awt.BasicStroke(focus?2:1));
                a.drawRoundRect(x+1,y+1,w-3,h-3,20,20); a.dispose();
            }
            // Get border insets.
            public Insets getBorderInsets(java.awt.Component c) { return new Insets(8,8,8,8); }
        });
    }
    // Dem ky tu cho nguoi dung; gioi han that van la byte UTF-8 va kich thuoc goi.
    private void capNhat() {
        String text=o.getText(); int bytes=text.strip().getBytes(StandardCharsets.UTF_8).length;
        boolean qua=bytes>Protocol.TIN_TOI_DA; int kyTu=text.codePointCount(0,text.length());
        int toiDa=bytes==0?Protocol.TIN_TOI_DA:(int)((long)Protocol.TIN_TOI_DA*kyTu/bytes);
        dem.setText(kyTu+"/"+toiDa); dem.setVisible(bytes>=Protocol.TIN_TOI_DA*4/5);
        dem.setForeground(qua?Theme.LOI:Theme.PHU); String thongBao=qua?Protocol.LOI_TIN_DAI:loiGui;
        loi.setText(thongBao); loi.setVisible(!thongBao.isBlank()); loi.setRows(thongBao.isBlank()?0:2);
        sticker.setEnabled(choPhep); file.setEnabled(choPhep);
        gui.setEnabled(choPhep && !dangGui && !text.isBlank() && !qua); o.setEnabled(choPhep && !dangGui);
        java.awt.Color nen=choPhep?Theme.THE:Theme.NEN_KHOA;
        vien.setBackground(nen); o.setBackground(nen); cuon.getViewport().setBackground(nen);
        int dong=0,rong=Math.max(150,o.getWidth()-8);
        for (String d:text.split("\n",-1)) { dong+=Math.max(1,(o.getFontMetrics(Theme.FONT).stringWidth(d)+rong-1)/rong); }
        cuon.setVerticalScrollBarPolicy(dong>5?JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED:JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        cuon.setPreferredSize(new Dimension(100,Math.max(24,Math.min(5,dong)*20+4))); vien(); revalidate();
    }
}
