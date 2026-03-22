package odme.application;

import odme.domain.coverage.ODDCoverageReport;
import odme.domain.model.PESTree;
import odme.domain.model.Scenario;
import odme.domain.traceability.TraceabilityMatrix;
import odme.odmeeditor.LatinHypercubeSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Orchestrates the Python plugin execution pipeline:
 * export project JSON → run Python script → import results.
 *
 * <p>This class is designed to be called from a SwingWorker or
 * background thread — it blocks until the Python process completes.</p>
 */
public class PluginRunner {

    private static final Logger log = LoggerFactory.getLogger(PluginRunner.class);
    private static final String EXPORT_FILENAME = "plugin_export.json";
    private static final String OUTPUT_DIRNAME = "plugin_output";
    private static final String RESULTS_FILENAME = "results.json";
    private static final long DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes

    private final PluginExporter exporter;
    private final PluginImporter importer;

    public PluginRunner() {
        this.exporter = new PluginExporter();
        this.importer = new PluginImporter();
    }

    /**
     * Runs a Python plugin script against the current project.
     *
     * @param scriptPath     absolute path to the .py file
     * @param session        active project session
     * @param scenarios      project scenarios (may be empty)
     * @param pesTrees       PES trees (may be empty)
     * @param parameters     LHS parameters (nullable)
     * @param testCases      LHS test cases (nullable)
     * @param traceability   traceability matrix (nullable)
     * @param coverage       coverage report (nullable)
     * @param outputConsumer receives stdout/stderr lines for display (nullable)
     * @return result with exit code, output, and imported verdicts
     */
    public PluginResult run(Path scriptPath,
                            ProjectSession session,
                            List<Scenario> scenarios,
                            List<PESTree> pesTrees,
                            List<LatinHypercubeSampler.Parameter> parameters,
                            List<LatinHypercubeSampler.TestCase> testCases,
                            TraceabilityMatrix traceability,
                            ODDCoverageReport coverage,
                            Consumer<String> outputConsumer) throws IOException {

        Path projectDir = session.getProjectDirectory();
        Path exportPath = projectDir.resolve(EXPORT_FILENAME);
        Path outputDir = projectDir.resolve(OUTPUT_DIRNAME);
        Files.createDirectories(outputDir);

        // Step 1: Export project to JSON
        log.info("Exporting project for plugin: {}", exportPath);
        exporter.export(session, scenarios, pesTrees, parameters,
            testCases, traceability, coverage, exportPath);

        if (outputConsumer != null) {
            outputConsumer.accept("Exported project to " + exportPath);
        }

        // Step 2: Find Python interpreter
        String python = findPython();
        if (python == null) {
            String msg = "Python not found. Set ODME_PYTHON environment variable.";
            log.error(msg);
            return new PluginResult(1, msg, List.of());
        }

        // Step 3: Run the Python script
        log.info("Running plugin: {} {} --odme-project {} --output-dir {}",
            python, scriptPath, exportPath, outputDir);

        ProcessBuilder pb = new ProcessBuilder(
            python,
            scriptPath.toString(),
            "--odme-project", exportPath.toString(),
            "--output-dir", outputDir.toString()
        );
        pb.redirectErrorStream(true);
        pb.directory(projectDir.toFile());

        StringBuilder output = new StringBuilder();
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (outputConsumer != null) {
                    outputConsumer.accept(line);
                }
            }
        }

        int exitCode;
        try {
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String msg = "Plugin timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds";
                log.warn(msg);
                return new PluginResult(124, output + "\n" + msg, List.of());
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PluginResult(130, output + "\nInterrupted", List.of());
        }

        log.info("Plugin exited with code {}", exitCode);

        // Step 4: Import results if available
        List<PluginImporter.VerdictResult> verdicts = List.of();
        Path resultsPath = outputDir.resolve(RESULTS_FILENAME);
        if (resultsPath.toFile().exists()) {
            verdicts = importer.importResults(resultsPath);
            if (outputConsumer != null) {
                outputConsumer.accept("Imported " + verdicts.size() + " verdicts from results.json");
            }
        }

        return new PluginResult(exitCode, output.toString(), verdicts);
    }

    /**
     * Finds a Python interpreter.
     * Checks ODME_PYTHON env var first, then tries python3, then python.
     */
    static String findPython() {
        String envPython = System.getenv("ODME_PYTHON");
        if (envPython != null && !envPython.isBlank()) {
            return envPython;
        }

        for (String candidate : new String[]{"python3", "python"}) {
            if (isPythonAvailable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isPythonAvailable(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version")
                .redirectErrorStream(true)
                .start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Result of a plugin execution.
     */
    public record PluginResult(
        int exitCode,
        String output,
        List<PluginImporter.VerdictResult> verdicts
    ) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
