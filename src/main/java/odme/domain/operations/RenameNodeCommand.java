package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Command: renames an SES node.
 */
public class RenameNodeCommand implements SESCommand {

    private static final Logger log = LoggerFactory.getLogger(RenameNodeCommand.class);

    private final String nodeId;
    private final String newName;
    private final String userId;
    private final Instant timestamp;
    private String previousName;

    public RenameNodeCommand(String nodeId, String newName, String userId) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.newName = Objects.requireNonNull(newName);
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));

        SESNode node = tree.findById(nodeId)
            .orElseThrow(() -> new SESCommandException("Node not found: " + nodeId));

        this.previousName = node.getName();
        node.setName(newName);
        log.debug("Renamed node '{}' from '{}' to '{}'", nodeId, previousName, newName);
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));

        SESNode node = tree.findById(nodeId)
            .orElseThrow(() -> new SESCommandException("Node not found for undo: " + nodeId));

        node.setName(previousName);
        log.debug("Undo rename: node '{}' restored to '{}'", nodeId, previousName);
    }

    @Override
    public String describe() {
        return String.format("Rename node '%s' to '%s'", nodeId, newName);
    }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String getUserId() { return userId; }
}
