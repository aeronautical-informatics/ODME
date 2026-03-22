package odme.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import odme.domain.coverage.ODDCoverageReport;
import odme.domain.model.PESTree;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import odme.domain.model.Scenario;
import odme.domain.traceability.TraceabilityEntry;
import odme.domain.traceability.TraceabilityMatrix;
import odme.odmeeditor.LatinHypercubeSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Exports the complete ODME project state as a single JSON file
 * for consumption by Python plugin scripts.
 *
 * <p>The JSON schema is the contract between the Java application and
 * the Python SDK. It contains the full SES tree, all scenarios with
 * inline PES trees, LHS parameters and test cases, coverage report,
 * and traceability entries.</p>
 */
public class PluginExporter {

    private static final Logger log = LoggerFactory.getLogger(PluginExporter.class);
    private static final String EXPORT_VERSION = "1.0";

    private final ObjectMapper mapper;

    public PluginExporter() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Exports the complete project state to a JSON file.
     *
     * @param session      the active ProjectSession
     * @param scenarios    all scenarios (may be empty)
     * @param pesTrees     PES trees for scenarios (may be empty)
     * @param parameters   LHS parameters (nullable)
     * @param testCases    generated test cases (nullable)
     * @param traceability traceability matrix (nullable)
     * @param coverage     ODD coverage report (nullable)
     * @param outputPath   where to write the JSON file
     */
    public void export(ProjectSession session,
                       List<Scenario> scenarios,
                       List<PESTree> pesTrees,
                       List<LatinHypercubeSampler.Parameter> parameters,
                       List<LatinHypercubeSampler.TestCase> testCases,
                       TraceabilityMatrix traceability,
                       ODDCoverageReport coverage,
                       Path outputPath) throws IOException {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");

        ProjectExport export = buildExport(session, scenarios, pesTrees,
            parameters, testCases, traceability, coverage);

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        mapper.writeValue(outputPath.toFile(), export);
        log.info("Exported project '{}' to {}", session.getProjectName(), outputPath);
    }

    /**
     * Exports to a JSON string (useful for testing).
     */
    public String exportToString(ProjectSession session,
                                 List<Scenario> scenarios,
                                 List<PESTree> pesTrees,
                                 List<LatinHypercubeSampler.Parameter> parameters,
                                 List<LatinHypercubeSampler.TestCase> testCases,
                                 TraceabilityMatrix traceability,
                                 ODDCoverageReport coverage) throws IOException {
        ProjectExport export = buildExport(session, scenarios, pesTrees,
            parameters, testCases, traceability, coverage);
        return mapper.writeValueAsString(export);
    }

    private ProjectExport buildExport(ProjectSession session,
                                      List<Scenario> scenarios,
                                      List<PESTree> pesTrees,
                                      List<LatinHypercubeSampler.Parameter> parameters,
                                      List<LatinHypercubeSampler.TestCase> testCases,
                                      TraceabilityMatrix traceability,
                                      ODDCoverageReport coverage) {
        // Build PES lookup by name
        Map<String, PESTree> pesByName = new HashMap<>();
        if (pesTrees != null) {
            for (PESTree pes : pesTrees) {
                pesByName.put(pes.getName(), pes);
            }
        }

        return new ProjectExport(
            EXPORT_VERSION,
            buildProjectMeta(session),
            session.getSESModel().map(this::buildSESTreeExport).orElse(null),
            buildScenarioExports(scenarios, pesByName),
            buildParameterExports(parameters),
            buildTestCaseExports(parameters, testCases),
            buildCoverageExport(coverage),
            buildTraceabilityExports(traceability)
        );
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private ProjectMeta buildProjectMeta(ProjectSession session) {
        return new ProjectMeta(
            session.getProjectId(),
            session.getProjectName(),
            session.getProjectDirectory().toString()
        );
    }

    private SESTreeExport buildSESTreeExport(SESTree tree) {
        return new SESTreeExport(
            tree.getId(),
            tree.getName(),
            tree.getSchemaVersion(),
            tree.getCreatedAt(),
            tree.getLastModifiedAt(),
            tree.getRoot().map(this::buildNodeExport).orElse(null)
        );
    }

    private NodeExport buildNodeExport(SESNode node) {
        List<NodeExport> childExports = new ArrayList<>();
        for (SESNode child : node.getChildren()) {
            childExports.add(buildNodeExport(child));
        }
        return new NodeExport(
            node.getId(),
            node.getName(),
            node.getType().name(),
            node.getLabel(),
            node.getPath(),
            node.getVariables().isEmpty() ? null : new LinkedHashMap<>(node.getVariables()),
            node.getBehaviours().isEmpty() ? null : new ArrayList<>(node.getBehaviours()),
            node.getConstraints().isEmpty() ? null : new ArrayList<>(node.getConstraints()),
            node.getFlags().isEmpty() ? null : new LinkedHashMap<>(node.getFlags()),
            childExports.isEmpty() ? null : childExports
        );
    }

    private List<ScenarioExport> buildScenarioExports(List<Scenario> scenarios,
                                                       Map<String, PESTree> pesByName) {
        if (scenarios == null || scenarios.isEmpty()) return List.of();

        List<ScenarioExport> exports = new ArrayList<>();
        for (Scenario s : scenarios) {
            PESTree pes = pesByName.get(s.getName());
            PESTreeExport pesExport = null;
            if (pes != null) {
                pesExport = new PESTreeExport(
                    pes.getId(),
                    pes.getName(),
                    pes.getSourceSESId(),
                    pes.getPrunedNodeIds(),
                    pes.getCreatedAt(),
                    pes.getRoot().map(this::buildNodeExport).orElse(null)
                );
            }
            exports.add(new ScenarioExport(
                s.getId(),
                s.getName(),
                s.getStatus().name(),
                s.getRisk(),
                s.getRemarks(),
                s.getSourceSESId(),
                s.getPesId(),
                s.getCreatedAt(),
                s.getCreatedBy(),
                pesExport
            ));
        }
        return exports;
    }

    private List<ParameterExport> buildParameterExports(
            List<LatinHypercubeSampler.Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) return List.of();

        List<ParameterExport> exports = new ArrayList<>();
        for (LatinHypercubeSampler.Parameter p : parameters) {
            exports.add(new ParameterExport(
                p.name,
                p.parentNode,
                p.parentNode + "." + p.name,
                p.dataType,
                p.min,
                p.max
            ));
        }
        return exports;
    }

