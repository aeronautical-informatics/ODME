package odme.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable record of a single action taken on a domain object.
 * Used to build the audit trail required by EASA AI Learning Assurance evidence.
 */
public record AuditEntry(
    String action,
    String userId,
    Instant timestamp
) {
    public AuditEntry {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public String toString() {
        return String.format("[%s] %s by %s", timestamp, action, userId != null ? userId : "system");
    }
}
