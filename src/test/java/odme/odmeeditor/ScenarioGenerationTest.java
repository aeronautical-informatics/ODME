package odme.odmeeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScenarioGeneration#importScenarioDatasFromCSVFile}.
 * Tests the CSV → XML conversion logic directly (no EditorContext dependency).
 */
class ScenarioGenerationTest {

    @TempDir
    Path tempDir;

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private Path writeCsv(String content) throws IOException {
        Path file = tempDir.resolve("input_" + System.nanoTime() + ".csv");
        Files.writeString(file, content);
        return file;
    }

    private Path outputDir() throws IOException {
        Path dir = tempDir.resolve("output_" + System.nanoTime());
        Files.createDirectories(dir);
        return dir;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Basic generation
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void importScenarioDatasFromCSVFile_twoDataRows_twoXmlFiles() throws Exception {
        Path csv = writeCsv(
                "EgoAC_Altitude,EgoAC_Speed\n" +
                "500,120\n" +
                "800,200\n");
        Path out = outputDir();

        String result = ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        assertTrue(result.startsWith("Files saved in"), "Expected success message, got: " + result);
        assertTrue(Files.exists(out.resolve("Scenario_1.xml")));
        assertTrue(Files.exists(out.resolve("Scenario_2.xml")));
        assertFalse(Files.exists(out.resolve("Scenario_3.xml")), "Should have exactly 2 files");
    }

    @Test
    void importScenarioDatasFromCSVFile_xmlContainsEntityAndVariable() throws Exception {
        Path csv = writeCsv(
                "EgoAC_Altitude,EgoAC_Speed\n" +
                "1000,150\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        String xml = Files.readString(out.resolve("Scenario_1.xml"));
        assertTrue(xml.contains("<entity name=\"EgoAC\">"), "Entity element missing");
        assertTrue(xml.contains("name=\"Altitude\""), "Variable Altitude missing");
        assertTrue(xml.contains("default=\"1000\""), "Value 1000 missing");
        assertTrue(xml.contains("name=\"Speed\""), "Variable Speed missing");
        assertTrue(xml.contains("default=\"150\""), "Value 150 missing");
    }

    @Test
    void importScenarioDatasFromCSVFile_xmlHasCorrectRootStructure() throws Exception {
        Path csv = writeCsv(
                "EgoAC_Altitude\n" +
                "500\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        String xml = Files.readString(out.resolve("Scenario_1.xml"));
        assertTrue(xml.contains("<?xml version=\"1.0\""), "XML declaration missing");
        assertTrue(xml.contains("<entity"), "Root entity missing");
        assertTrue(xml.contains("name=\"Scenario\""), "Scenario name missing");
        assertTrue(xml.contains("<aspect name=\"scenarioDec\">"), "Aspect missing");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Multiple entities
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void importScenarioDatasFromCSVFile_multipleEntities_allGroupedCorrectly() throws Exception {
        Path csv = writeCsv(
                "EgoAC_Speed,Traffic_Count,Weather_Condition\n" +
                "120,5,Rainy\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        String xml = Files.readString(out.resolve("Scenario_1.xml"));
        assertTrue(xml.contains("<entity name=\"EgoAC\">"), "EgoAC entity missing");
        assertTrue(xml.contains("<entity name=\"Traffic\">"), "Traffic entity missing");
        assertTrue(xml.contains("<entity name=\"Weather\">"), "Weather entity missing");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void importScenarioDatasFromCSVFile_emptyCsv_returnsEmptyMessage() throws Exception {
        Path csv = writeCsv("");  // completely empty
        Path out = outputDir();

        String result = ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());
        assertEquals("CSV is empty!", result);
    }

    @Test
    void importScenarioDatasFromCSVFile_headerOnlyNoDataRows_noXmlFiles() throws Exception {
        Path csv = writeCsv("EgoAC_Altitude,EgoAC_Speed\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        assertFalse(Files.exists(out.resolve("Scenario_1.xml")),
                "No XML should be created when there are no data rows");
    }

    @Test
    void importScenarioDatasFromCSVFile_columnsWithoutUnderscore_ignoredInEntities() throws Exception {
        // Columns without underscore separator cannot be grouped into entity/variable
        Path csv = writeCsv(
                "EgoACSpeed,EgoAC_Altitude\n" +
                "120,500\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        String xml = Files.readString(out.resolve("Scenario_1.xml"));
        // EgoACSpeed (no underscore) must NOT appear as an entity
        assertFalse(xml.contains("<entity name=\"EgoACSpeed\">"),
                "Column without underscore must not create an entity");
        // EgoAC_Altitude (with underscore) must appear
        assertTrue(xml.contains("<entity name=\"EgoAC\">"),
                "Column with underscore must create entity");
    }

    @Test
    void importScenarioDatasFromCSVFile_blankLinesInData_skipped() throws Exception {
        Path csv = writeCsv(
                "EgoAC_Speed\n" +
                "100\n" +
                "\n" +
                "200\n");
        Path out = outputDir();

        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        assertTrue(Files.exists(out.resolve("Scenario_1.xml")));
        assertTrue(Files.exists(out.resolve("Scenario_2.xml")));
        // The blank line must be skipped → only 2 files
        assertFalse(Files.exists(out.resolve("Scenario_3.xml")));
    }

    @Test
    void importScenarioDatasFromCSVFile_nonExistentCsv_returnsError() {
        String result = ScenarioGeneration.importScenarioDatasFromCSVFile(
                tempDir.resolve("does_not_exist.csv").toString(),
                tempDir.resolve("out").toString());
        assertTrue(result.startsWith("Error:"), "Expected an error message for missing file");
    }

    // ────────────────────────────────────────────────────────────────────────
    // fileExistValidator reset
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void importScenarioDatasFromCSVFile_resetsFileExistValidator() throws Exception {
        Path csv = writeCsv("EgoAC_Speed\n100\n");
        Path out = outputDir();

        // Set it to 1 before the call
        ScenarioGeneration.fileExistValidator = 1;
        ScenarioGeneration.importScenarioDatasFromCSVFile(csv.toString(), out.toString());

        assertEquals(0, ScenarioGeneration.fileExistValidator,
                "fileExistValidator must be reset to 0 after a normal run");
    }
}
