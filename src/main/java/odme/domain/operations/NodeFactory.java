package odme.domain.operations;

import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating SES nodes with consistent ID generation.
 *
 * Extracts the node-creation logic from JtreeToGraphAdd (1383 lines),
 * making it independently testable and removing the dependency on
 * the static JtreeToGraphVariables.nodeNumber counter.
 */
public class NodeFactory {

    private static final Logger log = LoggerFactory.getLogger(NodeFactory.class);
    private final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Creates a new SES node with a generated unique ID.
     *
     * @param name     the entity name (without type suffix)
     * @param type     the SES node type
     * @return a new SESNode, not yet attached to any tree
     */
    public SESNode create(String name, SESNodeType type) {
        String id = generateId(name, type);
        SESNode node = new SESNode(id, name, type);
        log.debug("Created node: {}", node);
        return node;
    }

    /**
     * Creates a new SES node with an explicit ID (for import/deserialization).
     */
    public SESNode createWithId(String id, String name, SESNodeType type) {
        return new SESNode(id, name, type);
    }

    /**
     * Generates a stable, collision-resistant ID for a node.
     * Format: {name}_{type}_{counter} — human-readable and unique within a session.
     */
    private String generateId(String name, SESNodeType type) {
        return name + "_" + type.name().toLowerCase() + "_" + counter.getAndIncrement();
    }
}
