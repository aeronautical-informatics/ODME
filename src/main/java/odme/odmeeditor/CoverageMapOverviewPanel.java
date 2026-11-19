package odme.odmeeditor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Coverage map that shows the coverage estimate itself: one strip per numeric parameter, split into the bins of the
 * Coverage Estimate tab. Red marks an empty bin. Inside a covered bin the colour fades from green at a sampled value
 * to yellow half a bin width or more away from the nearest sample in that bin.
 */
final class CoverageMapOverviewPanel extends JPanel {
    static final Color EMPTY = new Color(0xB91C1C);
    static final Color AT_SAMPLE = new Color(0x15803D);
    static final Color FAR = new Color(0xFACC15);

    private final JLabel status = new JLabel("Click Calculate all parameters to show the project scenario set.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Parameter · entity (full ODD path in tooltip)", "Range", "Coverage map (bins of the coverage estimate)", "Bins covered", "Samples"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    CoverageMapOverviewPanel(StructuralCoverageSupport.CoverageContext context,
                             Supplier<List<StructuralCoverageSupport.ParameterSelectionRow>> binSettings) {
        super(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton calculate = new JButton("Calculate all parameters");
        controls.add(calculate);
        JButton help = new JButton("?");
        controls.add(help);
        help.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "Each row shows one numeric ODD parameter across its ODD range, split into the bins set in the\n"
                + "Coverage Estimate tab (Bin Size or Custom Bins). The map therefore shows the same bins the\n"
                + "coverage report counts.\n\n"
                + "Red: empty bin (not covered).\n"
                + "Green to yellow: covered bin. Green at a sampled value, fading to yellow half a bin width or more\n"
                + "away from the nearest sample in that bin.\n"
                + "Black ticks: sampled values.\n\n"
                + "Bins covered is the per-parameter parameter coverage of the report. Rows are sorted worst first.\n"
                + "Advanced: single parameter keeps the kernel-density view for inspecting the spread inside bins.\n"
                + "This view is marginal (one parameter at a time), not coverage of parameter combinations.",
                "All parameters help", JOptionPane.INFORMATION_MESSAGE));
        JTable table = new JTable(model) {
            @Override public String getToolTipText(java.awt.event.MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                if (row >= 0 && columnAtPoint(event.getPoint()) == 0) {
                    Object map = getModel().getValueAt(convertRowIndexToModel(row), 2);
                    if (map instanceof StructuralCoverageSupport.BinMapRow binRow) {
                        return binRow.definition().pathDisplay();
                    }
                }
                return super.getToolTipText(event);
            }
        };
        table.setRowHeight(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(520);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setCellRenderer((t, value, selected, focused, row, col) ->
                new Strip((StructuralCoverageSupport.BinMapRow) value));
        JPanel header = new JPanel(new BorderLayout());
        header.add(controls, BorderLayout.NORTH);
        header.add(new Legend(), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        calculate.addActionListener(event -> {
            List<StructuralCoverageSupport.ParameterSelectionRow> settings;
            try {
                settings = binSettings.get();
            } catch (IllegalArgumentException error) {
                status.setText("Invalid bin settings: " + error.getMessage());
                return;
            }
            calculate.setEnabled(false);
            status.setText("Calculating all parameters...");
            BackgroundTaskRunner.run(this, "Parameter Coverage Map", "Loading scenarios and counting samples per bin...",
                    () -> StructuralCoverageSupport.analyzeBinCoverageMaps(context, settings),
                    rows -> {
                        List<StructuralCoverageSupport.BinMapRow> sorted = new ArrayList<>(rows);
                        sorted.sort(Comparator.comparingDouble(StructuralCoverageSupport.BinMapRow::coveredFraction));
                        model.setRowCount(0);
                        int covered = 0, total = 0;
                        for (var row : sorted) {
                            var d = row.definition();
                            covered += row.coveredBins();
                            total += row.bins().size();
                            model.addRow(new Object[]{d.variableName() + "  ·  " + lastSegment(d.pathDisplay()),
                                    format(d.min()) + " – " + format(d.max()), row,
                                    "bins covered: " + row.coveredBins() + " of " + row.bins().size(), row.sampleCount()});
                        }
                        status.setText(context.projectName() + " • " + rows.size() + " numeric parameters • "
                                + covered + " of " + total + " bins covered • bins from the Coverage Estimate tab");
                        calculate.setEnabled(true);
                    }, error -> {
                        status.setText("Calculation failed: " + error.getMessage());
                        calculate.setEnabled(true);
                    });
        });
    }

    static String lastSegment(String pathDisplay) {
        String[] segments = pathDisplay.split(" > ");
        return segments[segments.length - 1].trim();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.format(java.util.Locale.US, "%.0f", value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    static Color colourAt(double x, StructuralCoverageSupport.CoverageBin bin, List<Double> valuesInBin) {
        return colourAt(x, bin, valuesInBin, false);
    }

    /** Integer values use one colour throughout the unit [v, v + 1). */
    static Color colourAt(double x, StructuralCoverageSupport.CoverageBin bin, List<Double> valuesInBin, boolean integer) {
        if (valuesInBin.isEmpty()) {
            return EMPTY;
        }
        double nearest = Double.POSITIVE_INFINITY;
        for (double value : valuesInBin) {
            double distance = Math.abs((integer ? Math.floor(x) : x) - value);
            nearest = Math.min(nearest, distance);
        }
        double halfWidth = (bin.upper() - bin.lower()) / 2.0;
        double f = halfWidth <= 0 ? 0.0 : Math.min(1.0, nearest / halfWidth);
        return new Color(
                (int) Math.round(AT_SAMPLE.getRed() + f * (FAR.getRed() - AT_SAMPLE.getRed())),
                (int) Math.round(AT_SAMPLE.getGreen() + f * (FAR.getGreen() - AT_SAMPLE.getGreen())),
                (int) Math.round(AT_SAMPLE.getBlue() + f * (FAR.getBlue() - AT_SAMPLE.getBlue())));
    }

    private static final class Legend extends JComponent {
        Legend() { setPreferredSize(new Dimension(760, 26)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = 6, h = 14, x = 6;
            g2.setColor(EMPTY);
            g2.fillRect(x, y, 28, h);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("empty bin", x + 34, y + 12);
            x += 110;
            for (int i = 0; i < 60; i++) {
                double f = i / 59.0;
                g2.setColor(new Color(
                        (int) (AT_SAMPLE.getRed() + f * (FAR.getRed() - AT_SAMPLE.getRed())),
                        (int) (AT_SAMPLE.getGreen() + f * (FAR.getGreen() - AT_SAMPLE.getGreen())),
                        (int) (AT_SAMPLE.getBlue() + f * (FAR.getBlue() - AT_SAMPLE.getBlue()))));
                g2.fillRect(x + i, y, 1, h);
            }
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("covered bin: at a sample → half a bin away or more", x + 66, y + 12);
            x += 400;
            g2.drawLine(x, y, x, y + h);
            g2.drawString("sampled value", x + 6, y + 12);
        }
    }

    static final class Strip extends JComponent {
        private final StructuralCoverageSupport.BinMapRow row;

        Strip(StructuralCoverageSupport.BinMapRow row) {
            this.row = row;
            String empty = row.emptyBinLabels();
            setToolTipText(row.definition().variableName() + ": " + row.coveredBins() + " of " + row.bins().size()
                    + " bins covered" + (empty.isEmpty() ? "" : "; empty: " + empty));
        }

        @Override protected void paintComponent(Graphics g) {
            var d = row.definition();
            boolean integer = "int".equals(d.type());
            double min = integer ? Math.ceil(d.min()) : d.min();
            double maximum = integer ? Math.floor(d.max()) + 1.0 : d.max();
            double range = maximum - min;
            int left = 4, top = 7, height = getHeight() - 14;
            int width = Math.max(1, getWidth() - 8);
            if (range <= 0) {
                return;
            }
            for (int px = 0; px < width; px++) {
                double x = min + range * (px + 0.5) / width;
                int index = binIndex(integer ? Math.floor(x) : x);
                if (index < 0) {
                    continue;
                }
                g.setColor(colourAt(x, row.bins().get(index), row.binValues().get(index), integer));
                g.fillRect(left + px, top, 1, height);
            }
            g.setColor(Color.BLACK);
            for (List<Double> values : row.binValues()) {
                for (double value : values) {
                    int px = left + (int) Math.round(width * (value - min) / range);
                    g.drawLine(px, top, px, top + height / 3);
                }
            }
            g.setColor(Color.DARK_GRAY);
            for (var bin : row.bins()) {
                int px = left + (int) Math.round(width * (bin.lower() - min) / range);
                g.drawLine(px, top - 2, px, top + height + 1);
            }
            g.drawLine(left + width - 1, top - 2, left + width - 1, top + height + 1);
        }

        private int binIndex(double x) {
            List<StructuralCoverageSupport.CoverageBin> bins = row.bins();
            for (int i = 0; i < bins.size(); i++) {
                var bin = bins.get(i);
                if (bin.matchesAny(List.of(x))) {
                    return i;
                }
            }
            return -1;
        }
    }
}
