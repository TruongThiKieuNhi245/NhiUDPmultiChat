package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.common.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Kiem tra DATA nhi phan trung/dao thu tu, huy va timeout. Ai goi: TepSelfTest. Goi: TruyenTep, DatagramSocket. */
final class TepMangSelfTest {
    // Khong khoi tao lop test.
    private TepMangSelfTest() { }
    // ===== LOI MANG CO KIEM SOAT =====
    @SuppressWarnings("try") // Can dong som de kiem tra mat ket noi, sau do try van don tai nguyen.
    static void chay() throws Exception {
        Path dir=Files.createTempDirectory(Path.of(".selftest"),"tep-mang-"); byte[] data=new byte[2501]; new Random(20261113).nextBytes(data);
        Path goc=dir.resolve("goc.bin"); Files.write(goc,data); long crc=TruyenTep.crc(goc);
        try (TruyenTep nhan=new TruyenTep(0,0,1,8); DatagramSocket gui=new DatagramSocket()) {
            var ban=new InetSocketAddress(InetAddress.getLoopbackAddress(),gui.getLocalPort()); UUID id=UUID.randomUUID(); Path dich=dir.resolve("dao.part");
            var f=nhan.nhan(id,ban,dich,data.length,crc,n -> { });
            for (int so:new int[]{2,0,2,1,0}) { manh(gui,nhan.cong(),id,so,data); }
            can(Files.mismatch(goc,f.get(5,TimeUnit.SECONDS))==-1,"Dao thu tu/trung lam sai tep");
            UUID sai=UUID.randomUUID(); Path tam=dir.resolve("sai.part"); var loi=nhan.nhan(sai,ban,tam,data.length,crc+1,n -> { });
            for (int i=0;i<3;i++) { manh(gui,nhan.cong(),sai,i,data); } biChan(loi); can(!Files.exists(tam),"CRC sai con tam");
            UUID huy=UUID.randomUUID(); Path huyTam=dir.resolve("huy.part"); var h=nhan.nhan(huy,ban,huyTam,data.length,crc,n -> { }); nhan.huy(huy); biChan(h); can(!Files.exists(huyTam),"Huy con tam");
            UUID het=UUID.randomUUID(); Path hetTam=dir.resolve("timeout.part"); var timeout=nhan.nhan(het,ban,hetTam,data.length,crc,n -> { });
            try { timeout.get(70,TimeUnit.SECONDS); throw new AssertionError("Mat ket noi khong timeout"); } catch (ExecutionException dung) { can(dung.getCause().getMessage().contains("thời gian"),"Loi timeout khong ro"); }
            can(!Files.exists(hetTam),"60 giay con tep tam");
        }
        try (TruyenTep gui=new TruyenTep(0,1,2,0); TruyenTep nhan=new TruyenTep(0,0,3,8)) {
            UUID id=UUID.randomUUID(); var x=gui.gui(id,new InetSocketAddress(InetAddress.getLoopbackAddress(),nhan.cong()),goc,data.length,crc,n -> { });
            biChan(x); can(Files.exists(goc),"Loi gui xoa tep nguon");
            UUID id2=UUID.randomUUID(); Path tam=dir.resolve("dong.part"); var y=nhan.nhan(id2,new InetSocketAddress(InetAddress.getLoopbackAddress(),gui.cong()),tam,data.length,crc,n -> { });
            nhan.close(); biChan(y); can(!Files.exists(tam),"Dong Server con tam");
        }
    }
    // Tu tao DATA de chung minh engine ghi theo so manh thay vi thu tu den.
    private static void manh(DatagramSocket s,int port,UUID id,int so,byte[] data) throws Exception {
        int dau=so*TruyenTep.MANH,n=Math.min(TruyenTep.MANH,data.length-dau);
        byte[] g=ByteBuffer.allocate(TruyenTep.HEADER+n).put((byte)1).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).putInt(so).putShort((short)n).put(data,dau,n).array();
        s.send(new DatagramPacket(g,g.length,InetAddress.getLoopbackAddress(),port));
    }
}
