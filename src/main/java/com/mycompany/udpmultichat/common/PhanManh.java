package com.mycompany.udpmultichat.common;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.BitSet;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/** Vai tro: Xu ly anh va ghep manh co gioi han, CRC32. Ai goi: HoSoClient, QuanLyHoSo, test. Goi: ImageIO, CRC32. */
public final class PhanManh {
    // ===== DOC ANH AN TOAN =====
    // Lop chi cung cap ham tinh.
    private PhanManh() { }
    // Chi chap nhan PNG/JPG, kiem tra kich thuoc truoc khi giai nen.
    public static byte[] chuanHoa(Path tep) throws IOException {
        String ten=tep.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!ten.matches(".+\\.(png|jpg|jpeg)")) { throw new IOException("Chỉ chọn ảnh PNG hoặc JPG."); }
        if (Files.size(tep)>Protocol.ANH_DAU_VAO) { throw new IOException("Ảnh tối đa 2 MB."); }
        byte[] data;
        try (var in=Files.newInputStream(tep)) { data=in.readNBytes(Protocol.ANH_DAU_VAO+1); }
        BufferedImage goc=docAnh(data,false);
        int canh=Math.min(goc.getWidth(),goc.getHeight());
        BufferedImage anh=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);
        var g=anh.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int x=(goc.getWidth()-canh)/2,y=(goc.getHeight()-canh)/2;
        g.drawImage(goc,0,0,96,96,x,y,x+canh,y+canh,null); g.dispose();
        ByteArrayOutputStream out=new ByteArrayOutputStream(); ImageIO.write(anh,"png",out); return out.toByteArray();
    }
    // Server chi chap nhan anh da chuan hoa; khong tin duoi tep tu Client.
    public static BufferedImage docAnh(byte[] data,boolean daChuanHoa) throws IOException {
        int max=daChuanHoa?Protocol.ANH_PNG_TOI_DA:Protocol.ANH_DAU_VAO;
        if (data.length==0 || data.length>max) { throw new IOException("Kích thước ảnh không hợp lệ."); }
        try (var in=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var ds=ImageIO.getImageReaders(in);
            if (!ds.hasNext()) { throw new IOException("Tệp không phải ảnh PNG hoặc JPG."); }
            var doc=ds.next();
            try {
                doc.setInput(in); String loai=doc.getFormatName(); int w=doc.getWidth(0),h=doc.getHeight(0);
                if (!loai.matches("(?i)png|jpeg|jpg") || w<1 || h<1 || (long)w*h>16000000) { throw new IOException("Ảnh không hợp lệ hoặc quá lớn."); }
                if (daChuanHoa && (!loai.equalsIgnoreCase("png") || w!=96 || h!=96)) { throw new IOException("Avatar phải là PNG 96 × 96."); }
                return doc.read(0);
            } finally { doc.dispose(); }
        }
    }
    // ===== CHIA VA GHEP MANH =====
    // CRC32 kiem tra toan bo du lieu sau khi ghep.
    public static String crc(byte[] data) { CRC32 c=new CRC32(); c.update(data); return Long.toHexString(c.getValue()); }
    // Ma hoa tung manh 600 byte, khong ma hoa ca tep vao mot goi.
    public static String manh(byte[] data,int i) {
        if (i<0 || i>=(data.length+Protocol.MANH_ANH-1)/Protocol.MANH_ANH) { throw new IllegalArgumentException("Mảnh ảnh không hợp lệ."); }
        int dau=i*Protocol.MANH_ANH;
        if (dau<0 || dau>=data.length) { throw new IllegalArgumentException("Mảnh ảnh không hợp lệ."); }
        return Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(data,dau,Math.min(dau+Protocol.MANH_ANH,data.length)));
    }
    /** Bo dem co kich thuoc co dinh, cho phep nhan lai cung mot manh. */
    public static final class BoGhep {
        private final byte[] data;
        private final BitSet daNhan=new BitSet();
        private final String crc;
        // Cap phat toi da 64 KB cho mot avatar dang truyen.
        public BoGhep(int soByte,String crc) {
            if (soByte<1 || soByte>Protocol.ANH_PNG_TOI_DA || !crc.matches("[a-f0-9]{1,8}")) { throw new IllegalArgumentException("Kích thước hoặc mã kiểm tra ảnh không hợp lệ."); }
            data=new byte[soByte]; this.crc=crc;
        }
        // Sao chep manh dung vi tri, kiem tra do dai chinh xac.
        public synchronized void nhan(int i,String base64) {
            if (i<0 || i>=(data.length+Protocol.MANH_ANH-1)/Protocol.MANH_ANH) { throw new IllegalArgumentException("Mảnh ảnh không hợp lệ."); }
            byte[] m=Base64.getDecoder().decode(base64); int dau=i*Protocol.MANH_ANH;
            if (i<0 || dau<0 || dau>=data.length || m.length!=Math.min(Protocol.MANH_ANH,data.length-dau)) { throw new IllegalArgumentException("Mảnh ảnh không hợp lệ."); }
            System.arraycopy(m,0,data,dau,m.length); daNhan.set(i);
        }
        // Chi tra du lieu khi du manh va CRC trung khop.
        public synchronized byte[] ketQua() throws IOException {
            if (daNhan.cardinality()!=(data.length+Protocol.MANH_ANH-1)/Protocol.MANH_ANH || !PhanManh.crc(data).equals(crc)) { throw new IOException("Ảnh chưa đủ mảnh hoặc sai mã kiểm tra. Hãy thử lại."); }
            return data.clone();
        }
    }
}
