package odeme.behaviour;

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
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import behaviourtreetograph.JtreeToGraphConvert;
import behaviourtreetograph.JtreeToGraphGeneral;



public class ToolBarBehaviour {

	private JToolBar toolbar;
	public static Map<String, JButton> btnItems = new HashMap<>();
//	public static JButton btnScenario;

	public ToolBarBehaviour(JFrame frame) {

		toolbar = new JToolBar();
		toolbar.setBorder(new EtchedBorder());
		frame.add(toolbar, BorderLayout.NORTH);
	}

	public void show() {

		String[] names = {"Selector", "Add Decorator", "Add Selector", "Add Sequence","Add Parallel", "Delete Node", "Save Graph", "Undo", "Redo", "Zoom In", "Zoom Out", "Merge"};
		String[] images = {"cursor", "decoratorT", "selectorT", "sequenceT","parallelT", "delete", "save", "undo", "redo", "zoom-in", "zoom-out", "merge"};

		for (int i=0; i<names.length; i++) {
			ImageIcon Icon =
					new ImageIcon(ODMEBehaviourEditor.class.getClassLoader().getResource("images/"+images[i]+".png"));
			JButton btn = new JButton(Icon);
			btn.setName(names[i]);
			btn.setToolTipText(names[i]);
			toolbar.add(btn);
			btnItems.put(names[i], btn);
			btn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					switch (((JButton) e.getSource()).getName()) {
						case "Selector":
							selector();
							break;
						case "Add Decorator":
							addDecorator();
							break;
						case "Add Selector":
							addSelector();
							break;
						case "Add Sequence":
							addSequence();
							break;
						case "Add Parallel":
							addParallel();
							break;
						case "Delete Node":
							deleteNode();
							break;
						case "Save Graph":
							saveGraph();

							break;
						case "Undo":
							undo();
							break;
						case "Redo":
							redo();
							break;
						case "Zoom In":
							zoomIn();
							break;
						case "Zoom Out":
							zoomOut();
							break;
						case "Merge":
							System.out.println("merge clicked ");
							mergeXML();
							break;
					}
				}
			});
		}


		toolbar.add(Box.createHorizontalGlue());

	}

	private void selector(){
		ODMEBehaviourEditor.nodeAddDecorator = "";
	}

	private void addDecorator(){
		ODMEBehaviourEditor.nodeAddDecorator = "Decorator";
	}

	private void addSelector(){
		ODMEBehaviourEditor.nodeAddDecorator = "Selector";
	}

	private void addSequence() {
		ODMEBehaviourEditor.nodeAddDecorator = "Sequence";
	}

	private void addParallel(){
		ODMEBehaviourEditor.nodeAddDecorator = "Parallel";
	}

	private void deleteNode(){
		ODMEBehaviourEditor.nodeAddDecorator = "delete";
	}

	public void saveGraph() {

		ODMEBehaviourEditor.treePanel.saveTreeModel();
		JtreeToGraphConvert.behaviourGraphToXML();
		JOptionPane.showMessageDialog(MainWindow.frame, "Saved Successfully.", "Save",
				JOptionPane.INFORMATION_MESSAGE);
	}


	private void mergeXML(){
		JtreeToGraphConvert.mergeBehaviourWithXSD();
		JOptionPane.showMessageDialog(MainWindow.frame, "Merged Successfully.", "Merge Behaviour with XSD File",
				JOptionPane.INFORMATION_MESSAGE);
	}
	private void undo(){
		JtreeToGraphGeneral.undo();
		// undo actions for graph
		try {
			if (ODMEBehaviourEditor.undoJtree.canUndo()) {
				ODMEBehaviourEditor.undoJtree.undo();
				ODMEBehaviourEditor.treePanel.expandTree();
			}
		} catch (CannotUndoException ex) {
			System.out.println("Unable to undo: " + ex);
			ex.printStackTrace();
		}
	}

	private void redo(){
		// redo actions for jtree
		JtreeToGraphGeneral.redo();
		// redo actions for graph
		try {
			if (ODMEBehaviourEditor.undoJtree.canRedo()) {
				ODMEBehaviourEditor.undoJtree.redo();
				ODMEBehaviourEditor.treePanel.expandTree();
			}
		} catch (CannotRedoException ex) {
			System.out.println("Unable to redo: " + ex);
			ex.printStackTrace();
		}
	}

	private void zoomIn(){
		JtreeToGraphGeneral.zoomIn();
	}

	private void zoomOut(){
		JtreeToGraphGeneral.zoomOut();
	}
}

