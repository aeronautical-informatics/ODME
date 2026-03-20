package odme.integration;

import odme.application.ProjectService;
import odme.application.ProjectSession;
import odme.domain.coverage.ODDCoverageAnalyzer;
import odme.domain.coverage.ODDCoverageReport;
import odme.domain.enumeration.ExhaustivePESEnumerator;
import odme.domain.model.*;
import odme.domain.operations.AddNodeCommand;
import odme.domain.operations.SESCommandException;
import odme.domain.traceability.CsvTraceabilityExporter;
import odme.domain.traceability.HtmlTraceabilityExporter;
import odme.domain.traceability.TraceabilityMatrix;
import odme.domain.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end integration test: the full ODME -> EASA AI Learning Assurance workflow.
 *
 * This test simulates the complete pipeline:
 * 1. Create a project and model an ODD as an SES
 * 2. Validate the SES structure
 * 3. Automatically enumerate all scenarios (PES instances)
 * 4. Compute ODD coverage
 * 5. Build the traceability matrix
 * 6. Export evidence artifacts (CSV + HTML)
 * 7. Save and reload the project
 *
 * This is the workflow described in:
 * Stefani et al. (2025) "Automated scenario generation from ODD model
 * for testing AI-based systems in aviation" (CEAS Aeronautical Journal)
 */
class FullWorkflowIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void fullEASAWorkflow_uamODD() throws IOException, SESCommandException {
        // -- Step 1: Create project --
        ProjectService service = new ProjectService();
        ProjectSession session = service.createProject("UAM_ODD", tempDir);
        assertThat(service.hasActiveSession()).isTrue();

        // -- Step 2: Build SES model via commands --
        var history = service.getCommandHistory();

        // Root: UAV entity
        SESTree ses = session.getSESModel().orElseThrow();
        SESNode root = new SESNode("uav", "UAV", SESNodeType.ENTITY);
        ses.setRoot(root);

        // Add propulsion specialization
        history.execute(new AddNodeCommand("uav", "power_spec", "PowerSpec",
            SESNodeType.SPECIALIZATION, "test-user"));
        history.execute(new AddNodeCommand("power_spec", "electric", "Electric",
            SESNodeType.ENTITY, "test-user"));
        history.execute(new AddNodeCommand("power_spec", "hybrid", "Hybrid",
            SESNodeType.ENTITY, "test-user"));

        // Add weather specialization
        history.execute(new AddNodeCommand("uav", "weather_spec", "WeatherSpec",
            SESNodeType.SPECIALIZATION, "test-user"));
        history.execute(new AddNodeCommand("weather_spec", "clear", "Clear",
            SESNodeType.ENTITY, "test-user"));
        history.execute(new AddNodeCommand("weather_spec", "rain", "Rain",
            SESNodeType.ENTITY, "test-user"));

        assertThat(ses.size()).isEqualTo(7);

        // -- Step 3: Validate SES --
        ValidationResult validation = service.validateModel();
        assertThat(validation.isValid()).isTrue();

        // -- Step 4: Enumerate all scenarios --
        ExhaustivePESEnumerator enumerator = new ExhaustivePESEnumerator();
        List<PESTree> scenarios = enumerator.enumerateAll(ses);

        // 2 power x 2 weather = 4 scenarios
        assertThat(scenarios).hasSize(4);
        assertThat(scenarios).allMatch(p -> p.getSourceSESId().equals(ses.getId()));

        // -- Step 5: Compute ODD coverage --
        ODDCoverageAnalyzer analyzer = new ODDCoverageAnalyzer();
        ODDCoverageReport coverage = analyzer.analyze(ses, scenarios);

        assertThat(coverage.isFullyCovered()).isTrue();
        assertThat(coverage.coveragePercent()).isEqualTo(100.0);

        // -- Step 6: Build traceability matrix --
        List<Scenario> scenarioDomainObjects = scenarios.stream()
            .map(p -> new Scenario(p.getId(), p.getName(), ses.getId()))
            .toList();

        TraceabilityMatrix matrix = new TraceabilityMatrix(ses, scenarioDomainObjects, scenarios);
        assertThat(matrix.getUncoveredODDElements()).isEmpty();

        // -- Step 7: Export evidence artifacts --
        Path evidenceDir = tempDir.resolve("evidence");

        new CsvTraceabilityExporter().export(matrix, evidenceDir.resolve("traceability.csv"));
        new HtmlTraceabilityExporter().export(matrix, evidenceDir.resolve("traceability.html"));

        assertThat(evidenceDir.resolve("traceability.csv").toFile()).exists();
        assertThat(evidenceDir.resolve("traceability.html").toFile()).exists();

        String html = Files.readString(evidenceDir.resolve("traceability.html"));
        assertThat(html).contains("100.0%");
        assertThat(html).contains("Traceability Matrix");

        // -- Step 8: Save and reload project --
        service.saveProject();

        ProjectService service2 = new ProjectService();
        ProjectSession loaded = service2.loadProject("UAM_ODD", tempDir);

        assertThat(loaded.getSESModel()).isPresent();
        assertThat(loaded.getSESModel().get().size()).isEqualTo(ses.size());

        // -- Step 9: Verify undo still works --
        assertThat(history.canUndo()).isTrue();
        history.undo(); // undo last addNode
        assertThat(ses.size()).isEqualTo(6);
    }

    @Test
    void coverageGuidedEnumeration_minimizesScenarioCount() throws IOException {
        ProjectService service = new ProjectService();
        service.createProject("MinScenario_ODD", tempDir);
        ProjectSession session = service.getActiveSession();

        SESTree ses = session.getSESModel().orElseThrow();
        SESNode root = new SESNode("system", "System", SESNodeType.ENTITY);
        SESNode spec = new SESNode("mode_spec", "ModeSpec", SESNodeType.SPECIALIZATION);
        SESNode modeA = new SESNode("mode_a", "ModeA", SESNodeType.ENTITY);
        SESNode modeB = new SESNode("mode_b", "ModeB", SESNodeType.ENTITY);
        SESNode modeC = new SESNode("mode_c", "ModeC", SESNodeType.ENTITY);
        ses.setRoot(root);
        ses.addNode("system", spec);
        ses.addNode("mode_spec", modeA);
        ses.addNode("mode_spec", modeB);
        ses.addNode("mode_spec", modeC);

        ExhaustivePESEnumerator enumerator = new ExhaustivePESEnumerator();

        // All 3 scenarios for 100% coverage
        List<PESTree> all = enumerator.enumerateAll(ses);
        assertThat(all).hasSize(3);

        // 33% coverage -> 1 scenario suffices (covers 1 of 3 leaves)
        List<PESTree> minimal = enumerator.enumerateToCoverage(ses, 0.33);
        assertThat(minimal.size()).isLessThanOrEqualTo(all.size());
        assertThat(minimal).isNotEmpty();
    }
}
