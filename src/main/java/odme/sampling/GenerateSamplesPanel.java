package odme.sampling;

import odme.odmeeditor.BackgroundTaskRunner;
import odme.odmeeditor.ODMEEditor;
import odme.core.EditorContext;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Swing panel for the "Generate Samples" window.
 * Lets the user pick a YAML scenario file, specify the sample count,
 * choose an output CSV path, and run generation in the background.
 */
public class GenerateSamplesPanel extends JPanel {

    private final JTextField numSamplesField;
    private final JTextField filePathField;
    private final JTextField filePathFieldYaml;
    private final JComboBox<String> sourceSelector;
    private final JLabel currentModelValueLabel;
    private final JLabel yamlLabel;
    private final JButton browseButton;
    private final JButton browseButtonYaml;
    private final JButton generateButton;
    private final JButton cancelButton;

    public GenerateSamplesPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Sample source ---
        JLabel sourceLabel = new JLabel("Source:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        add(sourceLabel, gbc);

        sourceSelector = new JComboBox<>(new String[]{"Current Open Model", "YAML File"});
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 4;
        gbc.gridwidth = 2;
        add(sourceSelector, gbc);

        // --- Current model info ---
        JLabel currentModelLabel = new JLabel("Current Model:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        add(currentModelLabel, gbc);

        currentModelValueLabel = new JLabel(currentModelDescription());
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 4; gbc.gridwidth = 2;
        add(currentModelValueLabel, gbc);

        // --- YAML file picker ---
        yamlLabel = new JLabel("Select YAML:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        add(yamlLabel, gbc);

        filePathFieldYaml = new JTextField();
        filePathFieldYaml.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 4;
        add(filePathFieldYaml, gbc);

        browseButtonYaml = new JButton("Browse...");
        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0;
        add(browseButtonYaml, gbc);

        // --- Sample count ---
        JLabel numSamplesLabel = new JLabel("Number of Samples:");
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        add(numSamplesLabel, gbc);

        numSamplesField = new JTextField("100");
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1;
        gbc.gridwidth = 2;
        add(numSamplesField, gbc);

        // --- Output CSV path ---
        JLabel filePathLabel = new JLabel("Save As:");
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        gbc.gridwidth = 1;
        add(filePathLabel, gbc);

        filePathField = new JTextField();
        filePathField.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1;
        add(filePathField, gbc);

        browseButton = new JButton("Browse...");
        gbc.gridx = 2; gbc.gridy = 4; gbc.weightx = 0;
        add(browseButton, gbc);

        // --- Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateButton = new JButton("Generate");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        add(buttonPanel, gbc);

        updateSourceControls();
        addListeners();
    }

    private void addListeners() {
        sourceSelector.addActionListener(e -> updateSourceControls());

        browseButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(ODMEEditor.fileLocation);
            fc.setDialogTitle("Save Sampled Data As");
            fc.setSelectedFile(new File("generated_samples.csv"));
            if (fc.showSaveDialog(GenerateSamplesPanel.this) == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        browseButtonYaml.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(ODMEEditor.fileLocation);
            fc.setFileFilter(new FileNameExtensionFilter("YAML Files (*.yaml)", "yaml"));
            fc.setAcceptAllFileFilterUsed(false);
            fc.setDialogTitle("Select YAML File");
            if (fc.showOpenDialog(GenerateSamplesPanel.this) == JFileChooser.APPROVE_OPTION) {
                filePathFieldYaml.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        generateButton.addActionListener(e -> {
            String outputCsvPath = filePathField.getText().trim();
            if (outputCsvPath.isEmpty()) {
                JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                        "Please select an output file path.", "Input Required", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int numberOfSamples;
            try {
                numberOfSamples = Integer.parseInt(numSamplesField.getText().trim());
                if (numberOfSamples <= 0) {
                    JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                            "Please enter a positive number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                        "Invalid number format for samples.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean useCurrentModel = sourceSelector.getSelectedIndex() == 0;
            String yamlFilePath = filePathFieldYaml.getText().trim();
            if (!useCurrentModel && yamlFilePath.isEmpty()) {
                JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                        "Please select a YAML file or switch to Current Open Model.", "Input Required",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            setControlsEnabled(false);
            final int finalSamples = numberOfSamples;
            final boolean finalUseCurrentModel = useCurrentModel;

            BackgroundTaskRunner.run(GenerateSamplesPanel.this,
                    "Generate Samples",
                    "Generating valid samples...",
                    () -> {
                        SamplingManager samplingManager = new SamplingManager();
                        if (finalUseCurrentModel) {
                            samplingManager.generateSamplesForCurrentModel(finalSamples, outputCsvPath);
                        } else {
                            samplingManager.generateSamples(yamlFilePath, finalSamples, outputCsvPath);
                        }
                        return finalSamples;
                    },
                    generatedCount -> {
                        setControlsEnabled(true);
                        JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                                "Successfully generated " + generatedCount + " valid samples!",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        SwingUtilities.getWindowAncestor(GenerateSamplesPanel.this).dispose();
                    },
                    ex -> {
                        setControlsEnabled(true);
                        JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                                "An error occurred:\n" + ex.getMessage(),
                                "Sampling Error", JOptionPane.ERROR_MESSAGE);
                    });
        });

        cancelButton.addActionListener(e ->
                SwingUtilities.getWindowAncestor(GenerateSamplesPanel.this).dispose());
    }

    private void setControlsEnabled(boolean enabled) {
        numSamplesField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        sourceSelector.setEnabled(enabled);
        browseButtonYaml.setEnabled(enabled && sourceSelector.getSelectedIndex() == 1);
        generateButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private void updateSourceControls() {
        boolean useCurrentModel = sourceSelector.getSelectedIndex() == 0;
        filePathFieldYaml.setEnabled(!useCurrentModel);
        browseButtonYaml.setEnabled(!useCurrentModel);
        yamlLabel.setEnabled(!useCurrentModel);
        currentModelValueLabel.setEnabled(true);
    }

    private String currentModelDescription() {
        return EditorContext.getInstance().getWorkingDir() + File.separator + EditorContext.getInstance().getProjName();
    }
}
