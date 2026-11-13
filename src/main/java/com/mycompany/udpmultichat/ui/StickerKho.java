package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.StickerDanhMuc;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Vai tro: Nap PNG Noto Emoji va bang chon bon nhom. Ai goi: HangTin, SoanTinPanel, UiPreview. Goi: StickerDanhMuc, ImageIO, Theme. */
final class StickerKho {
    private static final Map<String,BufferedImage> ANH=new HashMap<>();
    // Kho tai nguyen co so luong co dinh, khong nap anh tu mang.
    private StickerKho() { }
    // ===== DOC ANH VA THU PHONG =====
    static String ten(String ma) {
        var m=StickerDanhMuc.tim(StickerDanhMuc.maCu(ma)); return m==null?"Sticker không hỗ trợ":m.ten();
    }
    // Ma cu chi dung doc lich su, khong gui moi len Server.
    static synchronized BufferedImage anh(String ma) {
        String hex=StickerDanhMuc.maCu(ma); if (StickerDanhMuc.tim(hex)==null) { return null; }
        return ANH.computeIfAbsent(hex,k -> {
            try (var in=StickerKho.class.getResourceAsStream("/stickers/"+k+".png")) { return in==null?null:ImageIO.read(in); }
            catch (java.io.IOException e) { return null; }
        });
    }
    // Bicubic va giu ti le, khong dung font emoji hay ve lai hinh goc.
    static Icon icon(String ma,int canh) {
        BufferedImage goc=anh(ma); if (goc==null) { return null; }
        BufferedImage nho=new BufferedImage(canh,canh,BufferedImage.TYPE_INT_ARGB); Graphics2D g=nho.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double tiLe=Math.min((double)canh/goc.getWidth(),(double)canh/goc.getHeight());
        int w=(int)Math.round(goc.getWidth()*tiLe),h=(int)Math.round(goc.getHeight()*tiLe);
        g.drawImage(goc,(canh-w)/2,(canh-h)/2,w,h,null); g.dispose(); return new ImageIcon(nho);
    }
    // ===== BANG CHON VA TIM NHANH =====
    static JPopupMenu bangChon(Consumer<String> chon) {
        JPopupMenu menu=new JPopupMenu(); JPanel noiDung=new JPanel(new BorderLayout(0,Theme.S8)); noiDung.setBorder(Theme.le(Theme.S8));
        JTextField tim=new JTextField(); tim.putClientProperty("JTextField.placeholderText","Tìm sticker...");
        JTabbedPane tab=new JTabbedPane(); tab.putClientProperty("JTabbedPane.tabType","underlined");
        Runnable loc=() -> {
            tab.removeAll();
            for (int nhom=0;nhom<StickerDanhMuc.NHOM.length;nhom++) { tab.addTab(StickerDanhMuc.NHOM[nhom],luoi(nhom,tim.getText(),menu,chon)); }
        };
        tim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loc.run(); }
            public void removeUpdate(DocumentEvent e) { loc.run(); }
            public void changedUpdate(DocumentEvent e) { loc.run(); }
        });
        loc.run(); noiDung.add(tim,BorderLayout.NORTH); noiDung.add(tab); menu.add(noiDung); return menu;
    }
    // Moi nhom chin anh, tim kiem khong lam phat sinh tai nguyen moi.
    private static JPanel luoi(int nhom,String tim,JPopupMenu menu,Consumer<String> chon) {
        JPanel p=new JPanel(new GridLayout(3,3,Theme.S4,Theme.S4)); p.setPreferredSize(new Dimension(384,300));
        String khoa=java.text.Normalizer.normalize(tim,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        for (var m:StickerDanhMuc.CAC_MUC) {
            String ten=java.text.Normalizer.normalize(m.ten(),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
            if (m.nhom()!=nhom || !ten.contains(khoa)) { continue; }
            JButton b=Theme.nutNhe("",""); b.setIcon(icon(m.ma(),64)); b.setToolTipText(m.ten());
            b.getAccessibleContext().setAccessibleName(m.ten()); b.addActionListener(e -> { menu.setVisible(false); chon.accept(m.ma()); }); p.add(b);
        }
        return p;
    }
}
