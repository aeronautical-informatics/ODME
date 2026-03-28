package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class DeleteNodeCommandTest {

    private ProjectSession session;
    private SESTree tree;

    @BeforeEach
    void setUp() {
        session = new ProjectSession("p1", "TestProject",
            Path.of(System.getProperty("java.io.tmpdir")));

        tree = new SESTree("ses-001", "TestSES");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.setRoot(root);
        tree.addNode("root", engine);
        session.setSESModel(tree);
    }

    @Test
    void execute_removesNodeFromTree() throws SESCommandException {
        DeleteNodeCommand cmd = new DeleteNodeCommand("engine", "user");
        cmd.execute(session);

        assertThat(tree.findById("engine")).isEmpty();
        assertThat(tree.size()).isEqualTo(1);
    }

    @Test
    void undo_restoresDeletedNode() throws SESCommandException {
        DeleteNodeCommand cmd = new DeleteNodeCommand("engine", "user");
        cmd.execute(session);
        cmd.undo(session);

        assertThat(tree.findById("engine")).isPresent();
        assertThat(tree.size()).isEqualTo(2);
    }

    @Test
    void execute_rootNode_throwsException() {
        DeleteNodeCommand cmd = new DeleteNodeCommand("root", "user");
        assertThatThrownBy(() -> cmd.execute(session))
            .isInstanceOf(SESCommandException.class)
            .hasMessageContaining("root");
    }

    @Test
    void execute_deletesSubtree() throws SESCommandException {
        // Add child of engine
        SESNode part = new SESNode("part", "FanBlade", SESNodeType.ENTITY);
        tree.addNode("engine", part);
        assertThat(tree.size()).isEqualTo(3);

        DeleteNodeCommand cmd = new DeleteNodeCommand("engine", "user");
        cmd.execute(session);

        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.findById("engine")).isEmpty();
        assertThat(tree.findById("part")).isEmpty();
    }
}
