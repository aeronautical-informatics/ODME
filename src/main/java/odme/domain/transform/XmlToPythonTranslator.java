package odme.domain.transform;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates an XML {@link Document} (or individual {@link Node}) into a
 * Python dictionary literal string.
 *
 * <p>Extracted from {@code odme.odmeeditor.Execution} so that the translation
 * logic can be tested and reused without any Swing dependency.</p>
 */
public class XmlToPythonTranslator {

    /**
     * Translates a complete XML document into a Python dictionary string.
     *
     * @param document the parsed XML document
     * @return a Python source string representing the document as a nested dict
     */
    public String translate(Document document) {
        if (document == null || document.getDocumentElement() == null) {
            throw new IllegalArgumentException("Document or document element is null");
        }
        StringBuilder pythonCode = new StringBuilder("# Translated XML to Python\n\n");
        pythonCode.append("data = {\n");
        translateNode(document.getDocumentElement(), pythonCode, 1);
        pythonCode.append("}\n");
        return pythonCode.toString();
    }

    /**
     * Recursively translates an XML node (and its children) into part of a
     * Python dictionary.
     *
     * <p>Translation rules:
     * <ul>
     *   <li>If the node has a "name" attribute, its value is used as the key;
     *       otherwise the node's tag name is used.</li>
     *   <li>Leaf nodes with text content produce a simple key/value pair.</li>
     *   <li>Leaf nodes with attributes (but no text) produce a nested dict of
     *       those attributes.</li>
     *   <li>Nodes whose children are all same-named leaves are rendered as a
     *       Python list.</li>
     *   <li>All other nodes recurse into a nested dict.</li>
     * </ul>
     *
     * @param node        the current XML node
     * @param pythonCode  accumulator for the generated Python source
     * @param indentLevel nesting depth (4 spaces per level)
     */
    public void translateNode(Node node, StringBuilder pythonCode, int indentLevel) {
        String indent = "    ".repeat(indentLevel);

        NamedNodeMap attributes = node.getAttributes();
        String nodeName = node.getNodeName();

        String entityName = getAttributeValue(attributes, "name", nodeName);
        String keyName = entityName != null ? entityName : nodeName;

        NodeList children = node.getChildNodes();
        List<Node> childElements = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add(child);
            }
        }

        // Handle leaf nodes (no child elements)
        if (childElements.isEmpty()) {
            String textContent = node.getTextContent().trim();

            if (!textContent.isEmpty()) {
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': ")
                        .append(formatValue(textContent))
                        .append(",\n");
                return;
            }

            if (attributes != null && attributes.getLength() > 0) {
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': {\n");
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    String attrName = attr.getNodeName();
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
                pythonCode.append(indent)
                        .append("'").append(keyName).append("': None,\n");
            }
            return;
        }

        // Handle nodes with child elements
        pythonCode.append(indent)
                .append("'").append(keyName).append("': ");

        Map<String, List<Node>> groupedChildren = new LinkedHashMap<>();
        for (Node child : childElements) {
            String childName = child.getNodeName();
            groupedChildren.computeIfAbsent(childName, k -> new ArrayList<>()).add(child);
        }

        if (groupedChildren.size() == 1 && allChildrenAreLeafNodes(childElements)) {
            pythonCode.append("[");
            for (Node child : childElements) {
                String childKey = getAttributeValue(child.getAttributes(), "name", child.getNodeName());
                pythonCode.append(formatValue(childKey)).append(", ");
            }
            pythonCode.append("],\n");
        } else {
            pythonCode.append("{\n");
            for (Map.Entry<String, List<Node>> entry : groupedChildren.entrySet()) {
                List<Node> nodes = entry.getValue();
                String childKey = entry.getKey();

                if (nodes.size() > 1) {
                    pythonCode.append(indent)
                            .append("    '").append(childKey).append("': [\n");
                    for (Node childNode : nodes) {
                        pythonCode.append(indent)
                                .append("        {\n");
                        translateNode(childNode, pythonCode, indentLevel + 3);
                        pythonCode.append(indent)
                                .append("        },\n");
                    }
                    pythonCode.append(indent)
                            .append("    ],\n");
                } else {
                    translateNode(nodes.get(0), pythonCode, indentLevel + 1);
                }
            }
            pythonCode.append(indent).append("},\n");
        }
    }

    /**
     * Checks whether every node in the list is a leaf (no element children
     * and no attributes).
     */
    boolean allChildrenAreLeafNodes(List<Node> nodes) {
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
     * Formats a string value as a Python literal. Numeric strings are output
     * without quotes; non-numeric strings are single-quoted with escaping.
     *
     * @param value the raw string value
     * @return the Python literal representation
     */
    String formatValue(String value) {
        try {
            if (value.contains(".")) {
                return String.valueOf(Double.parseDouble(value));
            }
            return String.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return "'" + value.replace("'", "\\'") + "'";
        }
    }

    /**
     * Retrieves the value of a named attribute, returning a default if absent.
     */
    String getAttributeValue(NamedNodeMap attributes, String attributeName, String defaultValue) {
        if (attributes != null) {
            Node attribute = attributes.getNamedItem(attributeName);
            if (attribute != null) {
                return attribute.getNodeValue();
            }
        }
        return defaultValue;
    }

    /**
     * Pretty-prints an XML document as a string with 4-space indentation.
     *
     * @param document the XML document to format
     * @return the formatted XML string
     * @throws TransformerException if formatting fails
     */
    public String formatXml(Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Removes the file extension from a filename.
     *
     * @param filename the filename (e.g. "model.xml")
     * @return the filename without extension (e.g. "model")
     */
    public static String removeExtension(String filename) {
        return filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
    }

    /**
     * Checks whether a filename ends with one of the given extensions.
     *
     * @param filename   the filename to check
     * @param extensions one or more extensions (without leading dot)
     * @return {@code true} if the filename matches any extension
     */
    public static boolean hasExtension(String filename, String... extensions) {
        String lower = filename.toLowerCase();
        for (String ext : extensions) {
            if (lower.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
