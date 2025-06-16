package odme.odmeeditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

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
            if (!hasExtension(file, "xml")) {
                console.append("\nError: Selected file is not an XML file.");
                return;
            }
            lastLoadedFilename = removeExtension(file.getName());
            console.append("\nLoading XML file: " + file.getName());
            progressBar.setValue(25);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(file);
                String formattedXml = formatXml(document);
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

    // here is the  helper method to remove the extension from a filename
    private String removeExtension(String filename) {
        return filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
    }


    /**
     * Formats an XML Document to a pretty-printed string with indentation.
     *
     * @param document The XML Document to format,
     * @return A string representation of the formatted XML
     * @throws Exception If an error occurs during formatting
     */
    private String formatXml(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
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

                    // Convert XML to Python dictionary
                    StringBuilder pythonCode = new StringBuilder("# Translated XML to Python\n\n");
                    pythonCode.append("data = {\n");
                    translateXmlToPython(document.getDocumentElement(), pythonCode, 1);
                    pythonCode.append("}\n");

                    // Update UI (on the Event Dispatch Thread)
                    SwingUtilities.invokeLater(() -> {
                        pythonEditor.setText(pythonCode.toString());
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

    /**
     * Recursively translates the given XML node (and its children) into part of a Python dictionary.
     *
     * The translation follows these rules:
     * - If the node has a "name" attribute, we use its value as the key; otherwise we use the node’s name.
     * - If the node has no child elements but some text content, that text (or a composed attribute dictionary)
     *   is used as the value.
     * - If the node has child elements, they are nested inside the current key.
     *
     * @param node        the current XML node to process
     * @param pythonCode  a StringBuilder that accumulates the Python code
     * @param indentLevel the current level of indentation (each level equals 4 spaces)
     */
    private void translateXmlToPython(Node node, StringBuilder pythonCode, int indentLevel) {
        // Create an indent string based on the current indentation level (4 spaces per level)
        String indent = "    ".repeat(indentLevel);

        // Retrieve the node's attributes and name
        NamedNodeMap attributes = node.getAttributes();
        String nodeName = node.getNodeName();

        // If a "name" attribute exists, use its value -- otherwise, fallback to the node name
        String entityName = getAttributeValue(attributes, "name", nodeName);
        String keyName = entityName != null ? entityName : nodeName;

        // Get all child nodes and filter out non-element nodes
        NodeList children = node.getChildNodes();
        List<Node> childElements = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add(child);
            }
        }

        // ----- Handle leaf nodes: nodes with no child elements -----
        if (childElements.isEmpty()) {
            // Get text content of the node, trimmed of extra spaces
            String textContent = node.getTextContent().trim();

            // If there is text, output it as the value for this key
            if (!textContent.isEmpty()) {
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': ")
                        .append(formatValue(textContent))
                        .append(",\n");
                return;
            }

            // If there are attributes, represent them as a dictionary value
            if (attributes != null && attributes.getLength() > 0) {
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': {\n");
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    String attrName = attr.getNodeName();
                    // Ignore the 'name' attribute as it was already used above
                    if (!attrName.equals("name")) {
                        pythonCode.append(indent)
                                .append("    '").append(attrName)
                                .append("': ")
                                .append(formatValue(attr.getNodeValue()))
                                .append(",\n");
                    }
                }
                pythonCode.append(indent).append("},\n");
            } else {
                // No text and no attributes: output a None value
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': None,\n");
            }
            return;
        }

        // ----- Handle nodes with child elements -----
        // Start by writing the key for this node
        pythonCode.append(indent)
                .append("'").append(keyName).append("': ");

        // Group child elements by their tag (node) name
        Map<String, List<Node>> groupedChildren = new LinkedHashMap<>();
        for (Node child : childElements) {
            String childName = child.getNodeName();
            groupedChildren.computeIfAbsent(childName, k -> new ArrayList<>()).add(child);
        }

        // Check if all children are similar leaf nodes so that we can represent them as a list
        if (groupedChildren.size() == 1 && allChildrenAreLeafNodes(childElements)) {
            pythonCode.append("[");
            for (Node child : childElements) {
                // For each child, try to use its "name" attribute if available
                String childKey = getAttributeValue(child.getAttributes(), "name", child.getNodeName());
                pythonCode.append(formatValue(childKey)).append(", ");
            }
            pythonCode.append("],\n");
        } else {
            // Otherwise, represent the children as a dictionary
            pythonCode.append("{\n");
            for (Map.Entry<String, List<Node>> entry : groupedChildren.entrySet()) {
                List<Node> nodes = entry.getValue();
                String childKey = entry.getKey();

                if (nodes.size() > 1) {
                    // If there are multiple children with the same name, represent them as a list
                    pythonCode.append(indent)
                            .append("    '").append(childKey).append("': [\n");
                    for (Node childNode : nodes) {
                        pythonCode.append(indent)
                                .append("        {\n");
                        translateXmlToPython(childNode, pythonCode, indentLevel + 3);
                        pythonCode.append(indent)
                                .append("        },\n");
                    }
                    pythonCode.append(indent)
                            .append("    ],\n");
                } else {
                    // If there's only one child for this key, process it normally
                    translateXmlToPython(nodes.get(0), pythonCode, indentLevel + 1);
                }
            }
            // Close the dictionary for children
            pythonCode.append(indent).append("},\n");
        }
    }

    // Helper method to check if all child nodes are leaf nodes
    private boolean allChildrenAreLeafNodes(List<Node> nodes) {
        for (Node node : nodes) {
            if (node.hasChildNodes()) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        return false;
                    }
                }
            }
            if (node.hasAttributes() && node.getAttributes().getLength() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to convert the given string to a number.
     * If the string contains a decimal point, it is treated as a Double;
     * otherwise, as an Integer. If the value cannot be parsed as a number,
     * it is returned as a quoted string with single quotes escaped.
     *
     * @param value the input string to format
     * @return a string representing either a numeric value or a quoted string literal
     */
    private String formatValue(String value) {
        try {
            if (value.contains(".")) {
                return String.valueOf(Double.parseDouble(value));
            }
            return String.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return "'" + value.replace("'", "\\'") + "'"; // Escape single quotes
        }
    }

    /**
     * Retrieves the value of the specified attribute from a collection of XML attributes.
     * If the attribute is not found or if the attributes collection is null, the default value is returned.
     *
     * @param attributes   the map of attributes from an XML element (may be null)
     * @param attributeName the name of the attribute to retrieve
     * @param defaultValue  the default value to return if the attribute isn't present
     * @return the value of the attribute if found, otherwise the defaultValue
     */

    private String getAttributeValue(NamedNodeMap attributes, String attributeName, String defaultValue) {
        if (attributes != null) {
            Node attribute = attributes.getNamedItem(attributeName);
            if (attribute != null) {
                return attribute.getNodeValue();
            }
        }
        return defaultValue;
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

            // Clean up the YAML data to remove leading hyphens and unnecessary whitespace
            Object cleanedData = cleanYamlData(yamlData);

            // Generate Python translation
            StringBuilder pythonCode = new StringBuilder("# Translated YAML to Python\n\n");
            pythonCode.append("config = ");
            generatePythonFromYaml(cleanedData, pythonCode, 0);
            pythonCode.append("\n");

            // Display the generated Python code
            pythonEditor.setText(pythonCode.toString());
            progressBar.setValue(100);
            statusBar.setText("YAML Translation Completed");
            console.append("\nYAML Translation Completed Successfully!");
        } catch (Exception ex) {
            console.append("\nError translating YAML: " + ex.getMessage());
            statusBar.setText("Error");
            progressBar.setValue(0);
        }
    }
    /**
     * Recursively generates Python code from YAML data.
     * Depending on the type of yamlData (Map, List, or a simple value),
     * it delegates the work to the corresponding helper method.
     *
     * @param yamlData    The YAML data to convert.
     * @param pythonCode  A StringBuilder where the Python code is accumulated.
     * @param indentLevel The current level of indentation (each level = 4 spaces).
     */
    private void generatePythonFromYaml(Object yamlData, StringBuilder pythonCode, int indentLevel) {
        // Create a string representing the current indentation
        String indent = "    ".repeat(indentLevel);

        // If yamlData is a dictionary (Map), handle it as a Python dictionary.
        if (yamlData instanceof Map) {
            handleMap((Map<?, ?>) yamlData, pythonCode, indentLevel, indent);
        }
        // If yamlData is a list, handle it as a Python list.
        else if (yamlData instanceof List) {
            handleList((List<?>) yamlData, pythonCode, indentLevel, indent);
        }
        // Otherwise, format the simple value (number, string, etc.) as a Python literal.
        else {
            pythonCode.append(formatPythonValue(yamlData));
        }
    }

    /**
     * Processes a Map (dictionary) from the YAML data.
     * Each key/value pair is translated into a Python key/value pair.
     *
     * @param map         The Map containing YAML key/values.
     * @param pythonCode  The StringBuilder that collects the Python code.
     * @param indentLevel The current level of indentation.
     * @param indent      The string representing current indentation.
     */
    private void handleMap(Map<?, ?> map, StringBuilder pythonCode, int indentLevel, String indent) {
        // Start with an opening brace and a newline.
        pythonCode.append("{\n");
        boolean firstEntry = true;

        // Process each entry in the map
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // Get and clean the key by trimming white spaces.
            String key = entry.getKey().toString().trim();
            // If the key starts with a hyphen (an artifact sometimes), remove it.
            if (key.startsWith("-")) {
                key = key.substring(1).trim();
            }
            Object value = entry.getValue();

            // Add commas between entries, but not before the first one.
            if (!firstEntry) {
                pythonCode.append(",\n");
            }
            // Increase the indentation for nested content.
            String childIndent = "    ".repeat(indentLevel + 1);

            // Create the Python key (wrapped in single quotes with escaped inner quotes)
            pythonCode.append(childIndent)
                    .append("'")
                    .append(key.replace("'", "\\'"))
                    .append("': ");
            // Recursively process the value associated with this key.
            generatePythonFromYaml(value, pythonCode, indentLevel + 1);
            firstEntry = false;
        }
        // Close the Python dictionary by matching the indentation.
        pythonCode.append("\n").append(indent).append("}");
    }

    /**
     * Processes a List from the YAML data.
     * Each element in the list is translated into a Python list item.
     *
     * @param list        The list of YAML items.
     * @param pythonCode  The StringBuilder that collects the Python code.
     * @param indentLevel The current level of indentation.
     * @param indent      The string representing the current indentation.
     */
    private void handleList(List<?> list, StringBuilder pythonCode, int indentLevel, String indent) {
        // Start with an opening bracket and a newline.
        pythonCode.append("[\n");
        // Process each element in the list one by one.
        for (int i = 0; i < list.size(); i++) {
            String childIndent = "    ".repeat(indentLevel + 1);
            // Add indentation for list items.
            pythonCode.append(childIndent);
            generatePythonFromYaml(list.get(i), pythonCode, indentLevel + 1);
            // Separate items with commas.
            if (i < list.size() - 1) {
                pythonCode.append(",");
            }
            pythonCode.append("\n");
        }
        // Close the list matching the current indent level.
        pythonCode.append(indent).append("]");
    }

    /**
     * Formats a simple value (such as a string, number, or boolean)
     * into the appropriate Python literal.
     *
     * @param value The object representing the value.
     * @return A string representing the Python literal for that value.
     */
    private String formatPythonValue(Object value) {
        // If there's no value, output the Python "None".
        if (value == null) return "None";

        // For empty maps, output '{}' instead of 'None'
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) return "{}";

        // Process string values: trim them and decide whether to treat as number or literal string.
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) return "None";
            // If the string is numeric, output it without quotes.
            try {
                Double.parseDouble(str);
                return str;
            } catch (NumberFormatException e) {
                // Otherwise, escape any single quotes and wrap the string in quotes.
                return "'" + str.replace("'", "\\'") + "'";
            }
        }
        // For numbers, convert them directly to string.
        if (value instanceof Number) return value.toString();
        // For boolean values, use Python's True or False.
        if (value instanceof Boolean) return (Boolean) value ? "True" : "False";
        // Fallback: treat any other type as a string literal.
        return "'" + value.toString().replace("'", "\\'") + "'";
    }

    /**
     * Recursively clean up YAML data by trimming keys and, if desired,
     * removing unwanted leading characters (such as hyphens).
     */
    private Object cleanYamlData(Object data) {
        if (data instanceof Map) {
            Map<Object, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                String key = entry.getKey().toString().trim();
                if (key.startsWith("-")) {
                    key = key.substring(1).trim();
                }
                map.put(key, cleanYamlData(entry.getValue()));
            }
            return map;
        } else if (data instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<?>) data) {
                list.add(cleanYamlData(item));
            }
            return list;
        } else {
            return data;
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

    // helper method to check the extension of the file before loading
    private boolean hasExtension(File file, String... extensions) {
        String fileName = file.getName().toLowerCase();
        for (String ext : extensions) {
            if (fileName.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

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


