package behaviourtreetograph;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviourGraphLoaderTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearAttributes() {
        JTreeToGraphBehaviour.behaviorsWithAttributes.clear();
    }

    @Test
    void importsExportedBehaviourTreeAsAnEditableGraph() throws Exception {
        Path tree = tempDir.resolve("OutputBehaviourxml.xml");
        Files.writeString(tree, """
                <root main_tree_to_execute="MainTree">
                  <BehaviourTree ID="MainTree">
                    <Sequence>
                      <Action name="detectRunway" Attributes="model = v1; threshold = 0.8"/>
                      <Action name="publishTestResult"/>
                    </Sequence>
                  </BehaviourTree>
                </root>
                """);

        mxGraph graph = new mxGraph();
        BehaviourGraphLoader.loadInto(graph, tree.toFile());

        assertThat(graph.getChildVertices(graph.getDefaultParent()))
                .extracting(cell -> String.valueOf(((mxCell) cell).getValue()))
                .contains("Events", "Sequence", "detectRunway", "publishTestResult");
        assertThat(JTreeToGraphBehaviour.behaviorsWithAttributes).hasSize(1);
        assertThat(JTreeToGraphBehaviour.behaviorsWithAttributes.get(0).getBehaviorName())
                .isEqualTo("detectRunway");
        assertThat(JTreeToGraphBehaviour.behaviorsWithAttributes.get(0).getAttributes())
                .extracting(attribute -> attribute.getName() + "=" + attribute.getValue())
                .containsExactly("model=v1", "threshold=0.8");
    }

    @Test
    void restoresSavedMxGraphWithoutRebuildingItsStructure() throws Exception {
        mxGraph saved = new mxGraph();
        Object parent = saved.getDefaultParent();
        saved.getModel().beginUpdate();
        try {
            Object root = saved.insertVertex(parent, "rootnode", "Events", 20, 20, 80, 30, "Entity");
            Object action = saved.insertVertex(parent, "action", "detectRunway", 150, 80, 100, 30, "Entity");
            saved.insertEdge(parent, "edge", "", root, action);
        } finally {
            saved.getModel().endUpdate();
        }
        Path graphFile = tempDir.resolve("samplebehaviourGraph.xml");
        var document = new mxCodec().encode(saved.getModel());
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(document), new StreamResult(graphFile.toFile()));

        mxGraph loaded = new mxGraph();
        BehaviourGraphLoader.loadInto(loaded, graphFile.toFile());

        assertThat(loaded.getChildVertices(loaded.getDefaultParent()))
                .extracting(cell -> String.valueOf(((mxCell) cell).getValue()))
                .containsExactlyInAnyOrder("Events", "detectRunway");
        assertThat(loaded.getChildEdges(loaded.getDefaultParent())).hasSize(1);
    }
}
