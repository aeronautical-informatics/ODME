package odme.odmeeditor;

import odme.core.EditorContext;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts CSV rows (sampled data) into individual XML scenario files.
 * Column headers follow the pattern "EntityName_VariableName" — e.g. "EgoAC_Altitude".
 * Each data row produces one Scenario_N.xml file in the specified output folder.
 */
public class ScenarioGeneration {

    public static int fileExistValidator = 0;
    public static String scenarioName = null;

    /**
     * Entry point: reads csvPath and writes XML scenario files under a new folder
     * named after nameScenarioList inside GeneratedScenarios/<projName>_Scenarios/.
     */
    public static String generateScenarios(String csvPath, String nameScenarioList) {
        scenarioName = nameScenarioList;
        String projName = EditorContext.getInstance().getProjName();
        String fileLocation = ODMEEditor.fileLocation;

        String pathParent = fileLocation + "/GeneratedScenarios/" + projName + "_Scenarios/" + nameScenarioList;
        File folder = new File(pathParent);

        if (folder.exists()) {
            fileExistValidator = 1;
            String newPathParent = fileLocation + "/GeneratedScenarios/" + projName + "_Scenarios/" + nameScenarioList + "1";
            folder = new File(newPathParent);
        }

        boolean created = folder.mkdirs();
        if (!created) {
            fileExistValidator = 0;
            return "Failed to create folder: " + folder.getAbsolutePath() +
                    "\n>>Restart the Process and Enter a NEW Scenario Name<<";
        }
        return importScenarioDatasFromCSVFile(csvPath, folder.getAbsolutePath());
    }

    public static String importScenarioDatasFromCSVFile(String csvFilePath, String outputDirectory) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) return "CSV is empty!";

            String[] headers = headerLine.split(",");
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) outputDir.mkdirs();

            String line;
            int fileCount = 1;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",");
                Map<String, String> data = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    data.put(headers[i].trim(), values[i].trim());
                }

                String xmlContent = buildXMLScenarioContent(data);
                File outputFile = new File(outputDir, "Scenario_" + fileCount + ".xml");
                try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                    writer.write(xmlContent);
                }
                fileCount++;
            }

            String result = "Files saved in " + outputDirectory;
            if (fileExistValidator == 1) {
                result = "Files saved in " + outputDirectory +
                        "\n NB: \n- The Scenario name " + scenarioName + " you entered already exists.\n" +
                        "- The new Scenario Name is " + scenarioName + "1.\n" +
                        "- Next time use a different Scenario name";
                fileExistValidator = 0;
                return result;
            }
            fileExistValidator = 0;
            return result;

        } catch (IOException e) {
            fileExistValidator = 0;
            return "Error: " + e.getMessage();
        }
    }

    private static String buildXMLScenarioContent(Map<String, String> data) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        xml.append("<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\"Scenario\">\n");
        xml.append("<aspect name=\"scenarioDec\">\n");

        // Group columns by entity prefix: "EgoAC_Altitude" → entity=EgoAC, var=Altitude
        Map<String, List<Map.Entry<String, String>>> entityGroups = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!key.contains("_")) continue;
            String[] parts = key.split("_", 2);
            String entityName = parts[0].trim();
            String varName = parts[1].trim();
            entityGroups.computeIfAbsent(entityName, k -> new ArrayList<>())
                    .add(Map.entry(varName, entry.getValue().trim()));
        }

        for (Map.Entry<String, List<Map.Entry<String, String>>> entity : entityGroups.entrySet()) {
            xml.append("<entity name=\"").append(entity.getKey()).append("\">\n");
            for (Map.Entry<String, String> variable : entity.getValue()) {
                xml.append("<var name=\"").append(variable.getKey())
                        .append("\" default=\"").append(variable.getValue()).append("\"> </var>\n");
            }
            xml.append("</entity>\n");
        }

        xml.append("</aspect>\n</entity>\n");
        return xml.toString();
    }
}
