package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JtreeToGraphGeneralTest {

    @AfterEach
    void clearStaticGraphState() {
        JTreeToGraphBehaviour.behaviorsWithAttributes.clear();
        JTreeToGraphBehaviour.benhaviourGraph = null;
    }

    @Test
    void behaviourChildNodesExportsDecoratorCondition() throws Exception {
        mxGraph graph = new mxGraph();
        JTreeToGraphBehaviour.benhaviourGraph = graph;

        mxCell decorator;
        graph.getModel().beginUpdate();
        try {
            decorator = (mxCell) graph.insertVertex(
                    graph.getDefaultParent(), null, "Decorator_repeatUntilDone", 0, 0, 80, 30, "Decorator");
        } finally {
            graph.getModel().endUpdate();
        }

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element element = JtreeToGraphGeneral.behaviourChildNodes(document, decorator);

        assertThat(element.getTagName()).isEqualTo("Decorator");
        assertThat(element.getAttribute("condition")).isEqualTo("repeatUntilDone");
    }

    @Test
    void behaviourChildNodesExportsEntityAttributes() throws Exception {
        mxGraph graph = new mxGraph();
        JTreeToGraphBehaviour.benhaviourGraph = graph;

        mxCell entity;
        graph.getModel().beginUpdate();
        try {
            entity = (mxCell) graph.insertVertex(graph.getDefaultParent(), null, "Walk", 0, 0, 80, 30, "Entity");
        } finally {
            graph.getModel().endUpdate();
        }

        BehaviorAttributeOverview overview = new BehaviorAttributeOverview("Walk");
        overview.addAttribute(new BehaviorAttribute("speed", "fast"));
        overview.addAttribute(new BehaviorAttribute("mode", "cautious"));
        JTreeToGraphBehaviour.behaviorsWithAttributes.add(overview);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element element = JtreeToGraphGeneral.behaviourChildNodes(document, entity);

        assertThat(element.getTagName()).isEqualTo("Walk");
        assertThat(element.getAttribute("Attributes")).isEqualTo("speed = fast; mode = cautious");
    }
}
