package com.mycompany.udpmultichat.common;

import java.util.List;

/** Vai tro: Danh muc PNG Noto Emoji dung chung. Ai goi: Protocol, StickerKho, SelfTest. Goi: khong goi mang/UI. */
public final class StickerDanhMuc {
    public record Muc(String ma,String ten,int nhom) { }
    public static final String[] NHOM={"Cảm xúc","Cử chỉ","Ăn uống - đồ vật","Chúc mừng"};
    public static final List<Muc> CAC_MUC=List.of(
        new Muc("1f600","Vui vẻ",0),
        new Muc("1f602","Cười ra nước mắt",0),
        new Muc("1f60d","Mê mẩn",0),
        new Muc("1f622","Buồn",0),
        new Muc("1f621","Giận",0),
        new Muc("1f62e","Ngạc nhiên",0),
        new Muc("1f634","Ngủ ngon",0),
        new Muc("1f60e","Tự tin",0),
        new Muc("1f914","Suy nghĩ",0),
        new Muc("1f44d","Đồng ý",1),
        new Muc("1f44e","Không đồng ý",1),
        new Muc("1f44f","Vỗ tay",1),
        new Muc("1f64c","Hoan hô",1),
        new Muc("1f64f","Cảm ơn",1),
        new Muc("1f44b","Xin chào",1),
        new Muc("1f4aa","Cố lên",1),
        new Muc("270c","Chiến thắng",1),
        new Muc("1f91d","Bắt tay",1),
        new Muc("1f355","Pizza",2),
        new Muc("2615","Cà phê",2),
        new Muc("1f354","Bánh mì kẹp",2),
        new Muc("1f35f","Khoai tây chiên",2),
        new Muc("1f370","Bánh ngọt",2),
        new Muc("1f366","Kem",2),
        new Muc("1f4da","Sách",2),
        new Muc("1f4bb","Máy tính",2),
        new Muc("1f381","Quà tặng",2),
        new Muc("1f389","Chúc mừng",3),
        new Muc("1f38a","Tung hoa",3),
        new Muc("1f386","Pháo hoa",3),
        new Muc("1f387","Pháo sáng",3),
        new Muc("1f388","Bóng bay",3),
        new Muc("1f382","Sinh nhật",3),
        new Muc("1f3c6","Cúp vô địch",3),
        new Muc("2b50","Ngôi sao",3),
        new Muc("2764","Yêu thương",3));
    // Khong khoi tao kho hang so.
    private StickerDanhMuc() { }
    // ===== TIM KIEM DANH MUC =====
    public static Muc tim(String ma) { return CAC_MUC.stream().filter(m -> m.ma().equals(ma)).findFirst().orElse(null); }
    // Ho tro lich su cu; Server chi nhan ma hex moi.
    public static String maCu(String ma) {
        return switch (ma) {
            case "vui" -> "1f600"; case "tim" -> "2764"; case "like" -> "1f44d"; case "votay" -> "1f44f";
            case "cuoi" -> "1f602"; case "nga" -> "1f62e"; case "buon" -> "1f622"; case "gian" -> "1f621";
            case "ngu" -> "1f634"; case "phao" -> "1f386"; case "pizza" -> "1f355"; case "cafe" -> "2615"; default -> ma;
        };
    }
}
