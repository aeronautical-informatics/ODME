package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class CommandHistoryTest {

    private ProjectSession session;
    private CommandHistory history;
    private SESTree tree;

    @BeforeEach
    void setUp() {
        session = new ProjectSession("p1", "Test",
            Path.of(System.getProperty("java.io.tmpdir")));
        tree = new SESTree("s1", "T");
        tree.setRoot(new SESNode("root", "Root", SESNodeType.ENTITY));
        session.setSESModel(tree);
        history = new CommandHistory(session);
    }

    @Test
    void newHistory_cannotUndoOrRedo() {
        assertThat(history.canUndo()).isFalse();
        assertThat(history.canRedo()).isFalse();
    }

    @Test
    void afterExecute_canUndo() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Node1", SESNodeType.ENTITY, "u"));
        assertThat(history.canUndo()).isTrue();
        assertThat(history.canRedo()).isFalse();
    }

    @Test
    void afterUndo_canRedo() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Node1", SESNodeType.ENTITY, "u"));
        history.undo();
        assertThat(history.canUndo()).isFalse();
        assertThat(history.canRedo()).isTrue();
    }

    @Test
    void afterUndo_treeReverted() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Node1", SESNodeType.ENTITY, "u"));
        assertThat(tree.size()).isEqualTo(2);
        history.undo();
        assertThat(tree.size()).isEqualTo(1);
    }

    @Test
    void afterRedo_treeReapplied() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Node1", SESNodeType.ENTITY, "u"));
        history.undo();
        history.redo();
        assertThat(tree.size()).isEqualTo(2);
    }

    @Test
    void newCommandAfterUndo_clearsRedoStack() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Node1", SESNodeType.ENTITY, "u"));
        history.undo();
        assertThat(history.canRedo()).isTrue();

        history.execute(new AddNodeCommand("root", "n2", "Node2", SESNodeType.ENTITY, "u"));
        assertThat(history.canRedo()).isFalse(); // new branch, redo stack cleared
    }

    @Test
    void getUndoDescription_returnsCommandDescription() throws SESCommandException {
        history.execute(new AddNodeCommand("root", "n1", "Engine", SESNodeType.ENTITY, "u"));
        assertThat(history.getUndoDescription()).isPresent();
        assertThat(history.getUndoDescription().get()).contains("Engine");
    }
}
