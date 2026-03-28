package odme.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * A Possible Entity Structure (PES) — one concrete scenario derived from an
 * {@link SESTree} by the pruning operation.
 *
 * <p>In the SES formalism, pruning removes specialization alternatives not
 * relevant to a specific scenario, leaving a tree representing one valid
 * system configuration. This is described in Zeigler (1984) and implemented
 * in the ODME pruning workflow.</p>
 *
 * <p>A PES is always linked to its source SES, enabling traceability
 * (required by EASA AI Roadmap 2.0 concept papers).</p>
 */
public class PESTree {

    private final String id;
    private String name;
    private final String sourceSESId;
    private SESNode root;
    private final Instant createdAt;
    private Instant lastModifiedAt;
    private final List<String> prunedNodeIds;

    public PESTree(String id, String name, String sourceSESId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sourceSESId = Objects.requireNonNull(sourceSESId, "sourceSESId must not be null");
        this.createdAt = Instant.now();
        this.lastModifiedAt = this.createdAt;
        this.prunedNodeIds = new ArrayList<>();
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
        touch();
    }

    /** The ID of the SESTree this PES was derived from. Enables traceability. */
    public String getSourceSESId() { return sourceSESId; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastModifiedAt() { return lastModifiedAt; }

    // ── Root ──────────────────────────────────────────────────────────────────

    public Optional<SESNode> getRoot() {
        return Optional.ofNullable(root);
    }

    public void setRoot(SESNode root) {
        this.root = root;
        touch();
    }

    // ── Pruning record ────────────────────────────────────────────────────────

    /**
     * Records a node that was pruned from the SES to produce this PES.
     * This supports the traceability requirement: knowing which SES nodes
     * are absent from each scenario.
     */
    public void recordPrunedNode(String nodeId) {
        prunedNodeIds.add(Objects.requireNonNull(nodeId));
        touch();
    }

    /** Returns IDs of all nodes pruned from the source SES to create this PES. */
    public List<String> getPrunedNodeIds() {
        return Collections.unmodifiableList(prunedNodeIds);
    }

    // ── Node access ───────────────────────────────────────────────────────────

    public List<SESNode> getAllNodes() {
        if (root == null) return List.of();
        return root.getSubtree();
    }

    public List<SESNode> getLeafNodes() {
        if (root == null) return List.of();
        return root.getLeafNodes();
    }

    public int size() {
        return getAllNodes().size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void touch() {
        this.lastModifiedAt = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("PESTree{id='%s', name='%s', sourceSES='%s', nodes=%d}",
            id, name, sourceSESId, size());
    }
}
