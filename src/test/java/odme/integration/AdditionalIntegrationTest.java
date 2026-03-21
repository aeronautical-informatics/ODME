package odme.integration;

import odme.domain.coverage.ODDCoverageAnalyzer;
import odme.domain.coverage.ODDCoverageReport;
import odme.domain.enumeration.ExhaustivePESEnumerator;
import odme.domain.model.*;
import odme.domain.persistence.XmlSESSerializer;
import odme.domain.traceability.CsvTraceabilityExporter;
import odme.domain.traceability.TraceabilityMatrix;
import odme.odmeeditor.LatinHypercubeSampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Additional integration tests covering XML round-trip, prune-and-enumerate,
 * traceability workflow, ODD coverage analysis, and Latin Hypercube Sampling.
 */
class AdditionalIntegrationTest {

    @TempDir
    Path tempDir;

    // ── Test 1: ProjectRoundTripTest ─────────────────────────────────────────

    @Test
    void xmlRoundTrip_preservesTreeStructureAndMetadata() throws IOException {
        // Build SES: root (ENTITY) -> aspectDec (ASPECT) -> 3 children with variables
        SESTree original = new SESTree("rt-ses", "RoundTripSES");

        SESNode root = new SESNode("root", "Vehicle", SESNodeType.ENTITY);
        original.setRoot(root);

        SESNode aspect = new SESNode("propulsion_dec", "Propulsion", SESNodeType.ASPECT);
        root.addChild(aspect);

        SESNode spec1 = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        spec1.putVariable("battery_kwh", "100");
        spec1.putVariable("range_km", "400");
        spec1.addConstraint("battery_kwh > 0");
        aspect.addChild(spec1);

        SESNode spec2 = new SESNode("hybrid", "Hybrid", SESNodeType.ENTITY);
        spec2.putVariable("fuel_type", "diesel");
        spec2.addConstraint("fuel_type != null");
        aspect.addChild(spec2);

        SESNode spec3 = new SESNode("hydrogen", "Hydrogen", SESNodeType.ENTITY);
        spec3.putVariable("tank_pressure_bar", "700");
        spec3.addBehaviour("refuel_sequence");
        aspect.addChild(spec3);

        assertThat(original.size()).isEqualTo(5);

        // Serialize to XML
        XmlSESSerializer serializer = new XmlSESSerializer();
        Path xmlPath = tempDir.resolve("roundtrip.xml");
        serializer.write(original, xmlPath);
        assertThat(xmlPath.toFile()).exists();

        // Deserialize back
        SESTree loaded = serializer.read(xmlPath);

        // Assert tree structure is identical
        assertThat(loaded.getId()).isEqualTo(original.getId());
        assertThat(loaded.getName()).isEqualTo(original.getName());
        assertThat(loaded.size()).isEqualTo(original.size());

        // Assert root and children
        SESNode loadedRoot = loaded.getRoot().orElseThrow();
        assertThat(loadedRoot.getName()).isEqualTo("Vehicle");
        assertThat(loadedRoot.getType()).isEqualTo(SESNodeType.ENTITY);
        assertThat(loadedRoot.getChildren()).hasSize(1);

        SESNode loadedAspect = loadedRoot.getChildren().get(0);
        assertThat(loadedAspect.getName()).isEqualTo("Propulsion");
        assertThat(loadedAspect.getType()).isEqualTo(SESNodeType.ASPECT);
        assertThat(loadedAspect.getChildren()).hasSize(3);

        // Assert variables are preserved
        SESNode loadedElectric = loaded.findById("electric").orElseThrow();
        assertThat(loadedElectric.getVariables())
            .containsEntry("battery_kwh", "100")
            .containsEntry("range_km", "400");

        SESNode loadedHybrid = loaded.findById("hybrid").orElseThrow();
        assertThat(loadedHybrid.getVariables())
            .containsEntry("fuel_type", "diesel");

        SESNode loadedHydrogen = loaded.findById("hydrogen").orElseThrow();
        assertThat(loadedHydrogen.getVariables())
            .containsEntry("tank_pressure_bar", "700");

        // Assert constraints are preserved
        assertThat(loadedElectric.getConstraints()).containsExactly("battery_kwh > 0");
        assertThat(loadedHybrid.getConstraints()).containsExactly("fuel_type != null");

        // Assert behaviours are preserved
        assertThat(loadedHydrogen.getBehaviours()).containsExactly("refuel_sequence");
    }

    // ── Test 2: PruneAndEnumerateTest ────────────────────────────────────────

