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
import javax.swing.JToolBar;



public class ToolBar {
	
	private JToolBar toolbar;
	public static Map<String, JButton> btnItems = new HashMap<>();
	public static JButton btnScenario;
	
	public ToolBar(JFrame frame) {

        toolbar = new JToolBar();
        toolbar.setBorder(new javax.swing.border.EmptyBorder(6, 10, 6, 10));
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
                    odme.controller.ToolbarController toolbarController = new odme.controller.ToolbarController();
                    toolbarController.executeToolbarAction(((JButton) e.getSource()).getName());
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
    
    public static void validation() {
        odme.controller.ToolbarController.validation();
    }
}

