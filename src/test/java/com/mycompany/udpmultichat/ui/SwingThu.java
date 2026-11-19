package com.mycompany.udpmultichat.ui;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;

/** Vai tro: Dieu khien su kien component trong JVM test, khong gia lap chuot/phim he dieu han. Ai goi: QuanTriTuongTacSelfTest. Goi: Swing EDT. */
final class SwingThu {
    // Khong khoi tao lop tien ich.
    private SwingThu() { }

    // Moi doc/ghi Swing deu vao EDT, ngoai le van tra ve thread test.
    static <T> T lay(Callable<T> viec) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) { return viec.call(); }
        FutureTask<T> ketQua=new FutureTask<>(viec);
        SwingUtilities.invokeAndWait(ketQua);
        return ketQua.get();
    }

    // Chay thao tac Swing khong co gia tri tra ve.
    static void trenEdt(Runnable viec) throws Exception {
        lay(() -> { viec.run(); return null; });
    }

    // Cho mot ket qua that voi gioi han; khong cho mang/EDT doi vo han.
    static void doi(Callable<Boolean> dieuKien,String loi) throws Exception {
        long het=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime()<het) {
            if (dieuKien.call()) { return; }
            Thread.sleep(40);
        }
        throw new AssertionError(loi);
    }

    // Future mang chi duoc cho tu thread test, khong bao gio cho tren EDT.
    static <T> T cho(CompletableFuture<T> f) throws Exception { return f.get(15,TimeUnit.SECONDS); }

    // Khong nuot AssertionError khi mot lenh phai bi tu choi.
    static void biChan(CompletableFuture<?> f) throws Exception {
        try { cho(f); }
        catch (ExecutionException dung) { return; }
        throw new AssertionError("Server chap nhan lenh phai bi chan");
    }

    // Duyet cay component cong khai, khong reflection vao private cua app.
    static <T extends Component> List<T> tim(Component c,Class<T> loai) {
        List<T> ds=new ArrayList<>();
        if (loai.isInstance(c)) { ds.add(loai.cast(c)); }
        if (c instanceof Container cha) {
            for (Component con:cha.getComponents()) { ds.addAll(tim(con,loai)); }
        }
        return ds;
    }

    // Mot nut phai duoc xac dinh duy nhat trong vung dang thu.
    static JButton nut(Container c,String ten) {
        var ds=tim(c,JButton.class).stream().filter(b -> ten.equals(b.getText())).toList();
        can(ds.size()==1,"Nut khong duy nhat: "+ten+" ("+ds.size()+")");
        return ds.get(0);
    }

    // Hanh dong doClick chay handler that, khong duoc goi la click chuot that.
    static void bam(Container c,String ten) throws Exception { trenEdt(() -> nut(c,ten).doClick()); }

    // Doi tab bang component that cua frame quan tri.
    static Container tab(QuanTriFrame f,int so) throws Exception {
        return lay(() -> {
            f.xemTab(so);
            return (Container)tim(f.getRootPane(),JTabbedPane.class).get(0).getSelectedComponent();
        });
    }

    // Doi form modeless that xuat hien sau su kien.
    static JDialog hopThoai(String ten) throws Exception {
        doi(() -> lay(() -> timDialog(ten)!=null),"Khong mo form "+ten);
        return lay(() -> timDialog(ten));
    }

    // Window.getWindows chi gom cua so trong JVM test nay.
    private static JDialog timDialog(String ten) {
        for (Window w:Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && d.getTitle().equals(ten)) { return d; }
        }
        return null;
    }

    // Tim control theo nhan cua hang form, khong phu thuoc thu tu field.
    private static Container hang(Container c,String nhan) {
        var ds=tim(c,JLabel.class).stream().filter(l -> nhan.equals(l.getText())).toList();
        can(ds.size()==1,"Khong tim thay nhan form: "+nhan);
        return ds.get(0).getParent();
    }

    // Dien text trong form that; DocumentListener/validator cua app van hoat dong.
    static void dien(Container c,String nhan,String text) throws Exception {
        trenEdt(() -> tim(hang(c,nhan),JTextField.class).get(0).setText(text));
    }

    // Tim trong bang roi chon dung model row sau khi filter/sort.
    static void chonDong(Container c,String timKiem) throws Exception {
        trenEdt(() -> tim(c,JTextField.class).get(0).setText(timKiem));
        doi(() -> lay(() -> tim(c,JTable.class).get(0).getRowCount()==1),"Bang khong co dung mot dong: "+timKiem);
        trenEdt(() -> tim(c,JTable.class).get(0).setRowSelectionInterval(0,0));
    }

    // Toolbar tao JPopupMenu that; kich hoat dung JMenuItem roi don selection nhu UI.
    static void menu(Container c,String ten) throws Exception {
        bam(c,"Thao tác...");
        trenEdt(() -> {
            JPopupMenu popup=null;
            for (MenuElement e:MenuSelectionManager.defaultManager().getSelectedPath()) {
                if (e instanceof JPopupMenu p) { popup=p; }
            }
            can(popup!=null,"Khong mo menu thao tac");
            var ds=tim(popup,JMenuItem.class).stream().filter(i -> ten.equals(i.getText())).toList();
            can(ds.size()==1,"Khong tim thay menu "+ten);
            MenuSelectionManager.defaultManager().clearSelectedPath();
            ds.get(0).doClick();
        });
    }

    // Thu binding Esc cua form, khong phat phim vao desktop cua nguoi dung.
    static void esc(JDialog d) throws Exception {
        trenEdt(() -> {
            JRootPane root=d.getRootPane();
            Object khoa=root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
            Action a=khoa==null?null:root.getActionMap().get(khoa);
            can(a!=null,"Form thieu binding Esc");
            a.actionPerformed(new ActionEvent(root,ActionEvent.ACTION_PERFORMED,"test-esc"));
        });
        doi(() -> lay(() -> !d.isDisplayable()),"Esc khong dong form");
    }

    // File chooser modal can khoi dong bang invokeLater de thread test con chon duoc.
    static void chonThuMuc(Container c,String tenNut,Path thu) throws Exception {
        trenEdt(() -> SwingUtilities.invokeLater(() -> nut(c,tenNut).doClick()));
        doi(() -> lay(() -> chooser()!=null),"Khong mo JFileChooser");
        trenEdt(() -> {
            JFileChooser chon=chooser();
            can(chon!=null,"File chooser da dong som");
            chon.setCurrentDirectory(thu.toFile());
            chon.setSelectedFile(thu.toFile());
            chon.approveSelection();
        });
    }

    // Chi doc chooser cua JVM test, khong truy cap Explorer hay cua so app khac.
    private static JFileChooser chooser() {
        for (Window w:Window.getWindows()) {
            if (!w.isShowing()) { continue; }
            var ds=tim(w,JFileChooser.class);
            if (!ds.isEmpty()) { return ds.get(0); }
        }
        return null;
    }

    // Assertion tach khoi handler de loi khong bi nuot tren EDT.
    static void can(boolean dung,String loi) { if (!dung) { throw new AssertionError(loi); } }
}
