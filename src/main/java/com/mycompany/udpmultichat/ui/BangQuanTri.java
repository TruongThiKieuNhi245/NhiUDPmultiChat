package com.mycompany.udpmultichat.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/** Vai tro: JTable quan tri dung chung, loc/sap xep va trang thai. Ai goi: QuanTriFrame, KiemDuyetUI, QuanTriPreview. Goi: Theme. */
@SuppressWarnings("serial")
final class BangQuanTri extends JPanel {
    private final JTable table; private final DefaultTableModel model;
    private final JTextField tim=new JTextField(); private final JComboBox<String> loc=new JComboBox<>(new String[]{"Tất cả","Online","Offline","Bị khóa","Admin"});
    private final JLabel trangThai=Theme.nhan("Chưa có dữ liệu",false);
    private java.util.List<java.util.List<String>> duLieu=java.util.List.of(); private final int[] cot;
    private final Consumer<String> action; private final String[] lenh;
    private final boolean locUser,locNhatKy;
    // Cot 0 cua du lieu la id, UI chi hien cac cot duoc chi dinh.
    BangQuanTri(String[] nhan,int[] cot,String[] lenh,Consumer<String> action,boolean locUser) {
        super(new BorderLayout(0,Theme.S12)); this.cot=cot; this.action=action; this.lenh=lenh;
        this.locUser=locUser; locNhatKy=cot.length==5 && cot[0]==0; setBorder(Theme.le(Theme.S16));
        String[] headers=Arrays.copyOf(nhan,nhan.length+1); headers[nhan.length]="...";
        model=new DefaultTableModel(headers,0) { public boolean isCellEditable(int r,int c) { return false; } }; table=new JTable(model);
        table.putClientProperty("html.disable",Boolean.TRUE); table.setRowHeight(36); table.setAutoCreateRowSorter(true); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setSelectionBackground(Theme.CHON); table.setSelectionForeground(Theme.CHU);
        table.getTableHeader().setFont(Theme.DAM); table.getTableHeader().setBackground(Theme.NEN); table.setDefaultRenderer(Object.class,new O());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i=0;i<nhan.length;i++) {
            boolean ngay=nhan[i].contains("Ngày") || nhan[i].contains("cuối") || nhan[i].equals("Thời gian");
            int rong=Math.max(ngay?168:120,table.getFontMetrics(Theme.DAM).stringWidth(nhan[i])+32);
            if (nhan[i].equals("Tên hiển thị") || nhan[i].equals("Tên")) { rong=180; }
            if (nhan[i].equals("Vai trò")) { rong=140; }
            if (nhan[i].equals("Chi tiết")) { rong=320; }
            if (nhan[i].startsWith("Số ") || Set.of("Tin","Tệp").contains(nhan[i])) { rong=90; }
            table.getColumnModel().getColumn(i).setPreferredWidth(rong);
        }
        table.getColumnModel().getColumn(cot.length).setPreferredWidth(44);
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,Theme.S8,0)); tim.setPreferredSize(new Dimension(240,32)); tim.putClientProperty("JTextField.placeholderText","Tìm trong bảng..."); toolbar.add(tim); loc.setVisible(locUser || locNhatKy); toolbar.add(loc);
        if (locNhatKy) { loc.setModel(new DefaultComboBoxModel<>(new String[]{"Tất cả"})); }
        for (String l:Arrays.copyOf(lenh,Math.min(2,lenh.length))) { JButton b=Theme.nut(l,l.equals("Thêm") || l.equals("Tạo phòng")); b.addActionListener(e -> action.accept(l)); toolbar.add(b); }
        if (lenh.length>2) { JButton more=Theme.nutNhe("Thao tác...",""); more.addActionListener(e -> { JPopupMenu m=new JPopupMenu(); for (String l:lenh) { JMenuItem i=new JMenuItem(l); i.addActionListener(x -> action.accept(l)); m.add(i); } m.show(more,0,more.getHeight()); }); toolbar.add(more); }
        JPanel dau=new JPanel(new BorderLayout(0,Theme.S8)); dau.add(toolbar,BorderLayout.NORTH); dau.add(trangThai); add(dau,BorderLayout.NORTH);
        JScrollPane cuon=new JScrollPane(table); cuon.setColumnHeaderView(table.getTableHeader()); add(cuon);
        tim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loc(); } public void removeUpdate(DocumentEvent e) { loc(); } public void changedUpdate(DocumentEvent e) { loc(); }
        }); loc.addActionListener(e -> loc());
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { moMenu(e); } public void mouseReleased(MouseEvent e) { moMenu(e); }
        });
    }
    // ===== CAP NHAT BANG VA LOC =====
    void duLieu(java.util.List<java.util.List<String>> rows) {
        var daChon=chon(); String idChon=daChon.isEmpty()?"":daChon.get(0);
        duLieu=java.util.List.copyOf(rows); model.setRowCount(0);
        for (var r:rows) { Object[] dong=new Object[cot.length+1]; for (int i=0;i<cot.length;i++) { dong[i]=r.get(cot[i]); } dong[cot.length]="..."; model.addRow(dong); }
        for (int c=0;c<cot.length;c++) { final int col=c;
            boolean so=locUser && Set.of(5,6).contains(cot[col]) || cot.length==10 && Set.of(6,7,8,9).contains(cot[col]);
            ((TableRowSorter<?>)table.getRowSorter()).setComparator(c,so?Comparator.comparing(v -> new java.math.BigInteger(v.toString())):Comparator.comparing(Object::toString));
        }
        if (locNhatKy) {
            String chon=String.valueOf(loc.getSelectedItem());
            loc.setModel(new DefaultComboBoxModel<>(java.util.stream.Stream.concat(java.util.stream.Stream.of("Tất cả"),rows.stream().map(r -> r.get(2)).distinct().sorted()).toArray(String[]::new)));
            loc.setSelectedItem(chon);
            if (loc.getSelectedIndex()<0) { loc.setSelectedIndex(0); }
        }
        trangThai.setText(rows.isEmpty()?"Chưa có dữ liệu":""+rows.size()+" dòng"); trangThai.setForeground(Theme.PHU); loc();
        // Refresh nen giu dung id sau filter/sort, khong mat doi tuong giua luc mo menu.
        for (int i=0;i<duLieu.size();i++) {
            if (!duLieu.get(i).get(0).equals(idChon)) { continue; }
            int hang=table.convertRowIndexToView(i);
            if (hang>=0) { table.setRowSelectionInterval(hang,hang); }
            break;
        }
    }
    // Bao trang thai tai/loi, khong xoa du lieu cu khi dang refresh nen.
    void trangThai(String text,boolean loi) { trangThai.setText(text); trangThai.setForeground(loi?Theme.LOI:Theme.PHU); }
    // Chuyen vi tri hang sau sort ve dung doi tuong trong snapshot.
    java.util.List<String> chon() { int i=table.getSelectedRow(); return i<0?java.util.List.of():duLieu.get(table.convertRowIndexToModel(i)); }
    // Tim toan hang theo chu thuong, loc user online/khoa/admin.
    @SuppressWarnings("unchecked")
    private void loc() {
        TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)table.getRowSorter(); String q=tim.getText().strip().toLowerCase(Locale.ROOT),f=String.valueOf(loc.getSelectedItem());
        sorter.setRowFilter(new RowFilter<>() { public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e) {
            var row=duLieu.get(e.getIdentifier()); String s=row.toString().toLowerCase(Locale.ROOT);
            boolean hop=f.equals("Tất cả") || locUser && (f.equals("Admin")?row.get(3).equals("ADMIN"):row.get(4).equals(f)) || locNhatKy && row.get(2).equals(f);
            return s.contains(q) && hop;
        } });
    }
    // Chuot phai va cot ba cham cung mo thao tac cho hang da chon.
    private void moMenu(MouseEvent e) {
        int hang=table.rowAtPoint(e.getPoint()); if (hang<0) { return; }
        table.setRowSelectionInterval(hang,hang);
        if (!e.isPopupTrigger() && !(e.getID()==MouseEvent.MOUSE_RELEASED && table.columnAtPoint(e.getPoint())==table.getColumnCount()-1)) { return; }
        JPopupMenu m=new JPopupMenu(); for (String l:lenh) { JMenuItem n=new JMenuItem(l); n.addActionListener(x -> action.accept(l)); m.add(n); } m.show(table,e.getX(),e.getY());
    }
    /** Renderer giu font token va trang thai mau, avatar nho o cot ten. */
    private final class O extends DefaultTableCellRenderer {
        private boolean vienThuoc;
        // Ve nhan trang thai nho gon tren nen cua hang.
        @Override protected void paintComponent(Graphics g) {
            if (!vienThuoc) { super.paintComponent(g); return; }
            Graphics2D d=(Graphics2D)g.create(); d.setColor(getBackground()); d.fillRect(0,0,getWidth(),getHeight());
            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); d.setFont(Theme.NHO);
            int w=Math.min(getWidth()-8,d.getFontMetrics().stringWidth(getText())+16); d.setColor(getText().equals("Bị khóa")?Theme.NEN_DO:getText().equals("Online")?Theme.NEN_XANH:Theme.NEN_KHOA); d.fillRoundRect(4,7,w,22,16,16);
            d.setColor(getForeground()); d.drawString(getText(),12,23); d.dispose();
        }
        // Ve o co le trong, nen xen ke va mau trang thai.
        @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col) {
            super.getTableCellRendererComponent(t,value,selected,focus,row,col); setBorder(Theme.le(Theme.S8)); setFont(Theme.FONT); setIcon(null);
            setToolTipText(String.valueOf(value));
            if (String.valueOf(value).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}.*")) {
                try {
                    String v=String.valueOf(value);
                    setText(v.length()==10?java.time.LocalDate.parse(v).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")):java.time.Instant.parse(v).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                } catch (java.time.DateTimeException e) { /* Du lieu cu duoc giu nguyen va co tooltip. */ }
            }
            setBackground(selected?Theme.CHON:row%2==0?Theme.THE:Theme.NEN); setForeground(Theme.CHU);
            putClientProperty("html.disable",Boolean.TRUE); String v=String.valueOf(value); vienThuoc=Set.of("Online","Offline","Bị khóa","ADMIN").contains(v); if (v.equals("Online")) { setForeground(Theme.THANH_CONG); } if (v.equals("Offline")) { setForeground(Theme.PHU); } if (v.equals("Bị khóa")) { setForeground(Theme.LOI); } if (v.equals("ADMIN")) { setForeground(Theme.AVATAR); }
            if (v.equals("THANH_VIEN")) { setText("Thành viên"); }
            if (v.equals("ADMIN")) { setText("Quản trị viên"); }
            if (v.equals("CHU_PHONG")) { setText("Chủ phòng"); }
            if (col==0 && cot[0]==1 && cot.length!=10 && !v.isEmpty()) { setIcon(Theme.avatar(v,null,22)); } return this;
        }
    }
}
