package com.mycompany.udpmultichat.common.model;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.Protocol;

/** Vai tro: Tin do Server gan so thu tu va thoi gian; moi goi deu co roomId. Ai goi: ClientCore, QuanLyPhong, ui. Goi: Goi, Protocol. */
public final class TinNhan {
    // ===== VAI TRO VA CAC THAO TAC =====
    public final long msgId, thoiGian;
    public final String roomId, userId, ten, noiDung;

    // Khoi tao lop va noi cac thanh phan can thiet.
    public TinNhan(long msgId, String roomId, String userId, String ten, String noiDung, long thoiGian) {
        this.msgId = msgId;
        this.roomId = roomId;
        this.userId = userId;
        this.ten = ten;
        this.noiDung = noiDung;
        this.thoiGian = thoiGian;
    }

    // Ma hoa tin de phat multicast.
    public String dongGoi() {
        return Goi.ghep(Protocol.TIN, roomId, Long.toString(msgId), userId, ten,
                Long.toString(thoiGian), noiDung);
    }

    // Doc goi tin da duoc kiem tra roomId.
    public static TinNhan tuTruong(String[] truong) {
        if (truong.length != 7) { throw new IllegalArgumentException("Tin sai dinh dang"); }
        return new TinNhan(Long.parseLong(truong[2]), truong[1], truong[3], truong[4],
                truong[6], Long.parseLong(truong[5]));
    }
}
