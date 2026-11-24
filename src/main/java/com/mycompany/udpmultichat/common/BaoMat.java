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
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + ":" + matKhau).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    // Web dung KDF cham; desktop giu bam cu de tuong thich timeout UDP hien co.
    public static String bamHienDai(String salt, String matKhau) {
        return "pbkdf2$210000$" + pbkdf2(salt, matKhau);
    }
    private static String pbkdf2(String salt, String password) {
        var spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 210000, 256);
        try {
            return HexFormat.of().formatHex(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    // Doc hash SHA-256 cu de nhap tu ban sao desktop; hash moi dung PBKDF2.
    public static boolean dungMatKhau(String salt, String hash, String matKhau) {
        String expected;
        if (hash.startsWith("pbkdf2$210000$")) { expected = bamHienDai(salt, matKhau); }
        else {
            try { expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + ":" + matKhau).getBytes(StandardCharsets.UTF_8))); }
            catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        }
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
