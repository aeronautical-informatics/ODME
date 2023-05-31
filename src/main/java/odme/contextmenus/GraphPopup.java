/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package odme.contextmenus;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import odme.jtreetograph.JtreeToGraphAdd;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <h1>GraphPopup</h1>
 * <p>
 * This class implements right click action of the mouse on the graphical editor
 * panel. It initiates mouse actions related to various elements node addition
 * on the panel.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class GraphPopup extends JPopupMenu {
	
    private static final long serialVersionUID = 1L;

    public GraphPopup(int x, int y) {
    	String[] items = {"Add Entity", "Add Specialization", "Add Aspect", "Add MultiAspect"};
    	JMenuItem item;
    	
    	for (int i=0; i<items.length; i++) {
    		item = new JMenuItem(items[i]);
    		add(item);
    		if (i<items.length-1)
    			add(new JSeparator());
    		
    		item.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
            	  switch (ae.getActionCommand()){
            	  case "Add Entity":
            		  JtreeToGraphAdd.addNodeFromGraphPopup("Entity", x, y);
	            	  break;
	              case "Add Specialization":
	            	  JtreeToGraphAdd.addNodeFromGraphPopup("Spec", x, y);
	            	  break;
	              case "Add Aspect":
	            	  JtreeToGraphAdd.addNodeFromGraphPopup("Dec", x, y);
		              break;
	              case "Add MultiAspect":
	            	  JtreeToGraphAdd.addNodeFromGraphPopup("MAsp", x, y);
		              break;
		          } 
              }
          });
    	}
    }
}
