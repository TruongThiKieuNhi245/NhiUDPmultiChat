package com.mycompany.udpmultichat.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Vai tro: Goi text UTF-8: escape dau gach dung, gach cheo va xuong dong. Ai goi: ReliableUdp, ClientCore, ServerCore. Goi: Protocol, TinNhan, UTF-8. */
public final class Goi {
    // Khoi tao lop va noi cac thanh phan can thiet.
    private Goi() { }

    // ===== MA HOA =====
    public static String ghep(String... truong) {
        List<String> ketQua = new ArrayList<>();
        for (String giaTri : truong) {
            ketQua.add(giaTri.replace("\\", "\\\\").replace("|", "\\p")
                    .replace("\n", "\\n").replace("\r", "\\r"));
        }
        return String.join("|", ketQua);
    }

    // Tach truong, tu choi escape sai de khong hieu nham goi.
    public static String[] tach(String duLieu) {
        List<String> ketQua = new ArrayList<>();
        StringBuilder truong = new StringBuilder();
        boolean escape = false;
        for (char kyTu : duLieu.toCharArray()) {
            if (escape) {
                switch (kyTu) {
                    case 'p': truong.append('|'); break;
                    case 'n': truong.append('\n'); break;
                    case 'r': truong.append('\r'); break;
                    case '\\': truong.append('\\'); break;
                    default: throw new IllegalArgumentException("Escape không hợp lệ");
                }
                escape = false;
            } else if (kyTu == '\\') {
                escape = true;
            } else if (kyTu == '|') {
                ketQua.add(truong.toString());
                truong.setLength(0);
            } else {
                truong.append(kyTu);
            }
        }
        if (escape) { throw new IllegalArgumentException("Gói bị cắt escape"); }
        ketQua.add(truong.toString());
        return ketQua.toArray(String[]::new);
    }

    // Noi cac mang truong ma khong ghep text trung gian.
    public static String[] noi(String[]... cacMang) {
        List<String> ketQua = new ArrayList<>();
        for (String[] mang : cacMang) { ketQua.addAll(List.of(mang)); }
        return ketQua.toArray(String[]::new);
    }

    // Kiem tra kich thuoc sau khi ma hoa, khong dem theo so ky tu.
    public static byte[] thanhByte(String duLieu) {
        byte[] ketQua = duLieu.getBytes(StandardCharsets.UTF_8);
        if (ketQua.length > Protocol.GOI_TOI_DA) {
            throw new IllegalArgumentException("Gói vượt 1399 byte UTF-8; hãy rút ngắn nội dung");
        }
        return ketQua;
    }

    // Du phong ca envelope RESYNC va ACK; noi dung nhieu escape co the het cho som.
    public static void kiemTraTin(com.mycompany.udpmultichat.common.model.TinNhan tin) {
        if (tin.noiDung.isBlank()) { throw new IllegalArgumentException("Hãy nhập nội dung tin nhắn."); }
        if (tin.noiDung.getBytes(StandardCharsets.UTF_8).length>Protocol.TIN_TOI_DA) {
            throw new IllegalArgumentException(Protocol.LOI_TIN_DAI);
        }
        String sync=ghep(noi(new String[]{Protocol.MUC_DONG_BO,tin.roomId,"x".repeat(36)},tach(tin.dongGoi())));
        try { thanhByte(ghep("R","x".repeat(80),sync)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException(Protocol.LOI_TIN_DAI); }
    }
}
