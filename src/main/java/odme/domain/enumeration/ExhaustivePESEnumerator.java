package odme.domain.enumeration;

import odme.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enumerates all valid PES instances from an SES by exhaustive combination
 * of specialization choices.
 *
 * Algorithm:
 * 1. Find all SPECIALIZATION nodes in the SES
 * 2. For each, the choices are its children (each represents one specialization)
 * 3. The Cartesian product of all choices = all valid PES instances
 * 4. For each combination, build a PES by copying the SES and removing
 *    the unchosen specialization branches
 *
 * This implements the automated scenario generation described in:
 * Gupta et al. (2026) "Automated scenario generation to maximize coverage
 * of an Operational Design Domain for AI-based systems in aviation"
 *
 * Note: For large SES trees with many specialization nodes, the number of
 * PES instances grows exponentially. Use {@link #enumerateToCoverage} for
 * large ODDs.
 */
public class ExhaustivePESEnumerator implements PESEnumerator {

    private static final Logger log = LoggerFactory.getLogger(ExhaustivePESEnumerator.class);
    private static final int MAX_PES = 1000; // safety limit

    @Override
    public List<PESTree> enumerateAll(SESTree ses) {
        if (ses.isEmpty()) return List.of();

        // Find all specialization nodes
        List<SESNode> specNodes = ses.getAllNodes().stream()
            .filter(n -> n.getType() == SESNodeType.SPECIALIZATION)
            .toList();

        if (specNodes.isEmpty()) {
            // No specializations → only one possible PES (same as SES)
            PESTree single = buildPES(ses, ses.getId() + "_pes_1", "Scenario_1",
                Collections.emptyMap());
            return List.of(single);
        }

        // Build Cartesian product of specialization choices
        List<Map<String, String>> combinations = cartesianProduct(specNodes);

        if (combinations.size() > MAX_PES) {
            log.warn("SES has {} possible PES instances (limit {}). Truncating.",
                combinations.size(), MAX_PES);
            combinations = combinations.subList(0, MAX_PES);
        }

        List<PESTree> result = new ArrayList<>();
        int counter = 1;
        for (Map<String, String> combination : combinations) {
            String pesId = ses.getId() + "_pes_" + counter;
            String pesName = "Scenario_" + counter;
            result.add(buildPES(ses, pesId, pesName, combination));
            counter++;
        }

        log.info("Enumerated {} PES instances from SES '{}'", result.size(), ses.getName());
        return result;
    }

    @Override
    public List<PESTree> enumerateToCoverage(SESTree ses, double targetCoverage) {
        List<PESTree> all = enumerateAll(ses);
        if (all.isEmpty()) return List.of();

        // Greedy coverage: pick scenarios one by one, choosing the one that adds most coverage
        Set<String> covered = new HashSet<>();
        List<PESTree> selected = new ArrayList<>();
        int totalLeaves = ses.getLeafNodes().size();

        for (PESTree candidate : all) {
            if (totalLeaves == 0) break;
            double currentCoverage = (double) covered.size() / totalLeaves;
            if (currentCoverage >= targetCoverage) break;

            // Compute marginal gain
            Set<String> candidateLeafIds = new HashSet<>();
            for (SESNode leaf : candidate.getLeafNodes()) {
                candidateLeafIds.add(leaf.getId());
            }
            candidateLeafIds.removeAll(covered);

            if (!candidateLeafIds.isEmpty()) {
                selected.add(candidate);
                covered.addAll(candidateLeafIds);
            }
        }

        double achieved = totalLeaves > 0 ? (double) covered.size() / totalLeaves * 100 : 0;
        log.info("Coverage-guided enumeration: {} scenarios achieve {}% coverage (target {}%)",
            selected.size(), String.format("%.1f", achieved), targetCoverage * 100);

        return selected;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a PES from an SES by retaining only the chosen specialization children.
     *
     * @param ses         source SES
     * @param pesId       ID for the new PES
     * @param pesName     name for the new PES
     * @param choices     map of specialization node ID → chosen child node ID
     */
    private PESTree buildPES(SESTree ses, String pesId, String pesName,
                              Map<String, String> choices) {
        PESTree pes = new PESTree(pesId, pesName, ses.getId());

        SESNode sesRoot = ses.getRoot().orElseThrow();
        SESNode pesRoot = copyWithChoices(sesRoot, choices, pes);
        pes.setRoot(pesRoot);

        // Record pruned nodes
        ses.getAllNodes().stream()
            .filter(n -> pes.getAllNodes().stream().noneMatch(p -> p.getId().equals(n.getId())))
            .forEach(n -> pes.recordPrunedNode(n.getId()));

        return pes;
    }

    private SESNode copyWithChoices(SESNode original, Map<String, String> choices,
                                     PESTree pes) {
        SESNode copy = shallowCopy(original);

        for (SESNode child : original.getChildren()) {
            if (original.getType() == SESNodeType.SPECIALIZATION) {
                // Keep only the chosen child
                String chosenId = choices.get(original.getId());
                if (chosenId != null && child.getId().equals(chosenId)) {
                    copy.addChild(copyWithChoices(child, choices, pes));
                } else if (chosenId == null && original.getChildren().indexOf(child) == 0) {
                    // No choice recorded → pick first (default)
                    copy.addChild(copyWithChoices(child, choices, pes));
                }
            } else {
                copy.addChild(copyWithChoices(child, choices, pes));
            }
        }
        return copy;
    }

    private SESNode shallowCopy(SESNode node) {
        SESNode copy = new SESNode(node.getId(), node.getName(), node.getType());
        node.getVariables().forEach(copy::putVariable);
        node.getBehaviours().forEach(copy::addBehaviour);
        node.getConstraints().forEach(copy::addConstraint);
        node.getFlags().forEach(copy::putFlag);
        return copy;
    }

    /**
     * Returns all combinations of specialization choices as maps:
     *   specNodeId → chosenChildId
     */
    private List<Map<String, String>> cartesianProduct(List<SESNode> specNodes) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(new HashMap<>());

        for (SESNode specNode : specNodes) {
            List<SESNode> children = specNode.getChildren();
            if (children.isEmpty()) continue;

            List<Map<String, String>> newResult = new ArrayList<>();
            for (Map<String, String> existing : result) {
                for (SESNode child : children) {
                    Map<String, String> combo = new HashMap<>(existing);
                    combo.put(specNode.getId(), child.getId());
                    newResult.add(combo);
                }
            }
            result = newResult;
        }
        return result;
    }
}
