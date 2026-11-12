package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** Diem chay Server va cua so giam sat; mang va luu tru nam trong ServerCore. */
@SuppressWarnings("serial")
public final class ServerMain extends JFrame implements ServerCore.Listener {
    private final JTextArea log = new JTextArea();
    private final JLabel thongKe = new JLabel("Chưa chạy Server");
    private final JButton batDau = new JButton("Bắt đầu"), dung = new JButton("Dừng");
    private final ExecutorService viecNen = Executors.newSingleThreadExecutor(viec -> {
        Thread t = new Thread(viec, "ui-server-task"); t.setDaemon(true); return t;
    });
    private final ServerCore server = new ServerCore(Path.of("data"), this);
    private boolean dangDong;

    public ServerMain() {
        setTitle("UDP Chat — Server :5000"); setSize(820, 520);
        setMinimumSize(new Dimension(600, 380)); setLocationByPlatform(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel thanh = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        thanh.add(batDau); thanh.add(dung); thanh.add(thongKe); dung.setEnabled(false);
        log.setEditable(false); log.setFont(new Font("Consolas", Font.PLAIN, 13));
        log.setBorder(new EmptyBorder(12, 12, 12, 12));
        add(thanh, BorderLayout.NORTH); add(new JScrollPane(log), BorderLayout.CENTER);
        batDau.addActionListener(e -> doiTrangThai(true));
        dung.addActionListener(e -> doiTrangThai(false));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (dangDong) { return; }
                dangDong = true;
                viecNen.execute(() -> { server.close(); viecNen.shutdown(); SwingUtilities.invokeLater(ServerMain.this::dispose); });
            }
        });
        onLog("Bấm Bắt đầu. Dữ liệu: " + Path.of("data").toAbsolutePath().normalize());
    }

    // ===== BAT / DUNG TREN LUONG NEN =====
    private void doiTrangThai(boolean bat) {
        batDau.setEnabled(false); dung.setEnabled(false);
        viecNen.execute(() -> {
            try {
                if (bat) { server.batDau(Protocol.CONG_SERVER); }
                else { server.close(); }
            } catch (Exception e) { onLog("Không khởi động được: " + e.getMessage()); }
            SwingUtilities.invokeLater(() -> {
                batDau.setEnabled(!server.dangChay()); dung.setEnabled(server.dangChay());
            });
        });
    }

    // ServerCore khong biet Swing; giao dien tu chuyen su kien ve EDT.
    @Override public void onLog(String noiDung) {
        String gio = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        SwingUtilities.invokeLater(() -> {
            log.append("[" + gio + "] " + noiDung + "\n");
            if (log.getDocument().getLength() > 120000) { log.replaceRange("", 0, 30000); }
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    @Override public void onThongKe(int tongUser, int online, long soTin) {
        SwingUtilities.invokeLater(() -> thongKe.setText("Tài khoản: " + tongUser + " | Online: " + online + " | Tin: " + soTin));
    }

    // Hai diem chay ro rang: ClientMain va server.ServerMain.
    public static void main(String[] args) {
        MangUtil.dungIPv4();
        SwingUtilities.invokeLater(() -> new ServerMain().setVisible(true));
    }
}
