package odme.controller;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxUndoManager;

import odme.core.EditorContext;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphModify;
import odme.jtreetograph.JtreeToGraphSave;
import odme.jtreetograph.JtreeToGraphVariables;
import odme.module.importFromCameo.FileImporter;
import odme.odmeeditor.About;
import odme.odmeeditor.BehaviourList;
import odme.odmeeditor.Console;
import odme.odmeeditor.Constraint;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Execution;
import odme.odmeeditor.Main;
import odme.odmeeditor.NewProject;
import odme.odmeeditor.ODDManager;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.ProjectTree;
import odme.odmeeditor.ScenarioList;
import odme.odmeeditor.Variable;

import static odme.odmeeditor.XmlUtils.sesview;

public class MenuController {
    
    private JFrame mainFrame;
    
    public MenuController(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    public void executeMenuAction(String action) {
        switch (action) {
            case "Save Scenario":
                saveScenario();
                break;
            case "Scenarios List":
                ScenarioList scenarioList = new ScenarioList();
                scenarioList.createScenarioListWindow();
                break;
            case "Execution":
                openExecutionWindow();
                break;
            case "New Project":
                newFunc();
                break;
            case "Open":
                openFunc();
                break;
            case "Save As":
                saveAsFunc();
                break;
            case "Save as PNG":
                saveAsPNGFunc();
                break;
            case "Import Template":
                importFunc();
                break;
            case "Sync Behaviour":
                BehaviourList b = new BehaviourList();
                b.createScenarioListWindow();
                break;
            case "Save as Template":
                exportFunc();
                break;
            case "Import From Cameo":
                cImportFunc();
                break;
            case "Exit":
                System.exit(1);
                break;
            case "Manual":
                manualFunc();
                break;
            case "About":
                About about = new About();
                about.aboutGUI();
                break;
            case "Generate OD":
                openODDManager("Generate OD");
                break;
            case "ODD Manager":
                openODDManager("ODD Manager");
                break;
        }
    }

    private void cImportFunc() {
        FileImporter fileImporter = new FileImporter();
        fileImporter.showImportDialog(mainFrame);
    }

    private void openODDManager(String mode) {
        ODMEEditor.saveFunc(false); 
        ODMEEditor.updateState();
        ODDManager nt=new ODDManager(mode);
        JFrame jd = new JFrame();
        jd.getContentPane().add(nt);
        jd.pack();
        jd.setVisible(true);
        jd.setLocationRelativeTo(null);
    }

    @SuppressWarnings("unchecked")
    private void saveScenario() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json")){
            Object obj = null;
            try {
                obj = jsonParser.parse(reader);
            } 
            catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JSONArray data = (JSONArray) obj;
            JTextField nameField = new JTextField();
            nameField.setText("Scenario" + Integer.toString(data.size()+1));
            Object[] message = {"Scenario Name:", nameField};

            int option = JOptionPane
                    .showConfirmDialog(Main.frame, message, "Create Scenario", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            
            if (option == JOptionPane.OK_OPTION) {
                createScenario(nameField.getText());
                JSONObject jo = new JSONObject();
                jo.put("name", nameField.getText());
                jo.put("risk", "");
                jo.put("remarks", "");
                JSONObject jom = new JSONObject();
                jom.put("scenario", jo);
                data.add(jom);
            } else {
                return;
            }
            
            try {
                 FileWriter file = new FileWriter(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");
                 file.write(data.toJSONString());
                 file.close();
                 ODMEEditor.graphWindow.setTitle(nameField.getText());
              } catch (IOException e) {
                 e.printStackTrace();
                 JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                 return;
              }
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createScenario(String ScenarioName) {
         EditorContext.getInstance().setCurrentScenario(ScenarioName);
         ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
                 EditorContext.getInstance().getFileLocation(),  ScenarioName, EditorContext.getInstance().getProjName()));
         ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
                 EditorContext.getInstance().getFileLocation(),  ScenarioName, EditorContext.getInstance().getProjName()));
         ODMEEditor.treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
                 EditorContext.getInstance().getFileLocation(),  ScenarioName, EditorContext.getInstance().getProjName()));
         ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
                 EditorContext.getInstance().getFileLocation(),  ScenarioName, EditorContext.getInstance().getProjName()));
        ODMEEditor.treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
                EditorContext.getInstance().getFileLocation(),  ScenarioName, EditorContext.getInstance().getProjName()));

        File f = new File(EditorContext.getInstance().getFileLocation() + "/" +  ScenarioName);
        f.mkdirs();
        
        ODMEEditor.updateState();
        ODMEEditor.changePruneColor();
    }
    
    private void newFunc() {
        DynamicTree.varMap = ArrayListMultimap.create();
        NewProject newProject = new NewProject();
        newProject.createNewProjectWindow();
        JtreeToGraphVariables.undoManager = new mxUndoManager();
    }
    
    private void openFunc() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(new File(ODMEEditor.repFslas));

        int result = fileChooser.showOpenDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            String oldProjectTreeProjectName = EditorContext.getInstance().getProjName();
            EditorContext.getInstance().setProjName(fileName);
            EditorContext.getInstance().setNewFileName(fileName);
            EditorContext.getInstance().setFileLocation(selectedFile.getParentFile().getAbsolutePath());
            JtreeToGraphGeneral.openExistingProject(fileName, oldProjectTreeProjectName);

            JtreeToGraphVariables.undoManager = new mxUndoManager();
            sesview.textArea.setText("");
            Console.consoleText.setText(">>");
            Variable.setNullToAllRows();
            Constraint.setNullToAllRows();
            
            if ("pes".equals(EditorContext.getInstance().getToolMode()))
                ODMEEditor.applyGuiSES();
        }
    }
    
    private void saveAsFunc() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(EditorContext.getInstance().getFileLocation()));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            EditorContext.getInstance().setFileLocation(selectedFile.getParentFile().getAbsolutePath());

            String newProjectName = selectedFile.getName();
            String oldProjectTreeProjectName = EditorContext.getInstance().getProjName();

            EditorContext.getInstance().setProjName(newProjectName);
            EditorContext.getInstance().setNewFileName(newProjectName);
            ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
                    EditorContext.getInstance().getFileLocation(), EditorContext.getInstance().getProjName(), newProjectName));
            ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
                    EditorContext.getInstance().getFileLocation(), EditorContext.getInstance().getProjName(), newProjectName));
            ODMEEditor.treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
                    EditorContext.getInstance().getFileLocation(), EditorContext.getInstance().getProjName(), newProjectName));
            ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
                    EditorContext.getInstance().getFileLocation(), EditorContext.getInstance().getProjName(), newProjectName));

            ProjectTree.projectName = newProjectName;
            ODMEEditor.projectPanel.changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);

            ODMEEditor.newProjectFolderCreation();
            ODMEEditor.treePanel.saveTreeModel();
            JtreeToGraphSave.saveGraph();

            JtreeToGraphConvert.convertTreeToXML(); 
            JtreeToGraphConvert.graphToXML();
            JtreeToGraphConvert.graphToXMLWithUniformity();
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void saveAsPNGFunc() {
        // remove border nodes
        JtreeToGraphVariables.graph.getModel().beginUpdate();
        Object position = JtreeToGraphVariables.graphComponent.getCellAt(0, 50000);
        JtreeToGraphVariables.graph.removeCells(new Object[]{position});
        position = JtreeToGraphVariables.graphComponent.getCellAt(50000, 0);
        JtreeToGraphVariables.graph.removeCells(new Object[]{position});
        JtreeToGraphVariables.graph.getModel().endUpdate();

        try{
            BufferedImage image = mxCellRenderer.createBufferedImage(JtreeToGraphVariables.graph, null, 1, Color.WHITE, true, null);
            String path = EditorContext.getInstance().getWorkingDir() + "/graph.png";
                
            ImageIO.write(image, "PNG", new File(path));
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save PNG", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.out.println("Error:" + e);
            JOptionPane.showMessageDialog(Main.frame, "Error:" + e, "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        // add border nodes again
        JtreeToGraphVariables.graph.getModel().beginUpdate();
        JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
        JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
        JtreeToGraphVariables.graph.getModel().endUpdate(); 
    }

    private void importFunc() {
        odme.odmeeditor.ImportProject impProj = new odme.odmeeditor.ImportProject();
        impProj.importProject();
    }

    private void exportFunc() {
        ToolbarController.validation();
        String fileName = EditorContext.getInstance().getProjName(); 
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);
        fileChooser.setSelectedFile(new File(fileName)); 
        fileChooser.setCurrentDirectory(new File(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName()));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            PrintWriter f0 = null;
            try {
                f0 = new PrintWriter(new FileWriter(selectedFile.getAbsolutePath() + ".xml"));
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Scanner in = null;
            try {
                in = new Scanner(new File(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/xmlforxsd.xml"));
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            while (in.hasNext()) { 
                String line = in.nextLine();
                f0.println(line);
            }
            in.close();
            f0.close();
        }
    }

    private void manualFunc() {
        File pdfTemp = null;
        if (Desktop.isDesktopSupported()) {
            try {
                java.net.URL resource = ODMEEditor.class.getClassLoader().getResource("docs/manual.pdf");
                try {
                    pdfTemp = new File(resource.toURI());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Desktop.getDesktop().open(pdfTemp);
            } catch (IOException e1) {
                System.out.println("erreur : " + e1);
            }
        }
    }

    private void openExecutionWindow() {
        Execution executionWindow = new Execution();
        executionWindow.setVisible(true);
    }
}
