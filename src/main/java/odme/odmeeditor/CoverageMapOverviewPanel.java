package odme.odmeeditor;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** A shared, minimal configuration and comparable marginal heat strips. */
final class CoverageMapOverviewPanel extends JPanel {
    private final JTextField bandwidth = new JTextField("5", 4);
    private final JTextField adequacy = new JTextField("1", 4);
    private final JLabel status = new JLabel("Click Calculate all parameters to show the project scenario set.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Parameter / ODD path", "Range", "Coverage heat (0 to 1)", "Score", "Samples", "Excluded"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    CoverageMapOverviewPanel(StructuralCoverageSupport.CoverageContext context) {
        super(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Bandwidth (% of each parameter range):"));
        controls.add(bandwidth);
        controls.add(new JLabel("Adequacy target:"));
        controls.add(adequacy);
        JButton calculate = new JButton("Calculate all parameters");
        controls.add(calculate);
        JButton help = new JButton("?");
        controls.add(help);
        help.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "Use the defaults and click Calculate all parameters.\n"
                + "Each row shows one numeric ODD parameter across its complete range.\n"
                + "Bandwidth is a percentage of each parameter's range (default 5%).\n"
                + "Adequacy is the local sample support needed for full green (default 1).\n"
                + "All rows use a triangular kernel, five equal regions, cold threshold 0.25,\n"
                + "and geometric scoring. Region boundaries do not change bandwidth.\n"
                + "Red = low support; yellow = intermediate; green = adequate.\n"
                + "Hover over a strip for its parameter and score.\n"
                + "Advanced: single parameter provides custom edges, kernels and exports.\n"
                + "This view covers numeric parameters exposed by the current model adapter.\n"
                + "It measures marginal support, not joint coverage of parameter combinations.",
                "All parameters help", JOptionPane.INFORMATION_MESSAGE));
        JTable table = new JTable(model);
        table.setRowHeight(48);
        table.getColumnModel().getColumn(0).setPreferredWidth(350);
        table.getColumnModel().getColumn(2).setPreferredWidth(430);
        table.getColumnModel().getColumn(2).setCellRenderer((t, value, selected, focused, row, col) ->
                new Strip((StructuralCoverageSupport.MapOverviewRow) value));
        JPanel header = new JPanel(new BorderLayout());
        header.add(controls, BorderLayout.NORTH);
        header.add(new JLabel("All numeric parameters • triangular kernel • geometric score • red 0 — yellow 0.5 — green 1"), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        calculate.addActionListener(event -> {
            try {
                double fraction = Double.parseDouble(bandwidth.getText().trim()) / 100;
                double target = Double.parseDouble(adequacy.getText().trim());
                calculate.setEnabled(false);
                status.setText("Calculating all parameters...");
                BackgroundTaskRunner.run(this, "Parameter Coverage Map", "Loading scenarios and calculating all parameters...",
                        () -> StructuralCoverageSupport.analyzeAllCoverageMaps(context, fraction, target),
                        rows -> {
                            model.setRowCount(0);
                            for (var row : rows) {
                                var d = row.definition();
                                var r = row.result();
                                model.addRow(new Object[]{d.variableName() + " / " + d.pathDisplay(),
                                        d.min() + " – " + d.max(), row,
                                        String.format(java.util.Locale.US, "%.1f%%", r.parameterScore() * 100),
                                        r.inRangeSampleCount(), r.excludedSampleCount()});
                            }
                            status.setText(context.projectName() + " • " + rows.size() + " numeric parameters • bandwidth "
                                    + fraction * 100 + "% • adequacy " + target + " • missing/inactive values supply no samples");
                            calculate.setEnabled(true);
                        }, error -> {
                            status.setText("Calculation failed: " + error.getMessage());
                            calculate.setEnabled(true);
                        });
            } catch (IllegalArgumentException error) {
                status.setText("Invalid settings: enter positive numbers for bandwidth and adequacy.");
                calculate.setEnabled(true);
            }
        });
    }

    private static final class Strip extends JComponent {
        private final StructuralCoverageSupport.MapOverviewRow row;
        Strip(StructuralCoverageSupport.MapOverviewRow row) {
            this.row = row;
            setToolTipText(row.definition().variableName() + ": " + row.definition().min() + " to "
                    + row.definition().max() + "; score " + Math.round(row.result().parameterScore() * 100) + "%");
        }
        @Override protected void paintComponent(Graphics g) {
            List<odme.domain.coverage.ParameterCoverageMapAnalyzer.HeatPoint> points = row.result().heatField();
            double min = row.definition().min(), range = row.definition().max() - min;
            int width = Math.max(1, getWidth() - 8);
            for (int i = 0; i < points.size() - 1; i++) {
                double heat = points.get(i).coverage();
                Color low = heat < 0.5 ? new Color(0xB91C1C) : new Color(0xFBBF24);
                Color high = heat < 0.5 ? new Color(0xFBBF24) : new Color(0x15803D);
                double f = heat < 0.5 ? heat * 2 : (heat - 0.5) * 2;
                g.setColor(new Color((int)(low.getRed() + f * (high.getRed() - low.getRed())),
                        (int)(low.getGreen() + f * (high.getGreen() - low.getGreen())),
                        (int)(low.getBlue() + f * (high.getBlue() - low.getBlue()))));
                int x = 4 + (int)(width * (points.get(i).x() - min) / range);
                int end = 4 + (int)(width * (points.get(i + 1).x() - min) / range);
                g.fillRect(x, 8, Math.max(1, end - x), getHeight() - 16);
            }
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i <= 5; i++) g.drawLine(4 + width * i / 5, 6, 4 + width * i / 5, getHeight() - 6);
        }
    }
}
