package odeme.behaviour;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import odme.odmeeditor.GraphWindow;
import odme.odmeeditor.ProjectTree;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;



public class PanelSplitor {
	
    public static int dividerLocation = 0;
    private JSplitPane projectPane, grapConsole, graphtree, variableAndCOnstraint,variableAndBehaviour, xml, graphVariable;
    
    
    private JSplitPane createSpliPane(int orientation, Component leftComponent,Component rightComponent, int dividerLocation) {
    	
    	JSplitPane pane = new JSplitPane(orientation, leftComponent, rightComponent);
    	pane.setOneTouchExpandable(true);
    	pane.setDividerLocation(dividerLocation); // define project Explorer height
    	pane.setDividerSize(6); // width of the line which split the window
    	pane.setBorder(null);
        
    	return pane;
    }
    public JSplitPane addSplitor(ProjectTree projectPanel,  GraphWindow graphWindow) {
    	

    	grapConsole = createSpliPane(JSplitPane.VERTICAL_SPLIT, graphWindow, projectPanel, 750);
//    	graphVariable = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, graphtree, xml, 1400);
    	return grapConsole;
    }
    private void graphtreeFunc() {
    	dividerLocation = graphtree.getDividerLocation();
        graphtree.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
        	@Override
        	public void propertyChange(PropertyChangeEvent evt) {
        		SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                	dividerLocation = graphtree.getDividerLocation();
                	}
                });
        		}
        	});
    }
}
