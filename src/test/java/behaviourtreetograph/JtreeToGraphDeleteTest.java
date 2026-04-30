package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JtreeToGraphDeleteTest {

    @AfterEach
    void clearStaticGraphState() {
        JTreeToGraphBehaviour.behaviorsWithAttributes.clear();
        JTreeToGraphBehaviour.benhaviourGraph = null;
    }

    @Test
    void deleteNodeFromGraphPopupRemovesSelectedSubtree() {
        mxGraph graph = new mxGraph();
        JTreeToGraphBehaviour.benhaviourGraph = graph;

        mxCell root;
        mxCell parent;
        mxCell child;
        graph.getModel().beginUpdate();
        try {
            Object graphParent = graph.getDefaultParent();
            root = (mxCell) graph.insertVertex(graphParent, "rootnode", "Events", 0, 0, 80, 30, "Entity");
            parent = (mxCell) graph.insertVertex(graphParent, "Walk", "Walk", 0, 80, 80, 30, "Entity");
            child = (mxCell) graph.insertVertex(graphParent, "Step", "Step", 0, 160, 80, 30, "Entity");
            graph.insertEdge(graphParent, null, "", root, parent);
            graph.insertEdge(graphParent, null, "", parent, child);
        } finally {
            graph.getModel().endUpdate();
        }

        BehaviorAttributeOverview overview = new BehaviorAttributeOverview("Walk");
        overview.addAttribute(new BehaviorAttribute("speed", "fast"));
        JTreeToGraphBehaviour.behaviorsWithAttributes.add(overview);

        JtreeToGraphDelete.deleteNodeFromGraphPopup(parent);

        String remainingValues = Arrays.stream(graph.getChildVertices(graph.getDefaultParent()))
                .map(mxCell.class::cast)
                .map(cell -> String.valueOf(cell.getValue()))
                .collect(Collectors.joining(","));

        assertThat(remainingValues).contains("Events");
        assertThat(remainingValues).doesNotContain("Walk");
        assertThat(remainingValues).doesNotContain("Step");
        assertThat(JTreeToGraphBehaviour.behaviorsWithAttributes).isEmpty();
    }

    @Test
    void deleteNodeFromGraphPopupKeepsProtectedRootNode() {
        mxGraph graph = new mxGraph();
        JTreeToGraphBehaviour.benhaviourGraph = graph;

        mxCell root;
        graph.getModel().beginUpdate();
        try {
            root = (mxCell) graph.insertVertex(graph.getDefaultParent(), "rootnode", "Events", 0, 0, 80, 30, "Entity");
        } finally {
            graph.getModel().endUpdate();
        }

        JtreeToGraphDelete.deleteNodeFromGraphPopup(root);

        String remainingValues = Arrays.stream(graph.getChildVertices(graph.getDefaultParent()))
                .map(mxCell.class::cast)
                .map(cell -> String.valueOf(cell.getValue()))
                .collect(Collectors.joining(","));

        assertThat(remainingValues).contains("Events");
    }
}
