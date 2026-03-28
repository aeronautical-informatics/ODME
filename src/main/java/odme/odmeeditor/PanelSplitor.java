package odme.odmeeditor;

import odme.behaviour.Behaviour;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

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
    
    public JSplitPane addSplitor(ProjectTree projectPanel, DynamicTree treePanel,
                                 GraphWindow graphWindow, Console console,
                                 Variable scenarioVariable, Behaviour scenarioBehaviour,
                                 Constraint scenarioConstraint, JTabbedPane tabbedPane) {
    	
    	projectPane = createSpliPane(JSplitPane.VERTICAL_SPLIT, projectPanel, treePanel, 250);
    	grapConsole = createSpliPane(JSplitPane.VERTICAL_SPLIT, graphWindow, console, 750);
    	graphtree = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, projectPane, grapConsole, 200);
    	graphtreeFunc();
    	variableAndBehaviour =  createSpliPane(JSplitPane.VERTICAL_SPLIT, scenarioVariable, scenarioBehaviour, 150);
//    	variableAndCOnstraint = createSpliPane(JSplitPane.VERTICAL_SPLIT, scenarioVariable, behaviourVariable,scenarioConstraint, 150);
    	variableAndCOnstraint = createSpliPane(JSplitPane.VERTICAL_SPLIT, variableAndBehaviour,scenarioConstraint, 150);
    	xml = createSpliPane(JSplitPane.VERTICAL_SPLIT, variableAndCOnstraint, tabbedPane, 300);
    	graphVariable = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, graphtree, xml, 1400);

        return graphVariable;
    }
    
    /**
     * Extended layout including Distribution, InterEntityConstraints and IntraEntityConstraint panels.
     */
    public JSplitPane addSplitor(ProjectTree projectPanel, DynamicTree treePanel,
                                 GraphWindow graphWindow, Console console,
                                 Variable scenarioVariable, Distribution scenarioDistribution,
                                 Behaviour scenarioBehaviour,
                                 InterEntityConstraints scenarioInterEntity,
                                 IntraEntityConstraint scenarioIntraEntity,
                                 JTabbedPane tabbedPane) {

        projectPane = createSpliPane(JSplitPane.VERTICAL_SPLIT, projectPanel, treePanel, 250);
        grapConsole = createSpliPane(JSplitPane.VERTICAL_SPLIT, graphWindow, console, 750);
        graphtree   = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, projectPane, grapConsole, 200);
        graphtreeFunc();

        JSplitPane varDist      = createSpliPane(JSplitPane.VERTICAL_SPLIT, scenarioVariable, scenarioDistribution, 120);
        JSplitPane varDistBeh   = createSpliPane(JSplitPane.VERTICAL_SPLIT, varDist, scenarioBehaviour, 250);
        JSplitPane withIntra    = createSpliPane(JSplitPane.VERTICAL_SPLIT, varDistBeh, scenarioIntraEntity, 380);
        variableAndCOnstraint   = createSpliPane(JSplitPane.VERTICAL_SPLIT, withIntra, scenarioInterEntity, 500);
        xml          = createSpliPane(JSplitPane.VERTICAL_SPLIT, variableAndCOnstraint, tabbedPane, 620);
        graphVariable = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, graphtree, xml, 1400);

        return graphVariable;
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
