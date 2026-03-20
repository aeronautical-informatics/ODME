package odme.application;

import odme.domain.audit.AuditLogger;
import odme.domain.model.SESTree;
import odme.domain.model.Scenario;
import odme.domain.operations.CommandHistory;
import odme.domain.persistence.JsonScenarioStore;
import odme.domain.persistence.ScenarioStore;
import odme.domain.persistence.SESSerializer;
import odme.domain.persistence.XmlSESSerializer;
import odme.domain.validation.SESStructureValidator;
import odme.domain.validation.SESValidator;
import odme.domain.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Application-layer service for project lifecycle operations.
 *
 * Orchestrates the domain objects and infrastructure for:
 * - Creating and opening projects
 * - Saving and loading SES models
 * - Managing scenarios
 * - Validating models
 *
 * This is the boundary between the UI layer and the domain layer.
 * UI code (Swing) should call this service rather than directly
 * manipulating domain objects.
 */
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final SESSerializer sesSerializer;
    private final ScenarioStore scenarioStore;
    private final SESValidator validator;

    private ProjectSession activeSession;
    private CommandHistory commandHistory;

    public ProjectService() {
        this.sesSerializer = new XmlSESSerializer();
        this.scenarioStore = new JsonScenarioStore();
        this.validator = new SESStructureValidator();
    }

    // Allow injection for testing
    public ProjectService(SESSerializer sesSerializer,
                          ScenarioStore scenarioStore,
                          SESValidator validator) {
        this.sesSerializer = sesSerializer;
        this.scenarioStore = scenarioStore;
        this.validator = validator;
    }

    // ── Project lifecycle ─────────────────────────────────────────────────────

    /**
     * Creates a new empty project.
     */
    public ProjectSession createProject(String projectName, Path projectDirectory)
        throws IOException {
        String projectId = UUID.randomUUID().toString();
        ProjectSession session = new ProjectSession(projectId, projectName, projectDirectory);

        SESTree sesTree = new SESTree(UUID.randomUUID().toString(), projectName);
        session.setSESModel(sesTree);

        this.activeSession = session;
        this.commandHistory = new CommandHistory(session);

        AuditLogger.sesCreated(projectName, System.getProperty("user.name"));
        log.info("Created new project: {}", projectName);
        return session;
    }

    /**
     * Saves the active session's SES model to disk.
     */
    public void saveProject() throws IOException {
        requireActiveSession();

        Path sesFile = activeSession.getProjectDirectory()
            .resolve(activeSession.getProjectName() + "_domain.xml");

        sesSerializer.write(activeSession.getSESModel().orElseThrow(), sesFile);
        AuditLogger.sesSaved(activeSession.getProjectName(), sesFile.toString(),
            System.getProperty("user.name"));
    }

    /**
     * Loads an SES model from disk into a new session.
     */
    public ProjectSession loadProject(String projectName, Path projectDirectory)
        throws IOException {
        Path sesFile = projectDirectory.resolve(projectName + "_domain.xml");
        SESTree tree = sesSerializer.read(sesFile);

        String projectId = UUID.randomUUID().toString();
        ProjectSession session = new ProjectSession(projectId, projectName, projectDirectory);
        session.setSESModel(tree);

        this.activeSession = session;
        this.commandHistory = new CommandHistory(session);

        log.info("Loaded project '{}' from {}: {} nodes", projectName, projectDirectory, tree.size());
        return session;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates the current SES model and returns the result.
     */
    public ValidationResult validateModel() {
        requireActiveSession();
        SESTree tree = activeSession.getSESModel().orElseThrow();
        ValidationResult result = validator.validate(tree);

        AuditLogger.sesValidated(
            activeSession.getProjectName(),
            result.isValid(),
            result.getErrors().size(),
            result.getWarnings().size()
        );

        log.info("Validation complete: {}", result);
        return result;
    }

    // ── Scenarios ─────────────────────────────────────────────────────────────

    public List<Scenario> loadScenarios() throws IOException {
        requireActiveSession();
        return scenarioStore.loadAll(activeSession.getProjectDirectory());
    }

    public void saveScenarios(List<Scenario> scenarios) throws IOException {
        requireActiveSession();
        scenarioStore.saveAll(scenarios, activeSession.getProjectDirectory());
    }

    // ── Command execution ─────────────────────────────────────────────────────

    public CommandHistory getCommandHistory() {
        requireActiveSession();
        return commandHistory;
    }

    // ── Session access ────────────────────────────────────────────────────────

    public ProjectSession getActiveSession() {
        return activeSession;
    }

    public boolean hasActiveSession() {
        return activeSession != null;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireActiveSession() {
        if (activeSession == null) {
            throw new IllegalStateException("No active project session");
        }
    }
}