    @Test
    void pruneAndEnumerate_producesCorrectPESCount() {
        // Build SES with 2 specialization nodes, each with 2 children
        SESTree ses = new SESTree("enum-ses", "EnumSES");

        SESNode root = new SESNode("system", "System", SESNodeType.ENTITY);
        ses.setRoot(root);

        // Specialization 1: propulsion
        SESNode propSpec = new SESNode("prop_spec", "PropSpec", SESNodeType.SPECIALIZATION);
        root.addChild(propSpec);
        propSpec.addChild(new SESNode("electric", "Electric", SESNodeType.ENTITY));
        propSpec.addChild(new SESNode("combustion", "Combustion", SESNodeType.ENTITY));

        // Specialization 2: weather
        SESNode weatherSpec = new SESNode("weather_spec", "WeatherSpec", SESNodeType.SPECIALIZATION);
        root.addChild(weatherSpec);
        weatherSpec.addChild(new SESNode("sunny", "Sunny", SESNodeType.ENTITY));
        weatherSpec.addChild(new SESNode("rainy", "Rainy", SESNodeType.ENTITY));

        // Enumerate all PES instances
        ExhaustivePESEnumerator enumerator = new ExhaustivePESEnumerator();
        List<PESTree> allPES = enumerator.enumerateAll(ses);

        // 2 propulsion x 2 weather = 4 scenarios
        assertThat(allPES).hasSize(4);

        // Each PES specialization should have exactly one child (the chosen alternative)
        for (PESTree pes : allPES) {
            List<SESNode> specNodes = pes.getAllNodes().stream()
                .filter(n -> n.getType() == SESNodeType.SPECIALIZATION)
                .toList();
            for (SESNode spec : specNodes) {
                assertThat(spec.getChildren())
                    .as("Specialization '%s' in PES '%s' should have exactly 1 child (pruned)",
                        spec.getName(), pes.getName())
                    .hasSize(1);
            }
        }

        // Each PES should contain the root
        for (PESTree pes : allPES) {
            assertThat(pes.getRoot()).isPresent();
            assertThat(pes.getRoot().get().getName()).isEqualTo("System");
        }

        // All 4 PES should have distinct leaf combinations
        Set<String> leafCombinations = allPES.stream()
            .map(pes -> pes.getLeafNodes().stream()
                .map(SESNode::getName)
                .sorted()
                .collect(Collectors.joining(",")))
            .collect(Collectors.toSet());
        assertThat(leafCombinations).hasSize(4);
    }

    // ── Test 3: TraceabilityWorkflowTest ─────────────────────────────────────

    @Test
    void traceabilityWorkflow_buildMatrixAndExportCsv() throws IOException {
        // Create SES
        SESTree ses = new SESTree("trace-ses", "TraceSES");
        SESNode root = new SESNode("drone", "Drone", SESNodeType.ENTITY);
        ses.setRoot(root);

        SESNode modeSpec = new SESNode("mode_spec", "ModeSpec", SESNodeType.SPECIALIZATION);
        root.addChild(modeSpec);
        modeSpec.addChild(new SESNode("autonomous", "Autonomous", SESNodeType.ENTITY));
        modeSpec.addChild(new SESNode("manual", "Manual", SESNodeType.ENTITY));

        // Enumerate scenarios
        ExhaustivePESEnumerator enumerator = new ExhaustivePESEnumerator();
        List<PESTree> pesTrees = enumerator.enumerateAll(ses);
        assertThat(pesTrees).hasSize(2);

        // Build Scenario domain objects
        List<Scenario> scenarios = pesTrees.stream()
            .map(p -> new Scenario(p.getId(), p.getName(), ses.getId()))
            .toList();

        // Build TraceabilityMatrix
        TraceabilityMatrix matrix = new TraceabilityMatrix(ses, scenarios, pesTrees);

        // Assert coverage percentage
        assertThat(matrix.getODDCoveragePercent()).isEqualTo(100.0);
        assertThat(matrix.getUncoveredODDElements()).isEmpty();

        // Export to CSV
        Path csvPath = tempDir.resolve("evidence/traceability.csv");
        new CsvTraceabilityExporter().export(matrix, csvPath);

        // Verify CSV output
        assertThat(csvPath.toFile()).exists();
        List<String> lines = Files.readAllLines(csvPath);

        // Filter out empty lines and comment lines
        List<String> dataLines = lines.stream()
            .filter(l -> !l.isBlank() && !l.startsWith("#"))
            .toList();

        // Header is the first line
        assertThat(dataLines.get(0)).contains("ODD Element ID");
        assertThat(dataLines.get(0)).contains("Scenario ID");

        // Should have header + at least 2 data rows (one per leaf-scenario link)
        assertThat(dataLines.size()).isGreaterThanOrEqualTo(3);
    }

    // ── Test 4: ODDCoverageWorkflowTest ──────────────────────────────────────

