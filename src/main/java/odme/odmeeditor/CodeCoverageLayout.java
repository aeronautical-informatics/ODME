package odme.odmeeditor;



import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CodeCoverageLayout extends JDialog {

    public CodeCoverageLayout(JFrame parent, Object[][] data) {
        // Call JDialog constructor with the parent JFrame and set modality
        super(parent, "Scenario Code Coverage", true); // "true" makes it modal

        // Set up dialog properties
        setSize(1000, 600);
        setLocationRelativeTo(parent); // Center relative to the parent JFrame

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Code Coverage", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Define column names
        String[] columns = {"Name", "Covered", "Uncovered", "Total", "Coverage"};

        // Create table model
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) { // Coverage column is double
                    return Double.class;
                }
                return String.class;
            }
        };

        // Create table
        JTable table = new JTable(model);
        table.setRowHeight(40); // Increased row height
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Apply custom renderers
        table.getColumnModel().getColumn(4).setCellRenderer(new ProgressBarRenderer());

        // Align child rows
        DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof String) {
                    String name = (String) value;
                    if (name.startsWith("    ")) { // Indented names
                        setFont(getFont().deriveFont(Font.PLAIN));
                        setForeground(Color.DARK_GRAY);
                    } else {
                        setFont(getFont().deriveFont(Font.BOLD));
                        setForeground(Color.BLACK);
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding around the cell content
                return c;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(nameRenderer);

        // Scroll pane for the table
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Add components to main panel
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    // Custom renderer for progress bars
    static class ProgressBarRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Double) {
                double coveragePercentage = (Double) value;
                return new CoverageProgressBar(coveragePercentage); // Return a progress bar component
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    // Custom progress bar component with padding
    static class CoverageProgressBar extends JPanel {
        private final double coveragePercentage;

        public CoverageProgressBar(double coveragePercentage) {
            this.coveragePercentage = coveragePercentage;
            setPreferredSize(new Dimension(100, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            int padding = 5; // Add padding around the progress bar

            // Calculate green bar width
            int greenWidth = (int) ((width - 2 * padding) * (coveragePercentage / 100.0));

            // Draw green and red bars
            g.setColor(Color.GREEN);
            g.fillRect(padding, padding, greenWidth, height - 2 * padding);

            g.setColor(Color.RED);
            g.fillRect(padding + greenWidth, padding, width - 2 * padding - greenWidth, height - 2 * padding);

            // Draw percentage text
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1f%%", coveragePercentage),
                    width / 2 - 15, height / 2 + 5);
        }
    }
}


