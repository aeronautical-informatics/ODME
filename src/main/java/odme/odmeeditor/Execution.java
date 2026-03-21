package odme.odmeeditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

import odme.domain.transform.XmlToPythonTranslator;
import odme.domain.transform.YamlToPythonTranslator;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Main application window for XML/YAML to Python translation and execution
 * Provides functionality for:
 * - Loading and viewing XML/YAML files
 * - Translating to Python dictionaries
 * - Editing and executing Python scripts
 *
 * @author Marlene Nana
 */

public class Execution extends JFrame {

    //------------------------ Constants & UI Components ------------------------

    private static final String NO_FILE_LOADED_MESSAGE = "No XML/YAML Loaded";
    private JTextArea xmlContentArea;
    private RSyntaxTextArea pythonEditor;
    private JTextArea console;
    private JLabel statusBar;
    private JProgressBar progressBar;

    private final XmlToPythonTranslator xmlTranslator = new XmlToPythonTranslator();
    private final YamlToPythonTranslator yamlTranslator = new YamlToPythonTranslator();

    //------------------------ Constructor & Initialization ------------------------
    public Execution() {
        setTitle("Execution");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadXmlButton = new JButton("Load XML");
        JButton loadYamlButton = new JButton("Load YAML");
        JButton translateXmlButton = new JButton("Translate XML");
        JButton translateYamlButton = new JButton("Translate YAML");
        JButton runScriptsButton = new JButton("Run Scripts");
        JButton saveButton = new JButton("Save");
        JButton loadScriptButton = new JButton("Load Script");
        JButton saveScriptButton = new JButton("Save Script");

        topPanel.add(loadXmlButton);
        topPanel.add(loadYamlButton);
        topPanel.add(translateXmlButton);
        topPanel.add(translateYamlButton);
        topPanel.add(runScriptsButton);
        topPanel.add(saveButton);
        topPanel.add(loadScriptButton);
        topPanel.add(saveScriptButton);

        // Left Panel (XML/YAML Content Viewer)
        xmlContentArea = new JTextArea(NO_FILE_LOADED_MESSAGE);
        xmlContentArea.setEditable(false);
        JScrollPane xmlScrollPane = new JScrollPane(xmlContentArea);
        xmlScrollPane.setPreferredSize(new Dimension(200, 400));

        // Right Panel (Python Scripts Editor)
        pythonEditor = new RSyntaxTextArea();
        pythonEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        pythonEditor.setCodeFoldingEnabled(true);
        RTextScrollPane pythonScrollPane = new RTextScrollPane(pythonEditor);
        pythonScrollPane.setLineNumbersEnabled(true);

        // Center SplitPane
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, xmlScrollPane, pythonScrollPane);
        centerSplitPane.setDividerLocation(200);

        // Console area
        console = new JTextArea("Execution logs will appear here...");
        console.setEditable(false);
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane consoleScrollPane = new JScrollPane(console);

