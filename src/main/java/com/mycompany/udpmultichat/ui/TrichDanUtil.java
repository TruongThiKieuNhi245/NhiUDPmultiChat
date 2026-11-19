package com.mycompany.udpmultichat.ui;

/**
 * Tien ich dong goi va phan tich trich dan / tra loi tin nhan (Reply/Quote).
 */
public final class TrichDanUtil {
    private TrichDanUtil() { }

    /** Thong tin trich dan tin nhan. */
    public record TrichDan(long msgId, String ten, String noiDung) { }

    /** Dong goi noi dung tra loi kem the trich dan. */
    public static String dongGoi(long msgId, String ten, String noiDungGoc, String noiDungMoi) {
        String tomTat = rutGon(noiDungGoc, 80);
        String tenGoc = rutGon(ten, 30);
        return "«reply:" + msgId + ":" + maHoa(tenGoc) + ":" + maHoa(tomTat) + "»" + noiDungMoi;
    }

    /** Phan tich tin nhan xem co chua trich dan khong. */
    public static TrichDan phanTich(String noiDung) {
        if (noiDung == null || !noiDung.startsWith("«reply:")) {
            return null;
        }
        int dong = noiDung.indexOf("»");
        if (dong <= 7) {
            return null;
        }
        String the = noiDung.substring(7, dong);
        String[] parts = the.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        try {
            long id = Long.parseLong(parts[0]);
            String ten = giaiMa(parts[1]);
            String nd = giaiMa(parts[2]);
            return new TrichDan(id, ten, nd);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lay noi dung that cua tin nhan (bo qua the trich dan). */
    public static String layNoiDungThat(String noiDung) {
        if (noiDung == null) {
            return "";
        }
        if (noiDung.startsWith("«reply:")) {
            int dong = noiDung.indexOf("»");
            if (dong > 0) {
                return noiDung.substring(dong + 1);
            }
        }
        return noiDung;
    }

    private static String rutGon(String s, int max) {
        if (s == null) return "";
        String clean = s.replace('\n', ' ').replace('\r', ' ').strip();
        return clean.length() <= max ? clean : clean.substring(0, max) + "…";
    }

    private static String maHoa(String s) {
        return s.replace("%", "%25").replace(":", "%3A").replace("»", "%BB").replace("«", "%AB");
    }

    private static String giaiMa(String s) {
        return s.replace("%BB", "»").replace("%AB", "«").replace("%3A", ":").replace("%25", "%");
    }
}
