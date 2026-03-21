package odme.domain.graph;

import java.util.Objects;

/**
 * Represents a single node in a System Entity Structure (SES) graph.
 * Identity is based on the unique {@code id}; two nodes with the same id
 * are considered equal regardless of their display name.
 */
public final class SESGraphNode {

    private final String id;
    private final String name;

    /**
     * Creates a new graph node.
     *
     * @param id   unique identifier for this node (must not be null)
     * @param name human-readable display name (must not be null)
     */
    public SESGraphNode(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /** Returns the unique identifier for this node. */
    public String getId() {
        return id;
    }

    /** Returns the human-readable display name. */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SESGraphNode other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "SESGraphNode{id='" + id + "', name='" + name + "'}";
    }
}
