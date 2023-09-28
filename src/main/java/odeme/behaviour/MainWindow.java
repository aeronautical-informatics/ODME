package odeme.behaviour;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import odme.odmeeditor.MenuBar;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.ToolBar;

public class MainWindow {

	public static JFrame  frame;
	private  ODMEBehaviourEditor newContentPane;
	private  MenuBarBehaviour menuBar;
	private  ToolBarBehaviour toolBar;


	public MainWindow(){

		// Create and set up the main window.
		frame = new JFrame("Behaviour Modeling Tool Environment");

		// Create and set up the content pane.
		newContentPane = new ODMEBehaviourEditor();
		frame.setContentPane(newContentPane);

		// add menubar
		menuBar = new MenuBarBehaviour(frame);
		menuBar.show();

		// add toolbar
		toolBar = new ToolBarBehaviour(frame);
		toolBar.show();

		frame.setVisible(true);
		frame.add(ODMEBehaviourEditor.splitPane, BorderLayout.CENTER);

		frame.pack();

	}
}
