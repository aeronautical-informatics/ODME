package odme.odmeeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import odme.sampling.GenerateSamplesPanel;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MenuBar {
	
	private JMenuBar menuBar;
	public static List<JMenuItem> fileMenuItems= new ArrayList<>();

	private static JFrame mainFrame = null;
	
	public MenuBar(JFrame frame) {
		menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
		mainFrame = frame;
    }
	
	public void show() {
		// File Menu
		final String[] items_file =  {"Save"       , "Save As"    , "Save as PNG" , null, "Exit"       };
		final int[] keyevents_file = {KeyEvent.VK_S, KeyEvent.VK_A, 0             , 0   , KeyEvent.VK_X};
		final String[] keys_file =   {"control S"  ,"control A"   , null          , null, "control X"  };
		final String[] images_file = {"save_icon"  , "save_icon"  , "png_icon"    , null, "exit_icon"  };
				
		addMenu("File", KeyEvent.VK_F, items_file, keyevents_file, keys_file, images_file);

		// Domain Modelling Menu
		final String[] items_domain_modelling =  {"New Project", "Open"       , "Import Template", "Save as Template", "Import From Cameo"};
		final int[] keyevents_domain_modelling = {KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_I    , KeyEvent.VK_E    , KeyEvent.VK_C     };
		final String[] keys_domain_modelling =   {"control N"  , "control O"  , "control I"      , "control E"      , "control C"       };
		final String[] images_domain_modelling = {"new_icon"   , "open_icon"  , "import_icon"    , "export_icon"    , "cimport_icon"     };
				
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
		final String[] items_scenario_manager =  {"Scenarios List", "Execution", "Feedback Loop", "Generate Samples"};
		final int[] keyevents_scenario_manager = {0               , 0         ,  0             ,  0               };
		final String[] keys_scenario_manager =   {null            , null      ,  null          ,  null            };
		final String[] images_scenario_manager = {"list"          ,"executionIcon","feedbackLoopIcon","list"       };

		addMenu("Scenario Manager", 0, items_scenario_manager, keyevents_scenario_manager, keys_scenario_manager, images_scenario_manager);

		// Tools Menu
		final String[] items_tools =  {"Run Python Plugin..."};
		final int[] keyevents_tools = {KeyEvent.VK_P};
		final String[] keys_tools =   {"control shift P"};
		final String[] images_tools = {"executionIcon"};

		addMenu("Tools", 0, items_tools, keyevents_tools, keys_tools, images_tools);
		
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
		menu.setBorder(new EmptyBorder(10, 20, 10, 20));

		final int itemLength = 40, itemWidth = 200;
		final int iconWidth = 20; // Set the width of the icon
		final int iconHeight = 20; // Set the height of the icon

		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				menu.addSeparator();
				continue;
			}

			JMenuItem menuItem = new JMenuItem(items[i], keyevents[i]);
			KeyStroke ctrlSKeyStroke = KeyStroke.getKeyStroke(keys[i]);
			menuItem.setAccelerator(ctrlSKeyStroke);
			menuItem.setPreferredSize(new Dimension(itemWidth, itemLength));

			if (images[i] != null) {
				URL imageUrl = ODMEEditor.class.getResource("/images/" + images[i] + ".png");
				if (imageUrl != null) {
					ImageIcon originalIcon = new ImageIcon(imageUrl);
					Image image = originalIcon.getImage();
					Image newimg = image.getScaledInstance(iconWidth, iconHeight, java.awt.Image.SCALE_SMOOTH);
					ImageIcon newIcon = new ImageIcon(newimg);
					menuItem.setIcon(newIcon);
				} else {
					System.out.println("Image not found: " + images[i]);
				}
			}

			if ("Save Scenario".equals(items[i]) || "Scenarios List".equals(items[i]) || "Execution".equals(items[i]) || "Feedback Loop".equals(items[i]) || "Generate Samples".equals(items[i]))
				menuItem.setEnabled(false);

			if ("New Project".equals(items[i]) || "Import Template".equals(items[i]) || "Save Scenario".equals(items[i]) ||
					"Open".equals(items[i]) || "Save as Template".equals(items[i]) || "Scenarios List".equals(items[i]) ||
					"Execution".equals(items[i]) || "Feedback Loop".equals(items[i]) || "Generate Samples".equals(items[i]) ||
					"Export XML".equals(items[i]) || "Export Yaml".equals(items[i])) {
				fileMenuItems.add(menuItem);
			}

			// Add "Generate Scenario" submenu under "Save Scenario"
			if ("Save Scenario".equals(items[i])) {
				JMenu generateScenarioSubMenu = new JMenu("Generate Scenario");
				JMenuItem csvItem = new JMenuItem("From CSV");
				csvItem.addActionListener(ev -> openGenerateScenarioWithCsvWindow());
				generateScenarioSubMenu.add(csvItem);
				menu.add(generateScenarioSubMenu);
			}

			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String cmd = e.getActionCommand();
					if ("Generate Samples".equals(cmd)) {
						openGenerateSamplesWindow();
					} else {
						odme.controller.MenuController menuController = new odme.controller.MenuController(mainFrame);
						menuController.executeMenuAction(cmd);
					}
				}
			});
			menu.add(menuItem);
		}
		menuBar.add(menu);
	}

	private void openGenerateSamplesWindow() {
		JFrame f = new JFrame("Generate Samples");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(new GenerateSamplesPanel());
		f.pack();
		f.setMinimumSize(new Dimension(500, 200));
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	public static void openGenerateScenarioWithCsvWindow() {
		JDialog dialog = new JDialog((java.awt.Frame) null, "Generate Scenario Using CSV File", true);
		dialog.setLayout(new GridBagLayout());
		dialog.setSize(520, 200);
		dialog.setLocationRelativeTo(null);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;

		JLabel nameLabel = new JLabel("Enter Scenario Name:");
		JTextField nameField = new JTextField();
		gbc.gridx = 0; gbc.gridy = 0; dialog.add(nameLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 0; dialog.add(nameField, gbc);

		JLabel pathLabel = new JLabel("CSV File Path:");
		JTextField pathField = new JTextField();
		pathField.setEditable(false);
		JButton browseButton = new JButton("Browse...");
		browseButton.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(ODMEEditor.fileLocation);
			fc.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
			fc.setAcceptAllFileFilterUsed(false);
			fc.setDialogTitle("Select CSV File");
			if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
				pathField.setText(fc.getSelectedFile().getAbsolutePath());
			}
		});
		gbc.gridx = 0; gbc.gridy = 1; dialog.add(pathLabel, gbc);
		gbc.gridx = 1; gbc.gridy = 1; dialog.add(pathField, gbc);
		gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0; dialog.add(browseButton, gbc);

		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1;
		dialog.add(buttonPanel, gbc);

		okButton.addActionListener(e -> {
			String scenarioName = nameField.getText().trim();
			String csvPath = pathField.getText().trim();
			if (scenarioName.isEmpty()) {
				JOptionPane.showMessageDialog(dialog, "Please enter a scenario name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (csvPath.isEmpty()) {
				JOptionPane.showMessageDialog(dialog, "Please select a CSV file.", "Missing File", JOptionPane.WARNING_MESSAGE);
				return;
			}
			BackgroundTaskRunner.run(dialog,
					"Generate Scenarios",
					"Generating scenario files from CSV...",
					() -> ScenarioGeneration.generateScenarios(csvPath, scenarioName),
					result -> {
						JOptionPane.showMessageDialog(dialog, result, "Done", JOptionPane.INFORMATION_MESSAGE);
						dialog.dispose();
					},
					error -> JOptionPane.showMessageDialog(dialog,
							"Failed:\n" + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
		});

		cancelButton.addActionListener(e -> dialog.dispose());
		dialog.setVisible(true);
	}
}
