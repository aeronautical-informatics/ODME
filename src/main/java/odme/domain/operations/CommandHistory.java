package odme.domain.operations;

import odme.application.ProjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Manages the undo/redo history for a project session.
 *
 * Replaces the mxUndoManager which only tracked graph-level changes.
 * This history tracks all domain-level commands — graph changes, tree changes,
 * variable edits, constraint edits — in a single unified stack.
 */
public class CommandHistory {

    private static final Logger log = LoggerFactory.getLogger(CommandHistory.class);
    private static final int MAX_HISTORY = 100;

    private final Deque<SESCommand> undoStack = new ArrayDeque<>();
    private final Deque<SESCommand> redoStack = new ArrayDeque<>();
    private final ProjectSession session;

    public CommandHistory(ProjectSession session) {
        this.session = session;
    }

    /**
     * Executes a command and pushes it onto the undo stack.
     */
    public void execute(SESCommand command) throws SESCommandException {
        command.execute(session);
        undoStack.push(command);
        redoStack.clear();  // a new command invalidates redo history
        if (undoStack.size() > MAX_HISTORY) {
            // Remove oldest entry
            SESCommand[] arr = undoStack.toArray(new SESCommand[0]);
            undoStack.clear();
            for (int i = 0; i < arr.length - 1; i++) undoStack.add(arr[i]);
        }
        log.debug("Executed: {}", command.describe());
    }

    /**
     * Undoes the most recently executed command.
     */
    public void undo() throws SESCommandException {
        if (undoStack.isEmpty()) {
            log.debug("Nothing to undo");
            return;
        }
        SESCommand command = undoStack.pop();
        command.undo(session);
        redoStack.push(command);
        log.debug("Undone: {}", command.describe());
    }

    /**
     * Redoes the most recently undone command.
     */
    public void redo() throws SESCommandException {
        if (redoStack.isEmpty()) {
            log.debug("Nothing to redo");
            return;
        }
        SESCommand command = redoStack.pop();
        command.execute(session);
        undoStack.push(command);
        log.debug("Redone: {}", command.describe());
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public Optional<String> getUndoDescription() {
        return undoStack.isEmpty() ? Optional.empty()
            : Optional.of(undoStack.peek().describe());
    }

    public Optional<String> getRedoDescription() {
        return redoStack.isEmpty() ? Optional.empty()
            : Optional.of(redoStack.peek().describe());
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public int getUndoDepth() { return undoStack.size(); }
}
