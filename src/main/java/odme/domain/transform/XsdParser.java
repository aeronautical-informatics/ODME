package odme.domain.transform;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses XSD files into a flat list of data rows suitable for table display
 * or further conversion (e.g. to YAML).
 *
 * <p>Extracted from {@code odme.odmeeditor.ODDManager} so that the parsing
 * logic can be tested and reused without any Swing dependency.</p>
 */
public class XsdParser {

    /**
     * Parses an XSD file at the given path and returns the extracted data rows.
     *
     * @param path absolute path to the XSD file
     * @return a list of {@code String[]} rows, each representing a node or variable
     * @throws SAXException                 if XML is malformed
     * @throws IOException                  if the file cannot be read
     * @throws ParserConfigurationException if the parser cannot be configured
     */
    public List<String[]> readXsd(String path) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(path));
        doc.getDocumentElement().normalize();
        return parseDocument(doc);
    }

    /**
     * Parses an XSD from an {@link InputStream} and returns the extracted data rows.
     *
     * @param inputStream the XSD input stream
     * @return a list of data rows
     * @throws SAXException                 if XML is malformed
     * @throws IOException                  if the stream cannot be read
     * @throws ParserConfigurationException if the parser cannot be configured
     */
    public List<String[]> readXsd(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(inputStream);
        doc.getDocumentElement().normalize();
        return parseDocument(doc);
    }

    /**
     * Parses an already-loaded DOM document.
     */
    private List<String[]> parseDocument(Document doc) {
        List<String[]> dataSource = new ArrayList<>();
        List<Element> processed = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("xs:element");
        processNodeList(nodes, dataSource, processed, 0);
        return dataSource;
    }

    /**
     * Recursively processes {@code xs:element} tags at one depth level. For each
     * unprocessed element, it extracts the node header and any contained
     * {@code xs:attribute} tags, then recurses into child {@code xs:element}
     * tags.
     *
     * @param nodes     the list of {@code xs:element} nodes to process
     * @param src       accumulator for extracted data rows
     * @param processed list of already-processed elements (to avoid duplicates)
     * @param indent    current indentation level
     */
    void processNodeList(NodeList nodes, List<String[]> src, List<Element> processed, int indent) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element curr = (Element) nodes.item(i);
            if (!processed.contains(curr)) {
                processElementTag(curr, src, indent);
                NodeList kids = curr.getElementsByTagName("xs:element");
                if (kids.getLength() > 0) {
                    processNodeList(kids, src, processed, indent + 1);
                }
                processed.add(curr);
            }
        }
    }

    /**
     * Processes a single {@code xs:element} tag without touching its children.
     * Extracts the node header and any direct {@code xs:attribute} tags.
     *
     * @param e      the element to process
     * @param src    accumulator for extracted data rows
     * @param indent current indentation level
     */
    void processElementTag(Element e, List<String[]> src, int indent) {
        src.add(getNodeHeaders(e, indent));

        NodeList attrs = e.getElementsByTagName("xs:attribute");
        int attrIndent = indent + 2;
        for (int i = 0; i < attrs.getLength(); i++) {
            Element curr = (Element) attrs.item(i);
            String firstParentName = findFirstParent(curr, "xs:element");
            if (firstParentName.equals(e.getAttribute("name"))
                    && (!curr.hasAttribute("use") || !curr.getAttribute("use").equals("optional"))) {
                src.add(processAttributeTag(curr, attrIndent));
            }
        }
    }

    /**
     * Finds the name attribute of the first ancestor of {@code e} whose tag name
     * matches the given {@code tagName}.
     *
     * @param e       the starting element
     * @param tagName the ancestor tag name to search for
     * @return the "name" attribute of the found ancestor, or "[NO_PARENT]"
     */
    String findFirstParent(Element e, String tagName) {
        Node curr = e;
        Node n;
        do {
            n = curr.getParentNode();
            curr = n;
        } while (n != null && !n.getNodeName().equals(tagName));
        if (n == null) return "[NO_PARENT]";
        return ((Element) n).getAttribute("name");
    }

    /**
     * Extracts a node's header information as a data row.
     *
     * @param e      the {@code xs:element}
     * @param indent current indentation level
     * @return a 5-element array: [indented name, "Node", null, null, null]
     */
    String[] getNodeHeaders(Element e, int indent) {
        String[] r = new String[5];
        r[0] = indentStr(e.getAttribute("name"), indent, "  ");
        r[1] = "Node";
        return r;
    }

    /**
     * Processes an {@code xs:attribute} tag and extracts its properties
     * (type restriction, min/max inclusive values).
     *
     * @param e      the {@code xs:attribute} element
     * @param indent current indentation level
     * @return a 6-element data row
     */
    String[] processAttributeTag(Element e, int indent) {
        String[] r = new String[6];
        r[0] = indentStr("-" + e.getAttribute("name"), indent);
        r[1] = "Variable";
        NodeList nl = e.getElementsByTagName("*");
        for (int i = 0; i < nl.getLength(); i++) {
            Element temp = (Element) nl.item(i);
            switch (temp.getNodeName()) {
                case "xs:restriction":
                    r[2] = temp.getAttribute("base").replace("xs:", "");
                    break;
                case "xs:minInclusive":
                    r[3] = temp.getAttribute("value");
                    break;
                case "xs:maxInclusive":
                    r[4] = temp.getAttribute("value");
                    break;
                default:
                    break;
            }
        }
        return r;
    }

    /**
     * Prepends whitespace indentation to a string using single-space indent chars.
     *
     * @param s     the string to indent
     * @param level the number of indent levels
     * @return the indented string
     */
    String indentStr(String s, int level) {
        return indentStr(s, level, " ");
    }

    /**
     * Prepends indentation to a string using a configurable indent string.
     *
     * @param s         the string to indent
     * @param level     the number of indent levels
     * @param indentStr the string used for each indent level
     * @return the indented string
     */
    String indentStr(String s, int level, String indentStr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(indentStr);
        }
        sb.append(s);
        return sb.toString();
    }
}
