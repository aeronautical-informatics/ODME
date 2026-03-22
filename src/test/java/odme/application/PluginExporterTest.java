package odme.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import odme.domain.coverage.ODDCoverageAnalyzer;
import odme.domain.coverage.ODDCoverageReport;
import odme.domain.model.*;
import odme.domain.traceability.TraceabilityMatrix;
import odme.odmeeditor.LatinHypercubeSampler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginExporterTest {

    private PluginExporter exporter;
    private ObjectMapper jsonMapper;
    private ProjectSession session;
    private SESTree sesTree;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        exporter = new PluginExporter();
        jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        session = new ProjectSession("proj-1", "TestProject", tempDir);

        // Build a simple SES tree: Root → EnvironmentDec → [Fair, Rainy]
        sesTree = new SESTree("ses-1", "TestODD");
        SESNode root = new SESNode("root", "System", SESNodeType.ENTITY);
        SESNode envDec = new SESNode("env-dec", "Environment", SESNodeType.SPECIALIZATION);
        SESNode fair = new SESNode("fair", "Fair", SESNodeType.ENTITY);
        fair.putVariable("visibility_m", "[5000, 15000]");
        fair.putVariable("precipitation_mm_h", "0.0");
        SESNode rainy = new SESNode("rainy", "Rainy", SESNodeType.ENTITY);
        rainy.putVariable("visibility_m", "[1000, 5000]");
        rainy.putVariable("precipitation_mm_h", "[1.0, 20.0]");

        root.addChild(envDec);
        envDec.addChild(fair);
        envDec.addChild(rainy);
        sesTree.setRoot(root);
        session.setSESModel(sesTree);
    }

    @Test
    void exportMinimalProject() throws IOException {
        Path output = tempDir.resolve("export.json");
        exporter.export(session, List.of(), List.of(), null, null, null, null, output);

        assertTrue(output.toFile().exists());
        JsonNode json = jsonMapper.readTree(output.toFile());

        assertEquals("1.0", json.get("version").asText());
        assertEquals("TestProject", json.get("project").get("name").asText());
        assertEquals("proj-1", json.get("project").get("id").asText());
        assertNotNull(json.get("sesTree"));
        assertEquals("TestODD", json.get("sesTree").get("name").asText());
    }

    @Test
    void exportSESTreeStructure() throws IOException {
        String json = exporter.exportToString(session, List.of(), List.of(),
            null, null, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode sesRoot = root.get("sesTree").get("root");

        assertEquals("System", sesRoot.get("name").asText());
        assertEquals("ENTITY", sesRoot.get("type").asText());
        assertEquals("/System", sesRoot.get("path").asText());

        JsonNode envDec = sesRoot.get("children").get(0);
        assertEquals("Environment", envDec.get("name").asText());
        assertEquals("SPECIALIZATION", envDec.get("type").asText());

        JsonNode fair = envDec.get("children").get(0);
        assertEquals("Fair", fair.get("name").asText());
        assertEquals("5000, 15000]", fair.get("variables").get("visibility_m").asText().substring(1));
    }

    @Test
    void exportNodePaths() throws IOException {
        String json = exporter.exportToString(session, List.of(), List.of(),
            null, null, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode fair = root.get("sesTree").get("root")
            .get("children").get(0)
            .get("children").get(0);

        assertEquals("/System/Environment/Fair", fair.get("path").asText());
    }

    @Test
    void exportWithParameters() throws IOException {
        List<LatinHypercubeSampler.Parameter> params = List.of(
            new LatinHypercubeSampler.Parameter("visibility_m", "Fair", "double", 5000, 15000),
            new LatinHypercubeSampler.Parameter("precipitation_mm_h", "Rainy", "double", 1.0, 20.0)
        );

        String json = exporter.exportToString(session, List.of(), List.of(),
            params, null, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode parameters = root.get("parameters");

        assertEquals(2, parameters.size());
        assertEquals("visibility_m", parameters.get(0).get("name").asText());
        assertEquals("Fair", parameters.get(0).get("parentNode").asText());
        assertEquals("Fair.visibility_m", parameters.get(0).get("qualifiedName").asText());
        assertEquals(5000.0, parameters.get(0).get("min").asDouble());
        assertEquals(15000.0, parameters.get(0).get("max").asDouble());
    }

    @Test
    void exportWithTestCases() throws IOException {
        List<LatinHypercubeSampler.Parameter> params = List.of(
            new LatinHypercubeSampler.Parameter("visibility_m", "Fair", "double", 5000, 15000)
        );

        LatinHypercubeSampler sampler = new LatinHypercubeSampler(42);
        List<LatinHypercubeSampler.TestCase> testCases = sampler.sample(params, 3);

        String json = exporter.exportToString(session, List.of(), List.of(),
            params, testCases, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode tcNode = root.get("testCases");

        assertEquals(3, tcNode.size());
        assertEquals(1, tcNode.get(0).get("id").asInt());
        assertTrue(tcNode.get(0).get("values").has("Fair.visibility_m"));
    }

    @Test
    void exportWithScenarios() throws IOException {
        Scenario scenario = new Scenario("sc-1", "Nominal", "ses-1");
        scenario.setRisk("Low");
        scenario.setRemarks("Baseline test");
        scenario.setCreatedBy("researcher");

        PESTree pes = new PESTree("pes-1", "Nominal", "ses-1");
        SESNode pesRoot = new SESNode("root", "System", SESNodeType.ENTITY);
        SESNode fair = new SESNode("fair", "Fair", SESNodeType.ENTITY);
        pesRoot.addChild(fair);
        pes.setRoot(pesRoot);
        pes.recordPrunedNode("rainy");

        String json = exporter.exportToString(session, List.of(scenario),
            List.of(pes), null, null, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode scenarios = root.get("scenarios");

        assertEquals(1, scenarios.size());
        assertEquals("Nominal", scenarios.get(0).get("name").asText());
        assertEquals("DRAFT", scenarios.get(0).get("status").asText());
        assertEquals("Low", scenarios.get(0).get("risk").asText());
        assertEquals("researcher", scenarios.get(0).get("createdBy").asText());

        // PES tree should be inlined
        JsonNode pesNode = scenarios.get(0).get("pesTree");
        assertNotNull(pesNode);
        assertEquals("ses-1", pesNode.get("sourceSESId").asText());
        assertEquals(1, pesNode.get("prunedNodeIds").size());
        assertEquals("rainy", pesNode.get("prunedNodeIds").get(0).asText());
    }

    @Test
    void exportWithCoverage() throws IOException {
        PESTree pes = new PESTree("pes-1", "Nominal", "ses-1");
        SESNode pesRoot = new SESNode("root", "System", SESNodeType.ENTITY);
        SESNode fair = new SESNode("fair", "Fair", SESNodeType.ENTITY);
        pesRoot.addChild(fair);
        pes.setRoot(pesRoot);

        ODDCoverageAnalyzer analyzer = new ODDCoverageAnalyzer();
        ODDCoverageReport report = analyzer.analyze(sesTree, List.of(pes));

        String json = exporter.exportToString(session, List.of(), List.of(),
            null, null, null, report);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode coverage = root.get("coverage");

        assertEquals(2, coverage.get("totalLeafNodes").asInt());
        assertEquals(1, coverage.get("coveredLeafNodes").asInt());
        assertEquals(50.0, coverage.get("coveragePercent").asDouble(), 0.1);
        assertEquals(1, coverage.get("uncoveredNodes").size());
        assertEquals("Rainy", coverage.get("uncoveredNodes").get(0).get("name").asText());
    }

    @Test
    void exportWithTraceability() throws IOException {
        Scenario scenario = new Scenario("sc-1", "Nominal", "ses-1");

        PESTree pes = new PESTree("pes-1", "Nominal", "ses-1");
        SESNode pesRoot = new SESNode("root", "System", SESNodeType.ENTITY);
        SESNode fair = new SESNode("fair", "Fair", SESNodeType.ENTITY);
        pesRoot.addChild(fair);
        pes.setRoot(pesRoot);

        TraceabilityMatrix matrix = new TraceabilityMatrix(
            sesTree, List.of(scenario), List.of(pes));

        String json = exporter.exportToString(session, List.of(scenario),
            List.of(pes), null, null, matrix, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode traceability = root.get("traceability");

        assertTrue(traceability.size() > 0);
        assertEquals("Fair", traceability.get(0).get("oddElementName").asText());
        assertEquals("Nominal", traceability.get(0).get("scenarioName").asText());
    }

    @Test
    void exportToFileCreatesDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c/export.json");
        exporter.export(session, List.of(), List.of(), null, null, null, null, nested);
        assertTrue(nested.toFile().exists());
    }

    @Test
    void exportEmptyTreeProducesNullOrMissingRoot() throws IOException {
        SESTree empty = new SESTree("empty", "EmptyTree");
        session.setSESModel(empty);

        String json = exporter.exportToString(session, List.of(), List.of(),
            null, null, null, null);
        JsonNode root = jsonMapper.readTree(json);
        JsonNode sesRoot = root.get("sesTree").get("root");

        // With NON_NULL, null root is omitted entirely
        assertTrue(sesRoot == null || sesRoot.isNull());
    }

    @Test
    void exportNodeVariablesBehavioursConstraints() throws IOException {
        SESNode root = sesTree.getRoot().orElseThrow();
        root.addBehaviour("detectSign()");
        root.addConstraint("visibility_m > 200");
        root.putFlag("safety_critical", "true");

        String json = exporter.exportToString(session, List.of(), List.of(),
            null, null, null, null);
        JsonNode node = jsonMapper.readTree(json);
        JsonNode sesRoot = node.get("sesTree").get("root");

        assertEquals("detectSign()", sesRoot.get("behaviours").get(0).asText());
        assertEquals("visibility_m > 200", sesRoot.get("constraints").get(0).asText());
        assertEquals("true", sesRoot.get("flags").get("safety_critical").asText());
    }

    @Test
    void exportWithNullOptionalFieldsProducesEmptyArrays() throws IOException {
        String json = exporter.exportToString(session, null, null,
            null, null, null, null);
        JsonNode root = jsonMapper.readTree(json);

        // Null scenarios/parameters should produce empty arrays
        assertTrue(root.get("scenarios").isEmpty());
        assertTrue(root.get("parameters").isEmpty());
        assertTrue(root.get("testCases").isEmpty());
        assertTrue(root.get("traceability").isEmpty());
        // Coverage is null → omitted with NON_NULL or explicitly null
        assertTrue(root.path("coverage").isMissingNode() || root.get("coverage").isNull());
    }
}
