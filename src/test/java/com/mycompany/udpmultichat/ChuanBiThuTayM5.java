package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.common.TruyenTep;
import com.mycompany.udpmultichat.server.KhoLuuTru;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Random;

/** Vai tro: Tao fixture thu tay M5 moi, khong mo mang/Swing. Ai goi: scripts/thu-tay-m5.ps1. Goi: KhoLuuTru, QuanLyNguoiDung. */
public final class ChuanBiThuTayM5 {
    // Khong khoi tao lop tien ich.
    private ChuanBiThuTayM5() { }

    // Chi tao data chua ton tai ben trong .selftest, khong ghi vao data that.
    // Che do: <duong-dan> --don => xoa thu muc data cu; <duong-dan> --moi => don roi tao moi.
    public static void main(String[] args) throws Exception {
        if (args.length<1) { throw new IllegalArgumentException("Cần đường dẫn data thử."); }
        String cheDoArg=args.length>=2?args[1]:"";
        Path goc=Path.of(".selftest").toAbsolutePath().normalize();
        // Chap nhan goc chua co, tu tao truoc khi lay realPath.
        Files.createDirectories(goc);
        goc=goc.toRealPath();
        Path data=Path.of(args[0]).toAbsolutePath().normalize();
        // Kiem tra data nam trong .selftest.
        boolean namTrong=false;
        try { namTrong=data.normalize().startsWith(goc); } catch (Exception ignored) { }
        if (!data.getFileName().toString().equals("data") || !namTrong) {
            throw new IllegalArgumentException("Chỉ thao tác trong .selftest; không được dùng data/ thật.");
        }
        boolean laDon=cheDoArg.equals("--don")||cheDoArg.equals("--moi");
        boolean laMoi=cheDoArg.equals("--moi")||cheDoArg.isEmpty();
        // Don thu muc cu neu co va duoc phep.
        if (laDon && Files.exists(data)) {
            don(data);
            System.out.println("Đã dọn fixture cũ: "+data);
        }
        if (!laMoi) { return; }
        // Khong ghi de data hien co.
        if (Files.exists(data,LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Data đã tồn tại. Dùng --moi để dọn rồi tạo lại, hoặc --don để chỉ dọn.");
        }
        Files.createDirectory(data);
        KhoLuuTru kho=new KhoLuuTru(data,s -> System.out.println(s));
        QuanLyNguoiDung nguoi=new QuanLyNguoiDung(kho);
        nguoi.dangKy("m5_a","abcdef123");
        nguoi.dangKy("m5_b","abcdef123");
        taoTep(data.getParent());
        System.out.println("Đã chuẩn bị "+nguoi.tongNguoiDung()+" tài khoản mẫu tại "+data);
    }

    // Xoa de quy thu muc fixture cu, chi trong .selftest.
    private static void don(Path thu) throws java.io.IOException {
        if (Files.isDirectory(thu)) {
            try (var ds=Files.newDirectoryStream(thu)) { for (Path con:ds) { don(con); } }
        }
        Files.deleteIfExists(thu);
    }

    // Tep biet truoc kich thuoc/CRC de so sau khi tai bang giao dien that; bo qua neu da co.
    private static void taoTep(Path cha) throws Exception {
        Path mau=cha.resolve("tep-mau-5MiB.bin");
        if (!Files.exists(mau)) {
            Random hat=new Random(20261113L); byte[] dem=new byte[8192];
            try (var ghi=Files.newOutputStream(mau,StandardOpenOption.CREATE_NEW)) {
                for (int i=0;i<640;i++) { hat.nextBytes(dem); ghi.write(dem); }
            }
        }
        Path txt=cha.resolve("tep-mau.txt"); Path prop=cha.resolve("tep-mau.properties");
        if (!Files.exists(txt)) { Files.writeString(txt,"Dữ liệu thử M5, không phải dữ liệu người dùng.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); }
        if (!Files.exists(prop)) { Files.writeString(prop,"size="+Files.size(mau)+"\ncrc="+TruyenTep.crc(mau)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); }
    }
}
