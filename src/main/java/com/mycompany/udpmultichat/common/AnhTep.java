package com.mycompany.udpmultichat.common;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/** Vai tro: Tao thumbnail anh, tu choi anh hong/qua nhieu pixel. Ai goi: TepClient, TepSelfTest. Goi: ImageIO, Java2D. */
public final class AnhTep {
    // Khong khoi tao lop tien ich.
    private AnhTep() { }
    // ===== THUMBNAIL KHONG CHAN EDT =====
    public static Path tao(Path nguon,Path dich) {
        try (ImageInputStream in=ImageIO.createImageInputStream(nguon.toFile())) {
            if (in==null) { return null; }
            var ds=ImageIO.getImageReaders(in); if (!ds.hasNext()) { return null; }
            var doc=ds.next();
            try { doc.setInput(in); return thuNho(doc,dich); } finally { doc.dispose(); }
        } catch (IOException | RuntimeException e) { return null; }
    }
    // Doc subsampling de anh may anh lon khong chiem hang tram MB RAM.
    private static Path thuNho(javax.imageio.ImageReader doc,Path dich) throws IOException {
        int w=doc.getWidth(0),h=doc.getHeight(0);
        if (w<1 || h<1 || (long)w*h>100_000_000L) { return null; }
        var p=doc.getDefaultReadParam(); int giam=Math.max(1,Math.max(w,h)/480); p.setSourceSubsampling(giam,giam,0,0);
        BufferedImage goc=doc.read(0,p); if (goc==null) { return null; }
        double tiLe=Math.min(1,240.0/Math.max(goc.getWidth(),goc.getHeight()));
        int nw=Math.max(1,(int)(goc.getWidth()*tiLe)),nh=Math.max(1,(int)(goc.getHeight()*tiLe));
        BufferedImage anh=new BufferedImage(nw,nh,BufferedImage.TYPE_INT_RGB); Graphics2D g=anh.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,nw,nh); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(goc,0,0,nw,nh,null); g.dispose(); ImageIO.write(anh,"jpg",dich.toFile()); return dich;
    }
}
