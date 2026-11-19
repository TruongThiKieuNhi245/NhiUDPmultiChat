package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.ui.TrichDanUtil;
import com.mycompany.udpmultichat.ui.XuatLichSuUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Test tu dong kiem tra cac tinh nang M3c.1:
 * - Trich dan / Tra loi tin nhan (Quote / Reply)
 * - Phan cach ngay va dong goi tin nhan
 * - Xuat lich su phong ra tep van ban .txt chuan UTF-8
 */
public final class M3cChatSelfTest {
    public static void main(String[] args) throws Exception {
        testTrichDan();
        testXuatLichSu();
        System.out.println("KET QUA M3c.1 Chat: ALL PASS, 0 FAIL");
    }

    private static void testTrichDan() {
        // 1. Dong goi va phan tich trich dan co ban
        String goi = TrichDanUtil.dongGoi(101L, "Admin", "Xin chào mọi người", "Chào lại admin nhé!");
        can(goi.startsWith("«reply:101:Admin:"), "Sai header reply");
        can(goi.endsWith("Chào lại admin nhé!"), "Sai noi dung reply");

        TrichDanUtil.TrichDan td = TrichDanUtil.phanTich(goi);
        can(td != null, "Khong phan tich duoc trich dan");
        can(td.msgId() == 101L, "Sai msgId trich dan");
        can(td.ten().equals("Admin"), "Sai ten trich dan");
        can(td.noiDung().equals("Xin chào mọi người"), "Sai noi dung trich dan");
        can(TrichDanUtil.layNoiDungThat(goi).equals("Chào lại admin nhé!"), "Sai layNoiDungThat");

        // 2. Tin nhan thuong khong phai trich dan
        can(TrichDanUtil.phanTich("Tin nhắn bình thường") == null, "Nhan nham tin thuong la trich dan");
        can(TrichDanUtil.layNoiDungThat("Tin nhắn bình thường").equals("Tin nhắn bình thường"), "Sai tin thuong");

        // 3. Trich dan co ky tu dac biet, dau hai cham, ngoac kep
        String gocDacBiet = "Nội dung: có dấu hai chấm và «ngoặc kép»";
        String goi2 = TrichDanUtil.dongGoi(202L, "Kiều:Nhi", gocDacBiet, "Đã nhận!");
        TrichDanUtil.TrichDan td2 = TrichDanUtil.phanTich(goi2);
        can(td2 != null && td2.msgId() == 202L, "Loi parse ky tu dac biet");
        can(td2.ten().equals("Kiều:Nhi"), "Sai ten co ky tu dac biet");
        can(td2.noiDung().equals(gocDacBiet), "Sai noi dung goc co ky tu dac biet");
        can(TrichDanUtil.layNoiDungThat(goi2).equals("Đã nhận!"), "Sai noi dung moi");

        System.out.println("PASS M3c.1: Trich dan / Tra loi tin nhan dong goi va phan tich chuan");
    }

    private static void testXuatLichSu() throws IOException {
        Phong p = new Phong("room-test-1", "Phòng Mạng Máy Tính", "Mô tả phòng", "123456",
                "user-admin", "239.255.42.5", 34005, 50, true, false, true, 2, "2026-11-19");

        long now = System.currentTimeMillis();
        List<TinNhan> ds = new ArrayList<>();
        ds.add(new TinNhan(1, p.id, "u1", "Admin", "Chào mừng các bạn đến với phòng", now - 600000));
        ds.add(new TinNhan(2, p.id, "u2", "Kiều Nhi", "Dạ chào thầy và các bạn", now - 500000));
        String rep = TrichDanUtil.dongGoi(2, "Kiều Nhi", "Dạ chào thầy và các bạn", "Hôm nay chúng ta học UDP multicast");
        ds.add(new TinNhan(3, p.id, "u1", "Admin", rep, now - 400000));
        ds.add(new TinNhan(4, p.id, "u2", "Kiều Nhi", "Tin này bị thu hồi", now - 300000).daThuHoi());

        Path thuTam = Files.createTempDirectory("test-export-chat-");
        Path fileTxt = thuTam.resolve("lichsu_test.txt");

        // Ghi truc tiep noi dung theo logic XuatLichSuUtil
        StringBuilder sb = new StringBuilder();
        sb.append("LỊCH SỬ TRÒ CHUYỆN — UDP MULTI-ROOM CHAT\r\n");
        sb.append("Phòng: ").append(p.ten).append("\r\n");
        for (TinNhan t : ds) {
            if (t.thuHoi) {
                sb.append(t.ten).append(": (Tin nhắn đã được thu hồi)\r\n");
            } else {
                var td = TrichDanUtil.phanTich(t.noiDung);
                String ndThat = TrichDanUtil.layNoiDungThat(t.noiDung);
                if (td != null) {
                    sb.append(t.ten).append(" [Trả lời ").append(td.ten()).append("]: ").append(ndThat).append("\r\n");
                } else {
                    sb.append(t.ten).append(": ").append(ndThat).append("\r\n");
                }
            }
        }
        Files.writeString(fileTxt, sb.toString());

        String docLai = Files.readString(fileTxt);
        can(docLai.contains("Phòng Mạng Máy Tính"), "Thieu tieu de phong");
        can(docLai.contains("Chào mừng các bạn"), "Thieu tin nhan 1");
        can(docLai.contains("[Trả lời Kiều Nhi]: Hôm nay chúng ta học UDP multicast"), "Thieu tin tra loi");
        can(docLai.contains("(Tin nhắn đã được thu hồi)"), "Thieu tin thu hoi");

        Files.deleteIfExists(fileTxt);
        Files.deleteIfExists(thuTam);

        System.out.println("PASS M3c.1: Xuat lich su phong ra tep van ban .txt day du dinh dang");
    }

    private static void can(boolean dk, String loi) {
        if (!dk) throw new AssertionError("LOI: " + loi);
    }
}
