package odme.odmeeditor;

import static odme.jtreetograph.JtreeToGraphVariables.nodeNumber;
import static odme.jtreetograph.JtreeToGraphVariables.undoManager;
import static odme.odmeeditor.XmlUtils.sesview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;
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

		JButton deleteSelectedBtn = new JButton("Delete Selected");
		deleteSelectedBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelectedScenarios();
			}
		});

		JButton deleteGeneratedBtn = new JButton("Delete Generated");
		deleteGeneratedBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteGeneratedScenarios();
			}
		});

		JButton refreshBtn = new JButton("Refresh Scenario List");
		refreshBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reloadTableData();
			}
		});

		toolBar.add(automaticScenarioGenerationBtn);
		toolBar.addSeparator();
		toolBar.add(deleteSelectedBtn);
		toolBar.addSeparator();
		toolBar.add(deleteGeneratedBtn);
		toolBar.addSeparator();
		toolBar.add(refreshBtn);
		toolBar.addSeparator();
		toolBar.add(structuralCoverageBtn);
    	
    	model = new DefaultTableModel(new String[]{"Name", "Risk", "Remarks"}, 0);

        table = new JTable(model);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        table.setAutoCreateRowSorter(true);
        reloadTableData();
        
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem deleteSelectedItem = new JMenuItem("Delete Selected");
        JMenuItem deleteGeneratedItem = new JMenuItem("Delete Generated");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	openSelectedScenario();
            }
        });
        
        popupMenu.add(openItem);
        popupMenu.add(deleteSelectedItem);
        popupMenu.add(deleteGeneratedItem);
        popupMenu.add(refreshItem);
        
        deleteSelectedItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	deleteSelectedScenarios();
            }
        });

        deleteGeneratedItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteGeneratedScenarios();
            }
        });

        refreshItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadTableData();
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

        AutomaticGenerationOptions options = showAutomaticScenarioGenerationDialog(preview);
        if (options == null) {
            return;
        }

        BackgroundTaskRunner.run(
                this,
                "Automatic Scenario Generation",
                "Generating pruned scenario models with constrained Latin Hypercube sampling...",
                () -> AutomaticScenarioGeneration.generateAll(options.prefix(), options.samplesPerCombination()),
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

    private AutomaticGenerationOptions showAutomaticScenarioGenerationDialog(
            AutomaticScenarioGeneration.GenerationPreview preview) {
        JTextField prefixField = new JTextField("AutoScenario");
        JSpinner samplesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        JLabel totalLabel = new JLabel();
        JLabel limitLabel = new JLabel(" ");

        Runnable refreshSummary = () -> {
            int samples = ((Number) samplesSpinner.getValue()).intValue();
            totalLabel.setText("Total scenario models to create: " + preview.projectedScenarioModels(samples));
            if (preview.exceedsScenarioLimit(samples)) {
                limitLabel.setText("This exceeds the current limit of "
                        + AutomaticScenarioGeneration.maxGeneratedScenarioModels() + " scenario models.");
            }
            else {
                limitLabel.setText(" ");
            }
        };
        ChangeListener changeListener = e -> refreshSummary.run();
        samplesSpinner.addChangeListener(changeListener);
        refreshSummary.run();

        JPanel summaryPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        summaryPanel.add(new JLabel("Project: " + preview.projectName()));
        summaryPanel.add(new JLabel("Structural combinations after pruning: " + preview.totalCombinations()));
        summaryPanel.add(new JLabel("Specialization nodes: " + preview.specializations().size()));
        summaryPanel.add(totalLabel);
        summaryPanel.add(limitLabel);

        JPanel inputPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        inputPanel.add(new JLabel("Scenario name prefix"));
        inputPanel.add(prefixField);
        inputPanel.add(new JLabel("Variable samples per pruned combination"));
        inputPanel.add(samplesSpinner);

        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.add(summaryPanel, BorderLayout.NORTH);
        container.add(inputPanel, BorderLayout.CENTER);

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                    frame != null ? frame : Main.frame,
                    container,
                    "Automatic Scenario Generation",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }

            int samples = ((Number) samplesSpinner.getValue()).intValue();
            if (!preview.exceedsScenarioLimit(samples)) {
                return new AutomaticGenerationOptions(prefixField.getText(), samples);
            }

            JOptionPane.showMessageDialog(
                    frame != null ? frame : Main.frame,
                    "Requested generation exceeds the current limit of "
                            + AutomaticScenarioGeneration.maxGeneratedScenarioModels()
                            + " scenario models.\nReduce the samples per combination and try again.",
                    "Automatic Scenario Generation",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private String buildSuccessMessage(AutomaticScenarioGeneration.GenerationResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Created ").append(result.createdCount())
                .append(" scenario model(s) for ").append(result.projectName()).append('.');
        message.append('\n').append('\n')
                .append("Structural combinations: ").append(result.structuralCombinationCount()).append('\n')
                .append("Variable samples per combination: ").append(result.samplesPerCombination());

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

    private record AutomaticGenerationOptions(String prefix, int samplesPerCombination) {
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

    private List<Integer> getSelectedModelRows() {
        List<Integer> rows = new ArrayList<>();
        if (table == null) {
            return rows;
        }

        int[] selectedViewRows = table.getSelectedRows();
        for (int selectedViewRow : selectedViewRows) {
            if (selectedViewRow >= 0) {
                rows.add(table.convertRowIndexToModel(selectedViewRow));
            }
        }
        return rows;
    }

    private List<String> getSelectedScenarioNames() {
        List<String> scenarioNames = new ArrayList<>();
        for (int row : getSelectedModelRows()) {
            if (row >= 0 && row < model.getRowCount()) {
                scenarioNames.add((String) model.getValueAt(row, 0));
            }
        }
        return scenarioNames;
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

    private void openSelectedScenario() {
        int row = getSelectedModelRow();
        if (row < 0) {
            return;
        }

        String scenarioName = (String) model.getValueAt(row, 0);
        String projectName = EditorContext.getInstance().getProjName();
        File scenarioDirectory = new File(EditorContext.getInstance().getProjectDir(), scenarioName);
        File scenarioTree = new File(scenarioDirectory, projectName + ".xml");
        File scenarioGraph = new File(scenarioDirectory, projectName + "Graph.xml");

        if (!scenarioTree.exists() || !scenarioGraph.exists()) {
            JOptionPane.showMessageDialog(
                    frame != null ? frame : Main.frame,
                    "Scenario files are missing for " + scenarioName + ".\n"
                            + "Expected:\n"
                            + scenarioTree.getAbsolutePath() + "\n"
                            + scenarioGraph.getAbsolutePath(),
                    "Open Scenario",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            DynamicTree.varMap = ArrayListMultimap.create();
            System.out.println("Selected file: " + scenarioName);

            EditorContext.getInstance().setCurrentScenario(scenarioName);
            EditorContext.getInstance().setNewFileName(projectName);

            nodeNumber = 1;
            JtreeToGraphGeneral.openExistingProject(projectName, projectName);

            undoManager = new mxUndoManager();
            sesview.textArea.setText("");
            Console.consoleText.setText(">>");
            Variable.setNullToAllRows();
            Constraint.setNullToAllRows();
            Behaviour.setNullToAllRows();

            ODMEEditor.graphWindow.setTitle(scenarioName);
            ODMEEditor.changePruneColor();
            ODMEEditor.updateState();
            XmlUtils.showViewer(
                    EditorContext.getInstance().getFileLocation(),
                    EditorContext.getInstance().getProjName(),
                    "xmlforxsd.xml",
                    XmlUtils.sesview
            );
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    frame != null ? frame : Main.frame,
                    "Unable to open scenario " + scenarioName + ".\n" + ex.getMessage(),
                    "Open Scenario",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        boolean success = true;
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    success &= deleteFolder(f);
                } else {
                    success &= f.delete();
                }
            }
        }
        return success & (!folder.exists() || folder.delete());
    }

    private void deleteSelectedScenarios() {
        List<String> selectedScenarios = getSelectedScenarioNames();
        if (selectedScenarios.isEmpty()) {
            JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                    "Select one or more scenarios to delete.",
                    "Delete Selected Scenarios",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        deleteScenarios(selectedScenarios, "Delete Selected Scenarios");
    }

    private void deleteGeneratedScenarios() {
        List<String> generatedScenarios = new ArrayList<>();
        for (String[] scenario : getJsonData()) {
            if (isGeneratedScenario(scenario[0])) {
                generatedScenarios.add(scenario[0]);
            }
        }

        if (generatedScenarios.isEmpty()) {
            JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                    "No generated AutoScenario_* scenarios were found.",
                    "Delete Generated Scenarios",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        deleteScenarios(generatedScenarios, "Delete Generated Scenarios");
    }

    private void deleteScenarios(List<String> requestedScenarioNames, String title) {
        Set<String> uniqueRequestedNames = new LinkedHashSet<>(requestedScenarioNames);
        List<String> deletableScenarios = new ArrayList<>();
        List<String> skippedScenarios = new ArrayList<>();

        for (String scenarioName : uniqueRequestedNames) {
            if (scenarioName == null || scenarioName.isBlank()) {
                continue;
            }
            if (isProtectedScenario(scenarioName)) {
                skippedScenarios.add(scenarioName);
                continue;
            }
            deletableScenarios.add(scenarioName);
        }

        if (deletableScenarios.isEmpty()) {
            JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                    buildSkippedMessage(skippedScenarios, "Nothing was deleted."),
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int dialogResult = JOptionPane.showConfirmDialog(
                frame != null ? frame : Main.frame,
                buildDeleteConfirmationMessage(deletableScenarios, skippedScenarios),
                title,
                JOptionPane.YES_NO_OPTION);
        if (dialogResult != JOptionPane.YES_OPTION) {
            return;
        }

        List<String> deletedScenarios = new ArrayList<>();
        List<String> failedScenarios = new ArrayList<>();
        for (String scenarioName : deletableScenarios) {
            File scenarioFolder = new File(EditorContext.getInstance().getProjectDir(), scenarioName);
            if (!scenarioFolder.exists() || deleteFolder(scenarioFolder)) {
                deletedScenarios.add(scenarioName);
            } else {
                failedScenarios.add(scenarioName);
            }
        }

        if (!deletedScenarios.isEmpty()) {
            deleteFromJson(new LinkedHashSet<>(deletedScenarios));
        } else {
            reloadTableData();
        }

        JOptionPane.showMessageDialog(frame != null ? frame : Main.frame,
                buildDeleteResultMessage(deletedScenarios, skippedScenarios, failedScenarios),
                title,
                failedScenarios.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private boolean isProtectedScenario(String scenarioName) {
        return "InitScenario".equals(scenarioName)
                || scenarioName.equals(EditorContext.getInstance().getCurrentScenario());
    }

    private boolean isGeneratedScenario(String scenarioName) {
        return scenarioName != null && scenarioName.startsWith("AutoScenario_");
    }

    private String buildDeleteConfirmationMessage(List<String> deletableScenarios, List<String> skippedScenarios) {
        StringBuilder message = new StringBuilder();
        message.append("Delete ").append(deletableScenarios.size()).append(" scenario(s)?");
        message.append("\n\n");
        appendScenarioPreview(message, "Will delete", deletableScenarios);
        if (!skippedScenarios.isEmpty()) {
            message.append("\n\n");
            appendScenarioPreview(message, "Will skip", skippedScenarios);
        }
        return message.toString();
    }

    private String buildDeleteResultMessage(List<String> deletedScenarios,
                                            List<String> skippedScenarios,
                                            List<String> failedScenarios) {
        StringBuilder message = new StringBuilder();
        message.append("Deleted ").append(deletedScenarios.size()).append(" scenario(s).");

        if (!skippedScenarios.isEmpty()) {
            message.append("\n\n");
            appendScenarioPreview(message, "Skipped", skippedScenarios);
        }
        if (!failedScenarios.isEmpty()) {
            message.append("\n\n");
            appendScenarioPreview(message, "Failed", failedScenarios);
        }

        return message.toString();
    }

    private String buildSkippedMessage(List<String> skippedScenarios, String prefix) {
        StringBuilder message = new StringBuilder(prefix);
        if (!skippedScenarios.isEmpty()) {
            message.append("\n\n");
            appendScenarioPreview(message, "Skipped", skippedScenarios);
        }
        return message.toString();
    }

    private void appendScenarioPreview(StringBuilder target, String label, List<String> scenarios) {
        target.append(label).append(":");
        int previewCount = Math.min(scenarios.size(), 10);
        for (int i = 0; i < previewCount; i++) {
            target.append("\n- ").append(scenarios.get(i));
        }
        if (scenarios.size() > previewCount) {
            target.append("\n- ... and ").append(scenarios.size() - previewCount).append(" more");
        }
    }
    
    @SuppressWarnings("unchecked")
	private void deleteFromJson(Set<String> scenariosToDelete) {
    	List<String[]> dataList = getJsonData();
		
		JSONArray ja = new JSONArray();
		for (String[] arr: dataList) {
			JSONObject jo = new JSONObject();
			if (scenariosToDelete.contains(arr[0])) {
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
