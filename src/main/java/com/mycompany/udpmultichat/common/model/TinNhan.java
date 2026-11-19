package com.mycompany.udpmultichat.common.model;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.Protocol;

/** Vai tro: Tin do Server gan so thu tu va thoi gian; moi goi deu co roomId. Ai goi: ClientCore, QuanLyPhong, ui. Goi: Goi, Protocol. */
public final class TinNhan {
    // ===== VAI TRO VA CAC THAO TAC =====
    public final long msgId, thoiGian;
    public final String roomId, userId, ten, noiDung,maClient,loai;
    public final boolean thuHoi;

    // Khoi tao lop va noi cac thanh phan can thiet.
    public TinNhan(long msgId, String roomId, String userId, String ten, String noiDung, long thoiGian) {
        this(msgId,roomId,userId,ten,noiDung,thoiGian,"","T");
    }
    // Ma Client la 128-bit Base64url; loai T=van ban, S=sticker, D=thu hoi.
    public TinNhan(long id,String room,String user,String ten,String nd,long luc,String ma,String loai) {
        msgId=id; roomId=room; userId=user; this.ten=ten; thoiGian=luc; maClient=ma; this.loai=loai;
        thuHoi=loai.equals("D"); noiDung=thuHoi?"":nd;
    }
    // Thu hoi xoa noi dung trong ban sao, khong de noi dung cu trong goi/lich su.
    public TinNhan daThuHoi() { return new TinNhan(msgId,roomId,userId,ten,"",thoiGian,maClient,"D"); }
    // Tao ma gui lai on dinh, khong phu thuoc seq cua transport.
    public static String taoMa() {
        java.util.UUID u=java.util.UUID.randomUUID();
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array());
    }

    // Ma hoa tin de phat multicast.
    public String dongGoi() {
        return Goi.ghep(Protocol.TIN, roomId, Long.toString(msgId), userId, ten,
                Long.toString(thoiGian), (maClient.isEmpty() && !thuHoi)?noiDung:"~"+maClient+"~"+loai+"~"+noiDung);
    }

    // Doc goi tin da duoc kiem tra roomId.
    public static TinNhan tuTruong(String[] truong) {
        if (truong.length != 7) { throw new IllegalArgumentException("Tin sai dinh dang"); }
        String nd=truong[6],ma="",loai="T";
        if (nd.matches("(?s)~(?:[A-Za-z0-9_-]{22}~[TSFDY]|~D)~.*")) {
            int i=nd.indexOf('~',1); ma=nd.substring(1,i); loai=nd.substring(i+1,i+2); nd=nd.substring(i+3);
        }
        return new TinNhan(Long.parseLong(truong[2]),truong[1],truong[3],truong[4],nd,Long.parseLong(truong[5]),ma,loai);
    }
}
