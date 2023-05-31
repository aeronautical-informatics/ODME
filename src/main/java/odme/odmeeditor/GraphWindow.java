package odme.odmeeditor;

import javax.swing.JInternalFrame;



public class GraphWindow extends JInternalFrame {

	private static final long serialVersionUID = 1L;

	public GraphWindow() {
        super(ODMEEditor.projName, false, // resizable
                false, // closable
                false, // maximizable
                false // iconifiable
        );
    }
}
