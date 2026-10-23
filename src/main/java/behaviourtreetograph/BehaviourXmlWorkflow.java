package behaviourtreetograph;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Path;

/** XML-safe export and merge operations for behavior models. */
public final class BehaviourXmlWorkflow {
    private static final String MAIN_TREE_ID = "MainTree";

    private BehaviourXmlWorkflow() { }

    /**
     * Converts the graph-oriented behavior XML into the behavior-tree payload
     * consumed by the merge operation, while preserving all action attributes.
     */
    public static void exportBehaviourTree(Path graphXml, Path outputXml) throws Exception {
        Document source = parse(graphXml);
        Document output = newDocument();
        Element root = output.createElement("root");
        root.setAttribute("main_tree_to_execute", MAIN_TREE_ID);
        output.appendChild(root);

        Element tree = output.createElement("BehaviourTree");
        tree.setAttribute("ID", MAIN_TREE_ID);
        root.appendChild(tree);

        appendConvertedChildren(source.getDocumentElement(), tree, output);
        write(output, outputXml);
    }

    /**
     * Adds a behavior-tree payload to a scenario document as a legal
     * {@code <behaviour>} child of the scenario root entity.
     */
    public static void mergeIntoScenario(Path scenarioXml, Path behaviourXml, Path outputXml)
            throws Exception {
        Document scenario = parse(scenarioXml);
        Element scenarioRoot = scenario.getDocumentElement();
        if (!"entity".equals(scenarioRoot.getTagName())) {
            throw new IOException("Scenario XML root must be <entity>: " + scenarioXml);
        }

        Document behaviour = parse(behaviourXml);
        String name = treeId(behaviour.getDocumentElement());
        removeExistingBehaviour(scenarioRoot, name);

        Element wrapper = scenario.createElement("behaviour");
        wrapper.setAttribute("name", name);
        wrapper.appendChild(scenario.importNode(behaviour.getDocumentElement(), true));
        scenarioRoot.appendChild(wrapper);
        write(scenario, outputXml);
    }

    private static void appendConvertedChildren(Node sourceParent, Element targetParent, Document output) {
        NodeList children = sourceParent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element source = (Element) child;
            if ("start".equals(source.getTagName())) {
                // Accept legacy files that contain an accidental second <start> wrapper.
                appendConvertedChildren(source, targetParent, output);
                continue;
            }
            targetParent.appendChild(convertElement(source, output));
        }
    }

    private static Element convertElement(Element source, Document output) {
        boolean hasElementChildren = hasElementChildren(source);
        Element target;
        if ("Events".equals(source.getTagName())) {
            target = output.createElement("RootNode");
            target.setAttribute("name", "Events");
        } else if (!hasElementChildren && !"Decorator".equals(source.getTagName())) {
            target = output.createElement("Action");
            target.setAttribute("name", source.getTagName());
        } else {
            target = output.createElement(source.getTagName());
        }
        copyAttributes(source, target);
        NodeList children = source.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                target.appendChild(convertElement((Element) child, output));
            }
        }
        return target;
    }

    private static boolean hasElementChildren(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static void copyAttributes(Element source, Element target) {
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            target.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    private static String treeId(Element behaviourRoot) {
        NodeList trees = behaviourRoot.getElementsByTagName("BehaviourTree");
        if (trees.getLength() > 0) {
            String id = ((Element) trees.item(0)).getAttribute("ID");
            if (!id.isBlank()) {
                return id;
            }
        }
        return MAIN_TREE_ID;
    }

    private static void removeExistingBehaviour(Element scenarioRoot, String name) {
        NodeList children = scenarioRoot.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "behaviour".equals(child.getNodeName())
                    && name.equals(((Element) child).getAttribute("name"))) {
                scenarioRoot.removeChild(child);
            }
        }
    }

    private static Document parse(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(path.toFile());
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().newDocument();
    }

    private static void write(Document document, Path path) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(document), new StreamResult(path.toFile()));
    }
}
