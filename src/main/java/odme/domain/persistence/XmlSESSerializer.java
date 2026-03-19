package odme.domain.persistence;

import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * XML-based implementation of {@link SESSerializer}.
 *
 * Writes and reads SES trees in a structured XML format compatible with the
 * ses.xsd schema described in Karmokar et al. (SummerSim 2019).
 *
 * This replaces the binary .ssdbeh/.ssdvar/.ssdcon/.ssdflag serialization,
 * producing human-readable, diffable, version-controllable project files.
 *
 * XML format:
 * <ses id="..." name="..." schemaVersion="1.0" createdAt="...">
 *   <entity id="..." name="...">
 *     <variables>
 *       <variable name="speed" value="100"/>
 *     </variables>
 *     <behaviours>
 *       <behaviour>hover</behaviour>
 *     </behaviours>
 *     <constraints>
 *       <constraint>speed &lt; 500</constraint>
 *     </constraints>
 *     <aspect id="..." name="...">
 *       ...children...
 *     </aspect>
 *   </entity>
 * </ses>
 */
public class XmlSESSerializer implements SESSerializer {

    private static final Logger log = LoggerFactory.getLogger(XmlSESSerializer.class);
    private static final String SCHEMA_VERSION = "1.0";

    @Override
    public void write(SESTree tree, Path path) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element root = doc.createElement("ses");
            root.setAttribute("id", tree.getId());
            root.setAttribute("name", tree.getName());
            root.setAttribute("schemaVersion", SCHEMA_VERSION);
            root.setAttribute("createdAt", tree.getCreatedAt().toString());
            root.setAttribute("lastModifiedAt", tree.getLastModifiedAt().toString());
            doc.appendChild(root);

            tree.getRoot().ifPresent(sesRoot -> {
                Element rootElement = serializeNode(doc, sesRoot);
                root.appendChild(rootElement);
            });

            // Write to file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            path.getParent().toFile().mkdirs();
            transformer.transform(new DOMSource(doc), new StreamResult(path.toFile()));

            log.info("Wrote SES '{}' to {}", tree.getName(), path);

        } catch (Exception e) {
            throw new IOException("Failed to write SES to " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public SESTree read(Path path) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(path.toFile());
            doc.getDocumentElement().normalize();

            Element sesElement = doc.getDocumentElement();
            String id = sesElement.getAttribute("id");
            String name = sesElement.getAttribute("name");

            if (id.isBlank()) id = UUID.randomUUID().toString();
            if (name.isBlank()) name = path.getFileName().toString();

            SESTree tree = new SESTree(id, name);

            // Find the single child element (the root node)
            NodeList children = sesElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child instanceof Element childElement) {
                    SESNode rootNode = deserializeNodeShallow(childElement);
                    tree.setRoot(rootNode);
                    deserializeChildren(childElement, rootNode.getId(), tree);
                    break;
                }
            }

            log.info("Read SES '{}' from {}: {} nodes", name, path, tree.size());
            return tree;

        } catch (Exception e) {
            throw new IOException("Failed to read SES from " + path + ": " + e.getMessage(), e);
        }
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    private Element serializeNode(Document doc, SESNode node) {
        String tag = node.getType().getXmlTag();
        Element element = doc.createElement(tag);
        element.setAttribute("id", node.getId());
        element.setAttribute("name", node.getName());

        // Variables
        if (!node.getVariables().isEmpty()) {
            Element vars = doc.createElement("variables");
            for (Map.Entry<String, String> entry : node.getVariables().entrySet()) {
                Element var = doc.createElement("variable");
                var.setAttribute("name", entry.getKey());
                var.setAttribute("value", entry.getValue());
                vars.appendChild(var);
            }
            element.appendChild(vars);
        }

        // Behaviours
        if (!node.getBehaviours().isEmpty()) {
            Element behaviours = doc.createElement("behaviours");
            for (String b : node.getBehaviours()) {
                Element beh = doc.createElement("behaviour");
                beh.setTextContent(b);
                behaviours.appendChild(beh);
            }
            element.appendChild(behaviours);
        }

        // Constraints
        if (!node.getConstraints().isEmpty()) {
            Element constraints = doc.createElement("constraints");
            for (String c : node.getConstraints()) {
                Element con = doc.createElement("constraint");
                con.setTextContent(c);
                constraints.appendChild(con);
            }
            element.appendChild(constraints);
        }

        // Flags
        if (!node.getFlags().isEmpty()) {
            Element flags = doc.createElement("flags");
            for (Map.Entry<String, String> entry : node.getFlags().entrySet()) {
                Element flag = doc.createElement("flag");
                flag.setAttribute("key", entry.getKey());
                flag.setAttribute("value", entry.getValue());
                flags.appendChild(flag);
            }
            element.appendChild(flags);
        }

        // Children (recursively)
        for (SESNode child : node.getChildren()) {
            element.appendChild(serializeNode(doc, child));
        }

        return element;
    }

    /**
     * Creates a shallow SESNode (without children) from an XML element,
     * populating only metadata (variables, behaviours, constraints, flags).
     */
    private SESNode deserializeNodeShallow(Element element) {
        String tag = element.getTagName();
        SESNodeType type = SESNodeType.fromXmlTag(tag);
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");

        if (id.isBlank()) id = UUID.randomUUID().toString();
        if (name.isBlank()) name = tag;

        SESNode node = new SESNode(id, name, type);

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element childElement)) continue;

            switch (childElement.getTagName()) {
                case "variables" -> deserializeVariables(childElement, node);
                case "behaviours" -> deserializeBehaviours(childElement, node);
                case "constraints" -> deserializeConstraints(childElement, node);
                case "flags" -> deserializeFlags(childElement, node);
                default -> { /* child nodes handled separately via deserializeChildren */ }
            }
        }

        return node;
    }

    /**
     * Recursively deserializes child SES nodes from an XML element,
     * attaching them to the tree using {@link SESTree#addNode}.
     */
    private void deserializeChildren(Element element, String parentId, SESTree tree) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element childElement)) continue;

            String tag = childElement.getTagName();
            if (tag.equals("variables") || tag.equals("behaviours")
                    || tag.equals("constraints") || tag.equals("flags")) {
                continue;
            }

            SESNode childNode = deserializeNodeShallow(childElement);
            tree.addNode(parentId, childNode);
            deserializeChildren(childElement, childNode.getId(), tree);
        }
    }

    private void deserializeVariables(Element varsElement, SESNode node) {
        NodeList vars = varsElement.getElementsByTagName("variable");
        for (int i = 0; i < vars.getLength(); i++) {
            Element var = (Element) vars.item(i);
            node.putVariable(var.getAttribute("name"), var.getAttribute("value"));
        }
    }

    private void deserializeBehaviours(Element behsElement, SESNode node) {
        NodeList behs = behsElement.getElementsByTagName("behaviour");
        for (int i = 0; i < behs.getLength(); i++) {
            node.addBehaviour(behs.item(i).getTextContent());
        }
    }

    private void deserializeConstraints(Element consElement, SESNode node) {
        NodeList cons = consElement.getElementsByTagName("constraint");
        for (int i = 0; i < cons.getLength(); i++) {
            node.addConstraint(cons.item(i).getTextContent());
        }
    }

    private void deserializeFlags(Element flagsElement, SESNode node) {
        NodeList flags = flagsElement.getElementsByTagName("flag");
        for (int i = 0; i < flags.getLength(); i++) {
            Element flag = (Element) flags.item(i);
            node.putFlag(flag.getAttribute("key"), flag.getAttribute("value"));
        }
    }
}
