package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mxgraph.io.mxCodec;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import odme.core.EditorContext;
import odme.core.FlagVariables;
import odme.jtreetograph.JtreeToGraphGeneral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.TreePath;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class ProjectLoadDataTest {
    @TempDir Path directory;

    @Test
    void backgroundLoadPreservesGraphEdgesTreeMetadataAndEditorContext() throws Exception {
        writeProject();
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        variables.put(new TreePath(new String[]{"Scenario", "Aircraft"}), "Speed,double,0,100,50");
        writeObject("Demo.ssdvar", variables);
        FlagVariables flags = new FlagVariables();
        flags.nodeNumber = 12;
        flags.uniformityNodeNumber = 7;
        writeObject("Demo.ssdflag", flags);
        String previousProject = EditorContext.getInstance().getProjName();

        SwingWorker<ProjectLoadData, Void> worker = new SwingWorker<>() {
            @Override protected ProjectLoadData doInBackground() throws Exception {
                assertThat(SwingUtilities.isEventDispatchThread()).isFalse();
                return ProjectLoadData.load(directory, "Demo");
            }
        };
        worker.execute();
        ProjectLoadData loaded = worker.get(10, TimeUnit.SECONDS);

        assertThat(EditorContext.getInstance().getProjName()).isEqualTo(previousProject);
        assertThat(loaded.graphModel().getCell("aircraft")).isNotNull();
        assertThat(loaded.graphModel().getCell("hideV")).isNull();
        Object edge = loaded.graphModel().getCell("edge");
        assertThat(loaded.graphModel().getTerminal(edge, true)).isSameAs(loaded.graphModel().getCell("root"));
        assertThat(loaded.graphModel().getTerminal(edge, false)).isSameAs(loaded.graphModel().getCell("aircraft"));
        assertThat(loaded.treeModel().getRoot().toString()).isEqualTo("Scenario");
        assertThat(loaded.treeModel().getChildCount(loaded.treeModel().getRoot())).isEqualTo(1);
        assertThat(loaded.variables().values()).containsExactly("Speed,double,0,100,50");
        assertThat(loaded.behaviours().isEmpty()).isTrue();
        assertThat(loaded.flags().nodeNumber).isEqualTo(12);
        assertThat(loaded.flags().uniformityNodeNumber).isEqualTo(7);
    }

    @Test
    void loadsLegacyConstraintsAndPrefersCombinedFile() throws Exception {
        writeProject();
        Multimap<TreePath, String> constraints = ArrayListMultimap.create();
        constraints.put(new TreePath("Scenario"), "legacy");
        writeObject("Demo.ssdintercon", constraints);
        writeObject("Demo.ssdintracons", constraints);
        assertThat(ProjectLoadData.load(directory, "Demo").constraints().values()).containsExactly("legacy", "legacy");
        constraints.clear();
        constraints.put(new TreePath("Scenario"), "combined");
        writeObject("Demo.ssdcon", constraints);
        assertThat(ProjectLoadData.load(directory, "Demo").constraints().values()).containsExactly("combined");
    }

    @Test
    void damagedInputsFailBeforeChangingTheEditor() throws Exception {
        writeProject();
        String previousProject = EditorContext.getInstance().getProjName();
        Files.writeString(directory.resolve("Demo.ssdvar"), "invalid serialized data");
        assertThatThrownBy(() -> ProjectLoadData.load(directory, "Demo")).isInstanceOf(Exception.class);
        assertThat(EditorContext.getInstance().getProjName()).isEqualTo(previousProject);
        Files.delete(directory.resolve("Demo.ssdvar"));
        Files.writeString(directory.resolve("Demo.xml"), "<broken>");
        assertThatThrownBy(() -> ProjectLoadData.load(directory, "Demo")).isInstanceOf(Exception.class);
        assertThat(EditorContext.getInstance().getProjName()).isEqualTo(previousProject);
    }

    @Test
    void rejectsViewUpdatesFromAWorkerThread() {
        assertThatThrownBy(() -> JtreeToGraphGeneral.applyLoadedProject(null, "Demo"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Swing event thread");
    }

    private void writeProject() throws Exception {
        mxGraph graph = new mxGraph();
        Object root = graph.insertVertex(graph.getDefaultParent(), "root", "Scenario", 0, 0, 80, 30);
        Object aircraft = graph.insertVertex(graph.getDefaultParent(), "aircraft", "Aircraft", 0, 60, 80, 30);
        graph.insertEdge(graph.getDefaultParent(), "edge", "", root, aircraft);
        graph.insertVertex(graph.getDefaultParent(), "hideV", "End of Canvas", 0, 50000, 80, 30);
        Files.writeString(directory.resolve("DemoGraph.xml"), mxXmlUtils.getXml(new mxCodec().encode(graph.getModel())));
        Files.writeString(directory.resolve("Demo.xml"), "<start><Scenario><Aircraft> </Aircraft></Scenario></start>");
    }

    private void writeObject(String name, Object value) throws Exception {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(directory.resolve(name)))) {
            output.writeObject(value);
        }
    }
}
