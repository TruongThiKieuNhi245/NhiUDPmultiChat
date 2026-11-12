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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** O soan nhieu dong; gioi han BYTE, Enter gui, Shift+Enter xuong dong, toi da 5 dong cao. */
@SuppressWarnings("serial")
final class SoanTinPanel extends JPanel {
    private final JTextArea o=new JTextArea(1,30) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) { g.setColor(Theme.PHU); g.setFont(Theme.FONT); g.drawString("Nhập tin nhắn…",4,20); }
        }
    };
    private final JButton gui=Theme.nut("Gửi",true);
    private final JLabel dem=Theme.nhan(" ",false);
    private final JScrollPane cuon=new JScrollPane(o);
    private final JPanel vien=new JPanel(new BorderLayout(Theme.S8,0));
    private boolean choPhep,dangGui;
    SoanTinPanel(Consumer<String> khiGui) {
        super(new BorderLayout(0,Theme.S4)); setBorder(Theme.le(Theme.S16)); setBackground(Theme.THE);
        vien.setBackground(Theme.THE); o.setFont(Theme.FONT); o.setLineWrap(true); o.setWrapStyleWord(true);
        o.setMargin(new Insets(4,4,4,4)); cuon.setBorder(null); cuon.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel choNut=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,0,0)); choNut.setOpaque(false);
        JButton emoji=Theme.nut("",false),file=Theme.nut("+",false);
        emoji.setIcon(Theme.icon("smile")); emoji.setEnabled(false); file.setEnabled(false); emoji.setToolTipText("Sticker/emoji ở M3"); file.setToolTipText("Đính kèm ở M4");
        choNut.add(emoji); choNut.add(file); vien.add(choNut,BorderLayout.WEST); vien.add(cuon,BorderLayout.CENTER); vien.add(gui,BorderLayout.EAST);
        add(vien,BorderLayout.CENTER); dem.setFont(Theme.NHO); add(dem,BorderLayout.SOUTH);
        o.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { capNhat(); } public void removeUpdate(DocumentEvent e) { capNhat(); }
            public void changedUpdate(DocumentEvent e) { capNhat(); }
        });
        o.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { vien(); } public void focusLost(FocusEvent e) { vien(); } });
        Runnable guiTin=() -> { if (gui.isEnabled()) { khiGui.accept(o.getText()); } };
        gui.addActionListener(e -> guiTin.run());
        o.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"gui");
        o.getActionMap().put("gui",new AbstractAction() { public void actionPerformed(ActionEvent e) { guiTin.run(); } });
        o.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"),"insert-break");
        capNhat();
    }
    void xemTruoc(String text) { choPhep(true,false); o.setText(text); }
    String noiDung() { return o.getText(); }
    void xoaNeuGiong(String cu) { if (cu.equals(o.getText())) { o.setText(""); } }
    void choPhep(boolean bat,boolean dangGui) { this.choPhep=bat; this.dangGui=dangGui; capNhat(); }
    private void vien() {
        boolean focus=o.hasFocus();
        vien.setBorder(new javax.swing.border.AbstractBorder() {
            @Override public void paintBorder(java.awt.Component c,Graphics g,int x,int y,int w,int h) {
                var a=(java.awt.Graphics2D)g.create(); a.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                a.setColor(focus?Theme.CHINH:Theme.VIEN_SOAN); a.setStroke(new java.awt.BasicStroke(focus?2:1));
                a.drawRoundRect(x+1,y+1,w-3,h-3,20,20); a.dispose();
            }
            @Override public Insets getBorderInsets(java.awt.Component c) { return new Insets(8,8,8,8); }
        });
    }
    private void capNhat() {
        int bytes=o.getText().strip().getBytes(StandardCharsets.UTF_8).length;
        boolean qua=bytes>Protocol.TIN_TOI_DA;
        dem.setText(bytes>=480?(bytes+"/600 byte UTF-8 · tối đa 600 ký tự ASCII hoặc 200 ký tự 3-byte"+(qua?" — Tin quá dài":"")):" ");
        dem.setForeground(qua?Theme.LOI:Theme.PHU);
        gui.setEnabled(choPhep && !dangGui && !o.getText().isBlank() && !qua); o.setEnabled(choPhep && !dangGui);
        int dong=Math.max(1,Math.min(5,o.getText().split("\n",-1).length + o.getText().length()/Math.max(35,o.getWidth()/8)));
        cuon.setPreferredSize(new Dimension(100,Math.max(28,dong*20+8))); vien(); revalidate();
    }
}
