package odme.odmeeditor;

import static odme.jtreetograph.JtreeToGraphVariables.nodeNumber;
import static odme.jtreetograph.JtreeToGraphVariables.undoManager;
import static odme.odmeeditor.MenuBar.getScenarioJsonData;
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

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;

import odeme.behaviour.Behaviour;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.svg.ParseException;

import odme.jtreetograph.JtreeToGraphGeneral;
import structuretest.*;


public class ScenarioList extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTable table;
	private DefaultTableModel model;

    public void createScenarioListWindow() {

		List<String[]> dataList = getJsonData();

		model = new DefaultTableModel(new String[]{"Name", "Risk", "Remarks"}, 0);
		for (String[] arr: dataList)
			model.addRow(arr);

        table = new JTable(model);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.setAutoCreateRowSorter(true);
        
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem deleteItem = new JMenuItem("Delete");
        
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				try {

					DynamicTree.varMap = ArrayListMultimap.create();

					int row = table.getSelectedRow();
					String fileName = (String) table.getModel().getValueAt(row, 0);

					System.out.println("Selected file: " + fileName);
					ODMEEditor.currentScenario = fileName;

					nodeNumber = 1;
					JtreeToGraphGeneral.openExistingProject(ODMEEditor.projName, ODMEEditor.projName);

					undoManager = new mxUndoManager();

					sesview.textArea.setText("");
					Console.consoleText.setText(">>");
					Variable.setNullToAllRows();
					Constraint.setNullToAllRows();
					Behaviour.setNullToAllRows();

					ODMEEditor.graphWindow.setTitle(fileName);
					ODMEEditor.changePruneColor();
				} catch (Exception ex){
				}
            }
        });
        
        popupMenu.add(openItem);
        popupMenu.add(deleteItem);
        
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				String fileName = (String) table.getModel().getValueAt(row, 0);
				if (fileName.equals(ODMEEditor.currentScenario)) {
					JOptionPane.showMessageDialog(Main.frame, "The Scenario is currently opened!", "Error",
                            JOptionPane.ERROR_MESSAGE);
					return;
				}

				int dialogResult = -1;
				dialogResult = JOptionPane.showConfirmDialog (null,
						"Do you want to delete "+fileName+"?","Delete Scenario",JOptionPane.YES_NO_OPTION);
				if(dialogResult == JOptionPane.YES_OPTION){
					deleteFolder(new File(ODMEEditor.fileLocation + "/" +  fileName));
					deleteFromJson(fileName);
				}
            }
        });
         
        table.setComponentPopupMenu(popupMenu);
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    int row = table.getSelectedRow();

                    String name = (String) target.getModel().getValueAt(row, 0);

					// Check if the "Structural Coverage" row is clicked with a double-click
					if (name.equals("Structural Coverage")) {
						// Open Structural Coverage Options
						performStructuralCoverage();

					} else {
						String risk = (String) target.getModel().getValueAt(row, 1);
						String remarks = (String) target.getModel().getValueAt(row, 2);

						updateTableData(name, risk, remarks);
					}
                }
            }
        });

        JFrame frame = new JFrame("Senario List");
        JPanel panelCenter = new JPanel();
                
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(480, 200));
                
        panelCenter.add(scroll);
                
        panelCenter.setBorder(new EtchedBorder());
                
        int width = 500;
        int height = 250;
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

	private void performStructuralCoverage(){

		List<String[]> dataList = getScenarioJsonData();

		String path = ODMEEditor.fileLocation  + "/graphxml.xml";

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

		Test t = new Test(dataList);
		System.out.println("Total buckets = " + t.getCoverageSummary());
		t.print();


		// Calculating percentages
		double specialisationPercentage = (specialisationNodeTest.getTotalSpecialisationNode() > 0)
				? (specialisationNodeTest.getMatchedSpecialisationNode() * 100.0 / specialisationNodeTest.getTotalSpecialisationNode())
				: 0.0;

		double behaviourPercentage = (behaviourCoverageTest.getTotalBehaviours() > 0)
				? (behaviourCoverageTest.getMatchedBehaviours() * 100.0 / behaviourCoverageTest.getTotalBehaviours())
				: 0.0;

		double variablePercentage = (t.getTotalBuckets() > 0)
				? (t.getTotalCoveredBuckets() * 100.0 / t.getTotalBuckets())
				: 0.0;


		// Creating the 2D array
		Object[][] data = {
				{"Specialisation Node", specialisationNodeTest.getTotalSpecialisationNode() ,
						specialisationNodeTest.getMatchedSpecialisationNode() ,
						specialisationNodeTest.getTotalSpecialisationNode()+specialisationNodeTest.getMatchedSpecialisationNode(),
						specialisationPercentage},
				{"MultiAspect Node" , multiAspectNodeTest.getTotalCoveredChildren(), multiAspectNodeTest.getTotalUncoveredChildren(),
						multiAspectNodeTest.getTotalUncoveredChildren()+ multiAspectNodeTest.getTotalCoveredChildren(),
						multiAspectNodeTest.getTotalPercentage()
				},
				{"Behaviours", behaviourCoverageTest.getTotalBehaviours(),
						behaviourCoverageTest.getMatchedBehaviours(),
						behaviourCoverageTest.getTotalBehaviours()+behaviourCoverageTest.getMatchedBehaviours(),
						behaviourPercentage},
				{
						"Variables" , t.getTotalCoveredBuckets(),t.getTotalUnCoveredBuckets(), t.getTotalBuckets(),
						variablePercentage
				}
		};

		CodeCoverageLayout layout = new CodeCoverageLayout(Main.frame , data);
		layout.setVisible(true);
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
			} else {
				jo.put("name", arr[0]);
				jo.put("risk", arr[1]);
				jo.put("remarks", arr[2]);

				JSONObject jom = new JSONObject();
				jom.put("scenario", jo);
				ja.add(jom);
			}
		}

		try {
			FileWriter file = new FileWriter(ODMEEditor.fileLocation  + "/scenarios.json");
			file.write(ja.toJSONString());
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		DefaultTableModel dm = (DefaultTableModel)table.getModel();
		while(dm.getRowCount() > 0) {
			dm.removeRow(0);
		}

		List<String[]> newDataList = getJsonData();

		for (String[] arr: newDataList)
			model.addRow(arr);
    }
    
    private List<String[]> getJsonData() {
		JSONParser jsonParser = new JSONParser();
		List<String[]> dataList = new ArrayList<String[]>();

		try (FileReader reader = new FileReader(ODMEEditor.fileLocation + "/scenarios.json")){

            Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}

			JSONArray data = (JSONArray) obj;

            for (Object dtObj:data) {
				dataList.add(parseObject((JSONObject)dtObj));
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
		// Add the Structural Coverage row at the end
		dataList.add(new String[] { "Structural Coverage", "", "" });

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
				} else {
					jo.put("name", arr[0]);
					jo.put("risk", arr[1]);
					jo.put("remarks", arr[2]);

					JSONObject jom = new JSONObject();
					jom.put("scenario", jo);
					ja.add(jom);
				}
			}

			try {
				FileWriter file = new FileWriter(ODMEEditor.fileLocation  + "/scenarios.json");
				file.write(ja.toJSONString());
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			DefaultTableModel dm = (DefaultTableModel)table.getModel();
			while(dm.getRowCount() > 0) {
				dm.removeRow(0);
			}

			List<String[]> newDataList = getJsonData();

			for (String[] arr: newDataList)
				model.addRow(arr);
		}
	}
}
