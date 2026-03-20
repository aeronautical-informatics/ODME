package odme.domain.traceability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Exports the traceability matrix as CSV — readable by spreadsheet tools,
 * importable into DOORS and other ALM tools used in aviation projects.
 */
public class CsvTraceabilityExporter implements TraceabilityExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvTraceabilityExporter.class);

    @Override
    public void export(TraceabilityMatrix matrix, Path path) throws IOException {
        path.getParent().toFile().mkdirs();

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Header
            writer.write("ODD Element ID,ODD Element Name,ODD Path," +
                "Scenario ID,Scenario Name,Test Case ID,Verdict,Generated At");
            writer.newLine();

            for (TraceabilityEntry entry : matrix.getEntries()) {
                writer.write(String.join(",",
                    csv(entry.oddElementId()),
                    csv(entry.oddElementName()),
                    csv(entry.oddElementPath()),
                    csv(entry.scenarioId()),
                    csv(entry.scenarioName()),
                    csv(entry.testCaseId() != null ? entry.testCaseId() : ""),
                    csv(entry.verdict() != null ? entry.verdict() : "NOT_EXECUTED"),
                    csv(Instant.now().toString())
                ));
                writer.newLine();
            }

            // Append summary at end as comments
            writer.newLine();
            writer.write("# Summary: " + matrix.getSummary());
            writer.newLine();
        }

        log.info("Exported traceability matrix to {}: {} entries",
            path, matrix.getEntries().size());
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
