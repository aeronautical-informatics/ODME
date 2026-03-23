package odme.odmeeditor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StructuralCoverageDialog extends JDialog {

    private final StructuralCoverageSupport.CoverageContext context;
    private final JTable parameterTable;
    private final DefaultTableModel parameterModel;

    private StructuralCoverageDialog(JFrame parent, StructuralCoverageSupport.CoverageContext context) {
        super(parent, "Estimate Coverage Configuration", true);
        this.context = context;

        setLayout(new BorderLayout(10, 10));

        JTextArea instructions = new JTextArea(
                "The table below shows numeric ODD parameters with variable ranges. "
                        + "\"Bin Size\" creates uniform bins across the ODD range. "
                        + "\"Custom Bins\" overrides Bin Size for that row and accepts values such as 0-2;2-8;8-10.");
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel infoLabel = new JLabel(
                "Project: " + context.projectName() + "    Generated scenarios: " + context.scenarioReferences().size(),
                SwingConstants.LEFT
        );
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(instructions);
        topPanel.add(infoLabel);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"ODD Path", "Variable", "Type", "Lower", "Upper", "Bin Size", "Custom Bins"};
        parameterModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 5;
            }
        };

        for (StructuralCoverageSupport.ParameterSelectionRow row : StructuralCoverageSupport.defaultSelectionRows(context)) {
            parameterModel.addRow(new Object[]{
                    row.pathDisplay(),
                    row.variableName(),
                    row.type(),
                    formatNumber(row.min()),
                    formatNumber(row.max()),
                    row.binSize(),
                    row.customBins()
            });
        }

        parameterTable = new JTable(parameterModel);
        parameterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        parameterTable.setAutoCreateRowSorter(true);

        JScrollPane tableScroll = new JScrollPane(parameterTable);
        tableScroll.setPreferredSize(new Dimension(980, 420));
        add(tableScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        if (context.parameterDefinitions().isEmpty()) {
            JLabel emptyLabel = new JLabel(
                    "No numeric ODD parameters with variable ranges were found. Coverage can still be estimated from pruning options.",
                    SwingConstants.CENTER
            );
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            bottomPanel.add(emptyLabel, BorderLayout.NORTH);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton calculateButton = new JButton("Calculate Coverage");
        calculateButton.addActionListener(e -> calculateCoverage());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(calculateButton);
        buttons.add(closeButton);
        bottomPanel.add(buttons, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(parent);
    }

    static void open(JFrame parent, List<String[]> scenarioRows) {
        BackgroundTaskRunner.run(
                parent,
                "Estimate Coverage",
                "Loading ODD parameters and generated scenarios...",
                () -> StructuralCoverageSupport.loadCoverageContext(scenarioRows),
                context -> {
                    if (context.scenarioReferences().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                parent,
                                "No generated scenarios were found. Generate or save scenarios first.",
                                "Estimate Coverage",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }
                    new StructuralCoverageDialog(parent, context).setVisible(true);
                },
                error -> JOptionPane.showMessageDialog(
                        parent,
                        "Unable to prepare coverage estimate: " + error.getMessage(),
                        "Estimate Coverage",
                        JOptionPane.ERROR_MESSAGE
                )
        );
    }

    private void calculateCoverage() {
        try {
            List<StructuralCoverageSupport.ParameterSelectionRow> rows = new ArrayList<>();
            for (int row = 0; row < context.parameterDefinitions().size(); row++) {
                StructuralCoverageSupport.ParameterDefinition definition = context.parameterDefinitions().get(row);
                rows.add(new StructuralCoverageSupport.ParameterSelectionRow(
                        definition.id(),
                        definition.pathDisplay(),
                        definition.variableName(),
                        definition.type(),
                        definition.min(),
                        definition.max(),
                        valueAt(row, 5),
                        valueAt(row, 6)
                ));
            }

            BackgroundTaskRunner.run(
                    this,
                    "Estimate Coverage",
                    "Calculating pruning and parameter coverage...",
                    () -> StructuralCoverageSupport.analyze(context, rows),
                    report -> {
                        CoverageReportDialog reportDialog = new CoverageReportDialog(this, report);
                        reportDialog.setVisible(true);
                    },
                    error -> JOptionPane.showMessageDialog(
                            this,
                            "Unable to calculate coverage estimate: " + error.getMessage(),
                            "Estimate Coverage",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Coverage Estimate Configuration", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String valueAt(int row, int column) {
        Object value = parameterModel.getValueAt(row, column);
        return value == null ? "" : String.valueOf(value);
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static final class CoverageReportDialog extends JDialog {

        private static final Color PAGE_BACKGROUND = new Color(0xF6F8FB);
        private static final Color CARD_BACKGROUND = Color.WHITE;
        private static final Color BORDER_COLOR = new Color(0xD9E2EC);
        private static final Color TEXT_PRIMARY = new Color(0x1F2933);
        private static final Color TEXT_SECONDARY = new Color(0x52606D);
        private static final Color OVERALL_COLOR = new Color(0x0A7D5A);
        private static final Color PRUNING_COLOR = new Color(0x1769AA);
        private static final Color PARAMETER_COLOR = new Color(0xE67E22);

        private CoverageReportDialog(JDialog parent, StructuralCoverageSupport.CoverageReport report) {
            super(parent, "Estimate Coverage Report", true);
            setLayout(new BorderLayout(10, 10));

            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setBackground(PAGE_BACKGROUND);
            header.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

            JLabel title = new JLabel("Coverage Report");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            title.setForeground(TEXT_PRIMARY);

            JLabel subtitle = new JLabel(
                    "Project: " + report.projectName() + "    Scenarios analyzed: " + report.scenarioCount(),
                    SwingConstants.LEFT
            );
            subtitle.setForeground(TEXT_SECONDARY);

            header.add(title);
            header.add(Box.createVerticalStrut(4));
            header.add(subtitle);
            add(header, BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Report", wrapPanel(buildSummaryReport(report)));
            tabs.addTab("Summary", wrapTable(buildSummaryTable(report)));
            tabs.addTab("Structural Details", wrapTable(buildStructuralTable(report)));
            tabs.addTab("Parameter Details", wrapTable(buildParameterTable(report)));
            add(tabs, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.setBackground(PAGE_BACKGROUND);
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dispose());
            buttons.add(closeButton);
            add(buttons, BorderLayout.SOUTH);

            setMinimumSize(new Dimension(1050, 640));
            getContentPane().setBackground(PAGE_BACKGROUND);
            pack();
            setLocationRelativeTo(parent);
        }

        private JScrollPane wrapTable(JTable table) {
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(980, 500));
            return scrollPane;
        }

        private JScrollPane wrapPanel(JPanel panel) {
            JScrollPane scrollPane = new JScrollPane(panel);
            scrollPane.setPreferredSize(new Dimension(980, 500));
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            return scrollPane;
        }

        private JPanel buildSummaryReport(StructuralCoverageSupport.CoverageReport report) {
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setBackground(PAGE_BACKGROUND);
            container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            container.add(buildHeroCard(report));
            container.add(Box.createVerticalStrut(12));

            JPanel metricGrid = new JPanel(new GridLayout(1, 3, 12, 0));
            metricGrid.setOpaque(false);
            metricGrid.add(buildMetricCard(
                    "Overall Coverage",
                    "Combined pruning and parameter coverage",
                    report.overallCoveredItems(),
                    report.overallTotalItems(),
                    report.overallCoveragePercent(),
                    OVERALL_COLOR
            ));
            metricGrid.add(buildMetricCard(
                    "Pruning Coverage",
                    "Coverage of specialization choices from pruning",
                    report.coveredStructuralOptions(),
                    report.totalStructuralOptions(),
                    report.structuralCoveragePercent(),
                    PRUNING_COLOR
            ));
            metricGrid.add(buildMetricCard(
                    "Parameter Coverage",
                    "Coverage of configured ODD value bins",
                    report.coveredParameterBins(),
                    report.totalParameterBins(),
                    report.parameterCoveragePercent(),
                    PARAMETER_COLOR
            ));
            container.add(metricGrid);
            container.add(Box.createVerticalStrut(12));

            JPanel insightPanel = createCardPanel();
            insightPanel.setLayout(new BoxLayout(insightPanel, BoxLayout.Y_AXIS));
            insightPanel.add(sectionTitle("Coverage Notes"));
            insightPanel.add(Box.createVerticalStrut(8));
            insightPanel.add(noteLabel(buildInsightText(report)));
            insightPanel.add(Box.createVerticalStrut(10));
            insightPanel.add(noteLabel("Pruning uncovered: "
                    + report.uncoveredStructuralOptions()
                    + " of " + report.totalStructuralOptions()
                    + " options"));
            insightPanel.add(Box.createVerticalStrut(4));
            insightPanel.add(noteLabel("Parameter bins uncovered: "
                    + report.uncoveredParameterBins()
                    + " of " + report.totalParameterBins()
                    + " bins"));
            container.add(insightPanel);

            return container;
        }

        private JPanel buildHeroCard(StructuralCoverageSupport.CoverageReport report) {
            JPanel card = createCardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel label = new JLabel("Overall Coverage Status");
            label.setForeground(TEXT_SECONDARY);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel percent = new JLabel(formatPercent(report.overallCoveragePercent()));
            percent.setForeground(OVERALL_COLOR);
            percent.setFont(percent.getFont().deriveFont(Font.BOLD, 28f));
            percent.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel details = new JLabel(
                    report.overallCoveredItems() + " covered items out of " + report.overallTotalItems()
                            + " across pruning and parameter space");
            details.setForeground(TEXT_PRIMARY);
            details.setAlignmentX(Component.LEFT_ALIGNMENT);

            JProgressBar progressBar = createProgressBar(report.overallCoveragePercent(), OVERALL_COLOR);
            progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(label);
            card.add(Box.createVerticalStrut(8));
            card.add(percent);
            card.add(Box.createVerticalStrut(4));
            card.add(details);
            card.add(Box.createVerticalStrut(12));
            card.add(progressBar);

            return card;
        }

        private JPanel buildMetricCard(String title,
                                       String subtitle,
                                       int covered,
                                       int total,
                                       double percentage,
                                       Color accentColor) {
            JPanel card = createCardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(TEXT_PRIMARY);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subtitleLabel = new JLabel("<html><body style='width:240px'>" + subtitle + "</body></html>");
            subtitleLabel.setForeground(TEXT_SECONDARY);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel ratioLabel = new JLabel(covered + " / " + total);
            ratioLabel.setForeground(accentColor);
            ratioLabel.setFont(ratioLabel.getFont().deriveFont(Font.BOLD, 22f));
            ratioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel percentLabel = new JLabel(formatPercent(percentage));
            percentLabel.setForeground(TEXT_PRIMARY);
            percentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JProgressBar progressBar = createProgressBar(percentage, accentColor);
            progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(titleLabel);
            card.add(Box.createVerticalStrut(6));
            card.add(subtitleLabel);
            card.add(Box.createVerticalStrut(12));
            card.add(ratioLabel);
            card.add(Box.createVerticalStrut(4));
            card.add(percentLabel);
            card.add(Box.createVerticalStrut(10));
            card.add(progressBar);

            return card;
        }

        private JProgressBar createProgressBar(double percentage, Color accentColor) {
            JProgressBar progressBar = new JProgressBar(0, 1000);
            progressBar.setValue((int) Math.round(Math.max(0.0, Math.min(100.0, percentage)) * 10));
            progressBar.setStringPainted(true);
            progressBar.setString(formatPercent(percentage));
            progressBar.setForeground(accentColor);
            progressBar.setBackground(new Color(0xE9EEF5));
            progressBar.setBorder(BorderFactory.createEmptyBorder());
            progressBar.setPreferredSize(new Dimension(260, 22));
            progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            return progressBar;
        }

        private JPanel createCardPanel() {
            JPanel panel = new JPanel();
            panel.setBackground(CARD_BACKGROUND);
            Border border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)
            );
            panel.setBorder(border);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            return panel;
        }

        private JLabel sectionTitle(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(TEXT_PRIMARY);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        private JLabel noteLabel(String text) {
            JLabel label = new JLabel("<html><body style='width:900px'>" + text + "</body></html>");
            label.setForeground(TEXT_SECONDARY);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        private String buildInsightText(StructuralCoverageSupport.CoverageReport report) {
            if (report.overallCoveragePercent() >= 90.0) {
                return "Coverage is strong across both structure and parameter space. Focus next on any remaining uncovered bins or edge-case scenario quality checks.";
            }
            if (report.overallCoveragePercent() >= 60.0) {
                return "Coverage is moderate. The generated set explores a useful part of the design space, but there are still meaningful gaps in pruning choices and/or ODD bins.";
            }
            return "Coverage is still low relative to the reachable design space. Generate additional scenarios or adjust bin definitions to improve structural and parameter exploration.";
        }

        private JTable buildSummaryTable(StructuralCoverageSupport.CoverageReport report) {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Metric", "Covered", "Uncovered", "Total", "Coverage"}, 0);
            model.addRow(new Object[]{
                    "Pruning Coverage",
                    report.coveredStructuralOptions(),
                    report.uncoveredStructuralOptions(),
                    report.totalStructuralOptions(),
                    formatPercent(report.structuralCoveragePercent())
            });
            model.addRow(new Object[]{
                    "Parameter Coverage",
                    report.coveredParameterBins(),
                    report.uncoveredParameterBins(),
                    report.totalParameterBins(),
                    formatPercent(report.parameterCoveragePercent())
            });
            model.addRow(new Object[]{
                    "Overall Coverage",
                    report.overallCoveredItems(),
                    Math.max(0, report.overallTotalItems() - report.overallCoveredItems()),
                    report.overallTotalItems(),
                    formatPercent(report.overallCoveragePercent())
            });
            JTable table = new JTable(model);
            table.setDefaultEditor(Object.class, null);
            table.setRowSelectionAllowed(false);
            table.setAutoCreateRowSorter(false);
            return table;
        }

        private JTable buildStructuralTable(StructuralCoverageSupport.CoverageReport report) {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Decision Node", "Option", "Domain Path", "Covered"}, 0);
            for (StructuralCoverageSupport.StructuralCoverageResult result : report.structuralResults()) {
                model.addRow(new Object[]{
                        result.decisionNode(),
                        result.optionLabel(),
                        result.pathDisplay(),
                        result.covered() ? "Yes" : "No"
                });
            }
            JTable table = new JTable(model);
            table.setDefaultEditor(Object.class, null);
            table.setAutoCreateRowSorter(true);
            return table;
        }

        private JTable buildParameterTable(StructuralCoverageSupport.CoverageReport report) {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"ODD Path", "Variable", "Type", "Range", "Covered", "Total Bins", "Coverage", "Uncovered Bins"}, 0);
            for (StructuralCoverageSupport.ParameterCoverageResult result : report.parameterResults()) {
                model.addRow(new Object[]{
                        result.pathDisplay(),
                        result.variableName(),
                        result.type(),
                        "[" + formatNumber(result.min()) + " - " + formatNumber(result.max()) + "]",
                        result.coveredBins(),
                        result.totalBins(),
                        formatPercent(result.coveragePercent()),
                        result.uncoveredBinLabels()
                });
            }
            JTable table = new JTable(model);
            table.setDefaultEditor(Object.class, null);
            table.setAutoCreateRowSorter(true);
            return table;
        }

        private String formatPercent(double value) {
            return String.format(Locale.US, "%.1f%%", value);
        }
    }
}
