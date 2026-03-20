package odme.domain.traceability;

import odme.domain.model.PESTree;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import odme.domain.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Builds and queries the traceability matrix: ODD elements ↔ Scenarios ↔ Test Cases.
 *
 * Required by EASA AI Roadmap 2.0 concept papers: every ODD element must be
 * traceable through the assurance chain to a test verdict.
 *
 * Usage:
 *   TraceabilityMatrix matrix = new TraceabilityMatrix(ses, scenarios, pesTrees);
 *   List<TraceabilityEntry> entries = matrix.getEntries();
 *   List<SESNode> gaps = matrix.getUncoveredODDElements();
 */
public class TraceabilityMatrix {

    private static final Logger log = LoggerFactory.getLogger(TraceabilityMatrix.class);

    private final SESTree ses;
    private final List<Scenario> scenarios;
    private final Map<String, PESTree> pesByScenarioId;
    private final List<TraceabilityEntry> entries;

    public TraceabilityMatrix(SESTree ses, List<Scenario> scenarios,
                               List<PESTree> pesTrees) {
        this.ses = Objects.requireNonNull(ses);
        this.scenarios = new ArrayList<>(Objects.requireNonNull(scenarios));
        this.pesByScenarioId = new HashMap<>();
        for (PESTree pes : pesTrees) {
            // Map by scenario name (PES name matches scenario name)
            this.pesByScenarioId.put(pes.getName(), pes);
        }
        this.entries = buildEntries();
        log.info("Built traceability matrix: {} SES nodes, {} scenarios, {} entries",
            ses.size(), scenarios.size(), entries.size());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<TraceabilityEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** ODD elements (SES leaf nodes) that appear in NO scenario. */
    public List<SESNode> getUncoveredODDElements() {
        Set<String> coveredIds = entries.stream()
            .map(TraceabilityEntry::oddElementId)
            .collect(Collectors.toSet());
        return ses.getLeafNodes().stream()
            .filter(n -> !coveredIds.contains(n.getId()))
            .toList();
    }

    /** Scenarios that have no test case assigned yet. */
    public List<Scenario> getScenariosWithoutTestCase() {
        Set<String> scenarioIdsWithTests = entries.stream()
            .filter(TraceabilityEntry::hasTestCase)
            .map(TraceabilityEntry::scenarioId)
            .collect(Collectors.toSet());
        return scenarios.stream()
            .filter(s -> !scenarioIdsWithTests.contains(s.getId()))
            .toList();
    }

    /** Scenarios that have test cases but no PASS verdict yet. */
    public List<Scenario> getScenariosWithFailingTests() {
        return entries.stream()
            .filter(e -> e.hasTestCase() && e.hasVerdict() && !e.isPassed())
            .map(e -> findScenario(e.scenarioId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();
    }

    /** Overall traceability completeness: fraction of ODD elements covered. */
    public double getODDCoveragePercent() {
        int total = ses.getLeafNodes().size();
        if (total == 0) return 100.0;
        long covered = ses.getLeafNodes().stream()
            .filter(n -> entries.stream()
                .anyMatch(e -> e.oddElementId().equals(n.getId())))
            .count();
        return (double) covered / total * 100.0;
    }

    /** Summary suitable for display or log. */
    public String getSummary() {
        int totalODD = ses.getLeafNodes().size();
        int uncovered = getUncoveredODDElements().size();
        int withoutTests = getScenariosWithoutTestCase().size();
        return String.format(Locale.US,
            "Traceability: %.1f%% ODD covered (%d/%d elements), %d scenarios missing test cases",
            getODDCoveragePercent(), totalODD - uncovered, totalODD, withoutTests);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<TraceabilityEntry> buildEntries() {
        List<TraceabilityEntry> result = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            PESTree pes = pesByScenarioId.get(scenario.getName());
            if (pes == null) continue;

            List<SESNode> pesLeaves = pes.getLeafNodes();
            for (SESNode leaf : pesLeaves) {
                // Check if this leaf exists in the SES
                ses.findById(leaf.getId()).ifPresent(sesNode -> {
                    TraceabilityEntry entry = new TraceabilityEntry(
                        sesNode.getId(),
                        sesNode.getName(),
                        sesNode.getPath(),
                        scenario.getId(),
                        scenario.getName(),
                        null,   // test case ID — populated later when tests are linked
                        null    // verdict — populated after test execution
                    );
                    result.add(entry);
                });
            }
        }
        return result;
    }

    private Optional<Scenario> findScenario(String scenarioId) {
        return scenarios.stream().filter(s -> s.getId().equals(scenarioId)).findFirst();
    }
}
