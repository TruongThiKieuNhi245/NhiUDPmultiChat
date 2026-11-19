package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Vai tro: Menu kiem duyet va form ly do/thoi han. Ai goi: ThanhVienPanel, QuanTriFrame. Goi: QuanTriClient, FormQuanTri, Theme. */
final class KiemDuyetUI {
    // Khong khoi tao lop tien ich.
    private KiemDuyetUI() { }
    // ===== MENU VA XAC NHAN =====
    static JPopupMenu menu(Consumer<String> chon) {
        JPopupMenu m=new JPopupMenu();
        for (String l:new String[]{"Kick","Cấm chat","Gỡ cấm chat","Cấm vào phòng","Gỡ cấm vào","Chuyển chủ phòng"}) { JMenuItem i=new JMenuItem(l); if (l.equals("Kick") || l.equals("Cấm vào phòng")) { i.setForeground(Theme.LOI); } i.addActionListener(e -> chon.accept(l)); m.add(i); } return m;
    }
    // Mot form xac nhan cho ca chu phong va admin, Server luon kiem tra lai.
    static void thaoTac(Window cha,ClientCore core,String room,String id,String ten,String lenh,Consumer<String> loi) {
        FormQuanTri f=new FormQuanTri("Xác nhận",lenh.equals("Kick") || lenh.equals("Cấm vào phòng")); f.moTa(ten,"Thao tác: "+lenh+". Người dùng sẽ được cập nhật ngay khi Server xác nhận.");
        if (lenh.equals("Cấm chat")) { f.chon("han","Thời hạn","5 phút","5 phút","1 giờ","24 giờ","Vĩnh viễn"); }
        f.text("lydo","Lý do (tùy chọn)","").kiemTra(m -> m.get("lydo").length()>200?"lydo|Lý do tối đa 200 ký tự.":"");
        f.hien(cha,lenh,m -> {
            String action=switch (lenh) { case "Kick" -> "KICK"; case "Chuyển chủ phòng" -> "TRANSFER"; case "Cấm vào phòng","Gỡ cấm vào" -> "BAN"; default -> "MUTE"; };
            long giay=lenh.startsWith("Gỡ")?-1:switch (m.getOrDefault("han","Vĩnh viễn")) { case "5 phút" -> 300; case "1 giờ" -> 3600; case "24 giờ" -> 86400; default -> 0; };
            core.quanTri().kiemDuyet(action,room,id,giay,m.get("lydo")).whenComplete((v,e) -> SwingUtilities.invokeLater(() -> { if (e!=null) { f.loi(ClientCore.loiDeHieu(e)); } else { f.dong(); } }));
        });
    }
    // Bang con co ca nguoi bi ban/da roi de go cam duoc, cot Trong phong phan biet ro.
    static void thanhVien(Window cha,ClientCore core,String room,Consumer<String> loi) {
        JDialog d=new JDialog(cha,"Thành viên và kiểm duyệt",Dialog.ModalityType.MODELESS); final BangQuanTri[] ref=new BangQuanTri[1];
        String[] actions={"Kick","Cấm chat","Gỡ cấm chat","Cấm vào phòng","Gỡ cấm vào","Chuyển chủ phòng"};
        BangQuanTri b=new BangQuanTri(new String[]{"Tài khoản","Tên","Vai trò","Trong phòng","Mute đến","Ban đến"},new int[]{1,2,3,4,5,6},actions,l -> {
            var n=ref[0].chon(); if (!n.isEmpty()) { thaoTac(d,core,room,n.get(0),n.get(2),l,loi); }
        },false); ref[0]=b; d.setContentPane(b); d.setSize(1050,600); d.setLocationRelativeTo(cha); d.setVisible(true);
        Runnable nap=() -> core.quanTri().thanhVien(room).whenComplete((ds,e) -> SwingUtilities.invokeLater(() -> { if (e!=null) { b.trangThai(ClientCore.loiDeHieu(e),true); } else { b.duLieu(ds); } }));
        javax.swing.Timer timer=new javax.swing.Timer(4000,e -> nap.run()); timer.start(); d.addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosed(java.awt.event.WindowEvent e) { timer.stop(); } }); d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); nap.run();
    }
}