    @Test
    void oddCoverage_partialThenFull() {
        // Create SES with environment aspects and parameters
        SESTree ses = new SESTree("cov-ses", "CoverageSES");
        SESNode root = new SESNode("uav", "UAV", SESNodeType.ENTITY);
        ses.setRoot(root);

        // Environment specialization
        SESNode envSpec = new SESNode("env_spec", "EnvSpec", SESNodeType.SPECIALIZATION);
        root.addChild(envSpec);

        SESNode urban = new SESNode("urban", "Urban", SESNodeType.ENTITY);
        urban.putVariable("building_density", "high");
        envSpec.addChild(urban);

        SESNode rural = new SESNode("rural", "Rural", SESNodeType.ENTITY);
        rural.putVariable("building_density", "low");
        envSpec.addChild(rural);

        // Weather specialization
        SESNode wxSpec = new SESNode("wx_spec", "WxSpec", SESNodeType.SPECIALIZATION);
        root.addChild(wxSpec);

        SESNode clear = new SESNode("clear", "Clear", SESNodeType.ENTITY);
        clear.putVariable("visibility_km", "10");
        wxSpec.addChild(clear);

        SESNode foggy = new SESNode("foggy", "Foggy", SESNodeType.ENTITY);
        foggy.putVariable("visibility_km", "1");
        wxSpec.addChild(foggy);

        // Enumerate all scenarios (2x2 = 4)
        ExhaustivePESEnumerator enumerator = new ExhaustivePESEnumerator();
        List<PESTree> allScenarios = enumerator.enumerateAll(ses);
        assertThat(allScenarios).hasSize(4);

        ODDCoverageAnalyzer analyzer = new ODDCoverageAnalyzer();

        // Partial coverage with first scenario only
        ODDCoverageReport partialReport = analyzer.analyze(ses, List.of(allScenarios.get(0)));
        assertThat(partialReport.isFullyCovered()).isFalse();
        assertThat(partialReport.coveragePercent()).isLessThan(100.0);
        assertThat(partialReport.coveragePercent()).isGreaterThan(0.0);
        assertThat(partialReport.uncoveredNodes()).isNotEmpty();

        // Full coverage with all scenarios
        ODDCoverageReport fullReport = analyzer.analyze(ses, allScenarios);
        assertThat(fullReport.isFullyCovered()).isTrue();
        assertThat(fullReport.coveragePercent()).isEqualTo(100.0);
        assertThat(fullReport.uncoveredNodes()).isEmpty();
        assertThat(fullReport.scenarioCount()).isEqualTo(4);
    }

    // ── Test 5: LHSIntegrationTest ───────────────────────────────────────────

    @Test
    void latinHypercubeSampling_producesWellDistributedSamples() {
        // Create parameter definitions
        LatinHypercubeSampler.Parameter tempParam =
            new LatinHypercubeSampler.Parameter("temperature", "Environment", "double", -20.0, 40.0);
        LatinHypercubeSampler.Parameter windParam =
            new LatinHypercubeSampler.Parameter("wind_speed", "Environment", "double", 0.0, 100.0);
        LatinHypercubeSampler.Parameter altParam =
            new LatinHypercubeSampler.Parameter("altitude", "Flight", "int", 0, 5000);
        LatinHypercubeSampler.Parameter visParam =
            new LatinHypercubeSampler.Parameter("visibility", "Weather", "double", 0.1, 20.0);

        List<LatinHypercubeSampler.Parameter> parameters =
            List.of(tempParam, windParam, altParam, visParam);

        int n = 10;

        // Use a fixed seed for reproducibility
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(42L);
        List<LatinHypercubeSampler.TestCase> testCases = sampler.sample(parameters, n);

        // Assert we get exactly 10 rows
        assertThat(testCases).hasSize(n);

        // Assert each test case has values for all parameters
        for (LatinHypercubeSampler.TestCase tc : testCases) {
            assertThat(tc.values).hasSize(parameters.size());
        }

        // Assert each column's values are well-distributed across the range
        // (no two values in the same stratum)
        for (LatinHypercubeSampler.Parameter param : parameters) {
            List<Double> values = new ArrayList<>();
            for (LatinHypercubeSampler.TestCase tc : testCases) {
                values.add(tc.values.get(param));
            }

            // All values within bounds
            for (double v : values) {
                assertThat(v)
                    .as("Parameter %s value should be within [%f, %f]",
                        param.name, param.min, param.max)
                    .isBetween(param.min, param.max);
            }

            // Each value should fall in a distinct stratum
            double range = param.max - param.min;
            Set<Integer> strata = new HashSet<>();
            for (double v : values) {
                int stratum = (int) Math.floor(n * (v - param.min) / range);
                stratum = Math.min(stratum, n - 1); // clamp for values at max
                strata.add(stratum);
            }
            assertThat(strata)
                .as("Parameter '%s' should have all %d strata covered", param.name, n)
                .hasSize(n);
        }

        // Verify integer parameter produces integer values
        for (LatinHypercubeSampler.TestCase tc : testCases) {
            double altValue = tc.values.get(altParam);
            assertThat(altValue % 1.0)
                .as("Altitude should be an integer value")
                .isEqualTo(0.0);
        }

        // Verify CSV export works
        String csv = LatinHypercubeSampler.toCsv(parameters, testCases);
        String[] csvLines = csv.split("\n");
        assertThat(csvLines).hasSize(n + 1); // header + n data rows
        assertThat(csvLines[0]).contains("TestCase_ID");
        assertThat(csvLines[0]).contains("Environment.temperature");
    }
}
