package odme.odmeeditor;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.svg.ParseException;

import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphModify;
import odme.jtreetograph.JtreeToGraphSave;
import odme.jtreetograph.JtreeToGraphVariables;

import static odme.odmeeditor.XmlUtils.sesview;

public class MenuBar {
	
	private JMenuBar menuBar;
	public static List<JMenuItem> fileMenuItems= new ArrayList<>();
	
	public MenuBar(JFrame frame) {
		menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
    }
	
	public void show() {
		// File Menu
		final String[] items_file =  {"Save"       , "Save As"    , "Save as PNG" , null, "Exit"       };
		final int[] keyevents_file = {KeyEvent.VK_S, KeyEvent.VK_A, 0             , 0   , KeyEvent.VK_X};
		final String[] keys_file =   {"control S"  ,"control A"   , null          , null, "control X"  };
		final String[] images_file = {"save_icon"  , "save_icon"  , "png_icon"    , null, "exit_icon"  };
				
		addMenu("File", KeyEvent.VK_F, items_file, keyevents_file, keys_file, images_file);

		// Domain Modelling Menu
		final String[] items_domain_modelling =  {"New Project", "Open"       , "Import Template", "Save as Template"};
		final int[] keyevents_domain_modelling = {KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_I    , KeyEvent.VK_E     };
		final String[] keys_domain_modelling =   {"control N"  , "control O"  , "control I"      , "control E"       };
		final String[] images_domain_modelling = {"new_icon"   , "open_icon"  , "import_icon"    , "export_icon"     };
				
		addMenu("Domain Modelling", 0, items_domain_modelling, keyevents_domain_modelling, keys_domain_modelling, images_domain_modelling);
		
		// Scenario Modelling Menu
		final String[] items_scenario_modelling =  {"Save Scenario"};
		final int[] keyevents_scenario_modelling = {0};
		final String[] keys_scenario_modelling =   {null};
		final String[] images_scenario_modelling = {"save_scenario"};
						
		addMenu("Scenario Modelling", 0, items_scenario_modelling, keyevents_scenario_modelling, keys_scenario_modelling, images_scenario_modelling);

		//Behavior Modelling
		String[] items_behaviour_modelling = {"Sync Behaviour"};
		final int[] keyevents_behaviour_modelling = {KeyEvent.VK_B};
		final String[] keys_behaviour_modelling =   {"control B"};
		final String[] images_behaviour_modelling = {"sync"};

		addMenu("Behavior Modelling " , 0 ,items_behaviour_modelling,
				keyevents_behaviour_modelling,
				keys_behaviour_modelling,images_behaviour_modelling);

		// #ROY - adding a new ICON
		// Operation Design Domain Menu
		final String[] items_operation_design_domain =  {"Generate OD","ODD Manager"};
		final int[] keyevents_operation_design_domain = {0,0};
		final String[] keys_operation_design_domain =   {null,null};
		final String[] images_operation_design_domain = {"export_icon","list"};
								
		addMenu("Operation Design Domain", 0, items_operation_design_domain, keyevents_operation_design_domain, keys_operation_design_domain, images_operation_design_domain);
		
		// Scenario Manager Menu
		final String[] items_scenario_manager =  {"Scenarios List", "Excution", "Feedback Loop"};
		final int[] keyevents_scenario_manager = {0               , 0         ,  0             };
		final String[] keys_scenario_manager =   {null            , null      ,  null          };
		final String[] images_scenario_manager = {"list"          , null      ,  null          };
										
		addMenu("Scenario Manager", 0, items_scenario_manager, keyevents_scenario_manager, keys_scenario_manager, images_scenario_manager);
		
		// Help Menu
		final String[] items_help =  {"Manual"     , "About"   };
		final int[] keyevents_help = {KeyEvent.VK_M, KeyEvent.VK_B};
		final String[] keys_help =   {"control M"  , "control B"  };
		final String[] images_help = {"manual_icon", "about_icon"  };
						
		addMenu("Help", KeyEvent.VK_H, items_help, keyevents_help, keys_help, images_help);


	}

