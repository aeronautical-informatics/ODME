package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.core.EditorContext;
import odme.core.XmlJTree;
import odme.jtreetograph.JtreeToGraphSave;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ScenarioArtifactSync {

    private static final String EXPORT_ROOT_DIR = "ScenarioExports";
    private static final String XML_EXPORT_DIR = "xml";

    private ScenarioArtifactSync() {
    }

    static void syncCurrentProject(List<String[]> scenarioRows) throws IOException {
        String projectName = EditorContext.getInstance().getProjName();
        Path projectDirectory = Path.of(EditorContext.getInstance().getFileLocation(), projectName);
        syncProject(projectDirectory, projectName, scenarioRows);
    }

    private static void syncProject(Path projectDirectory,
                                    String projectName,
                                    List<String[]> scenarioRows) throws IOException {
        Path exportRoot = projectDirectory.resolve(EXPORT_ROOT_DIR);
        Path xmlExportDirectory = exportRoot.resolve(XML_EXPORT_DIR);
        Files.createDirectories(xmlExportDirectory);
        clearXmlCopies(xmlExportDirectory);

        List<ScenarioSummary> summaries = new ArrayList<>();
        Set<String> variableHeaders = new LinkedHashSet<>();

        for (String[] scenarioRow : scenarioRows) {
            String scenarioName = safeField(scenarioRow, 0);
            if (scenarioName.isBlank()) {
                continue;
            }

            Path scenarioDirectory = projectDirectory.resolve(scenarioName);
            if (!Files.isDirectory(scenarioDirectory)) {
                continue;
            }

            Path readableScenarioXml = rebuildReadableScenarioXml(projectName, scenarioDirectory);
            Files.copy(
                    readableScenarioXml,
                    xmlExportDirectory.resolve(scenarioName + ".xml"),
                    StandardCopyOption.REPLACE_EXISTING
            );

            ScenarioSummary summary = summarizeScenario(
                    readableScenarioXml,
                    scenarioName,
                    safeField(scenarioRow, 1),
                    safeField(scenarioRow, 2)
            );
            summaries.add(summary);
            variableHeaders.addAll(summary.variableValues.keySet());
        }

        writeSummaryCsv(exportRoot.resolve("scenario-summary.csv"), summaries, new ArrayList<>(variableHeaders));
    }

    private static void clearXmlCopies(Path xmlExportDirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(xmlExportDirectory, "*.xml")) {
            for (Path xmlFile : stream) {
                Files.deleteIfExists(xmlFile);
            }
        }
    }

    private static Path rebuildReadableScenarioXml(String projectName, Path scenarioDirectory) throws IOException {
        Path treeXml = scenarioDirectory.resolve(projectName + ".xml");
        Path serializedVariables = scenarioDirectory.resolve(projectName + ".ssdvar");
        Path readableXml = scenarioDirectory.resolve("xmlforxsd.xml");

        if (!Files.exists(treeXml)) {
            throw new FileNotFoundException("Scenario tree not found: " + treeXml);
        }

        XmlJTree xmlJTree = new XmlJTree(treeXml.toString());
        if (xmlJTree.dtModel == null) {
            throw new IOException("Unable to read scenario tree: " + treeXml);
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) xmlJTree.dtModel.getRoot();
        Multimap<TreePath, String> variables = loadSerializedMultimap(serializedVariables.toFile());
        writeReadableScenarioXml(root, variables, readableXml);
        return readableXml;
    }

    @SuppressWarnings("unchecked")
    private static Multimap<TreePath, String> loadSerializedMultimap(File file)
            throws IOException {
        if (!file.exists()) {
            return ArrayListMultimap.create();
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            Object deserialized = objectInputStream.readObject();
            if (deserialized instanceof Multimap<?, ?> multimap) {
                return (Multimap<TreePath, String>) multimap;
            }
            throw new IOException("Unexpected metadata format in " + file.getAbsolutePath());
        } catch (ClassNotFoundException ex) {
            throw new IOException("Unable to read metadata from " + file.getAbsolutePath(), ex);
        }
    }

    private static void writeReadableScenarioXml(DefaultMutableTreeNode root,
                                                 Multimap<TreePath, String> variables,
                                                 Path target)
            throws IOException {
        try {
            DOMImplementation domImplementation =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            Document document = domImplementation.createDocument(null, null, null);
            Element rootElement = buildReadableScenarioElement(document, root, new ArrayList<>(), variables, true);
            document.appendChild(rootElement);
            JtreeToGraphSave.saveToXMLFile(document, target.toString());
        } catch (ParserConfigurationException | TransformerException ex) {
            throw new IOException("Failed to rebuild readable XML for " + target, ex);
        }
    }

    private static Element buildReadableScenarioElement(Document document,
                                                        DefaultMutableTreeNode node,
                                                        List<String> parentPath,
                                                        Multimap<TreePath, String> variables,
                                                        boolean isRoot) {
        String label = node.toString();
        Element element = createReadableElement(document, label, isRoot);
        List<String> currentPath = appendPath(parentPath, label);

        for (String rawVariable : metadataValuesForPath(variables, currentPath)) {
            appendVariableElement(document, element, rawVariable);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            element.appendChild(buildReadableScenarioElement(document, child, currentPath, variables, false));
        }

        return element;
    }

    private static Element createReadableElement(Document document, String label, boolean isRoot) {
        Element element;
        if (label.endsWith("Dec")) {
            element = document.createElement("aspect");
            element.setAttribute("name", label);
            return element;
        }
        if (label.endsWith("MAsp")) {
            element = document.createElement("multiAspect");
            element.setAttribute("name", label);
            return element;
        }
        if (label.endsWith("Spec")) {
            element = document.createElement("specialization");
            element.setAttribute("name", label);
            return element;
        }

        element = document.createElement("entity");
        element.setAttribute("name", label);
        if (isRoot) {
            element.setAttribute("xmlns:vc", "http://www.w3.org/2007/XMLSchema-versioning");
            element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            element.setAttribute("xsi:noNamespaceSchemaLocation", "ses.xsd");
        }
        return element;
    }

    private static List<String> metadataValuesForPath(Multimap<TreePath, String> source, List<String> pathParts) {
        List<String> values = new ArrayList<>();
        for (TreePath key : source.keySet()) {
            if (pathMatches(key, pathParts)) {
                values.addAll(source.get(key));
            }
        }
        return values;
    }

    private static boolean pathMatches(TreePath path, List<String> pathParts) {
        Object[] rawSegments = path.getPath();
        if (rawSegments.length != pathParts.size()) {
            return false;
        }

        for (int i = 0; i < rawSegments.length; i++) {
            if (!String.valueOf(rawSegments[i]).equals(pathParts.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void appendVariableElement(Document document, Element parent, String rawVariable) {
        if (rawVariable == null || rawVariable.isBlank()) {
            return;
        }

        String[] parts = rawVariable.split(",", -1);
        if (parts.length < 3) {
            return;
        }

        Element variableElement = document.createElement("var");
        variableElement.setAttribute("name", parts[0].trim());
        variableElement.setAttribute("type", parts[1].trim());
        variableElement.setAttribute("default", parts[2].trim());
        if (parts.length > 3 && !parts[3].trim().isEmpty()) {
            variableElement.setAttribute("lower", parts[3].trim());
        }
        if (parts.length > 4 && !parts[4].trim().isEmpty()) {
            variableElement.setAttribute("upper", parts[4].trim());
        }
        parent.appendChild(variableElement);
    }

    private static ScenarioSummary summarizeScenario(Path readableScenarioXml,
                                                     String scenarioName,
                                                     String risk,
                                                     String remarks) throws IOException {
        Document document = parseXml(readableScenarioXml);
        List<String> selectedNodes = new ArrayList<>();
        Map<String, String> variableValues = new LinkedHashMap<>();

        collectScenarioData(document.getDocumentElement(), new ArrayList<>(), selectedNodes, variableValues);

        return new ScenarioSummary(
                scenarioName,
                risk,
                remarks,
                String.join("; ", selectedNodes),
                variableValues
        );
    }

    private static Document parseXml(Path xmlPath) throws IOException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(xmlPath.toFile());
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception ex) {
            throw new IOException("Unable to read scenario XML " + xmlPath, ex);
        }
    }

    private static void collectScenarioData(Element element,
                                            List<String> entityPath,
                                            List<String> selectedNodes,
                                            Map<String, String> variableValues) {
        List<String> currentEntityPath = entityPath;
        if ("entity".equals(element.getTagName())) {
            String entityName = element.getAttribute("name").trim();
            currentEntityPath = appendPath(entityPath, entityName);

            for (Element childElement : childElements(element)) {
                if ("entity".equals(childElement.getTagName())) {
                    String childName = childElement.getAttribute("name").trim();
                    if (!entityName.isEmpty() && !childName.isEmpty()) {
                        selectedNodes.add(entityName + "=" + childName);
                    }
                } else if ("var".equals(childElement.getTagName())) {
                    String variableName = childElement.getAttribute("name").trim();
                    String variableValue = childElement.getAttribute("default").trim();
                    String header = buildVariableHeader(currentEntityPath, variableName);
                    variableValues.put(header, variableValue);
                }
            }
        }

        for (Element childElement : childElements(element)) {
            if (!"var".equals(childElement.getTagName())) {
                collectScenarioData(childElement, currentEntityPath, selectedNodes, variableValues);
            }
        }
    }

    private static List<Element> childElements(Element parent) {
        List<Element> childElements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) child);
            }
        }
        return childElements;
    }

    private static String buildVariableHeader(List<String> entityPath, String variableName) {
        List<String> relevantPath = entityPath;
        if (relevantPath.size() > 1) {
            relevantPath = relevantPath.subList(1, relevantPath.size());
        }

        if (relevantPath.isEmpty()) {
            return variableName;
        }
        return String.join("/", relevantPath) + "." + variableName;
    }

    private static void writeSummaryCsv(Path target,
                                        List<ScenarioSummary> summaries,
                                        List<String> variableHeaders) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            List<String> header = new ArrayList<>();
            header.add("Scenario");
            header.add("Risk");
            header.add("Remarks");
            header.add("SelectedNodes");
            header.addAll(variableHeaders);

            writer.write(csvLine(header));
            writer.newLine();

            for (ScenarioSummary summary : summaries) {
                List<String> row = new ArrayList<>();
                row.add(summary.scenarioName);
                row.add(summary.risk);
                row.add(summary.remarks);
                row.add(summary.selectedNodes);
                for (String variableHeader : variableHeaders) {
                    row.add(summary.variableValues.getOrDefault(variableHeader, ""));
                }

                writer.write(csvLine(row));
                writer.newLine();
            }
        }
    }

    private static String csvLine(List<String> values) {
        List<String> escapedValues = new ArrayList<>(values.size());
        for (String value : values) {
            escapedValues.add(escapeCsv(value));
        }
        return String.join(",", escapedValues);
    }

    private static String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")
                || normalized.contains("\r")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private static List<String> appendPath(List<String> path, String element) {
        List<String> result = new ArrayList<>(path);
        result.add(element);
        return result;
    }

    private static String safeField(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index];
    }

    private record ScenarioSummary(String scenarioName,
                                   String risk,
                                   String remarks,
                                   String selectedNodes,
                                   Map<String, String> variableValues) {
    }
}
