package odme.domain.operations;

import odme.application.ProjectSession;
import odme.domain.audit.AuditLogger;
import odme.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Command: prunes the SES to derive a PES (Possible Entity Structure) for one scenario.
 *
 * In the SES formalism, pruning removes specialization alternatives not relevant
 * to a specific scenario, leaving a tree representing one valid system configuration.
 *
 * This is the clean extraction of the pruning logic from JtreeToGraphPrune (994 lines).
 * The actual UI interaction (combo-box selection, graph updates) remains in the
 * legacy JtreeToGraphPrune during the transition — this command handles the
 * domain model side.
 */
public class PruneToScenarioCommand implements SESCommand {

    private static final Logger log = LoggerFactory.getLogger(PruneToScenarioCommand.class);

    private final String scenarioId;
    private final String scenarioName;
    private final Set<String> nodesToKeep;
    private final String userId;
    private final Instant timestamp;

    // Saved for undo
    private PESTree previousPES;

    /**
     * @param scenarioId    unique ID for the new scenario
     * @param scenarioName  human-readable name
     * @param nodesToKeep   IDs of SES nodes to include in the PES (all others pruned)
     * @param userId        who initiated the pruning
     */
    public PruneToScenarioCommand(String scenarioId, String scenarioName,
                                   Set<String> nodesToKeep, String userId) {
        this.scenarioId = Objects.requireNonNull(scenarioId);
        this.scenarioName = Objects.requireNonNull(scenarioName);
        this.nodesToKeep = new HashSet<>(Objects.requireNonNull(nodesToKeep));
        this.userId = userId;
        this.timestamp = Instant.now();
    }

    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        SESTree ses = session.getSESModel()
            .orElseThrow(() -> new SESCommandException("No SES model loaded"));

        String sesId = ses.getId();

        // Save current PES for undo
        this.previousPES = session.getActiveScenarioPES().orElse(null);

        // Build PES: deep-copy the SES, then prune nodes not in keep set
        PESTree pes = new PESTree(scenarioId, scenarioName, sesId);

        SESNode sesRoot = ses.getRoot()
            .orElseThrow(() -> new SESCommandException("SES has no root node"));

        SESNode pesRoot = copySubtree(sesRoot, nodesToKeep, pes);
        pes.setRoot(pesRoot);

        // Record what was pruned
        ses.getAllNodes().stream()
            .filter(n -> !nodesToKeep.contains(n.getId()))
            .forEach(n -> pes.recordPrunedNode(n.getId()));

        session.setActiveScenarioPES(pes);

        AuditLogger.pesDerived(session.getProjectName(), sesId, scenarioId, scenarioName, userId);
        log.info("Derived PES '{}' from SES '{}': {} nodes kept, {} pruned",
            scenarioName, sesId, nodesToKeep.size(),
            ses.getAllNodes().size() - nodesToKeep.size());
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        session.setActiveScenarioPES(previousPES);
        log.debug("Undone: prune to scenario '{}'", scenarioName);
    }

    @Override
    public String describe() {
        return "Prune SES to scenario '" + scenarioName + "'";
    }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String getUserId() { return userId; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Recursively copies an SES node, excluding nodes not in the keep set.
     * Returns null if this node should be pruned entirely.
     */
    private SESNode copySubtree(SESNode original, Set<String> keepIds, PESTree pes) {
        if (!keepIds.contains(original.getId())) {
            return null;
        }

        SESNode copy = new SESNode(original.getId(), original.getName(), original.getType());
        // Copy metadata
        original.getVariables().forEach(copy::putVariable);
        original.getBehaviours().forEach(copy::addBehaviour);
        original.getConstraints().forEach(copy::addConstraint);
        original.getFlags().forEach(copy::putFlag);

        // Recurse into children
        for (SESNode child : original.getChildren()) {
            SESNode childCopy = copySubtree(child, keepIds, pes);
            if (childCopy != null) {
                copy.addChild(childCopy);
            }
        }

        return copy;
    }
}
