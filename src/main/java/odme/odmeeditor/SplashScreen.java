package odme.odmeeditor;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Modern splash screen for ODME.
 *
 * <p>Displays the ODME logo, application name, and copyright notice
 * with a smooth progress bar. Uses a Swing Timer to avoid blocking
 * the EDT.</p>
 */
public class SplashScreen {

    private static final Color ACCENT = new Color(0x0A7D5A);
    private static final Color BG = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(0x666666);

    private final JFrame frame;
    private final JProgressBar progressBar;
    private final int durationMs;
    private volatile boolean finished;

    /**
     * @param durationSeconds total display time in seconds
     */
    public SplashScreen(int durationSeconds) {
        this.durationMs = durationSeconds * 1000;

        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(520, 340);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            BorderFactory.createEmptyBorder(30, 40, 20, 40)
        ));

        // Logo
        JLabel logoLabel = createLogoLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(logoLabel);
        content.add(Box.createVerticalStrut(16));

        // Application name
        JLabel nameLabel = new JLabel("Operational Design Domain Modelling Environment");
        nameLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        nameLabel.setForeground(ACCENT);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(24));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 6));
        progressBar.setMaximumSize(new Dimension(400, 6));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setBackground(new Color(0xE8E8E8));
        progressBar.setForeground(ACCENT);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(progressBar);
        content.add(Box.createVerticalGlue());

        // Copyright
        JLabel copyright = new JLabel("\u00A9 2023\u20132026 ODME Contributors \u00B7 MIT License");
        copyright.setFont(new Font("Helvetica Neue", Font.PLAIN, 11));
        copyright.setForeground(TEXT_SECONDARY);
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(16));
        content.add(copyright);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    /**
     * Runs the progress bar animation using a Swing Timer (non-blocking).
     * Returns after the splash has been dismissed.
     */
    public void runningPBar() {
        finished = false;
        int intervalMs = Math.max(durationMs / 100, 10);

        Timer timer = new Timer(intervalMs, null);
        timer.addActionListener(e -> {
            int val = progressBar.getValue() + 1;
            progressBar.setValue(val);
            if (val >= 100) {
                timer.stop();
                frame.dispose();
                finished = true;
            }
        });
        timer.start();

        // Wait for completion without blocking the EDT
        while (!finished) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private JLabel createLogoLabel() {
        URL logoUrl = getClass().getClassLoader().getResource("logo/odme_logo.png");
        if (logoUrl != null) {
            ImageIcon original = new ImageIcon(logoUrl);
            // Scale to fit splash width while maintaining aspect ratio
            int targetWidth = 360;
            int originalWidth = original.getIconWidth();
            int originalHeight = original.getIconHeight();
            if (originalWidth > 0 && originalHeight > 0) {
                int targetHeight = (int) ((double) originalHeight / originalWidth * targetWidth);
                // Cap height to keep splash balanced
                if (targetHeight > 160) {
                    targetHeight = 160;
                    targetWidth = (int) ((double) originalWidth / originalHeight * targetHeight);
                }
                Image scaled = original.getImage().getScaledInstance(
                    targetWidth, targetHeight, Image.SCALE_SMOOTH);
                return new JLabel(new ImageIcon(scaled));
            }
            return new JLabel(original);
        }
        // Fallback: text-only logo
        JLabel fallback = new JLabel("ODME");
        fallback.setFont(new Font("Helvetica Neue", Font.BOLD, 48));
        fallback.setForeground(ACCENT);
        return fallback;
    }
}
