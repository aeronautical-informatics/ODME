package odme.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import odme.domain.model.SESTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginRunnerTest {

    @TempDir
    Path tempDir;

    private ProjectSession session;
    private PluginRunner runner;

    @BeforeEach
    void setUp() {
        session = new ProjectSession("proj-1", "TestProject", tempDir);
        SESTree tree = new SESTree("ses-1", "TestODD");
        SESNode root = new SESNode("root", "System", SESNodeType.ENTITY);
        tree.setRoot(root);
        session.setSESModel(tree);

        runner = new PluginRunner();
    }

    @Test
    void findPythonReturnsNonNull() {
        String python = PluginRunner.findPython();
        // Python should be available in CI and on dev machines
        assertNotNull(python, "Python interpreter not found");
    }

    @Test
    void runSimplePythonScript() throws IOException {
        // Write a minimal Python script that reads the export and prints info
        Path script = tempDir.resolve("test_plugin.py");
        Files.writeString(script, """
            import json
            import argparse

            parser = argparse.ArgumentParser()
            parser.add_argument('--odme-project', required=True)
            parser.add_argument('--output-dir', required=True)
            args = parser.parse_args()

            with open(args.odme_project) as f:
                data = json.load(f)

            print(f"Project: {data['project']['name']}")
            print(f"Version: {data['version']}")
            print(f"SES: {data['sesTree']['name']}")
            """);

        List<String> lines = new ArrayList<>();
        PluginRunner.PluginResult result = runner.run(
            script, session, List.of(), List.of(),
            null, null, null, null, lines::add);

        assertEquals(0, result.exitCode(), "Script should succeed. Output: " + result.output());
        assertTrue(result.output().contains("Project: TestProject"));
        assertTrue(result.output().contains("Version: 1.0"));
        assertTrue(result.output().contains("SES: TestODD"));
    }

    @Test
    void runScriptThatWritesResults() throws IOException {
        Path script = tempDir.resolve("test_results.py");
        Files.writeString(script, """
            import json
            import argparse
            import os

            parser = argparse.ArgumentParser()
            parser.add_argument('--odme-project', required=True)
            parser.add_argument('--output-dir', required=True)
            args = parser.parse_args()

            os.makedirs(args.output_dir, exist_ok=True)
            results = {
                "version": "1.0",
                "verdicts": [
                    {"testCaseId": "TC_001", "scenarioName": "Nominal",
                     "verdict": "PASS", "detail": "All checks passed"},
                    {"testCaseId": "TC_002", "scenarioName": "Degraded",
                     "verdict": "FAIL", "detail": "Detection below threshold"}
                ]
            }
            with open(os.path.join(args.output_dir, 'results.json'), 'w') as f:
                json.dump(results, f)

            print("Results written")
            """);

        PluginRunner.PluginResult result = runner.run(
            script, session, List.of(), List.of(),
            null, null, null, null, null);

        assertEquals(0, result.exitCode());
        assertEquals(2, result.verdicts().size());
        assertEquals("TC_001", result.verdicts().get(0).getTestCaseId());
        assertEquals("PASS", result.verdicts().get(0).getVerdict());
        assertEquals("TC_002", result.verdicts().get(1).getTestCaseId());
        assertEquals("FAIL", result.verdicts().get(1).getVerdict());
    }

    @Test
    void runScriptThatFails() throws IOException {
        Path script = tempDir.resolve("fail.py");
        Files.writeString(script, """
            import sys
            print("Error: something went wrong", file=sys.stderr)
            sys.exit(1)
            """);

        PluginRunner.PluginResult result = runner.run(
            script, session, List.of(), List.of(),
            null, null, null, null, null);

        assertEquals(1, result.exitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.verdicts().isEmpty());
    }

    @Test
    void runNonExistentScript() throws IOException {
        Path script = tempDir.resolve("does_not_exist.py");

        PluginRunner.PluginResult result = runner.run(
            script, session, List.of(), List.of(),
            null, null, null, null, null);

        assertNotEquals(0, result.exitCode());
    }

    @Test
    void pluginImporterHandlesMissingFile() throws IOException {
        PluginImporter importer = new PluginImporter();
        List<PluginImporter.VerdictResult> results =
            importer.importResults(tempDir.resolve("nonexistent.json"));
        assertTrue(results.isEmpty());
    }

    @Test
    void pluginImporterReadsValidFile() throws IOException {
        Path resultsFile = tempDir.resolve("results.json");
        Files.writeString(resultsFile, """
            {
              "version": "1.0",
              "verdicts": [
                {"testCaseId": "TC_001", "verdict": "PASS", "detail": "OK"}
              ]
            }
            """);

        PluginImporter importer = new PluginImporter();
        List<PluginImporter.VerdictResult> results = importer.importResults(resultsFile);

        assertEquals(1, results.size());
        assertEquals("TC_001", results.get(0).getTestCaseId());
        assertEquals("PASS", results.get(0).getVerdict());
    }

    @Test
    void exportFileCreatedInProjectDir() throws IOException {
        Path script = tempDir.resolve("noop.py");
        Files.writeString(script, """
            import argparse
            parser = argparse.ArgumentParser()
            parser.add_argument('--odme-project', required=True)
            parser.add_argument('--output-dir', required=True)
            args = parser.parse_args()
            print("OK")
            """);

        runner.run(script, session, List.of(), List.of(),
            null, null, null, null, null);

        Path exportFile = tempDir.resolve("plugin_export.json");
        assertTrue(exportFile.toFile().exists(), "Export JSON should be in project dir");
    }
}
