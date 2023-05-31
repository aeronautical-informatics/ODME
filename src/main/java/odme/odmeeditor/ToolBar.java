package odme.odmeeditor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import odme.core.FileConvertion;
import odme.jtreetograph.JtreeToGraphAdd;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphModify;
import xml.schema.TypeInfoWriter;



public class ToolBar {
	
	private JToolBar toolbar;
	public static Map<String, JButton> btnItems = new HashMap<>();
	public static JButton btnScenario;
	
	public ToolBar(JFrame frame) {

        toolbar = new JToolBar();
        toolbar.setBorder(new EtchedBorder());
        frame.add(toolbar, BorderLayout.NORTH);
	}
        
    public void show() {
    	
    	String[] names = {"Selector", "Add Entity", "Add Aspect", "Add Specialization", "Add Multi-Aspect", "Delete Node", "Save Graph", "Undo", "Redo", "Zoom In", "Zoom Out", "Validation"};
    	String[] images = {"cursor", "en", "as16", "sp", "ma", "delete", "save", "undo", "redo", "zoom-in", "zoom-out", "validation"};
    	
    	for (int i=0; i<names.length; i++) {
    		ImageIcon Icon =
                    new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/"+images[i]+".png"));
            JButton btn = new JButton(Icon);
            btn.setName(names[i]);
            btn.setToolTipText(names[i]);
            toolbar.add(btn);
            btnItems.put(names[i], btn);
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { 
                	switch (((JButton) e.getSource()).getName()) {
              
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
            });
    	}
    	
    	
    	toolbar.add(Box.createHorizontalGlue());
    	
    	btnScenario = new JButton(" Scenarios List ");
    	toolbar.add(btnScenario);
    	toolbar.addSeparator();
    	btnScenario.setFont(btnScenario.getFont().deriveFont(Font.BOLD));
    	btnScenario.setIcon(new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/list.png")));
    	btnScenario.setVisible(false);
    	btnScenario.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
            	ScenarioList scenarioList = new ScenarioList();
            	scenarioList.createScenarioListWindow();
            }
        });
    }
    
    private void selector(){
    	ODMEEditor.nodeAddDetector = "";
    }
    
    private void addEntity(){
    	ODMEEditor.nodeAddDetector = "entity";
    }

    private void addAspect(){
    	ODMEEditor.nodeAddDetector = "aspect";
    }
    
    private void addSpecialization(){
    	ODMEEditor.nodeAddDetector = "specialization";
    }
    
    private void addMultiAspect(){
    	ODMEEditor.nodeAddDetector = "multiaspect";
    }
    
    private void deleteNode(){
    	ODMEEditor.nodeAddDetector = "delete";
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
    	// System.out.println("undo: if there is any node without input/output edge then
        // undo tree will not work-have to do it");
        // undo actions for jtree
    	JtreeToGraphGeneral.undo();
        // undo actions for graph
        try {
            if (ODMEEditor.undoJtree.canUndo()) {
            	ODMEEditor.undoJtree.undo();
            	ODMEEditor.treePanel.expandTree();
            }
        } 
        catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
        }
    }
    
    private void redo(){
    	// redo actions for jtree
    	JtreeToGraphGeneral.redo();
        // redo actions for graph
        try {
            if (ODMEEditor.undoJtree.canRedo()) {
            	ODMEEditor.undoJtree.redo();
            	ODMEEditor.treePanel.expandTree();
            }
        } 
        catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
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
        // this is SES validation
        Console.consoleText.setText(">>");

        ODMEEditor.saveChanges();
        
        String path = new String();
    	if (ODMEEditor.toolMode == "ses")
    		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName  + "/ses.xsd";
    	else
    		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario  + "/ses.xsd";
    	
        fileConversion.createSES(path);
        
        // have to fix this--------------------------------------
        fileConversion.modifyXmlOutputForXSD(); // changed the input file to graphxmluniformity
        JtreeToGraphConvert.rootToEndNodeSequenceSolve();
        JtreeToGraphConvert.rootToEndNodeVariable();
        // have to fix this end not needed all-----------------------------------
        // this is important here. others above have to check
        /*
         * if i use modifyXmlOutputFixForSameNameNode then var is added as entity and
         * SES is not generated showing error. have to check why
         */
        JtreeToGraphModify.modifyXmlOutputFixForSameNameNode();
        JtreeToGraphGeneral.xmlOutputForXSD();
        JtreeToGraphAdd.addconstraintToSESStructure();
        ODMEEditor.sesValidationControl = 1;
        TypeInfoWriter.validateXML();
//        if (ODMEEditor.errorPresentInSES == 1) {
//            sesview.textArea.setText("Error presents in the SES. Check console output for details.");
//            ODMEEditor.errorPresentInSES = 0;
//            Console.consoleText.setText(">>");
//            Console.addConsoleOutput(ODMEEditor.errorMessageInSES);
//        } 
//        else {
            XmlUtils.showViewer(ODMEEditor.fileLocation, ODMEEditor.projName, "xmlforxsd.xml", XmlUtils.sesview);
//        }
    }
}

