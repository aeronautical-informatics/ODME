package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.audit.AuditLogger;
import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Command: adds a new node to the SES tree.
 *
 * Encapsulates the logic previously spread across JtreeToGraphAdd (1383 lines).
 * By extracting the domain operation here, it can be tested independently of the UI.
 */
public class AddNodeCommand implements SESCommand {

    private static final Logger log = LoggerFactory.getLogger(AddNodeCommand.class);

    private final String parentId;
    private final String nodeId;
    private final String nodeName;
    private final SESNodeType nodeType;
    private final String userId;
    private final Instant timestamp;

    public AddNodeCommand(String parentId, String nodeId, String nodeName,
                          SESNodeType nodeType, String userId) {
        this.parentId = Objects.requireNonNull(parentId, "parentId");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.nodeName = Objects.requireNonNull(nodeName, "nodeName");
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded in session"));

        SESNode newNode = new SESNode(nodeId, nodeName, nodeType);
        try {
            tree.addNode(parentId, newNode);
        } catch (Exception e) {
            throw new SESCommandException("Failed to add node '" + nodeName + "': " + e.getMessage(), e);
        }

        AuditLogger.sesNodeAdded(session.getProjectName(), nodeId, nodeType.name(), parentId, userId);
        log.debug("Added {} node '{}' under '{}'", nodeType, nodeName, parentId);
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        SESTree tree = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded in session"));
        try {
            tree.removeNode(nodeId);
        } catch (Exception e) {
            throw new SESCommandException("Failed to undo add node '" + nodeName + "': " + e.getMessage(), e);
        }
        log.debug("Undone: add node '{}'", nodeName);
    }

    @Override
    public String describe() {
        return String.format("Add %s '%s' under '%s'", nodeType.name().toLowerCase(), nodeName, parentId);
    }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String getUserId() { return userId; }
}
