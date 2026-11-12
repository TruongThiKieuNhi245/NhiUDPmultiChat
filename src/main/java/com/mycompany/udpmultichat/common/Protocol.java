package com.mycompany.udpmultichat.common;

/** Vai tro: Hang so dung chung cho UDP dieu khien va multicast phong. Ai goi: common, client, server, ui. Goi: khong goi lop khac. */
public final class Protocol {
    public static final int CONG_SERVER = 5000;
    public static final int GOI_TOI_DA = 1399;
    public static final int CHO_ACK_MS = 500;
    public static final int GUI_LAI_TOI_DA = 3;
    public static final int PING_MS = 5000;
    public static final int HET_PHIEN_MS = 15000;
    public static final String SANH_ID = "sanh";
    public static final String SANH_NHOM = "239.255.42.1";
    public static final int SANH_CONG = 9001;
    public static final String DANG_KY = "REGISTER", DANG_NHAP = "LOGIN";
    public static final String PING = "PING", DANG_XUAT = "LOGOUT", GUI_TIN = "SEND";
    public static final String TRA_LOI = "RESPONSE", ONLINE = "ONLINE", TIN = "MSG";
    public static final String HE_THONG = "SYSTEM", HET_PHIEN = "EXPIRED";
    // ===== LENH PHONG VA DONG BO =====
    public static final String SAN_SANG = "READY";
    public static final String LAY_PHONG = "LIST";
    public static final String TAO_PHONG = "CREATE";
    public static final String SUA_PHONG = "UPDATE";
    public static final String XOA_PHONG = "DELETE";
    public static final String TRANG_THAI_PHONG = "STATE";
    public static final String CHUAN_BI_VAO = "PREPARE";
    public static final String HUY_VAO = "CANCEL";
    public static final String ROI_PHONG = "LEAVE";
    public static final String DA_ROI_PHONG = "ROOM_LEFT";
    public static final String LICH_SU = "HISTORY";
    public static final String DONG_BO = "RESYNC";
    public static final String MUC_DONG_BO = "SYNC_ITEM";
    public static final String HET_DONG_BO = "SYNC_END";
    public static final String MOC_TIN = "HEAD";
    public static final String DANH_SACH_PHONG = "ROOMS";
    public static final int TIN_TOI_DA = 1000;
    public static final String LOI_TIN_DAI = "Tin nhắn quá dài. Hãy rút gọn hoặc tách thành nhiều tin.";
    public static final String CHUYEN_CHU = "TRANSFER", KET_NOI_LAI = "RECONNECT", THU_SERVER = "HELLO";
    // Khoi tao lop va noi cac thanh phan can thiet.
    private Protocol() { }
}
