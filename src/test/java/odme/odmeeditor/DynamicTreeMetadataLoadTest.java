package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