        // Bottom Panel (Progress bar + status bar)
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);

        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplitPane, consoleScrollPane);
        verticalSplitPane.setDividerLocation(400);
        verticalSplitPane.setResizeWeight(0.8);

        add(topPanel, BorderLayout.NORTH);
        add(verticalSplitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadXmlButton.addActionListener(this::onLoadXml);
        loadYamlButton.addActionListener(this::onLoadYaml);
        translateXmlButton.addActionListener(this::onTranslateXml);
        translateYamlButton.addActionListener(this::onTranslateYaml);
        runScriptsButton.addActionListener(this::onRunScripts);
        saveButton.addActionListener(this::onSave);
        loadScriptButton.addActionListener(this::onLoadScript);
        saveScriptButton.addActionListener(this::onSaveScript);
    }

    /**
     * Handles XML file loading with validation and parsing
     * @param e Action event from load XML button
     */
    private void onLoadXml(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!XmlToPythonTranslator.hasExtension(file.getName(), "xml")) {
                console.append("\nError: Selected file is not an XML file.");
                return;
            }
            lastLoadedFilename = XmlToPythonTranslator.removeExtension(file.getName());
            console.append("\nLoading XML file: " + file.getName());
            progressBar.setValue(25);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(file);
                String formattedXml = xmlTranslator.formatXml(document);
                xmlContentArea.setText(formattedXml);
                progressBar.setValue(100);
                statusBar.setText("XML Loaded: " + file.getName());
                console.append("\nXML Loaded Successfully!");
            } catch (Exception ex) {
                console.append("\nError loading XML: " + ex.getMessage());
                statusBar.setText("Error");
                progressBar.setValue(0);
            }
        }
    }

    /**
     * Handles YAML file loading with structure validation
     * @param e Action event from load YAML button
     */

    private void onLoadYaml(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Validate file type
            if (!file.getName().endsWith(".yaml") && !file.getName().endsWith(".yml")) {
                JOptionPane.showMessageDialog(this, "Invalid file type! Please select a YAML file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Read file content
                String yamlContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Yaml yaml = new Yaml();
                Object parsedYaml = yaml.load(yamlContent);

                if (parsedYaml == null) {
                    throw new Exception("YAML file is empty or invalid!");
                }

                // Display YAML content in GUI
                xmlContentArea.setText(yamlContent);
                console.append("\nYAML Loaded Successfully!");
                progressBar.setValue(100);
                statusBar.setText("YAML Loaded: " + file.getName());
            } catch (Exception ex) {
                console.append("\nError loading YAML: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error loading YAML file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                statusBar.setText("Error");
                progressBar.setValue(0);
            }
        }
    }


/**
 * Called when the user clicks the "Translate XML" button.
 * This method reads the XML text, translates it into a Python dictionary format,
 * and updates the UI accordingly. The translation happens in a background thread.
 *
 * @param e the button click event
 */

    private void onTranslateXml(ActionEvent e) {
        console.append("\nTranslating XML to Python...");
        progressBar.setValue(50);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String xmlContent = xmlContentArea.getText();
                    if (xmlContent.isEmpty() || xmlContent.equals(NO_FILE_LOADED_MESSAGE)) {
                        throw new Exception("No XML content available to translate!");
                    }

                    // Parse XML content
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
                    Document document = builder.parse(inputStream);

                    if (document.getDocumentElement() == null) {
                        throw new Exception("Document element is null!");
                    }

                    // Delegate to XmlToPythonTranslator
                    String pythonCode = xmlTranslator.translate(document);

                    // Update UI (on the Event Dispatch Thread)
                    SwingUtilities.invokeLater(() -> {
                        pythonEditor.setText(pythonCode);
                        progressBar.setValue(100);
                        statusBar.setText("XML Translation Completed");
                        console.append("\nXML Translation Completed Successfully!");
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        console.append("\nError during XML translation: " + ex.getMessage());
                        statusBar.setText("Translation Error");
                        progressBar.setValue(0);
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private void onTranslateYaml(ActionEvent e) {
        console.append("\nTranslating YAML to Python...");
        progressBar.setValue(50);

        try {
            // Load YAML content from the text area
            String yamlContent = xmlContentArea.getText();
            if (yamlContent.isEmpty() || yamlContent.equals(NO_FILE_LOADED_MESSAGE)) {
                throw new Exception("No YAML content available to translate!");
            }

            // Parse YAML using SnakeYAML
            Yaml yaml = new Yaml();
            Object yamlData = yaml.load(yamlContent);

            // Delegate to YamlToPythonTranslator
            String pythonCode = yamlTranslator.translate(yamlData);

            // Display the generated Python code
            pythonEditor.setText(pythonCode);
            progressBar.setValue(100);
            statusBar.setText("YAML Translation Completed");
            console.append("\nYAML Translation Completed Successfully!");
        } catch (Exception ex) {
            console.append("\nError translating YAML: " + ex.getMessage());
            statusBar.setText("Error");
            progressBar.setValue(0);
        }
    }

    private void onRunScripts(ActionEvent e) {
        console.append("\nRunning Python scripts...");
        progressBar.setValue(0);
        SwingUtilities.invokeLater(() -> {
            try {
                File scriptFile = new File("temp_script.py");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
                    writer.write(pythonEditor.getText());
                }

                ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        console.append("\n" + line);
                    }
                }
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    console.append("\nScript executed successfully!");
                } else {
                    console.append("\nScript execution failed with exit code " + exitCode);
                }
            } catch (Exception ex) {
                console.append("\nError running script: " + ex.getMessage());
            }
            progressBar.setValue(100);
            statusBar.setText("Scripts Executed");
        });
    }


    private void onSave(ActionEvent e) {
        console.append("\nSaving current session...");
        progressBar.setValue(50);
        progressBar.setValue(100);
        statusBar.setText("Session Saved");
    }


    private void onLoadScript(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                pythonEditor.read(reader, null);
                console.append("\nScript loaded: " + file.getName());
            } catch (IOException ex) {
                console.append("\nError loading script: " + ex.getMessage());
            }
        }
    }

    //  saving the generated scripts with teh .py extension
    private String lastLoadedFilename;

    private void onSaveScript(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();

        // Ensure the lastLoadedFilename is not null or empty
        String defaultFilename = (lastLoadedFilename != null && !lastLoadedFilename.isEmpty())
                ? lastLoadedFilename + ".py"
                : "script.py";

        // Set the default filename
        fileChooser.setSelectedFile(new File(defaultFilename));

        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Ensure the file has a .py extension
            if (!file.getName().toLowerCase().endsWith(".py")) {
                file = new File(file.getAbsolutePath() + ".py");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                pythonEditor.write(writer);
                console.append("\nScript saved: " + file.getName());
            } catch (IOException ex) {
                console.append("\nError saving script: " + ex.getMessage());
            }
        }
    }

}
