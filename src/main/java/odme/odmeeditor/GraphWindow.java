package odme.odmeeditor;

import odme.core.EditorContext;
import javax.swing.JInternalFrame;



public class GraphWindow extends JInternalFrame {

	private static final long serialVersionUID = 1L;

	public GraphWindow() {
        super(EditorContext.getInstance().getProjName(), false, // resizable
                false, // closable
                false, // maximizable
                false // iconifiable
        );
    }
}