    private List<TestCaseExport> buildTestCaseExports(
            List<LatinHypercubeSampler.Parameter> parameters,
            List<LatinHypercubeSampler.TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) return List.of();

        List<TestCaseExport> exports = new ArrayList<>();
        for (LatinHypercubeSampler.TestCase tc : testCases) {
            Map<String, Double> values = new LinkedHashMap<>();
            for (Map.Entry<LatinHypercubeSampler.Parameter, Double> entry : tc.values.entrySet()) {
                String key = entry.getKey().parentNode + "." + entry.getKey().name;
                values.put(key, entry.getValue());
            }
            exports.add(new TestCaseExport(tc.id, values));
        }
        return exports;
    }

    private CoverageExport buildCoverageExport(ODDCoverageReport coverage) {
        if (coverage == null) return null;

        List<UncoveredNodeExport> uncovered = new ArrayList<>();
        for (SESNode node : coverage.uncoveredNodes()) {
            uncovered.add(new UncoveredNodeExport(
                node.getId(), node.getName(), node.getPath()));
        }
        return new CoverageExport(
            coverage.totalLeafNodes(),
            coverage.coveredLeafNodes(),
            coverage.coveragePercent(),
            coverage.scenarioCount(),
            uncovered
        );
    }

    private List<TraceabilityEntryExport> buildTraceabilityExports(
            TraceabilityMatrix traceability) {
        if (traceability == null) return List.of();

        List<TraceabilityEntryExport> exports = new ArrayList<>();
        for (TraceabilityEntry e : traceability.getEntries()) {
            exports.add(new TraceabilityEntryExport(
                e.oddElementId(),
                e.oddElementName(),
                e.oddElementPath(),
                e.scenarioId(),
                e.scenarioName(),
                e.testCaseId(),
                e.verdict()
            ));
        }
        return exports;
    }

    // ── DTOs (package-private for testing) ────────────────────────────────────

    record ProjectExport(
        String version,
        ProjectMeta project,
        SESTreeExport sesTree,
        List<ScenarioExport> scenarios,
        List<ParameterExport> parameters,
        List<TestCaseExport> testCases,
        CoverageExport coverage,
        List<TraceabilityEntryExport> traceability
    ) {}

    record ProjectMeta(String id, String name, String directory) {}

    record SESTreeExport(
        String id, String name, String schemaVersion,
        Instant createdAt, Instant lastModifiedAt,
        NodeExport root
    ) {}

    record NodeExport(
        String id, String name, String type, String label, String path,
        Map<String, String> variables,
        List<String> behaviours,
        List<String> constraints,
        Map<String, String> flags,
        List<NodeExport> children
    ) {}

    record ScenarioExport(
        String id, String name, String status, String risk, String remarks,
        String sourceSESId, String pesId,
        Instant createdAt, String createdBy,
        PESTreeExport pesTree
    ) {}

    record PESTreeExport(
        String id, String name, String sourceSESId,
        List<String> prunedNodeIds,
        Instant createdAt,
        NodeExport root
    ) {}

    record ParameterExport(
        String name, String parentNode, String qualifiedName,
        String dataType, double min, double max
    ) {}

    record TestCaseExport(int id, Map<String, Double> values) {}

    record CoverageExport(
        int totalLeafNodes, int coveredLeafNodes,
        double coveragePercent, int scenarioCount,
        List<UncoveredNodeExport> uncoveredNodes
    ) {}

    record UncoveredNodeExport(String id, String name, String path) {}

    record TraceabilityEntryExport(
        String oddElementId, String oddElementName, String oddElementPath,
        String scenarioId, String scenarioName,
        String testCaseId, String verdict
    ) {}
}
