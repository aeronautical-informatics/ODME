package odme.domain.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a validation operation on an SES model or scenario.
 *
 * <p>Collects both errors (blocking) and warnings (informational) so that
 * validation can report all issues in a single pass.</p>
 */
public final class ValidationResult {

    /**
     * Severity level of a validation finding.
     */
    public enum Severity { ERROR, WARNING, INFO }

    /**
     * A single validation finding (error, warning, or informational note).
     */
    public record Finding(Severity severity, String code, String message, String nodeId) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s%s",
                severity, code, message,
                nodeId != null ? " (node: " + nodeId + ")" : "");
        }
    }

    private final List<Finding> findings = new ArrayList<>();

    /** Creates an empty (passing) validation result. */
    public static ValidationResult empty() {
        return new ValidationResult();
    }

    public void addError(String code, String message) {
        findings.add(new Finding(Severity.ERROR, code, message, null));
    }

    public void addError(String code, String message, String nodeId) {
        findings.add(new Finding(Severity.ERROR, code, message, nodeId));
    }

    public void addWarning(String code, String message) {
        findings.add(new Finding(Severity.WARNING, code, message, null));
    }

    public void addWarning(String code, String message, String nodeId) {
        findings.add(new Finding(Severity.WARNING, code, message, nodeId));
    }

    public void addInfo(String code, String message) {
        findings.add(new Finding(Severity.INFO, code, message, null));
    }

    /** Returns true only if there are zero ERROR-severity findings. */
    public boolean isValid() {
        return findings.stream().noneMatch(f -> f.severity() == Severity.ERROR);
    }

    public List<Finding> getFindings() {
        return Collections.unmodifiableList(findings);
    }

    public List<Finding> getErrors() {
        return findings.stream().filter(f -> f.severity() == Severity.ERROR).toList();
    }

    public List<Finding> getWarnings() {
        return findings.stream().filter(f -> f.severity() == Severity.WARNING).toList();
    }

    public void merge(ValidationResult other) {
        findings.addAll(other.findings);
    }

    @Override
    public String toString() {
        if (findings.isEmpty()) return "ValidationResult{PASS}";
        return String.format("ValidationResult{%d errors, %d warnings}",
            getErrors().size(), getWarnings().size());
    }
}
