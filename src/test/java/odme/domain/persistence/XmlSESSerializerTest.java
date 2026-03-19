package odme.domain.persistence;

import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import odme.domain.model.SESTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for XmlSESSerializer — verifies round-trip fidelity.
 *
 * A round-trip test (write → read → compare) is the gold standard for
 * serialization code and detects regressions when the format changes.
 */
class XmlSESSerializerTest {

    @TempDir
    Path tempDir;

    private final XmlSESSerializer serializer = new XmlSESSerializer();

    @Test
    void roundTrip_simpleTree_preservesStructure() throws IOException {
        SESTree original = buildUAMTree();
        Path file = tempDir.resolve("test_ses.xml");

        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        assertThat(loaded.getName()).isEqualTo(original.getName());
        assertThat(loaded.size()).isEqualTo(original.size());
    }

    @Test
    void roundTrip_preservesNodeTypes() throws IOException {
        SESTree original = buildUAMTree();
        Path file = tempDir.resolve("types_test.xml");

        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        SESNode propDec = loaded.findById("prop_dec").orElseThrow();
        assertThat(propDec.getType()).isEqualTo(SESNodeType.ASPECT);

        SESNode rotorMasp = loaded.findById("rotor_masp").orElseThrow();
        assertThat(rotorMasp.getType()).isEqualTo(SESNodeType.MULTI_ASPECT);
    }

    @Test
    void roundTrip_preservesVariables() throws IOException {
        SESTree original = new SESTree("s1", "Test");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        root.putVariable("maxSpeed", "250");
        root.putVariable("altitude", "3000");
        original.setRoot(root);

        Path file = tempDir.resolve("vars_test.xml");
        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        SESNode loadedRoot = loaded.getRoot().orElseThrow();
        assertThat(loadedRoot.getVariables()).containsEntry("maxSpeed", "250");
        assertThat(loadedRoot.getVariables()).containsEntry("altitude", "3000");
    }

    @Test
    void roundTrip_preservesBehaviours() throws IOException {
        SESTree original = new SESTree("s1", "Test");
        SESNode root = new SESNode("root", "UAV", SESNodeType.ENTITY);
        root.addBehaviour("hover");
        root.addBehaviour("navigate");
        original.setRoot(root);

        Path file = tempDir.resolve("beh_test.xml");
        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        SESNode loadedRoot = loaded.getRoot().orElseThrow();
        assertThat(loadedRoot.getBehaviours()).containsExactly("hover", "navigate");
    }

    @Test
    void roundTrip_preservesConstraints() throws IOException {
        SESTree original = new SESTree("s1", "Test");
        SESNode root = new SESNode("root", "UAV", SESNodeType.ENTITY);
        root.addConstraint("speed < 500");
        original.setRoot(root);

        Path file = tempDir.resolve("con_test.xml");
        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        assertThat(loaded.getRoot().orElseThrow().getConstraints())
            .containsExactly("speed < 500");
    }

    @Test
    void roundTrip_preservesTreeHierarchy() throws IOException {
        SESTree original = buildUAMTree();
        Path file = tempDir.resolve("hierarchy_test.xml");

        serializer.write(original, file);
        SESTree loaded = serializer.read(file);

        // Root has children
        SESNode root = loaded.getRoot().orElseThrow();
        assertThat(root.getChildren()).isNotEmpty();

        // Aspect has entity children
        SESNode propDec = loaded.findById("prop_dec").orElseThrow();
        assertThat(propDec.getChildren()).hasSize(2);
    }

    @Test
    void write_createsParentDirectories() throws IOException {
        SESTree tree = new SESTree("s1", "T");
        tree.setRoot(new SESNode("r", "Root", SESNodeType.ENTITY));
        Path deepPath = tempDir.resolve("a/b/c/test.xml");

        assertThatCode(() -> serializer.write(tree, deepPath))
            .doesNotThrowAnyException();
        assertThat(deepPath.toFile()).exists();
    }

    @Test
    void emptyTree_writesAndReadsWithoutError() throws IOException {
        SESTree tree = new SESTree("empty", "EmptyTree");
        Path file = tempDir.resolve("empty.xml");

        serializer.write(tree, file);
        SESTree loaded = serializer.read(file);

        assertThat(loaded.isEmpty()).isTrue();
    }

    // ── Test fixture ──────────────────────────────────────────────────────────

    private SESTree buildUAMTree() {
        SESTree ses = new SESTree("ses-uam", "UAM_ODD");

        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode propDec = new SESNode("prop_dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        SESNode combustion = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);
        SESNode rotorMasp = new SESNode("rotor_masp", "RotorMAsp", SESNodeType.MULTI_ASPECT);
        SESNode rotor = new SESNode("rotor", "Rotor", SESNodeType.ENTITY);

        ses.setRoot(aircraft);
        ses.addNode("aircraft", propDec);
        ses.addNode("prop_dec", electric);
        ses.addNode("prop_dec", combustion);
        ses.addNode("aircraft", rotorMasp);
        ses.addNode("rotor_masp", rotor);

        return ses;
    }
}
