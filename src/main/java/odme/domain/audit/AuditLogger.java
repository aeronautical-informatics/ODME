package odme.domain.audit;

import odme.domain.model.Scenario;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Records auditable events in the ODME application.
 *
 * <p>All model-mutating operations should log through this class so that
 * a complete, timestamped record of changes is maintained. This supports
 * the evidence trail required by EASA AI Learning Assurance guidelines.</p>
 *
 * <p>Output goes to the {@code odme.domain.audit} logger, which is configured
 * in logback.xml to write to a dedicated audit log file.</p>
 */
public final class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("odme.domain.audit");

    private AuditLogger() { /* utility class */ }

    // ── SES operations ────────────────────────────────────────────────────────

    public static void sesCreated(String projectName, String userId) {
        log.info("ACTION=SES_CREATED project='{}' user='{}'", projectName, userId);
    }

    public static void sesNodeAdded(String projectName, String nodeId, String nodeType, String parentId, String userId) {
        log.info("ACTION=NODE_ADDED project='{}' node='{}' type='{}' parent='{}' user='{}'",
            projectName, nodeId, nodeType, parentId, userId);
    }

    public static void sesNodeDeleted(String projectName, String nodeId, String userId) {
        log.info("ACTION=NODE_DELETED project='{}' node='{}' user='{}'",
            projectName, nodeId, userId);
    }

    public static void sesSaved(String projectName, String filePath, String userId) {
        log.info("ACTION=SES_SAVED project='{}' path='{}' user='{}'",
            projectName, filePath, userId);
    }

    public static void sesValidated(String projectName, boolean passed, int errors, int warnings) {
        log.info("ACTION=SES_VALIDATED project='{}' passed={} errors={} warnings={}",
            projectName, passed, errors, warnings);
    }

    // ── PES / Scenario operations ─────────────────────────────────────────────

    public static void pesDerived(String projectName, String sesId, String pesId, String scenarioName, String userId) {
        log.info("ACTION=PES_DERIVED project='{}' sesId='{}' pesId='{}' scenario='{}' user='{}'",
            projectName, sesId, pesId, scenarioName, userId);
    }

    public static void scenarioStatusChanged(String projectName, String scenarioId,
                                             String fromStatus, String toStatus, String userId) {
        log.info("ACTION=SCENARIO_STATUS project='{}' scenario='{}' from='{}' to='{}' user='{}'",
            projectName, scenarioId, fromStatus, toStatus, userId);
    }

    // ── Export operations ─────────────────────────────────────────────────────

    public static void exported(String projectName, String format, String targetPath, String userId) {
        log.info("ACTION=EXPORTED project='{}' format='{}' path='{}' user='{}'",
            projectName, format, targetPath, userId);
    }

    public static void imported(String projectName, String sourceTool, String sourcePath, String userId) {
        log.info("ACTION=IMPORTED project='{}' tool='{}' source='{}' user='{}'",
            projectName, sourceTool, sourcePath, userId);
    }
}
