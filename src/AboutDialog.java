import java.awt.*;
import javax.swing.*;

/** Simple "About" dialog shared by the welcome screen and the main window. */
public final class AboutDialog {

    private AboutDialog() {}

    public static void show(Component parent) {
        JLabel title = new JLabel("OS Simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel body = new JLabel(
                "<html>Version 1.0<br><br>"
                + "A visual simulator for CPU scheduling, memory<br>"
                + "management, and process synchronization.<br><br>"
                + "Schedulers: Round Robin &middot; HRRN &middot; MLFQ</html>");
        body.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(body);

        JOptionPane.showMessageDialog(parent, content, "About OS Simulator",
                JOptionPane.PLAIN_MESSAGE);
    }
}
