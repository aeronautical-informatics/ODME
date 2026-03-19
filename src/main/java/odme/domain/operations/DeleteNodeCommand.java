package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.audit.AuditLogger;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Command: deletes a node (and its subtree) from the SES tree.
 *
 * Stores the deleted subtree so that undo can restore it completely.
 * This is the clean replacement for JtreeToGraphDelete (764 lines).
 */
public class DeleteNodeCommand implements SESCommand {

    private static final Logger log = LoggerFactory.getLogger(DeleteNodeCommand.class);

    private final String nodeId;
    private final String userId;
    private final Instant timestamp;

    // State saved for undo
    private SESNode deletedNode;
    private String deletedFromParentId;

    public DeleteNodeCommand(String nodeId, String userId) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));

        SESNode node = tree.findById(nodeId)
            .orElseThrow(() -> new SESCommandException("Node not found: " + nodeId));

        if (node.isRoot()) {
            throw new SESCommandException("Cannot delete the root node");
        }

        // Save for undo
        this.deletedNode = node;
        this.deletedFromParentId = node.getParent().getId();

        tree.removeNode(nodeId);
        AuditLogger.sesNodeDeleted(session.getProjectName(), nodeId, userId);
        log.debug("Deleted node '{}'", nodeId);
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        if (deletedNode == null) {
            throw new SESCommandException("Cannot undo: no node was saved for restoration");
        }
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));

        try {
            tree.addNode(deletedFromParentId, deletedNode);
        } catch (Exception e) {
            throw new SESCommandException(
                "Failed to restore node '" + nodeId + "': " + e.getMessage(), e);
        }
        log.debug("Restored deleted node '{}'", nodeId);
    }

    @Override
    public String describe() {
        return "Delete node '" + nodeId + "'";
    }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String getUserId() { return userId; }
}
