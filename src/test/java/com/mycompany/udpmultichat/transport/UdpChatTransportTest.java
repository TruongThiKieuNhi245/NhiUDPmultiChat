package com.mycompany.udpmultichat.transport;

import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.WebChatService;
import java.net.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class UdpChatTransportTest {
    @Test void lostDatagramRetriesAndDuplicateDatagramsCommitOnce(@TempDir Path directory) throws Exception {
        AtomicInteger sends = new AtomicInteger(), broadcasts = new AtomicInteger();
        try (var service = new WebChatService(directory);
             var transport = new UdpChatTransport(service, (socket, packet) -> {
                 if (sends.incrementAndGet() == 1) { return; }
                 socket.send(packet); socket.send(packet);
             })) {
            service.register("losstest", "sample-password");
            var id = service.login("losstest", "sample-password");
            transport.onCommitted(t -> broadcasts.incrementAndGet());
            var accepted = transport.send(id.token(), "sanh", TinNhan.taoMa(), "Mất gói đầu, nhận gói lặp").get(5, TimeUnit.SECONDS);
            assertEquals(1, accepted.message().msgId); assertEquals(2, sends.get());
            assertEquals(1, broadcasts.get()); assertEquals(1, service.history(id.token(), "sanh", 0).size());
        }
        System.out.println("PASS Web1: mat datagram dau, retry co han, datagram trung chi commit mot lan");
    }
    @Test void reorderedPacketsUseServerOrderAndUntrustedSourceIsRejected(@TempDir Path directory) throws Exception {
        DatagramPacket[] first = { null };
        try (var service = new WebChatService(directory);
             var transport = new UdpChatTransport(service, (socket, packet) -> {
                 synchronized (first) {
                     if (first[0] == null) { first[0] = packet; return; }
                     // Gui ban sao tu endpoint la truoc, noi dung giong het van bi tu choi.
                     try (var stranger = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0))) { stranger.send(first[0]); }
                     socket.send(packet); socket.send(first[0]);
                 }
             })) {
            service.register("reorder", "sample-password");
            var id = service.login("reorder", "sample-password");
            var one = transport.send(id.token(), "sanh", TinNhan.taoMa(), "Tin thứ nhất");
            var two = transport.send(id.token(), "sanh", TinNhan.taoMa(), "Tin thứ hai");
            assertEquals(1, two.get(5, TimeUnit.SECONDS).message().msgId);
            assertEquals(2, one.get(5, TimeUnit.SECONDS).message().msgId);
            var history = service.history(id.token(), "sanh", 0);
            assertEquals(2, history.size()); assertEquals("Tin thứ hai", history.getFirst().noiDung);
        }
        System.out.println("PASS Web1: dao thu tu datagram, msgId theo receive, chan endpoint la");
    }
}