    private void addMenu(String name, int key_event, String[] items, int[] keyevents, String[] keys, String[] images) {
    	
    	JMenu menu = new JMenu(name);
    	menu.setMnemonic(key_event);
    	menu.setBorder( new EmptyBorder(10,20,10,20));
    	
    	final int itemLength=40, itemWidth=200; 
		
		for(int i=0; i<items.length; i++) {
			if (items[i]==null) {
				menu.addSeparator();
				continue;
			}
			
			JMenuItem menuItem;
			
			menuItem = new JMenuItem(items[i], keyevents[i]);
		    KeyStroke ctrlSKeyStrokeNew = KeyStroke.getKeyStroke(keys[i]);
		    menuItem.setAccelerator(ctrlSKeyStrokeNew);
		    menuItem.setPreferredSize(new Dimension(itemWidth, itemLength));
		    if (images[i] != null) {
		    	ImageIcon newIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/"+images[i]+".png"));
		    	menuItem.setIcon(newIcon);
		    }
		    
		    if (items[i]=="Save Scenario" || items[i]=="Scenarios List" || items[i]=="Excution" || items[i]=="Feedback Loop")
		    	menuItem.setEnabled(false);
		    
		    if (items[i]=="New Project" || items[i]=="Import Template" || items[i]=="Save Scenario" ||
		    		items[i]=="Open" || items[i]=="Save as Template" || items[i]=="Scenarios List" ||
		    		items[i]=="Excution" || items[i]=="Feedback Loop" || items[i]=="Export XML" ||
		    		items[i]=="Export Yaml") {
		    	fileMenuItems.add(menuItem);
		    }
		    
		    menuItem.addActionListener(new ActionListener() {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		            switch (e.getActionCommand()) {
		            	case "Save Scenario":
		            		saveScenario();
		            		break;
		            	case "Scenarios List":
		            		ScenarioList scenarioList = new ScenarioList();
		                	scenarioList.createScenarioListWindow();
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
						case "Save as Template":
		            	  	exportFunc();
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
		    });
		    menu.add(menuItem);
		}
    	menuBar.add(menu);
    }
    
    /**
     * @author Roy
     * #ROY - adding new Functionality to see all the nodes 
     * */
    private void openODDManager(String mode) {
    	ODMEEditor.saveFunc(false); // save the results
    	ODMEEditor.updateState();
    	ODDManager nt=new ODDManager(mode);
    	JFrame jd = new JFrame();
    	jd.getContentPane().add(nt);
    	jd.pack();
    	jd.setVisible(true);
    	// jd.setLocation(128, 128);
    	jd.setLocationRelativeTo(null);
    	// jd.setAlwaysOnTop(true);
    }

   
	
    
    @SuppressWarnings("unchecked")
	private void saveScenario() {
    	JSONParser jsonParser = new JSONParser();
        
        try (FileReader reader = new FileReader(ODMEEditor.fileLocation + "/scenarios.json")){
            Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} 
			catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
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
        	}
        	
        	else
        		return;
        	
        	try {
		         FileWriter file = new FileWriter(ODMEEditor.fileLocation + "/scenarios.json");
		         file.write(data.toJSONString());
		         file.close();
		         ODMEEditor.graphWindow.setTitle(nameField.getText());
		      }
        	catch (IOException e) {
		         e.printStackTrace();
		      }

        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        catch (ParseException e) {
            e.printStackTrace();
        } 
    }
    
