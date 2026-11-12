package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.ui.ChatMulticastUI;
import javax.swing.SwingUtilities;

/** Diem chay Client; bat IPv4 truoc khi giao dien co the tao socket. */
public final class ClientMain {
    private ClientMain() { }

    // Chay nhieu tien trinh de mo nhieu cua so Client.
    public static void main(String[] args) {
        MangUtil.dungIPv4();
        SwingUtilities.invokeLater(() -> new ChatMulticastUI().setVisible(true));
    }
}
