package behaviourtreetograph;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static odme.jtreetograph.JtreeToGraphVariables.behaviourGraphComponent;

/** Loads either a saved mXGraph behavior graph or an exported behavior-tree XML file. */
public final class BehaviourGraphLoader {
    private BehaviourGraphLoader() { }

    public static void open(File file) throws Exception {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Behavior tree file does not exist: " + file);
        }
        loadInto(JTreeToGraphBehaviour.benhaviourGraph, file);
        JTreeToGraphBehaviour.behaviourParent = JTreeToGraphBehaviour.benhaviourGraph.getDefaultParent();
        behaviourGraphComponent.refresh();
        behaviourGraphComponent.revalidate();
        behaviourGraphComponent.repaint();
    }

    static void loadInto(mxGraph graph, File file) throws Exception {
        Document document = parse(file);
        graph.getModel().beginUpdate();
        try {
            JTreeToGraphBehaviour.behaviorsWithAttributes.clear();
            clear(graph);
            if ("mxGraphModel".equals(document.getDocumentElement().getTagName())) {
                mxCodec codec = new mxCodec(document);
                codec.decode(document.getDocumentElement(), graph.getModel());
            } else {
                importBehaviourTree(graph, document);
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    private static void clear(mxGraph graph) {
        Object[] cells = graph.getChildCells(graph.getDefaultParent(), true, true);
        if (cells.length > 0) {
            graph.removeCells(cells, true);
        }
    }

    private static void importBehaviourTree(mxGraph graph, Document document) {
        Element tree = firstElement(document, "BehaviourTree");
        if (tree == null) {
            throw new IllegalArgumentException("Expected an mXGraph model or a <BehaviourTree> XML document");
        }

        Object parent = graph.getDefaultParent();
        mxCell root = (mxCell) graph.insertVertex(parent, "rootnode", "Events", 80, 30, 100, 30, "Entity");
        AtomicInteger nextId = new AtomicInteger(1);
        AtomicInteger nextRow = new AtomicInteger(0);
        Element first = firstElementChild(tree);
        if (first != null) {
            importNode(graph, parent, root, first, 1, nextId, nextRow);
        }
        graph.insertVertex(parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
        graph.insertVertex(parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
    }

    private static void importNode(mxGraph graph, Object parent, mxCell parentCell, Element source,
                                   int depth, AtomicInteger nextId, AtomicInteger nextRow) {
        String tag = source.getTagName();
        String style = switch (tag) {
            case "Selector" -> "Selector";
            case "Sequence" -> "Sequence";
            case "Parallel" -> "Parallel";
            case "Decorator" -> "Decorator";
            default -> "Entity";
        };
        String label = switch (tag) {
            case "Action" -> source.getAttribute("name");
            case "Decorator" -> source.hasAttribute("condition") && !source.getAttribute("condition").isBlank()
                    ? "Decorator_" + source.getAttribute("condition") : "Decorator";
            case "RootNode" -> source.hasAttribute("name") ? source.getAttribute("name") : "Events";
            default -> tag;
        };
        int row = nextRow.getAndIncrement();
        double width = "Entity".equals(style) ? Math.max(100, label.length() * 8.0 + 30) : 45;
        mxCell cell = (mxCell) graph.insertVertex(parent, "loaded_" + nextId.getAndIncrement(), label,
                80 + depth * 150, 90 + row * 65, width, 30, style);
        graph.insertEdge(parent, null, "", parentCell, cell);
        restoreActionAttributes(source, label);

        NodeList children = source.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                importNode(graph, parent, cell, (Element) children.item(i), depth + 1, nextId, nextRow);
            }
        }
    }

    private static void restoreActionAttributes(Element source, String behaviorName) {
        if (!"Action".equals(source.getTagName()) || !source.hasAttribute("Attributes")) {
            return;
        }
        BehaviorAttributeOverview overview = null;
        for (BehaviorAttributeOverview candidate : JTreeToGraphBehaviour.behaviorsWithAttributes) {
            if (candidate.getBehaviorName().equals(behaviorName)) {
                overview = candidate;
                break;
            }
        }
        if (overview == null) {
            overview = new BehaviorAttributeOverview(behaviorName);
            JTreeToGraphBehaviour.behaviorsWithAttributes.add(overview);
        }
        for (String assignment : source.getAttribute("Attributes").split(";")) {
            String[] parts = assignment.trim().split("=", 2);
            if (parts.length == 2 && !parts[0].trim().isEmpty()) {
                overview.addAttribute(new BehaviorAttribute(parts[0].trim(), parts[1].trim()));
            }
        }
    }

    private static Element firstElement(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static Element firstElementChild(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) children.item(i);
            }
        }
        return null;
    }

    private static Document parse(File file) throws Exception {
        if (file.getName().endsWith("behaviourGraph.xml")) {
            return mxXmlUtils.parseXml(mxUtils.readFile(file.getAbsolutePath()));
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(file);
    }
}
