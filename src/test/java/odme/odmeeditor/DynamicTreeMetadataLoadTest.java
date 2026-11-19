package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicTreeMetadataLoadTest {

    @TempDir
    Path tempDir;

    @Test
    void readSerializedMultimapIfExistsReturnsEmptyWhenFileIsMissing() throws Exception {
        Multimap<TreePath, String> result =
                DynamicTree.readSerializedMultimapIfExists(tempDir.resolve("missing.ssdbeh").toFile());

        assertThat(result.asMap()).isEmpty();
    }

    @Test
    void loadConstraintsWithFallbackMergesLegacyConstraintFiles() throws Exception {
        TreePath entityPath = treePath("Scenario", "Aircraft");
        writeSerializedMultimap(
                tempDir.resolve("Refueling.ssdintercon"),
                multimap(entityPath, "if(@Speed > -1) then (@Altitude > 10) else true")
        );
        writeSerializedMultimap(
                tempDir.resolve("Refueling.ssdintracons"),
                multimap(entityPath, "if(@Speed > -1) then (@Altitude < 20) else true")
        );

        Multimap<TreePath, String> result =
                DynamicTree.loadConstraintsWithFallback(tempDir.resolve("Refueling").toString());

        assertThat(result.values())
                .containsExactlyInAnyOrder(
                        "if(@Speed > -1) then (@Altitude > 10) else true",
                        "if(@Speed > -1) then (@Altitude < 20) else true"
                );
    }

    @Test
    void selectingNodeShowsAndClearsVisibleConstraints() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var previousTreeModel = DynamicTree.treeModel;
            var previousConstraints = DynamicTree.constraintsList;
            var previousConstraintPanel = ODMEEditor.scenarioConstraint;
            var previousConstraintTable = Constraint.table;
            var previousConstraintModel = Constraint.model;
            var previousInterEntityPanel = ODMEEditor.scenarioInterEntityConstraints;
            var previousInterEntityTable = InterEntityConstraints.table;
            var previousInterEntityModel = InterEntityConstraints.model;
            try {
                DynamicTree dynamicTree = new DynamicTree();
                ODMEEditor.scenarioConstraint = new Constraint();
                ODMEEditor.scenarioInterEntityConstraints = new InterEntityConstraints();
                DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scenario");
                DefaultMutableTreeNode node = new DefaultMutableTreeNode("NaturalLightDec");
                root.add(node);
                TreePath constrainedPath = new TreePath(node.getPath());
                TreePath unconstrainedPath = new TreePath(root.getPath());
                String constraint = "if(@Light > 0) then (@Visibility > 0) else true";
                DynamicTree.constraintsList = multimap(treePath("Scenario", "NaturalLightDec"), constraint);
                dynamicTree.tree.setModel(new DefaultTreeModel(root));

                dynamicTree.showConstraintsInTable(constrainedPath);
                assertThat(InterEntityConstraints.table.getValueAt(0, 0)).isEqualTo(constraint);
                assertThat(Constraint.table.getValueAt(0, 0)).isEqualTo(constraint);
                dynamicTree.showConstraintsInTable(unconstrainedPath);
                assertVisibleConstraintTableEmpty();

                dynamicTree.tree.setSelectionPath(constrainedPath);
                assertThat(InterEntityConstraints.table.getValueAt(0, 0)).isEqualTo(constraint);
                dynamicTree.tree.setSelectionPath(unconstrainedPath);
                assertVisibleConstraintTableEmpty();

                ODMEEditor.scenarioInterEntityConstraints = null;
                dynamicTree.showConstraintsInTable(constrainedPath);
                assertThat(Constraint.table.getValueAt(0, 0)).isEqualTo(constraint);
            } finally {
                DynamicTree.treeModel = previousTreeModel;
                DynamicTree.constraintsList = previousConstraints;
                ODMEEditor.scenarioConstraint = previousConstraintPanel;
                Constraint.table = previousConstraintTable;
                Constraint.model = previousConstraintModel;
                ODMEEditor.scenarioInterEntityConstraints = previousInterEntityPanel;
                InterEntityConstraints.table = previousInterEntityTable;
                InterEntityConstraints.model = previousInterEntityModel;
            }
        });
    }

    private void assertVisibleConstraintTableEmpty() {
        assertThat(IntStream.range(0, InterEntityConstraints.table.getRowCount())
                .mapToObj(row -> InterEntityConstraints.table.getValueAt(row, 0)))
                .allMatch(""::equals);
    }

    private Multimap<TreePath, String> multimap(TreePath path, String value) {
        Multimap<TreePath, String> result = ArrayListMultimap.create();
        result.put(path, value);
        return result;
    }

    private TreePath treePath(String rootName, String childName) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootName);
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(childName);
        root.add(child);
        return new TreePath(child.getPath());
    }

    private void writeSerializedMultimap(Path file, Multimap<TreePath, String> data) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(file))) {
            objectOutputStream.writeObject(data);
        }
    }
}
