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
    public static void main(String[] args) throws Exception {
        if (args.length!=1) { throw new IllegalArgumentException("Cần đường dẫn data thử mới."); }
        Path goc=Path.of(".selftest").toAbsolutePath().normalize().toRealPath();
        Path data=Path.of(args[0]).toAbsolutePath().normalize();
        Path cha=data.getParent().toRealPath();
        if (!data.getFileName().toString().equals("data") || !cha.startsWith(goc) || Files.exists(data,LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Chỉ tạo data mới trong .selftest; không ghi đè dữ liệu.");
        }
        Files.createDirectory(data);
        KhoLuuTru kho=new KhoLuuTru(data,s -> System.out.println(s));
        QuanLyNguoiDung nguoi=new QuanLyNguoiDung(kho);
        nguoi.dangKy("m5_a","abcdef123");
        nguoi.dangKy("m5_b","abcdef123");
        taoTep(cha);
        System.out.println("Đã chuẩn bị "+nguoi.tongNguoiDung()+" tài khoản mẫu tại "+data);
    }

    // Tep biet truoc kich thuoc/CRC de so sau khi tai bang giao dien that.
    private static void taoTep(Path cha) throws Exception {
        Path mau=cha.resolve("tep-mau-5MiB.bin");
        Random hat=new Random(20261113L); byte[] dem=new byte[8192];
        try (var ghi=Files.newOutputStream(mau,StandardOpenOption.CREATE_NEW)) {
            for (int i=0;i<640;i++) { hat.nextBytes(dem); ghi.write(dem); }
        }
        Files.writeString(cha.resolve("tep-mau.txt"),"Dữ liệu thử M5, không phải dữ liệu người dùng.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        Files.writeString(cha.resolve("tep-mau.properties"),"size="+Files.size(mau)+"\ncrc="+TruyenTep.crc(mau)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
}
