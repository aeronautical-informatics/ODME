package odme.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * A scenario derived from a System Entity Structure, representing one specific
 * operational context for an AI-based system under test.
 *
 * <p>In the EASA AI Learning Assurance framework, scenarios must be traceable
 * from their source ODD model through to test execution and verdict. This class
 * captures the full scenario record including lifecycle status, authorship, and
 * ODD traceability.</p>
 */
public class Scenario {

    private final String id;
    private String name;
    private String risk;
    private String remarks;
    private ScenarioStatus status;
    private final String sourceSESId;
    private String pesId;

    private final Instant createdAt;
    private Instant lastModifiedAt;
    private String createdBy;
    private String lastModifiedBy;

    private final List<AuditEntry> history;

    public Scenario(String id, String name, String sourceSESId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.sourceSESId = sourceSESId; // nullable — legacy scenarios may not have this
        this.risk = "";
        this.remarks = "";
        this.status = ScenarioStatus.DRAFT;
        this.createdAt = Instant.now();
        this.lastModifiedAt = this.createdAt;
        this.history = new ArrayList<>();
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        requireEditable("rename");
        this.name = Objects.requireNonNull(name);
        touch("lastModifiedBy");
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    public String getRisk() { return risk; }

    public void setRisk(String risk) {
        this.risk = risk != null ? risk : "";
        touch(null);
    }

    public String getRemarks() { return remarks; }

    public void setRemarks(String remarks) {
        this.remarks = remarks != null ? remarks : "";
        touch(null);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public ScenarioStatus getStatus() { return status; }

    /**
     * Submits this scenario for review.
     *
     * @throws IllegalStateException if the scenario is not in DRAFT status
     */
    public void submitForReview(String userId) {
        if (status != ScenarioStatus.DRAFT) {
            throw new IllegalStateException(
                "Only DRAFT scenarios can be submitted for review. Current status: " + status);
        }
        this.status = ScenarioStatus.IN_REVIEW;
        recordHistory("SUBMITTED_FOR_REVIEW", userId);
        touch(userId);
    }

    /**
     * Approves this scenario for use in testing.
     *
     * @throws IllegalStateException if the scenario is not IN_REVIEW
     */
    public void approve(String userId) {
        if (status != ScenarioStatus.IN_REVIEW) {
            throw new IllegalStateException(
                "Only IN_REVIEW scenarios can be approved. Current status: " + status);
        }
        this.status = ScenarioStatus.APPROVED;
        recordHistory("APPROVED", userId);
        touch(userId);
    }

    /**
     * Returns this scenario to DRAFT status for further editing.
     */
    public void returnToDraft(String userId, String reason) {
        if (status == ScenarioStatus.DEPRECATED) {
            throw new IllegalStateException("Cannot reopen a deprecated scenario");
        }
        this.status = ScenarioStatus.DRAFT;
        recordHistory("RETURNED_TO_DRAFT: " + reason, userId);
        touch(userId);
    }

    /**
     * Marks this scenario as deprecated (superseded or invalid).
     */
    public void deprecate(String userId, String reason) {
        this.status = ScenarioStatus.DEPRECATED;
        recordHistory("DEPRECATED: " + reason, userId);
        touch(userId);
    }

    // ── Traceability ──────────────────────────────────────────────────────────

    /** The ID of the SES this scenario was derived from. */
    public String getSourceSESId() { return sourceSESId; }

    /** The ID of the PES (pruned tree) that represents this scenario's structure. */
    public String getPesId() { return pesId; }

    public void setPesId(String pesId) { this.pesId = pesId; }

    // ── Authorship ────────────────────────────────────────────────────────────

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastModifiedAt() { return lastModifiedAt; }

    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastModifiedBy() { return lastModifiedBy; }

    public List<AuditEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireEditable(String operation) {
        if (!status.isEditable()) {
            throw new IllegalStateException(
                "Cannot " + operation + " a scenario in status: " + status +
                ". Return to DRAFT first.");
        }
    }

    private void touch(String userId) {
        this.lastModifiedAt = Instant.now();
        if (userId != null) {
            this.lastModifiedBy = userId;
        }
    }

    private void recordHistory(String action, String userId) {
        history.add(new AuditEntry(action, userId, Instant.now()));
    }

    @Override
    public String toString() {
        return String.format("Scenario{id='%s', name='%s', status=%s}", id, name, status);
    }
}
