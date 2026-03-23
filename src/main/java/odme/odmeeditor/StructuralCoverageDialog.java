package odme.odmeeditor;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

        private CoverageReportDialog(JDialog parent, StructuralCoverageSupport.CoverageReport report) {
            super(parent, "Estimate Coverage Report", true);
            setLayout(new BorderLayout(10, 10));

            JLabel title = new JLabel(
                    "Coverage Report for " + report.projectName() + "    Scenarios analyzed: " + report.scenarioCount(),
                    SwingConstants.LEFT
            );
            title.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
            add(title, BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Summary", wrapTable(buildSummaryTable(report)));
            tabs.addTab("Structural Details", wrapTable(buildStructuralTable(report)));
            tabs.addTab("Parameter Details", wrapTable(buildParameterTable(report)));
            add(tabs, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dispose());
            buttons.add(closeButton);
            add(buttons, BorderLayout.SOUTH);

            setMinimumSize(new Dimension(1050, 640));
            pack();
            setLocationRelativeTo(parent);
        }

        private JScrollPane wrapTable(JTable table) {
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(980, 500));
            return scrollPane;
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
