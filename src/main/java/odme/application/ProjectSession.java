package odme.application;

import odme.domain.model.PESTree;
import odme.domain.model.SESTree;
import odme.domain.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds all state for one open project.
 *
 * <p>This class replaces the global static state in {@code JtreeToGraphVariables}
 * and the mutable singleton {@code EditorContext}. A new {@code ProjectSession}
 * is created when a project is opened and discarded when the project closes.</p>
 *
 * <p>By scoping state to a session object (rather than static fields), the
 * application can in principle support multiple simultaneously open projects,
 * and tests can construct isolated sessions without affecting global state.</p>
 *
 * <p>During the transition period, the legacy singletons ({@code EditorContext},
 * {@code JtreeToGraphVariables}) remain in place. New code should obtain state
 * from {@code ProjectSession} rather than the legacy singletons.</p>
 */
public class ProjectSession {

    private static final Logger log = LoggerFactory.getLogger(ProjectSession.class);

    private final String projectId;
    private final Path projectDirectory;
    private String projectName;

    private SESTree sesModel;
    private PESTree activeScenarioPES;
    private Scenario activeScenario;

    private ToolMode toolMode;

    /**
     * The two primary tool modes in ODME.
     * Replaces the {@code "ses"}/{@code "pes"} string check in {@code EditorContext}.
     */
    public enum ToolMode {
        /** Domain modelling — editing the full SES. */
        SES,
        /** Scenario modelling — editing a pruned PES derived from the SES. */
        PES
    }

    public ProjectSession(String projectId, String projectName, Path projectDirectory) {
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.projectName = Objects.requireNonNull(projectName, "projectName must not be null");
        this.projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        this.toolMode = ToolMode.SES;
        log.info("Project session created: {} at {}", projectName, projectDirectory);
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    public String getProjectId() { return projectId; }

    public String getProjectName() { return projectName; }

    public void setProjectName(String name) {
        this.projectName = Objects.requireNonNull(name);
    }

    public Path getProjectDirectory() { return projectDirectory; }

    // ── SES Model ─────────────────────────────────────────────────────────────

    public Optional<SESTree> getSESModel() {
        return Optional.ofNullable(sesModel);
    }

    public void setSESModel(SESTree model) {
        this.sesModel = model;
        log.debug("SES model set: {}", model);
    }

    // ── Active Scenario ───────────────────────────────────────────────────────

    public Optional<Scenario> getActiveScenario() {
        return Optional.ofNullable(activeScenario);
    }

    public void setActiveScenario(Scenario scenario) {
        this.activeScenario = scenario;
        log.info("Active scenario: {}", scenario != null ? scenario.getName() : "none");
    }

    public Optional<PESTree> getActiveScenarioPES() {
        return Optional.ofNullable(activeScenarioPES);
    }

    public void setActiveScenarioPES(PESTree pes) {
        this.activeScenarioPES = pes;
    }

    // ── Tool Mode ─────────────────────────────────────────────────────────────

    public ToolMode getToolMode() { return toolMode; }

    public void setToolMode(ToolMode mode) {
        log.info("Tool mode switching from {} to {}", this.toolMode, mode);
        this.toolMode = Objects.requireNonNull(mode);
    }

    public boolean isInSESMode() { return toolMode == ToolMode.SES; }

    public boolean isInPESMode() { return toolMode == ToolMode.PES; }

    @Override
    public String toString() {
        return String.format("ProjectSession{id='%s', name='%s', mode=%s}",
            projectId, projectName, toolMode);
    }
}
