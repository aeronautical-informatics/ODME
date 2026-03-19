package odme.odmeeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

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
		final String[] items_scenario_manager =  {"Scenarios List", "Execution", "Feedback Loop"};
		final int[] keyevents_scenario_manager = {0               , 0         ,  0             };
		final String[] keys_scenario_manager =   {null            , null      ,  null          };
		final String[] images_scenario_manager = {"list"          ,"executionIcon"      ,"feedbackLoopIcon"          };
										
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

			if (items[i]=="Save Scenario" || items[i]=="Scenarios List" || items[i]=="Execution" || items[i]=="Feedback Loop")
				menuItem.setEnabled(false);

			if (items[i]=="New Project" || items[i]=="Import Template" || items[i]=="Save Scenario" ||
					items[i]=="Open" || items[i]=="Save as Template" || items[i]=="Scenarios List" ||
					items[i]=="Execution" || items[i]=="Feedback Loop" || items[i]=="Export XML" ||
					items[i]=="Export Yaml") {
				fileMenuItems.add(menuItem);
			}

			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					odme.controller.MenuController menuController = new odme.controller.MenuController(mainFrame);
					menuController.executeMenuAction(e.getActionCommand());
				}
			});
			menu.add(menuItem);
		}
		menuBar.add(menu);
	}

}
