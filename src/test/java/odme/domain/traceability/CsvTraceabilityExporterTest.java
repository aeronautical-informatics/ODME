package odme.domain.traceability;

import odme.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CsvTraceabilityExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void export_createsCsvFile() throws IOException {
        TraceabilityMatrix matrix = buildMatrix();
        CsvTraceabilityExporter exporter = new CsvTraceabilityExporter();
        Path output = tempDir.resolve("traceability.csv");

        exporter.export(matrix, output);

        assertThat(output.toFile()).exists();
    }

    @Test
    void export_csvHasHeader() throws IOException {
        TraceabilityMatrix matrix = buildMatrix();
        CsvTraceabilityExporter exporter = new CsvTraceabilityExporter();
        Path output = tempDir.resolve("t.csv");

        exporter.export(matrix, output);

        String content = Files.readString(output);
        assertThat(content).contains("ODD Element ID");
        assertThat(content).contains("Scenario Name");
        assertThat(content).contains("Verdict");
    }

    @Test
    void export_csvContainsNodeData() throws IOException {
        TraceabilityMatrix matrix = buildMatrix();
        CsvTraceabilityExporter exporter = new CsvTraceabilityExporter();
        Path output = tempDir.resolve("t.csv");

        exporter.export(matrix, output);

        String content = Files.readString(output);
        assertThat(content).contains("Electric");
        assertThat(content).contains("ElectricScenario");
    }

    private TraceabilityMatrix buildMatrix() {
        SESTree ses = new SESTree("ses-1", "UAM_ODD");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        ses.setRoot(aircraft);
        ses.addNode("aircraft", electric);

        Scenario scenario = new Scenario("s1", "ElectricScenario", "ses-1");

        PESTree pes = new PESTree("pes-1", "ElectricScenario", "ses-1");
        SESNode a = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode e = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        a.addChild(e);
        pes.setRoot(a);

        return new TraceabilityMatrix(ses, List.of(scenario), List.of(pes));
    }
}
