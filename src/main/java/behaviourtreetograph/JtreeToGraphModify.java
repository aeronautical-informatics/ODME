package behaviourtreetograph;

import odme.core.EditorContext;

import javax.swing.JOptionPane;
import java.nio.file.Path;

import static odme.behaviour.BehaviourToTree.selectedScenario;

/** Converts graph export XML into a behavior-tree XML payload. */
public final class JtreeToGraphModify {
    private JtreeToGraphModify() { }

    /**
     * Retained for callers using the historic API. The conversion is now DOM
     * based so element and action attributes are preserved.
     */
    public static void modifiyXMLFile() {
        Path scenarioDirectory = Path.of(EditorContext.getInstance().getFileLocation(),
                EditorContext.getInstance().getProjName(), selectedScenario);
        try {
            BehaviourXmlWorkflow.exportBehaviourTree(
                    scenarioDirectory.resolve("behaviourxml.xml"),
                    scenarioDirectory.resolve("OutputBehaviourxml.xml"));
        } catch (Exception exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + exception.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
