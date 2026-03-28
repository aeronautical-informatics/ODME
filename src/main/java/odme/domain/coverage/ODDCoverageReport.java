package odme.domain.coverage;

import odme.domain.model.SESNode;

import java.util.List;
import java.util.Map;

/**
 * The result of an ODD coverage analysis.
 *
 * Reports which SES leaf nodes are covered by the scenario set,
 * which are missing, and how many times each appears.
 */
public record ODDCoverageReport(
    int totalLeafNodes,
    int coveredLeafNodes,
    double coveragePercent,
    List<SESNode> uncoveredNodes,
    Map<String, Integer> nodeFrequency,
    int scenarioCount
) {
    /** Whether 100% of SES leaf nodes appear in at least one scenario. */
    public boolean isFullyCovered() {
        return coveredLeafNodes == totalLeafNodes;
    }

    /** A summary string suitable for display in the UI status bar. */
    public String summary() {
        return String.format("%.1f%% ODD coverage (%d/%d nodes, %d scenarios)",
            coveragePercent, coveredLeafNodes, totalLeafNodes, scenarioCount);
    }

    @Override
    public String toString() {
        return summary();
    }
}
