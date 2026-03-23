package odme.odmeeditor;

import static odme.jtreetograph.JtreeToGraphVariables.nodeNumber;
import static odme.jtreetograph.JtreeToGraphVariables.undoManager;
import static odme.odmeeditor.XmlUtils.sesview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;

import odme.behaviour.Behaviour;
import odme.core.EditorContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxUndoManager;

import odme.jtreetograph.JtreeToGraphGeneral;
import structuretest.BehaviourCoverageTest;
import structuretest.MultiAspectNodeTest;
import structuretest.ParamterCoverage;
import structuretest.SpecialisationNodeTest;


public class ScenarioList extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTable table;
	private DefaultTableModel model;
    private JFrame frame;

    public void createScenarioListWindow() {

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		JButton automaticScenarioGenerationBtn = new JButton("Automatic Scenario Generation");
		automaticScenarioGenerationBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performAutomaticScenarioGeneration();
			}
		});

		JButton structuralCoverageBtn = new JButton("Structural Coverage");
		structuralCoverageBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performStructuralCoverage();
			}
		});

		toolBar.add(automaticScenarioGenerationBtn);
		toolBar.addSeparator();
		toolBar.add(structuralCoverageBtn);
    	
    	model = new DefaultTableModel(new String[]{"Name", "Risk", "Remarks"}, 0);

        table = new JTable(model);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.setAutoCreateRowSorter(true);
        reloadTableData();
        
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem deleteItem = new JMenuItem("Delete");
        
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try {
            		int row = getSelectedModelRow();
            		if (row < 0) {
            			return;
            		}
            		DynamicTree.varMap = ArrayListMultimap.create();

            		String fileName = (String) model.getValueAt(row, 0);

            		System.out.println("Selected file: " + fileName);
            		EditorContext.getInstance().setCurrentScenario(fileName);

            		nodeNumber = 1;
            		JtreeToGraphGeneral.openExistingProject(EditorContext.getInstance().getProjName(), EditorContext.getInstance().getProjName());

            		undoManager = new mxUndoManager();

            		sesview.textArea.setText("");
            		Console.consoleText.setText(">>");
            		Variable.setNullToAllRows();
            		Constraint.setNullToAllRows();
            		Behaviour.setNullToAllRows();

            		ODMEEditor.graphWindow.setTitle(fileName);
            		ODMEEditor.changePruneColor();
            	}
            	catch (Exception ex){
            	}
            }
        });
        
        popupMenu.add(openItem);
        popupMenu.add(deleteItem);
        
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	int row = getSelectedModelRow();
            	if (row < 0) {
            		return;
            	}
            	String fileName = (String) model.getValueAt(row, 0);
            	if (fileName.equals(EditorContext.getInstance().getCurrentScenario())) {
            		JOptionPane.showMessageDialog(Main.frame, "The Scenario is currently opened!", "Error",
                            JOptionPane.ERROR_MESSAGE);
            		return;
            	}
            	
            	int dialogResult = -1;
            	dialogResult = JOptionPane.showConfirmDialog (null,
        				"Do you want to delete "+fileName+"?","Delete Scenario",JOptionPane.YES_NO_OPTION);
        		if(dialogResult == JOptionPane.YES_OPTION){
        			deleteFolder(new File(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/" +  fileName));
        			deleteFromJson(fileName);
        		}
            }
        });
         
        table.setComponentPopupMenu(popupMenu);
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = getSelectedModelRow();
                    if (row < 0) {
                    	return;
                    }

                    String name = (String) model.getValueAt(row, 0);
                    String risk = (String) model.getValueAt(row, 1);
                    String remarks = (String) model.getValueAt(row, 2);
 
                    updateTableData(name, risk, remarks);
                }
            }
        });
        
        
