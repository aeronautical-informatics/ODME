package odme.domain.coverage;

import odme.domain.model.PESTree;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes how well a set of scenarios (PES instances) covers the ODD (SES).
 *
 * This implements the coverage metric described in:
 * Gupta et al. (2026) "Automated scenario generation to maximize coverage
 * of an Operational Design Domain for AI-based systems"
 *
 * Coverage is measured as the fraction of SES leaf nodes that appear
 * in at least one scenario's PES.
 */
public class ODDCoverageAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ODDCoverageAnalyzer.class);

    /**
     * Computes coverage of the ODD by the given set of scenarios.
     *
     * @param ses       the full SES (the ODD model)
     * @param scenarios the derived PES instances (the test scenarios)
     * @return a detailed coverage report
     */
    public ODDCoverageReport analyze(SESTree ses, List<PESTree> scenarios) {
        Objects.requireNonNull(ses, "SES must not be null");
        Objects.requireNonNull(scenarios, "Scenarios must not be null");

        List<SESNode> allSESLeaves = ses.getLeafNodes();
        Set<String> allLeafIds = allSESLeaves.stream()
            .map(SESNode::getId)
            .collect(Collectors.toSet());

        // Count how often each leaf appears across scenarios
        Map<String, Integer> nodeFrequency = new HashMap<>();
        Set<String> coveredLeafIds = new HashSet<>();

        for (PESTree pes : scenarios) {
            Set<String> pesLeafIds = pes.getLeafNodes().stream()
                .map(SESNode::getId)
                .collect(Collectors.toSet());

            for (String leafId : pesLeafIds) {
                if (allLeafIds.contains(leafId)) {
                    coveredLeafIds.add(leafId);
                    nodeFrequency.merge(leafId, 1, Integer::sum);
                }
            }
        }

        Set<String> uncoveredLeafIds = new HashSet<>(allLeafIds);
        uncoveredLeafIds.removeAll(coveredLeafIds);

        List<SESNode> uncoveredNodes = allSESLeaves.stream()
            .filter(n -> uncoveredLeafIds.contains(n.getId()))
            .toList();

        double coveragePct = allLeafIds.isEmpty() ? 0.0
            : (double) coveredLeafIds.size() / allLeafIds.size() * 100.0;

        ODDCoverageReport report = new ODDCoverageReport(
            allSESLeaves.size(),
            coveredLeafIds.size(),
            coveragePct,
            uncoveredNodes,
            nodeFrequency,
            scenarios.size()
        );

        log.info("ODD coverage: {}/{} leaf nodes covered ({}%) by {} scenarios",
            coveredLeafIds.size(), allSESLeaves.size(),
            String.format("%.1f", coveragePct), scenarios.size());

        return report;
    }
}
