package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Command: sets or updates a variable on an SES node.
 *
 * Replaces the ad-hoc variable-setting code in Variable.java and the
 * .ssdvar binary serialization. Variables are now first-class model attributes
 * persisted as part of the domain model.
 */
public class SetNodeVariableCommand implements SESCommand {

    private static final Logger log = LoggerFactory.getLogger(SetNodeVariableCommand.class);

    private final String nodeId;
    private final String variableName;
    private final String newValue;
    private final String userId;
    private final Instant timestamp;
    private String previousValue;

    public SetNodeVariableCommand(String nodeId, String variableName, String newValue, String userId) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.variableName = Objects.requireNonNull(variableName);
        this.newValue = Objects.requireNonNull(newValue);
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        SESNode node = findNode(session);
        this.previousValue = node.getVariables().get(variableName);
        node.putVariable(variableName, newValue);
        log.debug("Set variable '{}' on node '{}': '{}' -> '{}'",
            variableName, nodeId, previousValue, newValue);
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        SESNode node = findNode(session);
        if (previousValue == null) {
            node.removeVariable(variableName);
        } else {
            node.putVariable(variableName, previousValue);
        }
        log.debug("Undo: variable '{}' on node '{}' restored to '{}'",
            variableName, nodeId, previousValue);
    }

    @Override
    public String describe() {
        return String.format("Set variable '%s' = '%s' on node '%s'", variableName, newValue, nodeId);
    }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String getUserId() { return userId; }

    private SESNode findNode(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));
        return tree.findById(nodeId)
            .orElseThrow(() -> new SESCommandException("Node not found: " + nodeId));
    }
}
