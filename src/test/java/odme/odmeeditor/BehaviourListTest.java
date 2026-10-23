package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviourListTest {

    @TempDir
    Path tempDir;

    @Test
    void excludesScenarioSidecarWhenItContainsNoBehaviours() throws Exception {
        Path sidecar = tempDir.resolve("empty.ssdbeh");
        writeSidecar(sidecar, ArrayListMultimap.create());

        assertThat(BehaviourList.containsBehaviour(sidecar.toFile())).isFalse();
    }

    @Test
    void includesScenarioSidecarWhenItContainsABehaviour() throws Exception {
        Path sidecar = tempDir.resolve("behaviour.ssdbeh");
        Multimap<TreePath, String> behaviours = ArrayListMultimap.create();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scenario");
        behaviours.put(new TreePath(root), "initializeRunwayDetection");
        writeSidecar(sidecar, behaviours);

        assertThat(BehaviourList.containsBehaviour(sidecar.toFile())).isTrue();
    }

    @Test
    void excludesCorruptBehaviourSidecars() throws Exception {
        Path sidecar = tempDir.resolve("corrupt.ssdbeh");
        Files.writeString(sidecar, "not serialized behavior metadata");

        assertThat(BehaviourList.containsBehaviour(sidecar.toFile())).isFalse();
    }

    private void writeSidecar(Path sidecar, Multimap<TreePath, String> behaviours) throws Exception {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(sidecar))) {
            output.writeObject(behaviours);
        }
    }
}
