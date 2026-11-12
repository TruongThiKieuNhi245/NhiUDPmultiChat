package com.mycompany.udpmultichat.common;

/** Hang so dung chung cho UDP dieu khien va multicast phong. */
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
    private Protocol() { }
}
