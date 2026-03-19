package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AddNodeCommand — verifies node addition without any Swing UI.
 */
class AddNodeCommandTest {

    private ProjectSession session;
    private SESTree tree;

    @BeforeEach
    void setUp() {
        session = new ProjectSession("p1", "TestProject",
            Path.of(System.getProperty("java.io.tmpdir")));

        tree = new SESTree("ses-001", "TestSES");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        tree.setRoot(root);
        session.setSESModel(tree);
    }

    @Test
    void execute_addsNodeToTree() throws SESCommandException {
        AddNodeCommand cmd = new AddNodeCommand(
            "root", "engine_entity_1", "Engine", SESNodeType.ENTITY, "test-user");

        cmd.execute(session);

        assertThat(tree.findById("engine_entity_1")).isPresent();
        assertThat(tree.size()).isEqualTo(2);
    }

    @Test
    void execute_setsCorrectParent() throws SESCommandException {
        AddNodeCommand cmd = new AddNodeCommand(
            "root", "prop_aspect_1", "Propulsion", SESNodeType.ASPECT, "test-user");

        cmd.execute(session);

        SESNode added = tree.findById("prop_aspect_1").orElseThrow();
        assertThat(added.getParent().getId()).isEqualTo("root");
        assertThat(added.getType()).isEqualTo(SESNodeType.ASPECT);
    }

    @Test
    void undo_removesAddedNode() throws SESCommandException {
        AddNodeCommand cmd = new AddNodeCommand(
            "root", "engine_entity_1", "Engine", SESNodeType.ENTITY, "test-user");

        cmd.execute(session);
        assertThat(tree.size()).isEqualTo(2);

        cmd.undo(session);
        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.findById("engine_entity_1")).isEmpty();
    }

    @Test
    void execute_unknownParent_throwsException() {
        AddNodeCommand cmd = new AddNodeCommand(
            "nonexistent", "e1", "Engine", SESNodeType.ENTITY, "user");

        assertThatThrownBy(() -> cmd.execute(session))
            .isInstanceOf(SESCommandException.class);
    }

    @Test
    void execute_noModelLoaded_throwsException() {
        ProjectSession emptySession = new ProjectSession("p2", "Empty",
            Path.of(System.getProperty("java.io.tmpdir")));

        AddNodeCommand cmd = new AddNodeCommand("root", "e1", "E", SESNodeType.ENTITY, "user");

        assertThatThrownBy(() -> cmd.execute(emptySession))
            .isInstanceOf(SESCommandException.class)
            .hasMessageContaining("No SES model");
    }

    @Test
    void describe_returnsReadableString() {
        AddNodeCommand cmd = new AddNodeCommand(
            "root", "e1", "Engine", SESNodeType.ENTITY, "user");
        assertThat(cmd.describe()).contains("Engine").contains("root");
    }
}
