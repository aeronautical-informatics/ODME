package odme.controller;

import javax.swing.JOptionPane;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import odme.core.EditorContext;
import odme.core.FileConvertion;
import odme.jtreetograph.JtreeToGraphAdd;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphModify;
import odme.odmeeditor.Console;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.XmlUtils;
import xml.schema.TypeInfoWriter;
import static odme.odmeeditor.XmlUtils.sesview;

public class ToolbarController {

    public void executeToolbarAction(String action) {
        switch (action) {
            case "Selector":
                selector();
                break;
            case "Add Entity":
                addEntity();
                break;
            case "Add Aspect":
                addAspect();
                break;
            case "Add Specialization":
                addSpecialization();
                break;
            case "Add Multi-Aspect":
                addMultiAspect();
                break;
            case "Delete Node":
                deleteNode();
                break;
            case "Save Graph":
                saveGraph();
                break;
            case "Undo":
                undo();
                break;   
            case "Redo":
                redo();
                break; 
            case "Zoom In":
                zoomIn();
                break;
            case "Zoom Out":
                zoomOut();
                break; 
            case "Validation":
                validation();
                break; 
        }
    }

    private void selector(){
        EditorContext.getInstance().setNodeAddDetector("");
    }
    
    private void addEntity(){
        EditorContext.getInstance().setNodeAddDetector("entity");
    }

    private void addAspect(){
        EditorContext.getInstance().setNodeAddDetector("aspect");
    }
    
    private void addSpecialization(){
        EditorContext.getInstance().setNodeAddDetector("specialization");
    }
    
    private void addMultiAspect(){
        EditorContext.getInstance().setNodeAddDetector("multiaspect");
    }
    
    private void deleteNode(){
        EditorContext.getInstance().setNodeAddDetector("delete");
    }
    
    private void saveGraph(){
        ODMEEditor.treePanel.saveTreeModel();
        JtreeToGraphConvert.convertTreeToXML();
        JtreeToGraphConvert.graphToXML();
        JtreeToGraphConvert.graphToXMLWithUniformity();
        JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void undo(){
        JtreeToGraphGeneral.undo();
        try {
            if (ODMEEditor.undoJtree.canUndo()) {
                ODMEEditor.undoJtree.undo();
                ODMEEditor.treePanel.expandTree();
            }
        } 
        catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void redo(){
        JtreeToGraphGeneral.redo();
        try {
            if (ODMEEditor.undoJtree.canRedo()) {
                ODMEEditor.undoJtree.redo();
                ODMEEditor.treePanel.expandTree();
            }
        } 
        catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void zoomIn(){
        JtreeToGraphGeneral.zoomIn();
    }
    
    private void zoomOut(){
        JtreeToGraphGeneral.zoomOut();
    }
    
    public static void validation(){
        FileConvertion fileConversion = new FileConvertion();
        Console.consoleText.setText(">>");

        ODMEEditor.saveChanges();
        
        String path = EditorContext.getInstance().getWorkingDir() + "/ses.xsd";
        
        fileConversion.createSES(path);
        
        fileConversion.modifyXmlOutputForXSD(); 
        JtreeToGraphConvert.rootToEndNodeSequenceSolve();
        JtreeToGraphConvert.rootToEndNodeVariable();
        JtreeToGraphModify.modifyXmlOutputFixForSameNameNode();
        JtreeToGraphGeneral.xmlOutputForXSD();
        JtreeToGraphAdd.addconstraintToSESStructure();
        ODMEEditor.sesValidationControl = 1;
        TypeInfoWriter.validateXML();

        XmlUtils.showViewer(EditorContext.getInstance().getFileLocation(), EditorContext.getInstance().getProjName(), "xmlforxsd.xml", XmlUtils.sesview);
    }
}
