package com.mycompany.udpmultichat.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import javax.swing.*;

/** Vai tro: Kiem chung loc/sort/header component that trong UiPreview. Ai goi: QuanTriPreview. Goi: BangQuanTri. */
final class QuanTriBangKiemTra {
    // Khong khoi tao lop tien ich.
    private QuanTriBangKiemTra() { }

    // Ten/intro chua admin/online khong duoc lam sai bo loc trang thai va vai tro.
    static void chay() {
        BangQuanTri b=new BangQuanTri(new String[]{"Login","Tên","Vai trò","Trạng thái","Số phòng"},new int[]{1,2,3,4,5},new String[0],s -> { },true);
        var rows=List.of(user("1","2","Admin Online","THANH_VIEN","Offline","999999999999999999999"),
            user("2","10","Tên hai","ADMIN","Offline","2"),user("3","333","Tên ba","THANH_VIEN","Online","10"));
        b.duLieu(rows);
        JTable table=tim(b,JTable.class); JComboBox<?> loc=tim(b,JComboBox.class);
        loc.setSelectedItem("Online"); can(table.getRowCount()==1 && table.getValueAt(0,0).equals("333"),"Loc Online tim nham ten");
        loc.setSelectedItem("Admin"); can(table.getRowCount()==1 && table.getValueAt(0,0).equals("10"),"Loc Admin tim nham ten");
        loc.setSelectedItem("Tất cả"); table.getRowSorter().toggleSortOrder(0);
        can(table.getValueAt(0,0).equals("10"),"Login so bi sort nhu so");
        table.getRowSorter().toggleSortOrder(4);
        can(table.getValueAt(0,4).equals("2"),"So thong ke khong sort theo so");
        table.setRowSelectionInterval(0,0); String id=b.chon().get(0);
        b.duLieu(rows);
        can(!b.chon().isEmpty() && b.chon().get(0).equals(id),"Refresh lam mat doi tuong dang chon");
        JScrollPane cuon=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,table);
        can(cuon.getColumnHeader()!=null && cuon.getColumnHeader().getView()==table.getTableHeader(),"Thieu header trong preview");
        BangQuanTri audit=new BangQuanTri(new String[]{"Giờ","Ai","Loại","Đích","Chi tiết"},new int[]{0,1,2,3,4},new String[0],s -> { },false);
        audit.duLieu(List.of(List.of("2026-11-18","admin","MUTE","x","Có chữ BAN"),List.of("2026-11-18","admin","BAN","x","")));
        tim(audit,JComboBox.class).setSelectedItem("BAN"); can(tim(audit,JTable.class).getRowCount()==1,"Loc audit tim nham chi tiet");
        System.out.println("PASS UiPreview M5: filter dung cot, sort login/so lon, audit theo loai va header");
    }
    // User mau du schema snapshot, co ten/gioi thieu danh lua loc toan dong cu.
    private static List<String> user(String id,String login,String ten,String vai,String state,String so) {
        return List.of(id,login,ten,vai,state,so,"0","2026-11-18","2026-11-18","0","0","admin online");
    }
    // Duyet component that, khong truy cap private/reflection.
    private static <T extends Component> T tim(Container c,Class<T> loai) {
        for (Component con:c.getComponents()) {
            if (loai.isInstance(con)) { return loai.cast(con); }
            if (con instanceof Container cha) { T k=tim(cha,loai); if (k!=null) { return k; } }
        }
        return null;
    }
    // Dung ngay khi component khong con dung quy tac.
    private static void can(boolean dung,String loi) { if (!dung) { throw new AssertionError(loi); } }
}
