package odme.domain.traceability;

import java.util.Objects;

/**
 * A single traceability link: ODD element → Scenario → (optional) Test Case.
 *
 * In the EASA AI Learning Assurance framework (EASA AI Roadmap 2.0,
 * EUROCAE ED-324 concept), every ODD element must be traceable to at least
 * one test scenario, and every scenario to at least one test case with a verdict.
 */
public record TraceabilityEntry(
    String oddElementId,
    String oddElementName,
    String oddElementPath,
    String scenarioId,
    String scenarioName,
    String testCaseId,       // nullable — may not yet have a test case assigned
    String verdict           // nullable — PASS, FAIL, or null if not yet executed
) {
    public TraceabilityEntry {
        Objects.requireNonNull(oddElementId, "oddElementId must not be null");
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
    }

    public boolean hasTestCase() { return testCaseId != null; }
    public boolean hasVerdict()  { return verdict != null; }
    public boolean isPassed()    { return "PASS".equalsIgnoreCase(verdict); }
}
