package com.mycompany.udpmultichat.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Vai tro: Tao salt, token va bam SHA-256 bang API san co cua Java. Ai goi: QuanLyNguoiDung, QuanLyPhong, KhoLuuTru. Goi: MessageDigest, SecureRandom. */
public final class BaoMat {
    // ===== VAI TRO VA CAC THAO TAC =====
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();
    // Khoi tao lop va noi cac thanh phan can thiet.
    private BaoMat() { }

    // Tao chuoi ngau nhien cho salt hoac token phien.
    public static String ngauNhien(int soByte) {
        byte[] duLieu = new byte[soByte];
        NGAU_NHIEN.nextBytes(duLieu);
        return HexFormat.of().formatHex(duLieu);
    }

    // Bam salt va mat khau; file du lieu chi luu salt va ket qua bam.
    public static String bam(String salt, String matKhau) {
        try {
            byte[] duLieu = (salt + ":" + matKhau).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(duLieu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Java không hỗ trợ SHA-256", e);
        }
    }

    // So sanh hash voi thoi gian khong phu thuoc vi tri khac nhau.
    public static boolean dungMatKhau(String salt, String hash, String matKhau) {
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8),
                bam(salt, matKhau).getBytes(StandardCharsets.UTF_8));
    }
}
