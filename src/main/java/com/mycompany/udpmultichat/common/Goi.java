package com.mycompany.udpmultichat.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Goi text UTF-8: escape dau gach dung, gach cheo va xuong dong. */
public final class Goi {
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
}
