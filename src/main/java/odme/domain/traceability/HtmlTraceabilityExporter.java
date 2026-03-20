package odme.domain.traceability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Exports the traceability matrix as a self-contained HTML report
 * suitable for review and inclusion in an assurance evidence package.
 */
public class HtmlTraceabilityExporter implements TraceabilityExporter {

    private static final Logger log = LoggerFactory.getLogger(HtmlTraceabilityExporter.class);

    @Override
    public void export(TraceabilityMatrix matrix, Path path) throws IOException {
        path.getParent().toFile().mkdirs();

        List<TraceabilityEntry> entries = matrix.getEntries();
        double coverage = matrix.getODDCoveragePercent();
        int uncoveredCount = matrix.getUncoveredODDElements().size();

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
            w.write("<title>ODME Traceability Matrix</title>");
            w.write("<style>");
            w.write("body{font-family:sans-serif;margin:2rem;color:#333}");
            w.write("h1{color:#1a5276}");
            w.write(".summary{background:#eaf2ff;padding:1rem;border-radius:4px;margin-bottom:1rem}");
            w.write(".metric{display:inline-block;margin-right:2rem}");
            w.write(".metric .value{font-size:2rem;font-weight:bold;color:#1a5276}");
            w.write(".metric .label{font-size:0.85rem;color:#555}");
            w.write("table{border-collapse:collapse;width:100%;margin-top:1rem}");
            w.write("th{background:#1a5276;color:#fff;padding:0.5rem;text-align:left}");
            w.write("td{padding:0.4rem 0.5rem;border-bottom:1px solid #ddd}");
            w.write("tr:hover{background:#f5f5f5}");
            w.write(".pass{color:#27ae60;font-weight:bold}");
            w.write(".fail{color:#e74c3c;font-weight:bold}");
            w.write(".pending{color:#f39c12}");
            w.write(".gap{background:#fdf2f8}");
            w.write("</style></head><body>");

            w.write("<h1>ODME Traceability Matrix</h1>");
            w.write("<p>Generated: " + Instant.now() + "</p>");

            // Summary metrics
            w.write("<div class='summary'>");
            w.write(metric(String.format(Locale.US, "%.1f%%", coverage), "ODD Coverage"));
            w.write(metric(String.valueOf(entries.size()), "Trace Links"));
            w.write(metric(String.valueOf(uncoveredCount), "Uncovered ODD Elements"));
            w.write(metric(String.valueOf(matrix.getScenariosWithoutTestCase().size()), "Scenarios Without Tests"));
            w.write("</div>");

            // Uncovered elements (gaps)
            if (uncoveredCount > 0) {
                w.write("<h2>&#9888; Coverage Gaps</h2>");
                w.write("<p>The following ODD elements are not covered by any scenario:</p>");
                w.write("<ul>");
                for (var node : matrix.getUncoveredODDElements()) {
                    w.write("<li class='gap'><code>" + esc(node.getPath()) + "</code> (" + esc(node.getName()) + ")</li>");
                }
                w.write("</ul>");
            }

            // Main table
            w.write("<h2>Full Traceability Table</h2>");
            w.write("<table><thead><tr>");
            w.write("<th>ODD Element</th><th>Path</th><th>Scenario</th><th>Test Case</th><th>Verdict</th>");
            w.write("</tr></thead><tbody>");

            for (TraceabilityEntry entry : entries) {
                String verdictClass = entry.verdict() == null ? "pending"
                    : entry.isPassed() ? "pass" : "fail";
                String verdictText = entry.verdict() != null ? entry.verdict() : "\u2013";

                w.write("<tr>");
                w.write("<td>" + esc(entry.oddElementName()) + "</td>");
                w.write("<td><code>" + esc(entry.oddElementPath()) + "</code></td>");
                w.write("<td>" + esc(entry.scenarioName()) + "</td>");
                w.write("<td>" + (entry.hasTestCase() ? esc(entry.testCaseId()) : "<em>\u2013</em>") + "</td>");
                w.write("<td class='" + verdictClass + "'>" + verdictText + "</td>");
                w.write("</tr>");
            }

            w.write("</tbody></table>");
            w.write("<footer><p><em>ODME \u2013 Operational Design Domain Modeling Environment</em></p></footer>");
            w.write("</body></html>");
        }

        log.info("Exported HTML traceability report to {}", path);
    }

    private String metric(String value, String label) {
        return "<div class='metric'><div class='value'>" + value +
            "</div><div class='label'>" + label + "</div></div>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
