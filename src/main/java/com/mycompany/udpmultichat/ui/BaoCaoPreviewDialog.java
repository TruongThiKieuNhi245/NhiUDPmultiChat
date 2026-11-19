package com.mycompany.udpmultichat.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Vai tro: Cua so xem truoc bao cao, ho tro in an va xuat PDF. Ai goi: QuanTriFrame. Goi: Theme. */
@SuppressWarnings("serial")
public final class BaoCaoPreviewDialog extends JDialog {
    private final JEditorPane pane=new JEditorPane("text/html","");
    private final String html;

    // Khoi tao hop thoai xem truoc bao cao HTML va PDF.
    public BaoCaoPreviewDialog(JFrame cha,String html,String tieuDe) {
        super(cha,"Xem trước báo cáo — "+tieuDe,true);
        this.html=html;
        setSize(1020,760); setMinimumSize(new Dimension(800,560));
        setLocationRelativeTo(cha);
        JPanel goc=new JPanel(new BorderLayout(0,Theme.S8));
        goc.setBorder(Theme.le(Theme.S12));

        JPanel thanh=new JPanel(new BorderLayout());
        JLabel t=Theme.nhan("Xem trước bản in / PDF — "+tieuDe,true);
        t.setFont(Theme.DAM.deriveFont(16f)); thanh.add(t,BorderLayout.WEST);

        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S8,0));
        JButton inPdf=Theme.nut("In / Lưu PDF",true);
        JButton moBrowser=Theme.nut("Mở trình duyệt",false);
        JButton luu=Theme.nut("Lưu HTML",false);
        JButton dong=Theme.nut("Đóng",false);

        nut.add(inPdf); nut.add(moBrowser); nut.add(luu); nut.add(dong);
        thanh.add(nut,BorderLayout.EAST); goc.add(thanh,BorderLayout.NORTH);

        pane.setEditable(false);
        pane.setText(html);
        pane.setCaretPosition(0);
        JScrollPane cuon=new JScrollPane(pane);
        cuon.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.VIEN));
        goc.add(cuon,BorderLayout.CENTER);

        inPdf.addActionListener(e -> inAn());
        moBrowser.addActionListener(e -> moTrinhDuyet());
        luu.addActionListener(e -> luuFile());
        dong.addActionListener(e -> dispose());

        setContentPane(goc);
    }

    // In hoac luu PDF bang hop thoai in he dieu hanh.
    private void inAn() {
        try {
            MessageFormat header=new MessageFormat("Báo cáo UDP Chat");
            MessageFormat footer=new MessageFormat("Trang {0}");
            boolean ok=pane.print(header,footer,true,null,null,true);
            if (ok) {
                JOptionPane.showMessageDialog(this,"Đã gửi lệnh in / xuất PDF thành công.","In báo cáo",JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Lỗi khi in: "+ex.getMessage(),"Lỗi in",JOptionPane.ERROR_MESSAGE);
        }
    }

    // Mo trong trinh duyet mac dinh de xem hoac luu PDF bang Ctrl+P.
    private void moTrinhDuyet() {
        try {
            Path tam=Files.createTempFile("baocao_preview_",".html");
            Files.writeString(tam,html,StandardCharsets.UTF_8);
            Desktop.getDesktop().open(tam.toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Không thể mở trình duyệt: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);
        }
    }

    // Luu truc tiep thanh tep HTML tu chua.
    private void luuFile() {
        JFileChooser chon=new JFileChooser();
        chon.setSelectedFile(new java.io.File("baocao_udpmultichat.html"));
        if (chon.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chon.getSelectedFile().toPath(),html,StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this,"Đã lưu tệp báo cáo HTML thành công.","Thông báo",JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Lỗi lưu tệp: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
