package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.TepTin;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Tien ich xuat lich su tro chuyen phong ra tep van ban .txt chuan UTF-8.
 */
public final class XuatLichSuUtil {
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private XuatLichSuUtil() { }

    /** Mo hop thoai luu va ghi danh sach tin nhan cua phong ra tep .txt. */
    public static Path xuat(Component cha, Phong phong, List<TinNhan> danhSachTin) throws IOException {
        if (phong == null || danhSachTin == null) {
            return null;
        }
        String gio = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(Instant.now().atZone(ZoneId.systemDefault()));
        String tenGoiY = "lichsu_" + anToanTenTep(phong.ten) + "_" + gio + ".txt";

        JFileChooser chon = new JFileChooser();
        chon.setDialogTitle("Xuất lịch sử trò chuyện — " + phong.ten);
        chon.setSelectedFile(new File(tenGoiY));
        chon.setFileFilter(new FileNameExtensionFilter("Tệp văn bản (*.txt)", "txt"));

        if (chon.showSaveDialog(cha) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File f = chon.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".txt")) {
            f = new File(f.getParentFile(), f.getName() + ".txt");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\r\n");
        sb.append("LỊCH SỬ TRÒ CHUYỆN — UDP MULTI-ROOM CHAT\r\n");
        sb.append("Phòng: ").append(phong.ten).append(" (Mã phòng: ").append(phong.maPhong).append(")\r\n");
        sb.append("Thời điểm xuất: ").append(GIO_NGAY.format(Instant.now())).append("\r\n");
        sb.append("Tổng số tin nhắn: ").append(danhSachTin.size()).append("\r\n");
        sb.append("============================================================\r\n\r\n");

        for (TinNhan t : danhSachTin) {
            String thoiGian = GIO_NGAY.format(Instant.ofEpochMilli(t.thoiGian));
            if (t.thuHoi) {
                sb.append("[").append(thoiGian).append("] ").append(t.ten).append(": (Tin nhắn đã được thu hồi)\r\n");
                continue;
            }
            if ("Y".equals(t.loai)) {
                sb.append("[").append(thoiGian).append("] [HỆ THỐNG]: ").append(t.noiDung).append("\r\n");
                continue;
            }
            if ("S".equals(t.loai)) {
                sb.append("[").append(thoiGian).append("] ").append(t.ten).append(": [Sticker: ").append(StickerKho.ten(t.noiDung)).append("]\r\n");
                continue;
            }
            if ("F".equals(t.loai)) {
                try {
                    var tep = TepTin.doc(t.roomId, t.userId, t.noiDung, t.thoiGian);
                    sb.append("[").append(thoiGian).append("] ").append(t.ten).append(": [Tệp: ").append(tep.ten())
                            .append(" · ").append(TepTin.dungLuong(tep.kichThuoc())).append("]\r\n");
                } catch (Exception e) {
                    sb.append("[").append(thoiGian).append("] ").append(t.ten).append(": [Tệp tin]\r\n");
                }
                continue;
            }
            var trichDan = TrichDanUtil.phanTich(t.noiDung);
            String ndThat = TrichDanUtil.layNoiDungThat(t.noiDung);
            if (trichDan != null) {
                sb.append("[").append(thoiGian).append("] ").append(t.ten)
                        .append(" [Trả lời ").append(trichDan.ten()).append(": \"").append(trichDan.noiDung()).append("\"]: ")
                        .append(ndThat).append("\r\n");
            } else {
                sb.append("[").append(thoiGian).append("] ").append(t.ten).append(": ").append(ndThat).append("\r\n");
            }
        }

        Files.writeString(f.toPath(), sb.toString(), StandardCharsets.UTF_8);
        return f.toPath();
    }

    /** Mo file van ban vua xuat trong chuong trinh mac dinh. */
    public static void mo(Path p) {
        if (p == null) return;
        try {
            Desktop.getDesktop().open(p.toFile());
        } catch (Exception ignored) { }
    }

    private static String anToanTenTep(String ten) {
        return ten.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
