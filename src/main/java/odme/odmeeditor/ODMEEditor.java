package odme.odmeeditor;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxUndoManager;

import odeme.behaviour.Behaviour;
import odme.core.EditorUndoableEditListener;
import odme.core.FileConvertion;
import odme.jtreetograph.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;

import static odme.odmeeditor.ODDManager.getOpenedFile;
import static odme.odmeeditor.XmlUtils.sesview;


public class ODMEEditor extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String toolMode = "ses";
	
    public static String nodeName = "NewNode ";
    public static int openClicked = 0;
    public static String openFileName = "Scenario";
    public static String nodeAddDetector = "";
    
    public static String currentScenario = "InitScenario";
    public static String projName = "Main";
    
    
    public static UndoManager undoJtree = new UndoManager();
    public static boolean undoControlForSubTree = false;
    public static int sesValidationControl = 0;
    public static int errorPresentInSES  = 0;
    public static String errorMessageInSES = null;
    public static DynamicTree treePanel;
    
    public static Constraint scenarioConstraint;
    public static Behaviour scenarioBehaviour;
    
    public static ProjectTree projectPanel;
    public static GraphWindow graphWindow;
    public static Console console;
    public static Variable scenarioVariable;

    public static JTabbedPane tabbedPane = null;
    public static JSplitPane splitPane;
    
    private static Path path = Paths.get("").toAbsolutePath();
    public static String repFslas = path.toString().replace("\\", "/");
    public static String fileLocation = repFslas;
    public static String importFileLocation = "";
    public static String importFileName = "";
    
    public static JButton btnMode = new JButton("Domain Modelling");
    public static JLabel statusLabel = new JLabel("Current Mode: Domain Modelling");
    
    public static FileConvertion fileConversion = new FileConvertion();
    
    public ODMEEditor() {
        super(new BorderLayout());
//      -------------------------------------
        DynamicTree.projectFileName = JtreeToGraphVariables.newFileName;
        
        //
        tabbedPane =  new JTabbedPane();
        
        treePanel = new DynamicTree();
        treePanel.addUndoableEditListener(new EditorUndoableEditListener());
        treePanel.setPreferredSize(new Dimension(200, 600));
        
//      -------------------------------------
        projectPanel = new ProjectTree();
        
//      -------------------------------------
        scenarioVariable = new Variable();
        scenarioVariable.setPreferredSize(new Dimension(100, 100));
        scenarioVariable.setBorder(new EtchedBorder());
    
        
//      -------------------------------------
        scenarioConstraint = new Constraint();
        scenarioConstraint.setPreferredSize(new Dimension(100, 100));
        scenarioConstraint.setBorder(new EtchedBorder());

        scenarioBehaviour = new Behaviour();
        scenarioBehaviour.setPreferredSize(new Dimension(100 , 100));
        scenarioBehaviour.setBorder(new EtchedBorder());
      
//        -------------------------------------
        // Adding jgraph window in the center
        graphWindow = new GraphWindow();
        graphWindow.setPreferredSize(new Dimension(800, 400));
        removeTopLeftIcon(graphWindow);
        graphWindow.pack();
        graphWindow.setVisible(true);
        JtreeToGraphCreate.createGraph(graphWindow);
        
//        -------------------------------------
        // Console
        console = new Console();
        console.setPreferredSize(new Dimension(250, 100));
        removeTopLeftIcon(console);
        console.pack();
        console.setVisible(true);
        
//      -------------------------------------
        XmlUtils.ontologyview = XmlUtils.initView("Ontology");
        XmlUtils.sesview = XmlUtils.initView("XML");
        XmlUtils.schemaview = XmlUtils.initView("Schema");

        // creating tab window
        tabbedPaneChange();
        tabbedPane.addTab("Ontology", XmlUtils.ontologyview);
        tabbedPane.addTab("Schema", XmlUtils.schemaview);
        
//    -------------------------------------
        // add panelSpliter with main window's parts 
        PanelSplitor panelSplitor = new PanelSplitor();
        splitPane = panelSplitor.addSplitor(projectPanel, treePanel, graphWindow,
        			console, scenarioVariable, scenarioBehaviour,scenarioConstraint, tabbedPane);  
       
    }
    
    public static void addStatuBar(JFrame frame) {
    	
    	// create the status bar panel and show it down the bottom of the frame
    	JPanel statusPanel = new JPanel();
    	statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    	frame.add(statusPanel, BorderLayout.SOUTH);
    	statusPanel.setPreferredSize(new Dimension(frame.getWidth(), 32));
    	statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
    	
    	statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
    	statusPanel.add(statusLabel);
    	
    	Border border = statusLabel.getBorder();
    	Border margin = new EmptyBorder(0,10,0,0);
    	statusLabel.setBorder(new CompoundBorder(border, margin));
    	statusPanel.add(Box.createHorizontalGlue());
    			
    	btnMode.setBackground(new Color(255,255,255));
    	btnMode.setFont(btnMode.getFont().deriveFont(Font.BOLD));
    	btnMode.setIcon(new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/switch.png")));
        statusPanel.add(btnMode);
        btnMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
            	if (ODMEEditor.toolMode == "ses") {
            		//Save Current
            		ODMEEditor.treePanel.saveTreeModel();

                	JtreeToGraphConvert.convertTreeToXML();
                	JtreeToGraphConvert.graphToXML();
                	JtreeToGraphConvert.graphToXMLWithUniformity();
                	switchToPes();
            	}

            	else {
            		//Save Current (Pes)
            		treePanel.saveTreeModel();
                	JtreeToGraphConvert.convertTreeToXML();
                	JtreeToGraphConvert.graphToXML();
                	JtreeToGraphConvert.graphToXMLWithUniformity();

                	//open Ses
                	toolMode = "ses";
                	
                	File selectedFile = new File(fileLocation);
                	String fileName = selectedFile.getName();
                    String oldProjectTreeProjectName = projName;
                    projName = fileName;
                    fileLocation = selectedFile.getParentFile().getAbsolutePath();
                    JtreeToGraphGeneral.openExistingProject(projName, oldProjectTreeProjectName);
                    JtreeToGraphVariables.
                            undoManager = new mxUndoManager();

                    sesview.textArea.setText("");
                    Console.consoleText.setText(">>");
                    Variable.setNullToAllRows();
                    Constraint.setNullToAllRows();
                    Behaviour.setNullToAllRows();
                    applyGuiSES();
            	}
            }
        }); 
    }
    
    public static void applyGuiSES() {
    	// Apply Gui changes
        btnMode.setText("Domain Modelling");
		
		for (Map.Entry<String, JButton> entry : ToolBar.btnItems.entrySet()) {
			JButton btn = entry.getValue();
			btn.setVisible(true);
		}

		// Change MenuBar 
		for (JMenuItem item : MenuBar.fileMenuItems) {
			if (item.isEnabled())
				item.setEnabled(false);
			else
				item.setEnabled(true);
		}
		
		statusLabel.setText("Current Mode: Domain Modelling");
		tabbedPane.removeAll();
		tabbedPane.addTab("Ontology", XmlUtils.ontologyview);
        tabbedPane.addTab("Schema", XmlUtils.schemaview);
		
		
		ToolBar.btnScenario.setVisible(false);
		ODMEEditor.graphWindow.setTitle(projName);
		
		JTableHeader th = Variable.table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        TableColumn tc = tcm.getColumn(3);
        tc.setHeaderValue( "Default Value" );
        th.repaint();
    }
    
    private static void switchToPes() {
    	btnMode.setText("Scenario Modelling");
    	statusLabel.setText("Current Mode: Scenario Modelling");
		ODMEEditor.toolMode = "pes";
		currentScenario = "InitScenario";
		
		// Change Toollbar
		for (Map.Entry<String, JButton> entry : ToolBar.btnItems.entrySet()) {
			String name = entry.getKey();
			JButton btn = entry.getValue();
		    if (name=="Add Entity" || name== "Add Aspect" || name=="Add Specialization" ||
		    		name=="Add Multi-Aspect" || name=="Delete Node") {
		    	btn.setVisible(false);
		    }
		}
		
		// Change MenuBar 
		for (JMenuItem item : MenuBar.fileMenuItems) {
			if (item.isEnabled())
				item.setEnabled(false);
			else
				item.setEnabled(true);
    	}
    	
    	//Save as to create Pes version
        fileLocation = fileLocation+"/"+projName;
        JtreeToGraphVariables.newFileName = currentScenario;
        JtreeToGraphVariables.projectFileNameGraph = currentScenario;
  
        JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
    			fileLocation, currentScenario, projName));
        treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
        		fileLocation, currentScenario, projName));
        treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
        		fileLocation, currentScenario, projName));
        treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
        		fileLocation, currentScenario, projName));

        treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
        		fileLocation, currentScenario, projName));
        
        treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
        		fileLocation, currentScenario, projName));

        File f = new File(fileLocation + "/" + currentScenario);
        f.mkdirs();
        
        treePanel.saveTreeModel();
        
        JtreeToGraphConvert.convertTreeToXML();
        JtreeToGraphConvert.graphToXML();
        JtreeToGraphConvert.graphToXMLWithUniformity();
        
        tabbedPane.removeAll();
        tabbedPane.addTab("XML", XmlUtils.sesview);
        changePruneColor();
        ToolBar.btnScenario.setVisible(true);
        ODMEEditor.graphWindow.setTitle(currentScenario);
        nodeAddDetector = "";
        
        JTableHeader th = Variable.table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        TableColumn tc = tcm.getColumn(3);
        tc.setHeaderValue( "Value" );
        th.repaint();

        JtreeToGraphPrune.behMapTransfer = ArrayListMultimap.create();
        JtreeToGraphPrune.varMapTransfer = ArrayListMultimap.create();
    }
    
    public static void changePruneColor() {
    	JtreeToGraphVariables.graph.clearSelection();  
    	JtreeToGraphVariables.graph.selectAll();
        Object[] cells = JtreeToGraphVariables.graph.getSelectionCells(); //here you have all cells
        for (Object c : cells) {
        	mxCell cell = (mxCell) c; //cast
        	if (cell.isVertex()) { //isVertex
        		Object[] temp =  new Object[1];
        		temp[0] = cell;
        		
        		if (cell.getValue().toString().endsWith("Dec")){}
        		
        		else if (cell.getValue().toString().endsWith("MAsp"))
        			JtreeToGraphVariables.graph.setCellStyle("Multiaspectp", temp);
        		else if (cell.getValue().toString().endsWith("Spec"))
        			JtreeToGraphVariables.graph.setCellStyle("Specializationp", temp);
        		else {
        			for (TreePath key : DynamicTree.varMap.keySet()) {
        				String[] arr = key.toString().substring(1,key.toString().length()-1).split(",");
        				if (cell.getValue().toString().trim().equals(arr[arr.length-1].trim())) {
        					JtreeToGraphVariables.graph.setCellStyle("Entityp", temp);
        					break;
        				}
        			}
        		}	
        	}
        	else { //is not a vertex, so u can get source and target 
        		//cell.getChildCount(); //Returns the number of child cells. (edges)
        	}
        }
        JtreeToGraphVariables.graph.clearSelection();
    }
    
    
    /**
     * This function saves all the required files based on current addition.
     */
    public static void saveChanges() {
        treePanel.saveTreeModel();
        JtreeToGraphConvert.convertTreeToXML();
        JtreeToGraphConvert.graphToXML();
        JtreeToGraphConvert.graphToXMLWithUniformity();
    }

    public static void newProjectFolderCreation() {
        File f = new File(fileLocation + "/" + projName);
        f.mkdirs();
    }

    /**
     * editted by Roy: for compatibility issues, added a function that calls
     * the saveFunc but with parameter (used polymorphism). this is to enable
     * the user functions to use saveFunc without feeling any changes
     * */
    public static void saveFunc() {
        saveFunc(true);
    }

    /**
     * editted by Roy: added this boolean that enables showing a message
     * */
    public static void saveFunc(boolean showMessage) {
        // this code is also present in convert to xml button click action.
        ODMEEditor.treePanel.saveTreeModel();
        JtreeToGraphSave.saveGraph();

        JtreeToGraphConvert.convertTreeToXML(); // this function is using for converting project tree into xml file
        JtreeToGraphConvert.graphToXML();
        JtreeToGraphConvert.graphToXMLWithUniformity();

        if(showMessage)
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * @author Roy
     * this function is being used at multiple places
     * to update the state of the program and make sure
     * the latest version of the projet is being read
     * */
    public static void updateState() {
        ODMEEditor.treePanel.saveTreeModel();
        JtreeToGraphConvert.convertTreeToXML();
        JtreeToGraphConvert.graphToXML();
        JtreeToGraphConvert.graphToXMLWithUniformity();
        ODMEEditor.graphWindow.setTitle( ODMEEditor.currentScenario);

        ODMEEditor.saveChanges();
        ODMEEditor.fileConversion.modifyXmlOutputForXSD();
        JtreeToGraphConvert.rootToEndNodeSequenceSolve();
        JtreeToGraphConvert.rootToEndNodeVariable();

        JtreeToGraphModify.modifyXmlOutputFixForSameNameNode();
        JtreeToGraphGeneral.xmlOutputForXSD();
        ODMEEditor.fileConversion.xmlToXSDConversion();
    }

    public static void chooseAndSaveFile(String content, String suggestedPath, Object o) {
        chooseAndSaveFile(content, suggestedPath,null);
    }
    /**
     * @author Roy
     * #ROY - add new Functionality (set the path to human readable content)
     * */
    public static void chooseAndSaveFile(String content,String suggestedPath,String ext) {
        FileWriter fw=null;
        try{
            fw=new FileWriter(getOpenedFile(suggestedPath));
            fw.write(content);
            javax.swing.JOptionPane.showMessageDialog(null,"File Saved Successfully.");
        }catch(IOException ioe) {ioe.printStackTrace();}

        // handle leakage and canceling
        try { if(fw!=null) fw.close(); }
        catch(IOException ioe1) { ioe1.printStackTrace(); }
    }

    private void tabbedPaneChange() {
    	
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (tabbedPane.getSelectedIndex() == 0) {
                	
                	if (ODMEEditor.toolMode == "ses") {
                		sesview.setTitle("Ontology");
                		fileConversion.createSES(fileLocation + "/" + projName + "/ses.xsd");
                		XmlUtils.showViewer(fileLocation, projName, "ses.xsd", XmlUtils.ontologyview);
                	}
                	else {
                		sesview.setTitle("XML");
                        saveChanges();
                        
                    	fileConversion.createSES(fileLocation + "/" + currentScenario + "/ses.xsd");
                        
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
                        sesValidationControl = 1;
                        //TypeInfoWriter.validateXML();
//                        if (errorPresentInSES == 1) {
//                            sesview.textArea
//                                    .setText("Error presents in the SES. Check console output for details.");
//                            errorPresentInSES = 0;
//                        } 
//                        else {
                            XmlUtils.showViewer(fileLocation, projName, "xmlforxsd.xml", XmlUtils.sesview);
//                        }
                	}
                		
//                		fileConversion.createSES(fileLocation + "/" + currentScenario + "/ses.xsd");
//                    
//                    XmlUtils.showViewer(fileLocation, projName, "ses.xsd", XmlUtils.ontologyview);
                } 
                else if (tabbedPane.getSelectedIndex() == 1) {
                	XmlUtils.schemaview.setTitle("Schema"); // SES Ontology / Schema Viewer
                    saveChanges();
                    fileConversion.modifyXmlOutputForXSD();
                    JtreeToGraphConvert.rootToEndNodeSequenceSolve();
                    JtreeToGraphConvert
                            .rootToEndNodeVariable(); // have to try using saving keys in a list like i did in
                    JtreeToGraphModify.modifyXmlOutputFixForSameNameNode();
                    fileConversion.xmlToXSDConversion();
                    fileConversion.placeAssertInRightPosition();
                    XmlUtils.showViewer(fileLocation, projName, "xsdfromxml.xsd", XmlUtils.schemaview);
                } 
                else if (tabbedPane.getSelectedIndex() == 2) {
                    
                }
            }
        });
    }
    
    public static void removeTopLeftIcon(JInternalFrame internalFrame){
    	// this is for removing the top-left icon of the internal frame
        BasicInternalFrameUI ui = (BasicInternalFrameUI) internalFrame.getUI();
        Container north = ui.getNorthPane();
        north.remove(0);
        north.validate();
        north.repaint();
    }
}

