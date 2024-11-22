package odme.odmeeditor;



import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CodeCoverageLayout extends JDialog {

    public CodeCoverageLayout(JFrame parent , Object[][] data ) {
        // Call JDialog constructor with the parent JFrame and set modality
        super(parent, "OpenCover: Code Coverage Metrics with CI build", true); // "true" makes it modal

        // Set up dialog properties
        setSize(800, 600);
        setLocationRelativeTo(parent); // Center relative to the parent JFrame


        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel(" Code Coverage", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Subtitle
//        JLabel subtitleLabel = new JLabel("May 15, 2016 · 5 min read · Code", JLabel.CENTER);
//        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        // Coverage table
        String[] columns = {"Name", "Covered", "Uncovered", "Total", "Line Coverage"};
//        Object[][] data = {
//                {"TW.Restful.Core", "123", "56", "179", 93.8},
//                {"TW.Restful.Core.Resource", "16", "0", "16", 100.0},
//                {"TW.Restful.Framework.Testing", "10", "5", "15", 66.7},
//                {"TW.Restful.Fixture.Builders", "22", "6", "28", 78.6},
//                {"TW.Restful.Fixture.FileSys", "13", "0", "13", 100.0},
//        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Set column class to Double for coverage column for correct rendering
                if (columnIndex == 4) {
                    return Double.class;
                }
                return String.class;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Apply custom renderer to Line Coverage column to show progress bars
        table.getColumnModel().getColumn(4).setCellRenderer(new ProgressBarRenderer());

        // Scroll pane for the table
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Add components to main panel
        mainPanel.add(titleLabel, BorderLayout.NORTH);
//        mainPanel.add(subtitleLabel, BorderLayout.CENTER);
        mainPanel.add(tableScrollPane, BorderLayout.SOUTH);

        add(mainPanel);

        // Debug statement to confirm JFrame properties
        System.out.println("CodeCoverageLayout set up completed");
    }

    // Custom renderer for displaying coverage as a progress bar
    static class ProgressBarRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Double) {
                double coveragePercentage = (Double) value;
                return new CoverageProgressBar(coveragePercentage); // Return a custom progress bar component
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    // Custom component to display the coverage as a green and red progress bar
    static class CoverageProgressBar extends JPanel {
        private double coveragePercentage;

        public CoverageProgressBar(double coveragePercentage) {
            this.coveragePercentage = coveragePercentage;
            setPreferredSize(new Dimension(100, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();


            // Calculate the width of the green portion based on the coverage percentage
            int greenWidth = (int) (width * (coveragePercentage / 100.0));

            // Draw the green portion
            g.setColor(Color.GREEN);
            g.fillRect(10, 10, greenWidth, height);

            // Draw the red portion (remaining part)
            g.setColor(Color.RED);
            g.fillRect(greenWidth, 10, width - greenWidth, height);


            // Draw coverage percentage text in the center
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1f%%", coveragePercentage),
                    width / 2 - 15,
                    height / 2 + 5);
        }
    }
}

