package com.mycompany.udpmultichat.transport;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.WebChatService;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hai socket loopback dung chung; chi nhanh receive goi nghiep vu va phat tin. */
public final class UdpChatTransport implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(UdpChatTransport.class);
    private final WebChatService service;
    private final Delivery delivery;
    private final Object wireOrder = new Object();
    private final DatagramSocket sender;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor timer = createTimer();
    private static ScheduledThreadPoolExecutor createTimer() {
        var executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "web-udp-retry"); t.setDaemon(true); return t;
        });
        executor.setRemoveOnCancelPolicy(true); return executor;
    }
    private volatile DatagramSocket receiver;
    private volatile InetSocketAddress destination;
    private volatile boolean closed;
    private volatile Consumer<TinNhan> committed = t -> { };
    private volatile long lastReceived;

    public UdpChatTransport(WebChatService service) throws SocketException {
        this(service, DatagramSocket::send);
    }
    // Seam package-private chi de fixture dieu khien mat/trung/dao thu tu tren socket that.
    UdpChatTransport(WebChatService service, Delivery delivery) throws SocketException {
        this.service = service; this.delivery = delivery;
        sender = socket();
        try { resumeReceiver(); }
        catch (SocketException e) { sender.close(); timer.shutdownNow(); throw e; }
    }
    private static DatagramSocket socket() throws SocketException {
        return new DatagramSocket(new InetSocketAddress("127.0.0.1", 0));
    }
    public void onCommitted(Consumer<TinNhan> listener) { committed = listener; }

    public CompletableFuture<WebChatService.Accepted> send(String token,String room,String id,String text) {
        return send(token,room,id,text,"T",0);
    }
    public CompletableFuture<WebChatService.Accepted> send(String token,String room,String id,String text,String kind,long replyTo) {
        if (closed) { return CompletableFuture.failedFuture(new IOException("UDP đã dừng.")); }
        if (id == null || !id.matches("[A-Za-z0-9_-]{22}")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Mã tin không hợp lệ."));
        }
        try {
            if (room == null || !room.matches("sanh|[a-f0-9]{24}")) { throw new IllegalArgumentException("Phòng không hợp lệ."); }
            if (text == null) { throw new IllegalArgumentException("Hãy nhập nội dung tin nhắn."); }
            if (!Set.of("T","S").contains(kind) || replyTo<0) throw new IllegalArgumentException("Loại tin không hợp lệ.");
            if (kind.equals("S") && (replyTo!=0 || !Protocol.stickerHopLe(text))) throw new IllegalArgumentException("Sticker không hợp lệ.");
            String payload=kind.equals("T")?WebReply.encode(text,replyTo):text;
            Goi.kiemTraTin(new TinNhan(1,room,"x".repeat(24),"Tên người dùng",payload,0,id,kind));
            String nonce = BaoMat.ngauNhien(16);
            byte[] bytes = Goi.thanhByte(Goi.ghep("CHAT2", nonce, token, room, id, text,kind,Long.toString(replyTo)));
            Pending p = new Pending(token, room, id, bytes);
            synchronized (pending) {
                if (pending.size() >= 256) { throw new IllegalArgumentException("Hàng gửi đang đầy. Hãy thử lại."); }
                pending.put(nonce, p);
            }
            p.result.whenComplete((v, e) -> {
                pending.remove(nonce, p);
                ScheduledFuture<?> retry = p.retry;
                if (retry != null) { retry.cancel(false); }
            });
            transmit(nonce, p, 0);
            return p.result;
        } catch (IllegalArgumentException e) { return CompletableFuture.failedFuture(e); }
    }

    // Timeout nghiep vu, khong coi send thanh cong la tin da luu.
    private void transmit(String nonce, Pending p, int attempt) {
        if (p.result.isDone()) { return; }
        if (closed || attempt >= 3) {
            p.result.completeExceptionally(new IOException("Chưa nhận xác nhận UDP. Hãy gửi lại cùng mã tin.")); return;
        }
        try {
            synchronized (wireOrder) {
                delivery.send(sender, new DatagramPacket(p.bytes, p.bytes.length, destination));
                trace("UDP_SEND", p.room, p.id, 0);
            }
            ScheduledFuture<?> next = timer.schedule(() -> transmit(nonce, p, attempt + 1), 800, TimeUnit.MILLISECONDS);
            p.retry = next;
            if (p.result.isDone()) { next.cancel(false); }
        } catch (IOException | RejectedExecutionException e) { p.result.completeExceptionally(e); }
    }

    private void receive(DatagramSocket socket) {
        while (!socket.isClosed()) {
            try {
                byte[] bytes = new byte[Protocol.GOI_TOI_DA + 1];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);
                if (packet.getLength() > Protocol.GOI_TOI_DA || !packet.getSocketAddress().equals(sender.getLocalSocketAddress())) { continue; }
                String wire = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(packet.getData(), 0, packet.getLength())).toString();
                String[] fields = Goi.tach(wire);
                if (fields.length != 8 || !fields[0].equals("CHAT2")) { continue; }
                Pending p = pending.get(fields[1]);
                if (p == null || !Arrays.equals(p.bytes, Arrays.copyOf(packet.getData(), packet.getLength()))) { continue; }
                lastReceived = System.currentTimeMillis();
                synchronized (wireOrder) { trace("UDP_RECEIVE", fields[3], fields[4], 0); }
                try {
                    var accepted = service.receive(fields[2],fields[3],fields[4],fields[5],fields[6],Long.parseLong(fields[7]));
                    TinNhan message = accepted.message();
                    trace(accepted.duplicate() ? "MESSAGE_REPLAY" : "MESSAGE_COMMITTED", message.roomId, message.maClient, message.msgId);
                    if (!accepted.duplicate()) { committed.accept(message); }
                    p.result.complete(accepted);
                } catch (IOException | RuntimeException e) { p.result.completeExceptionally(e); }
            } catch (IOException | IllegalArgumentException e) {
                if (!socket.isClosed()) { LOG.warn("UDP_PACKET_REJECTED"); }
            }
        }
    }
    public static void trace(String stage, String room, String id, long msgId) {
        LOG.info("{} roomId={} clientMsgId={} msgId={}", stage, room, id, msgId);
    }
    // Chi fixture Java su dung, khong co API dieu khien nay trong artifact production.
    public synchronized void pauseReceiver() { if (receiver != null) { receiver.close(); receiver = null; } }
    public synchronized void resumeReceiver() throws SocketException {
        if (closed || receiver != null) { return; }
        receiver = socket(); destination = (InetSocketAddress) receiver.getLocalSocketAddress();
        DatagramSocket current = receiver;
        Thread t = new Thread(() -> receive(current), "web-udp-receive"); t.setDaemon(true); t.start();
    }
    public boolean running() { return receiver != null && !receiver.isClosed(); }
    public long lastReceived() { return lastReceived; }
    @Override public synchronized void close() {
        closed = true; pauseReceiver(); sender.close(); timer.shutdownNow();
        pending.values().forEach(p -> p.result.completeExceptionally(new IOException("UDP đã dừng."))); pending.clear();
    }
    @FunctionalInterface interface Delivery { void send(DatagramSocket socket, DatagramPacket packet) throws IOException; }
    private static final class Pending {
        final String token, room, id;
        final byte[] bytes;
        volatile ScheduledFuture<?> retry;
        final CompletableFuture<WebChatService.Accepted> result = new CompletableFuture<>();
        Pending(String token, String room, String id, byte[] bytes) { this.token = token; this.room = room; this.id = id; this.bytes = bytes; }
    }
}
