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
    // ACK cu lap lai nhieu lan chi la cung mot bang chung, khong phai ba manh moi.
    static void ackCuLap() throws Exception {
        Path dir=Files.createTempDirectory(Path.of(".selftest"),"tep-ack-cu-");
        Path goc=dir.resolve("goc.bin"); Files.write(goc,new byte[32001]); UUID id=UUID.randomUUID();
        try (TruyenTep gui=new TruyenTep(0,0,1,8); DatagramSocket ban=new DatagramSocket()) {
            ban.setSoTimeout(3000);
            var f=gui.gui(id,new InetSocketAddress(InetAddress.getLoopbackAddress(),ban.getLocalPort()),goc,32001,TruyenTep.crc(goc),n -> { });
            boolean du=false;
            while (!du) {
                DatagramPacket p=new DatagramPacket(new byte[1100],1100); ban.receive(p);
                du=ByteBuffer.wrap(p.getData()).getInt(17)==31;
            }
            // Gia lap mat manh 0; ben nhan lien tuc phat lai dung mot SACK 31 manh.
            for (int i=0;i<12;i++) { ack(ban,gui.cong(),id,0,0xfffffffeL); Thread.sleep(25); }
            can(!f.isCompletedExceptionally(),"SACK cu lap da dot het retry8");
            ack(ban,gui.cong(),id,32,0);
            du=false;
            while (!du) {
                DatagramPacket p=new DatagramPacket(new byte[1100],1100); ban.receive(p);
                du=ByteBuffer.wrap(p.getData()).getInt(17)==32;
            }
            ack(ban,gui.cong(),id,33,0); can(f.get(3,TimeUnit.SECONDS).equals(goc),"Khong ket thuc sau ACK cuoi");
        }
    }
    // Tao ACK bitmap dung wire format de chu dong lap/dao ma khong doi engine loss.
    private static void ack(DatagramSocket s,int port,UUID id,int dau,long bits) throws Exception {
        byte[] g=ByteBuffer.allocate(TruyenTep.HEADER+8).put((byte)2).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).putInt(dau).putShort((short)8).putLong(bits).array();
        s.send(new DatagramPacket(g,g.length,InetAddress.getLoopbackAddress(),port));
    }
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
