package odme.core;

import java.io.File;

public class EditorContext {
    private static EditorContext instance = new EditorContext();

    public static EditorContext getInstance() {
        return instance;
    }

    private String fileLocation = java.nio.file.Paths.get("").toAbsolutePath().toString().replace("\\", "/");
    private String projName = "Main";
    private String currentScenario = "InitScenario";
    private String toolMode = "ses";
    private String importFileLocation = "";
    private String importFileName = "";
    private String newFileName = "Main";
    
    public String getFileLocation() { return fileLocation; }
    public void setFileLocation(String fileLocation) { this.fileLocation = fileLocation; }

    public String getProjName() { return projName; }
    public void setProjName(String projName) { this.projName = projName; }

    public String getCurrentScenario() { return currentScenario; }
    public void setCurrentScenario(String currentScenario) { this.currentScenario = currentScenario; }

    public String getToolMode() { return toolMode; }
    public void setToolMode(String toolMode) { this.toolMode = toolMode; }

    public String getImportFileLocation() { return importFileLocation; }
    public void setImportFileLocation(String importFileLocation) { this.importFileLocation = importFileLocation; }

    public String getImportFileName() { return importFileName; }
    public void setImportFileName(String importFileName) { this.importFileName = importFileName; }

    public String getNewFileName() { return newFileName; }
    public void setNewFileName(String newFileName) { this.newFileName = newFileName; }

    public String getProjectDir() {
        return fileLocation + "/" + projName;
    }

    public String getScenarioDir() {
        return getProjectDir() + "/" + currentScenario;
    }

    /**
     * Returns the working directory for the current mode.
     * SES mode: fileLocation/projName
     * PES mode: fileLocation/projName/currentScenario
     */
    public String getWorkingDir() {
        if ("ses".equals(toolMode)) {
            return getProjectDir();
        } else {
            return getScenarioDir();
        }
    }

    public File getSsdFileGraph() {
        return new File(String.format("%s/%sGraph.xml", getWorkingDir(), newFileName));
    }

    private String nodeAddDetector = "";

    public String getNodeAddDetector() {
        return nodeAddDetector;
    }

    public void setNodeAddDetector(String nodeAddDetector) {
        this.nodeAddDetector = nodeAddDetector;
    }
}
