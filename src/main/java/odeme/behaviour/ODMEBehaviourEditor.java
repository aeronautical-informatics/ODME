package odeme.behaviour;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.undo.UndoManager;

import behaviourtreetograph.JTreeToGraphBehaviour;
import odme.odmeeditor.GraphWindow;


public class ODMEBehaviourEditor extends JPanel{
	
	public static GraphWindow graphWindow;
	public static JSplitPane splitPane;

    public static UndoManager undoJtree = new UndoManager();
    public static boolean undoControlForSubTree = false;
    
	public static String nodeAddDecorator = "";
    public static String nodeBehaviour = "";

    public static BehaviourToTree treePanel;


    public ODMEBehaviourEditor() {
		super(new BorderLayout());

        // Adding jgraph window in the centre
        graphWindow = new GraphWindow();
        graphWindow.setPreferredSize(new Dimension(1000, 800));
        removeTopLeftIcon(graphWindow);
       
        graphWindow.pack();
        graphWindow.setVisible(true);
		
//      -------------------------------------
        treePanel = new BehaviourToTree();

        treePanel.setPreferredSize(new Dimension(200, 600));
        
        // add panelSpliter with main window's parts 
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(graphWindow);
        splitPane.setDividerSize(20);
        splitPane.setLastDividerLocation(150);

        JTreeToGraphBehaviour.createGraph(graphWindow);
	}
	
	public static void removeTopLeftIcon(JInternalFrame internalFrame){
    	// this is for removing the top-left icon of the internal frame
        BasicInternalFrameUI ui = (BasicInternalFrameUI) internalFrame.getUI();
        Container north = ui.getNorthPane();
        north.remove(0);
        north.validate();
        north.repaint();
    }

}
