package odme.odmeeditor;

import odme.domain.coverage.ParameterCoverageMapAnalyzer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Swing presentation for the marginal (one-parameter-at-a-time) coverage map. */
final class ParameterCoverageMapPanel extends JPanel {

    private final StructuralCoverageSupport.CoverageContext context;
    private final JComboBox<StructuralCoverageSupport.ParameterDefinition> parameterChooser;
    private final JTextField edgesField = new JTextField(26);
    private final JTextField bandwidthField = new JTextField(7);
    private final JTextField adequacyField = new JTextField("1.0", 5);
    private final JTextField coldField = new JTextField("0.25", 5);
    private final JTextField priorityField = new JTextField(14);
    private final JComboBox<ParameterCoverageMapAnalyzer.Kernel> kernelChooser =
            new JComboBox<>(ParameterCoverageMapAnalyzer.Kernel.values());
    private final JComboBox<ParameterCoverageMapAnalyzer.Weighting> weightingChooser =
            new JComboBox<>(ParameterCoverageMapAnalyzer.Weighting.values());
    private final JCheckBox sampleMarkers = new JCheckBox("Sample markers", true);
    private final JCheckBox classifiedView = new JCheckBox("Classified-region view");
    private final HeatStrip strip = new HeatStrip();
    private final DefaultTableModel regionModel = new DefaultTableModel(
            new Object[]{"Interval", "Samples", "Mean", "Minimum", "Cold", "Class", "Weight"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable regionTable = new JTable(regionModel);
    private final JLabel details = new JLabel("Calculate coverage, then select a region for its traceability details.");
    private ParameterCoverageMapAnalyzer.Result result;

    ParameterCoverageMapPanel(StructuralCoverageSupport.CoverageContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        parameterChooser = new JComboBox<>(context.parameterDefinitions().toArray(StructuralCoverageSupport.ParameterDefinition[]::new));
        parameterChooser.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value instanceof StructuralCoverageSupport.ParameterDefinition definition
                        ? definition.pathDisplay() + " / " + definition.variableName() : "";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        parameterChooser.addActionListener(event -> resetDefaults());
        resetDefaults();

        // Keep the map controls on two explicit rows. A single FlowLayout in a
        // BorderLayout.NORTH panel reports only its one-row preferred height,
        // which used to clip the Calculate map button on wide parameter paths.
        JPanel configuration = new JPanel();
        configuration.setLayout(new javax.swing.BoxLayout(configuration, javax.swing.BoxLayout.Y_AXIS));
        JPanel configurationHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel configurationLabel = new JLabel("Parameter Coverage Map configuration");
        configurationLabel.setFont(configurationLabel.getFont().deriveFont(java.awt.Font.BOLD));
        JButton helpButton = new JButton("?");
        helpButton.setToolTipText("Explain Parameter Coverage Map controls");
        helpButton.setMargin(new java.awt.Insets(0, 7, 0, 7));
        helpButton.addActionListener(event -> showConfigurationHelp());
        // Keep the primary action in the header rather than at the end of a
        // long settings row, where a narrow dialog could hide it.
        JButton calculate = new JButton("Calculate map");
        calculate.setToolTipText("Calculate coverage for the selected parameter and settings");
        calculate.addActionListener(event -> calculate());
        configurationHeader.add(configurationLabel);
        configurationHeader.add(helpButton);
        configurationHeader.add(calculate);
        configuration.add(configurationHeader);
        JPanel selectionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        parameterChooser.setPreferredSize(new Dimension(620, 28));
        parameterChooser.setMaximumSize(new Dimension(620, 28));
        parameterChooser.setToolTipText("Select the numeric ODD parameter to analyse.");
        selectionRow.add(new JLabel("Parameter:"));
        selectionRow.add(parameterChooser);
        selectionRow.add(new JLabel("Edges:"));
        selectionRow.add(edgesField);

        JPanel calculationRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        calculationRow.add(new JLabel("Kernel:"));
        calculationRow.add(kernelChooser);
        calculationRow.add(new JLabel("Bandwidth:"));
        calculationRow.add(bandwidthField);
        calculationRow.add(new JLabel("Adequacy:"));
        calculationRow.add(adequacyField);
        calculationRow.add(new JLabel("Cold:"));
        calculationRow.add(coldField);
        calculationRow.add(new JLabel("Weighting:"));
        calculationRow.add(weightingChooser);
        calculationRow.add(new JLabel("Priority weights:"));
        calculationRow.add(priorityField);
        calculationRow.add(sampleMarkers);
        calculationRow.add(classifiedView);
        configuration.add(selectionRow);
        configuration.add(calculationRow);
        sampleMarkers.addActionListener(event -> strip.repaint());
        classifiedView.addActionListener(event -> strip.repaint());
        weightingChooser.addActionListener(event -> priorityField.setEnabled(
                weightingChooser.getSelectedItem() == ParameterCoverageMapAnalyzer.Weighting.PRIORITY));
        priorityField.setEnabled(false);
        add(configuration, BorderLayout.NORTH);

        JPanel mapPanel = new JPanel(new BorderLayout());
        mapPanel.setBorder(BorderFactory.createTitledBorder("Marginal coverage heat — fixed adequacy scale (not multidimensional ODD coverage)"));
        mapPanel.add(strip, BorderLayout.CENTER);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        legend.add(new JLabel("Cold"));
        legend.add(colourSwatch(new Color(0xB91C1C)));
        legend.add(new JLabel("Partial"));
        legend.add(colourSwatch(new Color(0xFBBF24)));
        legend.add(new JLabel("Adequate"));
        legend.add(colourSwatch(new Color(0x15803D)));
        legend.add(new JLabel("Click a region to inspect it. Edges retain true physical width."));
        mapPanel.add(legend, BorderLayout.SOUTH);

        regionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        regionTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showRegionDetails(regionTable.getSelectedRow());
        });
        JPanel detailPanel = new JPanel(new BorderLayout(4, 4));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Region metrics and traceability"));
        detailPanel.add(new JScrollPane(regionTable), BorderLayout.CENTER);
        details.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        detailPanel.add(details, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapPanel, detailPanel);
        split.setResizeWeight(0.52);
        add(split, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportCsv = new JButton("Export CSV");
        exportCsv.addActionListener(event -> exportCsv());
        JButton exportSvg = new JButton("Export SVG");
        exportSvg.addActionListener(event -> exportSvg());
        JButton copyConstraints = new JButton("Copy cold-region constraints");
        copyConstraints.addActionListener(event -> copyColdRegions());
        actions.add(copyConstraints);
        actions.add(exportCsv);
        actions.add(exportSvg);
        add(actions, BorderLayout.SOUTH);
    }

    private void showConfigurationHelp() {
        JOptionPane.showMessageDialog(this, """
                Row 1 — choose the numeric ODD parameter and its region edges.
                Edges are comma-separated and must begin at the parameter minimum
                and end at its maximum. Example: 0, 1, 3, 8, 9, 10.

                Row 2 — configure how local support is calculated:
                • Kernel: use TRIANGULAR as the usual default.
                • Bandwidth: how far each sample contributes heat.
                • Adequacy: support required for full (green) coverage.
                • Cold: coverage below this threshold is counted as cold.
                • Weighting: GEOMETRIC weights physical range widths; EQUAL_REGION
                  weights each region equally; PRIORITY uses supplied weights that sum to 1.

                Click Calculate map to update the strip and region metrics.
                The bottom Calculate Coverage button belongs to the original
                coverage estimate and does not calculate this map.

                Sample markers toggles black sample dots. Classified-region view
                shows discrete red/yellow/green classes instead of the heat gradient.
                """, "Parameter Coverage Map help", JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel colourSwatch(Color color) {
        JLabel swatch = new JLabel("   ");
        swatch.setOpaque(true);
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        return swatch;
    }

    private void resetDefaults() {
        StructuralCoverageSupport.ParameterDefinition definition = selectedDefinition();
        if (definition == null) return;
        List<Double> edges = StructuralCoverageSupport.defaultRegionEdges(definition);
        edgesField.setText(join(edges));
        bandwidthField.setText(number((definition.max() - definition.min()) / 20.0));
        result = null;
        regionModel.setRowCount(0);
        strip.repaint();
    }

    private void calculate() {
        try {
            StructuralCoverageSupport.ParameterDefinition definition = selectedDefinition();
            if (definition == null) throw new IllegalArgumentException("Select an ODD parameter.");
            List<Double> edges = StructuralCoverageSupport.parseRegionEdges(edgesField.getText(), definition);
            ParameterCoverageMapAnalyzer.Kernel kernel = (ParameterCoverageMapAnalyzer.Kernel) kernelChooser.getSelectedItem();
            ParameterCoverageMapAnalyzer.Weighting weighting = (ParameterCoverageMapAnalyzer.Weighting) weightingChooser.getSelectedItem();
            double bandwidth = parsePositive(bandwidthField, "Bandwidth");
            double adequacy = parsePositive(adequacyField, "Adequacy target");
            double cold = Double.parseDouble(coldField.getText().trim());
            List<Double> weights = weighting == ParameterCoverageMapAnalyzer.Weighting.PRIORITY
                    ? parseWeights(priorityField.getText()) : List.of();
            BackgroundTaskRunner.run(this, "Parameter Coverage Map", "Calculating marginal parameter coverage...",
                    () -> StructuralCoverageSupport.analyzeCoverageMap(context, definition, edges, kernel, bandwidth,
                            adequacy, cold, weighting, weights),
                    this::showResult,
                    error -> JOptionPane.showMessageDialog(this, error.getMessage(), "Parameter Coverage Map", JOptionPane.ERROR_MESSAGE));
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Invalid coverage-map configuration", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showResult(ParameterCoverageMapAnalyzer.Result nextResult) {
        result = nextResult;
        regionModel.setRowCount(0);
        for (ParameterCoverageMapAnalyzer.RegionResult region : result.regions()) {
            regionModel.addRow(new Object[]{
                    "[" + number(region.start()) + ", " + number(region.end()) + "]", region.sampleCount(),
                    percent(region.meanCoverage()), percent(region.minimumCoverage()), percent(region.coldFraction()),
                    display(region.classification()), percent(region.weight())
            });
        }
        details.setText("Score: " + percent(result.parameterScore()) + " (" + display(result.weighting())
                + "); in-range samples: " + result.inRangeSampleCount() + "; excluded: " + result.excludedSampleCount());
        strip.repaint();
    }

    private void showRegionDetails(int row) {
        if (result == null || row < 0 || row >= result.regions().size()) return;
        ParameterCoverageMapAnalyzer.RegionResult region = result.regions().get(row);
        String warning = region.width() > (selectedDefinition().max() - selectedDefinition().min()) / 3.0
                && region.coldFraction() > 0.10 ? " — warning: wide internally cold region" : "";
        details.setText("[" + number(region.start()) + ", " + number(region.end()) + "] width " + number(region.width())
                + "; samples: " + String.join(", ", region.sampleIds()) + "; influencing: "
                + String.join(", ", region.influencingSampleIds()) + warning);
    }

    private void exportCsv() {
        if (result == null) { noResult(); return; }
        Path target = chooseTarget("parameter-coverage-map.csv");
        if (target == null) return;
        StringBuilder csv = new StringBuilder("parameter,start,end,width,sample_count,influencing_sample_count,mean_coverage,minimum_coverage,cold_fraction,hot_fraction,weight,classification,sample_ids,influencing_sample_ids\n");
        for (ParameterCoverageMapAnalyzer.RegionResult region : result.regions()) {
            csv.append(csv(selectedDefinition().variableName())).append(',').append(region.start()).append(',').append(region.end())
                    .append(',').append(region.width()).append(',').append(region.sampleCount()).append(',')
                    .append(region.influencingSampleCount()).append(',').append(region.meanCoverage()).append(',')
                    .append(region.minimumCoverage()).append(',').append(region.coldFraction()).append(',')
                    .append(region.hotFraction()).append(',').append(region.weight()).append(',').append(region.classification())
                    .append(',').append(csv(String.join(";", region.sampleIds()))).append(',')
                    .append(csv(String.join(";", region.influencingSampleIds()))).append('\n');
        }
        write(target, csv.toString());
    }

    private void exportSvg() {
        if (result == null) { noResult(); return; }
        Path target = chooseTarget("parameter-coverage-map.svg");
        if (target == null) return;
        StructuralCoverageSupport.ParameterDefinition definition = selectedDefinition();
        StringBuilder svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"180\" viewBox=\"0 0 1200 180\">\n");
        svg.append("<text x=\"20\" y=\"25\" font-family=\"sans-serif\" font-size=\"16\">")
                .append(escape(definition.variableName())).append(" — Parameter Coverage Map (score ")
                .append(percent(result.parameterScore())).append(")</text>\n");
        double range = definition.max() - definition.min();
        for (ParameterCoverageMapAnalyzer.RegionResult region : result.regions()) {
            double x = 20 + 1140 * (region.start() - definition.min()) / range;
            double width = 1140 * region.width() / range;
            svg.append("<rect x=\"").append(x).append("\" y=\"55\" width=\"").append(width)
                    .append("\" height=\"50\" fill=\"").append(hex(colour(region.meanCoverage()))).append("\" stroke=\"#334155\"/>\n");
            svg.append("<text x=\"").append(x).append("\" y=\"130\" font-family=\"sans-serif\" font-size=\"11\">")
                    .append(number(region.start())).append("</text>\n");
        }
        svg.append("<text x=\"1140\" y=\"130\" font-family=\"sans-serif\" font-size=\"11\">")
                .append(number(definition.max())).append("</text></svg>\n");
        write(target, svg.toString());
    }

    private void copyColdRegions() {
        if (result == null) { noResult(); return; }
        StringBuilder constraints = new StringBuilder();
        for (ParameterCoverageMapAnalyzer.RegionResult region : result.regions()) {
            if (region.classification() == ParameterCoverageMapAnalyzer.Classification.COLD
                    || region.classification() == ParameterCoverageMapAnalyzer.Classification.UNCOVERED) {
                constraints.append(selectedDefinition().variableName()).append(" in [")
                        .append(number(region.start())).append(", ").append(number(region.end())).append("]")
                        .append("; mean coverage=").append(percent(region.meanCoverage()))
                        .append("; cold fraction=").append(percent(region.coldFraction())).append('\n');
            }
        }
        if (constraints.isEmpty()) constraints.append("No cold or uncovered regions.");
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(constraints.toString()), null);
        JOptionPane.showMessageDialog(this, "Cold-region constraints copied for a sampling workflow.", "Parameter Coverage Map", JOptionPane.INFORMATION_MESSAGE);
    }

    private Path chooseTarget(String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(suggestedName));
        return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    private void write(Path path, String content) {
        try {
            Files.writeString(path, content);
            JOptionPane.showMessageDialog(this, "Exported " + path, "Parameter Coverage Map", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, "Unable to write export: " + error.getMessage(), "Parameter Coverage Map", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void noResult() { JOptionPane.showMessageDialog(this, "Calculate the map before exporting it.", "Parameter Coverage Map", JOptionPane.INFORMATION_MESSAGE); }
    private StructuralCoverageSupport.ParameterDefinition selectedDefinition() { return (StructuralCoverageSupport.ParameterDefinition) parameterChooser.getSelectedItem(); }
    private double parsePositive(JTextField field, String name) { double value = Double.parseDouble(field.getText().trim()); if (value <= 0) throw new IllegalArgumentException(name + " must be positive."); return value; }
    private List<Double> parseWeights(String raw) { List<Double> values = new ArrayList<>(); for (String token : raw.split("[,;\\s]+")) if (!token.isBlank()) values.add(Double.parseDouble(token)); return values; }
    private static String join(List<Double> values) { return values.stream().map(ParameterCoverageMapPanel::number).collect(java.util.stream.Collectors.joining(", ")); }
    private static String number(double value) { return String.format(Locale.US, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static String percent(double value) { return String.format(Locale.US, "%.1f%%", value * 100.0); }
    private static String display(Object value) { return String.valueOf(value).toLowerCase(Locale.ROOT).replace('_', ' '); }
    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String hex(Color colour) { return String.format("#%02x%02x%02x", colour.getRed(), colour.getGreen(), colour.getBlue()); }
    private static Color colour(double coverage) { return blend(new Color(0xB91C1C), new Color(0x15803D), coverage); }
    private static Color blend(Color low, Color high, double value) { double t = Math.max(0.0, Math.min(1.0, value)); return new Color((int) (low.getRed() + (high.getRed() - low.getRed()) * t), (int) (low.getGreen() + (high.getGreen() - low.getGreen()) * t), (int) (low.getBlue() + (high.getBlue() - low.getBlue()) * t)); }

    private final class HeatStrip extends JPanel {
        HeatStrip() {
            setPreferredSize(new Dimension(980, 210));
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) { selectRegion(event.getX()); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (result == null || selectedDefinition() == null) {
                graphics.drawString("Configure a parameter and calculate its coverage map.", 14, 30);
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 70, right = getWidth() - 30, top = 54, height = 70;
            StructuralCoverageSupport.ParameterDefinition definition = selectedDefinition();
            double range = definition.max() - definition.min();
            List<ParameterCoverageMapAnalyzer.HeatPoint> points = result.heatField();
            for (int i = 0; i < points.size() - 1; i++) {
                ParameterCoverageMapAnalyzer.HeatPoint point = points.get(i);
                int x = (int) Math.round(left + (right - left) * (point.x() - definition.min()) / range);
                int next = (int) Math.round(left + (right - left) * (points.get(i + 1).x() - definition.min()) / range);
                g.setColor(classifiedView.isSelected() ? classifiedColour(point.coverage()) : colour(point.coverage()));
                g.fillRect(x, top, Math.max(1, next - x + 1), height);
            }
            g.setStroke(new BasicStroke(1.2f));
            for (ParameterCoverageMapAnalyzer.RegionResult region : result.regions()) {
                int x = (int) Math.round(left + (right - left) * (region.start() - definition.min()) / range);
                g.setColor(new Color(0x334155));
                g.drawLine(x, top - 6, x, top + height + 6);
                g.drawString(number(region.start()), x - 8, top + height + 25);
            }
            g.drawString(number(definition.max()), right - 8, top + height + 25);
            if (sampleMarkers.isSelected()) drawSampleMarkers(g, left, right, top, height, definition, range);
            g.setColor(Color.DARK_GRAY);
            g.drawString(definition.variableName() + " [" + number(definition.min()) + " — " + number(definition.max()) + "]", left, 28);
            g.dispose();
        }

        private void drawSampleMarkers(Graphics2D g, int left, int right, int top, int height,
                                       StructuralCoverageSupport.ParameterDefinition definition, double range) {
            g.setColor(Color.BLACK);
            for (ParameterCoverageMapAnalyzer.Sample sample : result.samples()) {
                int x = (int) Math.round(left + (right - left) * (sample.value() - definition.min()) / range);
                g.fillOval(x - 2, top + height / 2 - 2, 4, 4);
            }
        }

        private Color classifiedColour(double coverage) {
            return coverage < 0.25 ? new Color(0xB91C1C) : coverage < 0.75 ? new Color(0xFBBF24) : new Color(0x15803D);
        }

        private void selectRegion(int x) {
            if (result == null || selectedDefinition() == null) return;
            int left = 70, right = getWidth() - 30;
            double value = selectedDefinition().min() + (selectedDefinition().max() - selectedDefinition().min()) * (x - left) / (double) (right - left);
            for (int index = 0; index < result.regions().size(); index++) {
                ParameterCoverageMapAnalyzer.RegionResult region = result.regions().get(index);
                if (value >= region.start() && value <= region.end()) {
                    regionTable.setRowSelectionInterval(index, index);
                    return;
                }
            }
        }
    }
}