    private void createScenario(String ScenarioName) {
        ODMEEditor.currentScenario = ScenarioName;
        
        JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
    			 ODMEEditor.fileLocation, ScenarioName, ODMEEditor.projName));
    	 ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
    			 ODMEEditor.fileLocation,  ScenarioName, ODMEEditor.projName));
    	 ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
    			 ODMEEditor.fileLocation,  ScenarioName, ODMEEditor.projName));
    	 ODMEEditor.treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
    			 ODMEEditor.fileLocation,  ScenarioName, ODMEEditor.projName));
    	 ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
    			 ODMEEditor.fileLocation,  ScenarioName, ODMEEditor.projName));

		ODMEEditor.treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
				ODMEEditor.fileLocation,  ScenarioName, ODMEEditor.projName));

        File f = new File(ODMEEditor.fileLocation + "/" +  ScenarioName);
        f.mkdirs();
        
        ODMEEditor.updateState();
        ODMEEditor.changePruneColor();
    }
    
    private void newFunc() {
    	DynamicTree.varMap = ArrayListMultimap.create();
    	NewProject newProject = new NewProject();
	  	newProject.createNewProjectWindow();
	  	// resetting undoManager so that it will remove its indexes from previous projects
	  	JtreeToGraphVariables.undoManager = new mxUndoManager();
    }
    
    private void openFunc() {
    	// filechooser
    	//DynamicTree.varMap = ArrayListMultimap.create();
    	//JtreeToGraphVariables.variableList = new String[100];
    	
    	
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(
                new File(ODMEEditor.repFslas)); // this is ok because normally all the file will be
        // in default location. so don't need to add fileLocation

        int result = fileChooser.showOpenDialog(Main.frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            System.out.println("Selected file: " + selectedFile.getName());

            String oldProjectTreeProjectName = ODMEEditor.projName;
            ODMEEditor.projName = fileName;
            ODMEEditor.fileLocation = selectedFile.getParentFile().getAbsolutePath();
            JtreeToGraphGeneral.openExistingProject(fileName, oldProjectTreeProjectName);

            JtreeToGraphVariables.undoManager = new mxUndoManager();
            sesview.textArea.setText("");
            Console.consoleText.setText(">>");
            Variable.setNullToAllRows();
            Constraint.setNullToAllRows();
            
            if (ODMEEditor.toolMode == "pes")
            	ODMEEditor.applyGuiSES();
        }
    }
    
    private void saveAsFunc() {
    	JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            ODMEEditor.fileLocation = selectedFile.getParentFile().getAbsolutePath();

            String newProjectName = selectedFile.getName();
            String oldProjectTreeProjectName = ODMEEditor.projName;

            ODMEEditor.projName = newProjectName;
            JtreeToGraphVariables.newFileName = newProjectName;
            JtreeToGraphVariables.projectFileNameGraph = newProjectName;

            JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
            		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
            ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
            		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
            ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
            		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
            ODMEEditor.treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
            		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
            ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
            		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));

            ProjectTree.projectName = newProjectName;
            ODMEEditor.projectPanel.changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);

            ODMEEditor.newProjectFolderCreation();
            ODMEEditor.treePanel.saveTreeModel();
            JtreeToGraphSave.saveGraph();

            // also it will convert after saving from here
            // this code is also present in convert to xml button click action.
            JtreeToGraphConvert.convertTreeToXML(); // this function is using for converting project tree into xml file
            JtreeToGraphConvert.graphToXML();
            JtreeToGraphConvert.graphToXMLWithUniformity();
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
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

        // saving drawn graph as a png
    	try{
    		BufferedImage image = mxCellRenderer.createBufferedImage(JtreeToGraphVariables.graph, null, 1, Color.WHITE, true, null);
    		String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName  + "/graph.png";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/graph.png";
        		
            ImageIO.write(image, "PNG", new File(path));
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save PNG",
                    JOptionPane.INFORMATION_MESSAGE);
        } 
    	catch (Exception e) {
            System.out.println("Error:" + e);
            JOptionPane.showMessageDialog(Main.frame, "Error:" + e, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    	
    	// add border nodes again
    	JtreeToGraphVariables.graph.getModel().beginUpdate();
    	JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
    	JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
    	JtreeToGraphVariables.graph.getModel().endUpdate();	
    }

    private void importFunc() {
    	ImportProject impProj = new ImportProject();
        impProj.importProject();
    }

    private void exportFunc() {
    	ToolBar.validation();
        String fileName = ODMEEditor.projName; // don't know why not fetching the file name here
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);
        fileChooser.setSelectedFile(new File(fileName)); // not working because filename is null
        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Exported file path: " + selectedFile.getAbsolutePath());
            PrintWriter f0 = null;
            try {
                f0 = new PrintWriter(new FileWriter(
                        selectedFile.getAbsolutePath() + ".xml"));
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            Scanner in = null;
            try {
                in = new Scanner(new File(
                        ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xmlforxsd.xml"));
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
            }
            while (in.hasNext()) { // Iterates each line in the file
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
				}
                // Open the PDF
                Desktop.getDesktop().open(pdfTemp);
            }
            catch (IOException e1) {
                System.out.println("erreur : " + e1);
            }
        }
    }
}
