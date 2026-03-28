package odme.domain.graph;

import java.util.Objects;

/**
 * Represents a directed edge between two nodes in a System Entity Structure (SES) graph.
 * An edge connects a {@code source} node to a {@code target} node.
 */
public final class SESGraphEdge {

    private final SESGraphNode source;
    private final SESGraphNode target;

    /**
     * Creates a new directed edge.
     *
     * @param source the origin node (must not be null)
     * @param target the destination node (must not be null)
     */
    public SESGraphEdge(SESGraphNode source, SESGraphNode target) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.target = Objects.requireNonNull(target, "target must not be null");
    }

    /** Returns the origin node of this edge. */
    public SESGraphNode getSource() {
        return source;
    }

    /** Returns the destination node of this edge. */
    public SESGraphNode getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SESGraphEdge other)) return false;
        return source.equals(other.source) && target.equals(other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        return "SESGraphEdge{" + source.getName() + " -> " + target.getName() + "}";
    }
}
