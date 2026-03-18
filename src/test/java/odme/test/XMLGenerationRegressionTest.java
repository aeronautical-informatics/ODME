package odme.test;

import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.ProjectTree;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphVariables;

import odme.core.EditorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XMLGenerationRegressionTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() throws java.io.IOException {
        // Setup initial expected global state
        EditorContext.getInstance().setFileLocation(tempDir.toString());
        EditorContext.getInstance().setProjName("TestProject");
        EditorContext.getInstance().setCurrentScenario("InitScenario");
        EditorContext.getInstance().setToolMode("ses");
        EditorContext.getInstance().setNewFileName("TestProject");
        // ssdFileGraph is computed from context variables
        
        Files.createDirectories(tempDir.resolve("TestProject"));
        Files.createDirectories(tempDir.resolve("InitScenario"));
        
        // Setup mock tree
        ODMEEditor.treePanel = new DynamicTree();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("TestProject");
        ODMEEditor.treePanel.addObject(null, root);
        DefaultMutableTreeNode entity = ODMEEditor.treePanel.addObject(root, "TestEntity");
        ODMEEditor.treePanel.addObject(entity, "TestAspectDec");

        ODMEEditor.projectPanel = new odme.odmeeditor.ProjectTree();
        DefaultMutableTreeNode projRoot = new DefaultMutableTreeNode("Project");
        ODMEEditor.projectPanel.projectTree.setModel(new javax.swing.tree.DefaultTreeModel(projRoot));
    }

    @Test
    public void testConvertTreeToXML_CreatesBaseFile() {
        // Act
        JtreeToGraphConvert.convertTreeToXML();
        
        // Assert
        Path expectedXmlPath = tempDir.resolve("TestProject").resolve("projectTree.xml");
        assertTrue(expectedXmlPath.toFile().exists(), "Expected XML file to be generated at " + expectedXmlPath);
    }
}