//        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
//        table.setRowSorter(sorter);
//
//        List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
//        sortKeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
//        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
//        sorter.setSortKeys(sortKeys);
                
        frame = new JFrame("Scenario List");
        JPanel panelCenter = new JPanel();
                
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(480, 200));
                
        panelCenter.add(scroll);
                
        panelCenter.setBorder(new EtchedBorder());

		frame.add(toolBar, BorderLayout.NORTH);

        int width = 760;
        int height = 280;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;

        frame.pack();
                
        frame.setBounds(x, y, width, height);
        frame.setSize(width, height);
                
        frame.add(panelCenter, BorderLayout.CENTER);

        frame.setResizable(false);
        frame.setVisible(true);
    }

    private void performAutomaticScenarioGeneration() {
        AutomaticScenarioGeneration.GenerationPreview preview;
        try {
            preview = AutomaticScenarioGeneration.inspectProject();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                    "Unable to inspect the current domain model.\n" + ex.getMessage(),
                    "Automatic Scenario Generation",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (preview.multiAspectCount() > 0) {
            JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                    "Automatic Scenario Generation currently supports specialization pruning only.\n"
                            + "Detected " + preview.multiAspectCount() + " multi-aspect node(s) in the domain model.",
                    "Automatic Scenario Generation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int proceed = JOptionPane.showConfirmDialog(
                frame != null ? frame : Main.frame,
                buildPreviewMessage(preview),
                "Automatic Scenario Generation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (proceed != JOptionPane.OK_OPTION) {
            return;
        }

        String requestedPrefix = (String) JOptionPane.showInputDialog(
                frame != null ? frame : Main.frame,
                "Scenario name prefix:",
                "Automatic Scenario Generation",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                "AutoScenario"
        );
        if (requestedPrefix == null) {
            return;
        }

        BackgroundTaskRunner.run(
                this,
                "Automatic Scenario Generation",
                "Generating pruned scenario models...",
                () -> AutomaticScenarioGeneration.generateAll(requestedPrefix),
                result -> {
                    reloadTableData();
                    JOptionPane.showMessageDialog(
                            frame != null ? frame : Main.frame,
                            buildSuccessMessage(result),
                            "Automatic Scenario Generation",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
        );
    }

    private String buildPreviewMessage(AutomaticScenarioGeneration.GenerationPreview preview) {
        StringBuilder message = new StringBuilder();
        message.append("Project: ").append(preview.projectName()).append('\n');
        message.append("Specialization nodes: ").append(preview.specializations().size()).append('\n');
        message.append("Scenario combinations: ").append(preview.totalCombinations()).append('\n');

        if (preview.specializations().isEmpty()) {
            message.append('\n').append("No specialization nodes were found. ODME will create one scenario copy.");
        }
        else {
            message.append('\n').append("Specializations:\n");
            int previewCount = Math.min(preview.specializations().size(), 8);
            for (int i = 0; i < previewCount; i++) {
                AutomaticScenarioGeneration.SpecializationDescriptor descriptor = preview.specializations().get(i);
                message.append("- ").append(descriptor.label())
                        .append(" (").append(descriptor.optionLabels().size()).append(" options)")
                        .append('\n');
            }
            if (preview.specializations().size() > previewCount) {
                message.append("- ...\n");
            }
        }

        message.append('\n')
                .append("Generated scenarios will be stored under the current project folder and added to Scenario Manager.\n")
                .append("Continue?");
        return message.toString();
    }

    private String buildSuccessMessage(AutomaticScenarioGeneration.GenerationResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Created ").append(result.createdCount())
                .append(" scenario model(s) for ").append(result.projectName()).append('.');

        if (!result.createdScenarioNames().isEmpty()) {
            message.append('\n').append('\n').append("First generated scenarios:\n");
            int previewCount = Math.min(result.createdScenarioNames().size(), 10);
            for (int i = 0; i < previewCount; i++) {
                message.append("- ").append(result.createdScenarioNames().get(i)).append('\n');
            }
            if (result.createdScenarioNames().size() > previewCount) {
                message.append("- ...");
            }
        }

        return message.toString();
    }

    private int getSelectedModelRow() {
        if (table == null) {
            return -1;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private void reloadTableData() {
        if (model == null) {
            return;
        }

        model.setRowCount(0);
        List<String[]> dataList = getJsonData();
        for (String[] arr : dataList) {
            model.addRow(arr);
        }
    }
    
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    @SuppressWarnings("unchecked")
	private void deleteFromJson(String scenario) {
    	List<String[]> dataList = getJsonData();
		
		JSONArray ja = new JSONArray();
		for (String[] arr: dataList) {
			JSONObject jo = new JSONObject();
			if (arr[0].equals(scenario)) {
				continue;
			}
			else {
				jo.put("name", arr[0]);
				jo.put("risk", arr[1]);
				jo.put("remarks", arr[2]);
				
				JSONObject jom = new JSONObject();
				jom.put("scenario", jo);
				ja.add(jom);
			}
		}
		
		try {
	         FileWriter file = new FileWriter(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");
	         file.write(ja.toJSONString());
	         file.close();
	      } catch (IOException e) {
	         e.printStackTrace();
             JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
             return;
	      }

		reloadTableData();
    }


	private void performStructuralCoverage(){

		List<String[]> dataList = getScenarioJsonData();

		String path = EditorContext.getInstance().getProjectDir()  + "/graphxml.xml";

		SpecialisationNodeTest specialisationNodeTest = new SpecialisationNodeTest(path);
		Map c = specialisationNodeTest.getSpecialisationNodes();

		specialisationNodeTest.checkMatchedNodes(dataList);

		//Now behaviour test
		BehaviourCoverageTest behaviourCoverageTest = new BehaviourCoverageTest();
		behaviourCoverageTest.checkCodeCoverageForBehaviours(dataList);

		//Now MultiAspect nodes
		MultiAspectNodeTest multiAspectNodeTest  = new MultiAspectNodeTest();
		multiAspectNodeTest.parseNodes(path);

		multiAspectNodeTest.checkCodeCoverageMultiAspect(dataList);

//		Test t = new Test(dataList);
//		Map<String, Integer> map = t.getBucketStatistics();

		ParamterCoverage t = new ParamterCoverage(dataList);
		Map<String, Integer> map = t.getBucketStatistics();

		int totalBuckets = map.get("totalBuckets");
		int totalCoveredBuckets = map.get("totalCoveredBuckets");
		System.out.println("totalBuckets " + totalBuckets);
		System.out.println("totalCoveredBuckets " + totalCoveredBuckets);


		double specialisationPercentage = (specialisationNodeTest.getTotalSpecialisationNode() > 0)
				? (((double) specialisationNodeTest.getMatchedSpecialisationNode()  / specialisationNodeTest.getTotalSpecialisationNode())) * 100
				: 0.0;


		double behaviourPercentage = (behaviourCoverageTest.getTotalBehaviours() > 0)
				? (behaviourCoverageTest.getMatchedBehaviours() * 100.0 / behaviourCoverageTest.getTotalBehaviours())
				: 0.0;

		double parameterPercentage = ((double) totalCoveredBuckets / totalBuckets) * 100;
		System.out.println("parameterPercentage = " + parameterPercentage);
		double variablePercent =  ((double) 408 / 925) * 100;
		double overAllPercentage = (specialisationPercentage + variablePercent)/2;

		// Creating the 2D array
		Object[][] data = {
				{"Structural Coverage ",null, null, null, null},

				{"          Specialisation Coverage",
						specialisationNodeTest.getMatchedSpecialisationNode() ,
						specialisationNodeTest.getTotalSpecialisationNode() - specialisationNodeTest.getMatchedSpecialisationNode() ,
						specialisationNodeTest.getTotalSpecialisationNode(),
						specialisationPercentage
				},

				{"          MultiAspect Coverage" , multiAspectNodeTest.getTotalCoveredChildren(), multiAspectNodeTest.getTotalUncoveredChildren(),
						multiAspectNodeTest.getTotalUncoveredChildren() + multiAspectNodeTest.getTotalCoveredChildren(),
						multiAspectNodeTest.getTotalPercentage()
				},
				{"          Behaviours", behaviourCoverageTest.getMatchedBehaviours(),
						behaviourCoverageTest.getTotalBehaviours() - behaviourCoverageTest.getMatchedBehaviours(),
						behaviourCoverageTest.getTotalBehaviours(),
						behaviourPercentage},
				{
						"Parameter Coverage" ,  totalCoveredBuckets ,
						totalBuckets - totalCoveredBuckets , totalBuckets,
						parameterPercentage
//						((double) totalCoveredBuckets / totalBuckets) * 100
//						variableCoverageTest.getTotalCoveredBuckets(),variableCoverageTest.getTotalUnCoveredBuckets(),
//						variableCoverageTest.getTotalBuckets(),
//						variablePercentage
				},
				{
						"Overall Coverage" , null,
//						specialisationPercentage
//						specialisationNodeTest.getMatchedSpecialisationNode()+
//						multiAspectNodeTest.getTotalCoveredChildren()
//						+variableCoverageTest.getTotalCoveredBuckets(), //unCovered starts
						null,
//						(specialisationNodeTest.getTotalSpecialisationNode() - specialisationNodeTest.getMatchedSpecialisationNode()) +
//								multiAspectNodeTest.getTotalUncoveredChildren()
//								+variableCoverageTest.getTotalUnCoveredBuckets(), // Total starts
						null,
//						specialisationNodeTest.getTotalSpecialisationNode() + multiAspectNodeTest.getMultiAspectNodeCount()
//						+ variableCoverageTest.getTotalBuckets(),
						(specialisationPercentage + multiAspectNodeTest.getTotalPercentage() + behaviourPercentage + parameterPercentage)/ 4
//						overAllPercentage
				}
		};

		CodeCoverageLayout layout = new CodeCoverageLayout(Main.frame , data);
		layout.setVisible(true);
	}

	public List<String[]> getScenarioJsonData() {
		JSONParser jsonParser = new JSONParser();
		List<String[]> dataList = new ArrayList<String[]>();
		System.out.println("fileLocation = " + EditorContext.getInstance().getFileLocation());
		try (FileReader reader = new FileReader(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() +  "/scenarios.json")){
			Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return dataList;
			}

			JSONArray data = (JSONArray) obj;

			for (Object dtObj:data) {
				dataList.add(parseObject((JSONObject)dtObj));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return dataList;
	}


	private List<String[]> getJsonData() {
    	JSONParser jsonParser = new JSONParser();
    	List<String[]> dataList = new ArrayList<String[]>();
    	
        try (FileReader reader = new FileReader(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json")){

            Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} 
			catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return dataList;
			}
			
            JSONArray data = (JSONArray) obj;

            for (Object dtObj:data) {
            	dataList.add(parseObject((JSONObject)dtObj));
            }
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } 
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } 
    	
    	return dataList;
    }
    
    private String[] parseObject(JSONObject obj) {
        JSONObject dataObject = (JSONObject) obj.get("scenario");
        
        String name = (String) dataObject.get("name");   
        String risk = (String) dataObject.get("risk");  
        String remarks = (String) dataObject.get("remarks");
        
        String[] arr = {name, risk, remarks};
        
        return arr;
    }
    
    @SuppressWarnings("unchecked")
	public void updateTableData(String name, String risk, String remarks) {
    	JTextField nameField = new JTextField();
    	JTextField riskField = new JTextField();
    	JTextField remarksField = new JTextField();

    	nameField.setEnabled(false);
    	
    	nameField.setText(name);
    	riskField.setText(risk);
    	remarksField.setText(remarks);


    	Object[] message = {"Scenario Name:", nameField, "Risk:", riskField, "Remarks:", remarksField};

    	int option = JOptionPane
    			.showConfirmDialog(Main.frame, message, "Update Scenario", JOptionPane.OK_CANCEL_OPTION,
    			JOptionPane.PLAIN_MESSAGE);

    	if (option == JOptionPane.OK_OPTION) {
    			name = nameField.getText();
    			risk = riskField.getText();
    			remarks = remarksField.getText();
    			
    			List<String[]> dataList = getJsonData();
    			
    			JSONArray ja = new JSONArray();
    			for (String[] arr: dataList) {
    				JSONObject jo = new JSONObject();
    				if (arr[0].equals(name)) {
    					jo.put("name", name);
    					jo.put("risk", risk);
    					jo.put("remarks", remarks);
    					
    					JSONObject jom = new JSONObject();
    					jom.put("scenario", jo);
    					ja.add(jom);
    				}
    				else {
    					jo.put("name", arr[0]);
    					jo.put("risk", arr[1]);
    					jo.put("remarks", arr[2]);
    					
    					JSONObject jom = new JSONObject();
    					jom.put("scenario", jo);
    					ja.add(jom);
    				}
    			}
    			
    			try {
    		         FileWriter file = new FileWriter(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");
    		         file.write(ja.toJSONString());
    		         file.close();
    		      } catch (IOException e) {
    		         e.printStackTrace();
    		      }

    			reloadTableData();
    	}	
    }
}
