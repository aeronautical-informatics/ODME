package odme.domain.operations;

import odme.application.ProjectSession;
import java.time.Instant;

/**
 * A reversible operation on the SES domain model.
 *
 * All model-mutating operations are encapsulated as Commands. This enables:
 * - Full undo/redo (replacing the legacy mxUndoManager-only approach)
 * - Audit trail of every model change (required by EASA AI Learning Assurance)
 * - Unit testing of operations without a Swing UI
 * - Replay of operations for consistency checking
 */
public interface SESCommand {

    /**
     * Executes this command against the given session.
     * @param session the project session to mutate
     * @throws SESCommandException if the command cannot be executed
     */
    void execute(ProjectSession session) throws SESCommandException;

    /**
     * Reverses the effect of this command.
     * @param session the project session to restore
     * @throws SESCommandException if the undo cannot be performed
     */
    void undo(ProjectSession session) throws SESCommandException;

    /**
     * Returns a human-readable description for use in the undo menu and audit log.
     * Example: "Add entity 'Engine' under 'Propulsion'"
     */
    String describe();

    /** When this command was created. */
    Instant getTimestamp();

    /** ID of the user who initiated this command, or null for system actions. */
    String getUserId();
}
