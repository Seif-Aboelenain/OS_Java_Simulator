import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Initial welcome screen shown before the simulator starts.
 * Clicking "Start Simulation" closes this screen and runs the provided callback.
 */
public class WelcomeScreen extends JFrame {

    private static final Color BG_TOP       = new Color(238, 242, 250);
    private static final Color BG_BOTTOM    = new Color(220, 228, 245);
    private static final Color ACCENT       = new Color(47, 111, 237);
    private static final Color ACCENT_HOVER = new Color(64, 126, 250);
    private static final Color ACCENT_PRESS = new Color(30, 90, 214);
    private static final Color TEXT_DARK    = new Color(28, 32, 38);
    private static final Color TEXT_MUTED   = new Color(103, 112, 125);
    private static final Color LOGO_TOP     = new Color(64, 132, 255);
    private static final Color LOGO_BOTTOM  = new Color(35, 89, 209);

    public WelcomeScreen(Runnable onStart) {
        super("OS Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 400);
        setMinimumSize(new Dimension(460, 340));
        setLayout(new BorderLayout());

        GradientPanel content = new GradientPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(40, 40, 30, 40));

        JComponent logo = new LogoBadge();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("OS Simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("CPU Scheduling & Memory Management Visualizer");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedButton startButton = new RoundedButton("Start Simulation");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setToolTipText("Open the simulator window");
        startButton.addActionListener(e -> {
            dispose();
            onStart.run();
        });

        JButton aboutLink = new JButton("About");
        aboutLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aboutLink.setForeground(TEXT_MUTED);
        aboutLink.setBorderPainted(false);
        aboutLink.setContentAreaFilled(false);
        aboutLink.setFocusPainted(false);
        aboutLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        aboutLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutLink.setToolTipText("Learn more about this simulator");
        aboutLink.addActionListener(e -> AboutDialog.show(this));

        JLabel version = new JLabel("v1.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(150, 158, 170));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(Box.createVerticalGlue());
        content.add(logo);
        content.add(Box.createVerticalStrut(18));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(34));
        content.add(startButton);
        content.add(Box.createVerticalStrut(12));
        content.add(aboutLink);
        content.add(Box.createVerticalGlue());
        content.add(version);

        add(content, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Soft top-to-bottom gradient backdrop for the welcome screen. */
    private static final class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded circular badge with a simple monitor glyph, standing in for an app icon. */
    private static final class LogoBadge extends JComponent {
        private static final int SIZE = 72;

        LogoBadge() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(20, 40, 90, 40));
            g2.fillOval(6, 10, SIZE - 12, SIZE - 12);

            g2.setPaint(new GradientPaint(0, 0, LOGO_TOP, 0, SIZE, LOGO_BOTTOM));
            g2.fillOval(4, 4, SIZE - 8, SIZE - 8);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int mx = SIZE / 2, my = SIZE / 2;
            g2.drawRoundRect(mx - 16, my - 13, 32, 21, 4, 4);
            g2.drawLine(mx - 7, my + 11, mx + 7, my + 11);
            g2.drawLine(mx, my + 8, mx, my + 11);

            g2.dispose();
        }
    }

    /** Pill-shaped, hover/press-aware primary action button drawn with custom paint. */
    private static final class RoundedButton extends JButton {
        private boolean hover = false;
        private boolean pressed = false;

        RoundedButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 34, 12, 34));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = pressed ? ACCENT_PRESS : (hover ? ACCENT_HOVER : ACCENT);
            int arc = getHeight();

            g2.setColor(new Color(30, 60, 130, 55));
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, arc, arc);

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
