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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists and loads a {@link SESTree} as XML.
 *
 * <p>The XML format mirrors the SES tree structure: each {@code <node>} element
 * corresponds to one {@link SESNode}, with attributes for id, name, and type,
 * and nested child {@code <node>} elements for the tree hierarchy.</p>
 */
public class XmlSESSerializer implements SESSerializer {

    private static final Logger log = LoggerFactory.getLogger(XmlSESSerializer.class);

    @Override
    public void write(SESTree tree, Path path) throws IOException {
        try {
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element root = doc.createElement("ses");
            root.setAttribute("id", tree.getId());
            root.setAttribute("name", tree.getName());
            root.setAttribute("schemaVersion", tree.getSchemaVersion());
            doc.appendChild(root);

            tree.getRoot().ifPresent(rootNode -> {
                Element nodeElement = writeNode(doc, rootNode);
                root.appendChild(nodeElement);
            });

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(path.toFile()));

            log.info("Wrote SES '{}' to {}", tree.getName(), path);
        } catch (Exception e) {
            throw new IOException("Failed to write SES to " + path, e);
        }
    }

    @Override
    public SESTree read(Path path) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(path.toFile());
            doc.getDocumentElement().normalize();

            Element sesElement = doc.getDocumentElement();
            String id = sesElement.getAttribute("id");
            String name = sesElement.getAttribute("name");

            SESTree tree = new SESTree(id, name);

            NodeList nodeList = sesElement.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i) instanceof Element nodeElement) {
                    SESNode rootNode = readNode(nodeElement);
                    tree.setRoot(rootNode);
                    break;
                }
            }

            log.info("Read SES '{}' from {}: {} nodes", name, path, tree.size());
            return tree;
        } catch (Exception e) {
            throw new IOException("Failed to read SES from " + path, e);
        }
    }

    private Element writeNode(Document doc, SESNode node) {
        Element element = doc.createElement("node");
        element.setAttribute("id", node.getId());
        element.setAttribute("name", node.getName());
        element.setAttribute("type", node.getType().name());

        // Variables
        node.getVariables().forEach((k, v) -> {
            Element varEl = doc.createElement("variable");
            varEl.setAttribute("name", k);
            varEl.setAttribute("value", v);
            element.appendChild(varEl);
        });

        // Behaviours
        for (String b : node.getBehaviours()) {
            Element behEl = doc.createElement("behaviour");
            behEl.setTextContent(b);
            element.appendChild(behEl);
        }

        // Constraints
        for (String c : node.getConstraints()) {
            Element conEl = doc.createElement("constraint");
            conEl.setTextContent(c);
            element.appendChild(conEl);
        }

        // Flags
        node.getFlags().forEach((k, v) -> {
            Element flagEl = doc.createElement("flag");
            flagEl.setAttribute("key", k);
            flagEl.setAttribute("value", v);
            element.appendChild(flagEl);
        });

        // Children
        for (SESNode child : node.getChildren()) {
            element.appendChild(writeNode(doc, child));
        }

        return element;
    }

    private SESNode readNode(Element element) {
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        String typeStr = element.getAttribute("type");
        SESNodeType type = SESNodeType.valueOf(typeStr);

        SESNode node = new SESNode(id, name, type);

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                switch (child.getTagName()) {
                    case "node" -> node.addChild(readNode(child));
                    case "variable" -> node.putVariable(
                        child.getAttribute("name"), child.getAttribute("value"));
                    case "behaviour" -> node.addBehaviour(child.getTextContent());
                    case "constraint" -> node.addConstraint(child.getTextContent());
                    case "flag" -> node.putFlag(
                        child.getAttribute("key"), child.getAttribute("value"));
                    default -> { /* unknown element — ignore */ }
                }
            }
        }

        return node;
    }
}
