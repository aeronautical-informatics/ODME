package odme.odmeeditor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public final class BackgroundTaskRunner {

    private static final Color SURFACE     = Color.WHITE;
    private static final Color BORDER      = new Color(0xD7DFEA);
    private static final Color SURFACE_ALT = new Color(0xF7FAFC);
    private static final Color ACCENT      = new Color(0x0A7D5A);
    private static final Color TEXT        = new Color(0x1E2933);

    private BackgroundTaskRunner() {}

    @FunctionalInterface
    public interface Task<T> {
        T run() throws Exception;
    }

    public static <T> void run(Component parent,
                               String title,
                               String message,
                               Task<T> task,
                               Consumer<T> onSuccess) {
        run(parent, title, message, task, onSuccess,
                error -> showError(parent, title, error));
    }

    public static <T> void run(Component parent,
                               String title,
                               String message,
                               Task<T> task,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onError) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog progressDialog = createProgressDialog(owner, title, message);
        Cursor previousCursor = owner != null ? owner.getCursor() : null;

        if (owner != null) {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                if (owner != null && previousCursor != null) {
                    owner.setCursor(previousCursor);
                }

                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    if (onError != null) {
                        onError.accept(ex);
                    }
                }
                catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (onError != null) {
                        onError.accept(cause);
                    }
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    private static JDialog createProgressDialog(Window owner, String title, String message) {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        panel.setPreferredSize(new Dimension(320, 110));

        JLabel label = new JLabel(message);
        label.setForeground(TEXT);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBackground(SURFACE_ALT);
        progressBar.setForeground(ACCENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(14));
        panel.add(progressBar);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    private static void showError(Component parent, String title, Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
