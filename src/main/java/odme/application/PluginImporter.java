package odme.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports results from a Python plugin execution.
 *
 * <p>Reads a {@code results.json} file containing test verdicts
 * produced by a Python plugin script. These verdicts can be linked
 * back to the traceability matrix.</p>
 */
public class PluginImporter {

    private static final Logger log = LoggerFactory.getLogger(PluginImporter.class);

    private final ObjectMapper mapper;

    public PluginImporter() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Reads plugin results from a JSON file.
     *
     * @param resultsPath path to results.json
     * @return list of verdict results
     */
    public List<VerdictResult> importResults(Path resultsPath) throws IOException {
        if (!resultsPath.toFile().exists()) {
            log.debug("No results file found at {}", resultsPath);
            return List.of();
        }

        ResultsFile results = mapper.readValue(resultsPath.toFile(), ResultsFile.class);
        log.info("Imported {} verdicts from {}", results.verdicts.size(), resultsPath);
        return results.verdicts;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ResultsFile {
        public String version;
        public List<VerdictResult> verdicts = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerdictResult {
        private String testCaseId;
        private String scenarioName;
        private String verdict;
        private String detail;

        public VerdictResult() {}

        public VerdictResult(String testCaseId, String scenarioName,
                             String verdict, String detail) {
            this.testCaseId = testCaseId;
            this.scenarioName = scenarioName;
            this.verdict = verdict;
            this.detail = detail;
        }

        public String getTestCaseId() { return testCaseId; }
        public String getScenarioName() { return scenarioName; }
        public String getVerdict() { return verdict; }
        public String getDetail() { return detail; }
    }
}
