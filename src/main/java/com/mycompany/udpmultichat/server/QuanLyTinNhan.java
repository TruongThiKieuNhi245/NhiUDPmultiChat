package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.util.*;

/** Vai tro: Lich su, loai trung ma Client, thu hoi va gioi han typing. Ai goi: QuanLyPhong, ServerCore. Goi: KhoLuuTru, Goi, TinNhan. */
public final class QuanLyTinNhan {
    private final KhoLuuTru kho;
    private final Map<String,List<TinNhan>> lichSu=new LinkedHashMap<>();
    private final Map<String,Map<String,TinNhan>> daGui=new HashMap<>();
    private final Map<String,Long> typing=new HashMap<>();
    // Lich su duoc nap truoc khi Server mo cong.
    public QuanLyTinNhan(KhoLuuTru kho) { this.kho=kho; }
    // ===== LICH SU VA LOAI TRUNG =====
    public void nap(String id) throws IOException {
        List<TinNhan> ds=kho.docTin(id); lichSu.put(id,ds); Map<String,TinNhan> ma=new HashMap<>();
        ds.stream().filter(t -> !t.maClient.isEmpty()).forEach(t -> ma.put(t.userId+":"+t.maClient,t)); daGui.put(id,ma);
    }
    // Tao phong chua co tin.
    public void tao(String id) { lichSu.put(id,new ArrayList<>()); daGui.put(id,new HashMap<>()); }
    // Bo tham chieu phong da xoa, khong xoa tep luu tru.
    public void xoa(String id) { lichSu.remove(id); daGui.remove(id); typing.keySet().removeIf(k -> k.startsWith(id+":")); }
    // Moc msgId van tang deu khi tin bi thu hoi.
    public long moc(String id) { return lichSu.get(id).size(); }
    // Tong tin gom ca tin thu hoi.
    public long tong() { return lichSu.values().stream().mapToLong(List::size).sum(); }
    // Ghi thanh cong moi them RAM, retry cung user/room/ma khong tao msgId moi.
    public TinNhan them(Phien p,String id,String nd,String ma,String loai) throws IOException {
        if (!ma.isEmpty() && !ma.matches("[A-Za-z0-9_-]{22}")) { throw new IllegalArgumentException("Mã tin nhắn không hợp lệ."); }
        if (!loai.equals("T") && !loai.equals("S") && !loai.equals("F")) { throw new IllegalArgumentException("Loại tin không hợp lệ."); }
        if (loai.equals("S") && !Protocol.stickerHopLe(nd)) { throw new IllegalArgumentException("Sticker không hỗ trợ."); }
        TinNhan cu=daGui.get(id).get(p.nguoi.id+":"+ma);
        if (!ma.isEmpty() && cu!=null) {
            if (!cu.thuHoi && (!cu.noiDung.equals(nd) || !cu.loai.equals(loai))) { throw new IllegalArgumentException("Mã tin đã dùng cho nội dung khác."); }
            return cu;
        }
        TinNhan t=new TinNhan(moc(id)+1,id,p.nguoi.id,p.nguoi.tenHienThi,nd,System.currentTimeMillis(),ma,loai);
        Goi.kiemTraTin(t); kho.ghiTin(t); lichSu.get(id).add(t);
        if (!ma.isEmpty()) { daGui.get(id).put(p.nguoi.id+":"+ma,t); } return t;
    }
    // Lich su 50 tin va tung lo RESYNC 50 tin.
    public List<TinNhan> lay(String id,boolean ganNhat,long sau) {
        List<TinNhan> ds=lichSu.get(id); int dau=ganNhat?Math.max(0,ds.size()-50):(int)Math.min(Math.max(0,sau),ds.size());
        return List.copyOf(ds.subList(dau,ganNhat?ds.size():Math.min(ds.size(),dau+50)));
    }
    // Tra dung msgId truoc khi thu hoi de don tep, tu choi id ngoai lich su.
    public TinNhan theoId(String room,long id) {
        var ds=lichSu.get(room); if (id<1 || id>ds.size()) { throw new IllegalArgumentException("Không tìm thấy tin nhắn."); } return ds.get((int)id-1);
    }
    // Khoa Client luu ben vung, giup gui lai tep sau mat ACK/restart khong tao tin trung.
    public TinNhan theoMa(String room,String user,String ma) { return daGui.get(room).get(user+":"+ma); }
    // Danh muc file con tin hop le, dung cho quyen GET va phuc hoi sau crash.
    public Set<String> cacTep() {
        Set<String> ids=new HashSet<>();
        for (var ds:lichSu.values()) { for (var t:ds) { if (t.loai.equals("F")) { ids.add(TepTin.doc(t.roomId,t.userId,t.noiDung,t.thoiGian).id()); } } }
        return ids;
    }
    // ===== THU HOI CO QUYEN VA LUU BEN VUNG =====
    public TinNhan thuHoi(Phien p,Phong r,long msgId) throws IOException {
        List<TinNhan> ds=lichSu.get(r.id);
        if (msgId<1 || msgId>ds.size()) { throw new IllegalArgumentException("Không tìm thấy tin nhắn."); }
        TinNhan t=ds.get((int)msgId-1);
        if (!duocThuHoi(p.nguoi,r,t,System.currentTimeMillis())) { throw new IllegalArgumentException("Bạn không có quyền thu hồi hoặc tin đã quá 10 phút."); }
        if (t.thuHoi) { return t; }
        TinNhan moi=t.daThuHoi(); List<TinNhan> ban=new ArrayList<>(ds); ban.set((int)msgId-1,moi);
        kho.ghiLaiTin(r.id,ban); ds.set((int)msgId-1,moi);
        if (!t.maClient.isEmpty()) { daGui.get(r.id).put(t.userId+":"+t.maClient,moi); } return moi;
    }
    // Ham tinh tach rieng de kiem tra moc 10 phut ma khong doi dong ho he thong.
    public static boolean duocThuHoi(NguoiDung p,Phong r,TinNhan t,long luc) {
        return QuanLyPhong.duocQuanLy(p,r) || (p.id.equals(t.userId) && luc-t.thoiGian<=Protocol.THU_HOI_MS && luc>=t.thoiGian);
    }
    // Revision bang tong so tin da thu hoi, nap lai van giu nguyen.
    public long phienBan(String id) { return lichSu.get(id).stream().filter(t -> t.thuHoi).count(); }
    // Tra tung lo tombstone 50 ID, khong gui lai noi dung cu.
    public String[] danhSachThuHoi(String id,long sau) {
        var ds=lichSu.get(id).stream().filter(t -> t.thuHoi && t.msgId>sau).limit(50).toList();
        long cuoi=ds.isEmpty()?moc(id):ds.get(ds.size()-1).msgId;
        boolean con=lichSu.get(id).stream().anyMatch(t -> t.thuHoi && t.msgId>cuoi);
        return Goi.noi(new String[]{Long.toString(phienBan(id)),Long.toString(cuoi),Boolean.toString(con)},ds.stream().map(t -> Long.toString(t.msgId)).toArray(String[]::new));
    }
    // ===== TYPING KHONG LUU TIN =====
    public boolean choTyping(String room,String user,long luc) {
        String k=room+":"+user; Long cu=typing.get(k);
        if (cu!=null && luc-cu<Protocol.TYPING_MS) { return false; }
        typing.entrySet().removeIf(e -> luc-e.getValue()>Protocol.TYPING_HIEN_MS); typing.put(k,luc); return true;
    }
}
