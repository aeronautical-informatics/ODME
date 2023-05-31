package odme.contextmenus;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

import odme.odmeeditor.ODMEEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <h1>ProjectTreePopup</h1>
 * <p>
 * This class implements the JTree node pop up action of the project tree. Only
 * delete action is implemented here. The main module can't be deleted. But
 * added module file name can be deleted using this delete functionality.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class ProjectTreePopup extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	public ProjectTreePopup(JTree tree) {
        JMenuItem itemDelete = new JMenuItem("Delete Node");

        itemDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                popUpActionDeleteProjectTree();
            }
        });
        add(itemDelete);
    }
    
  public void popUpActionDeleteProjectTree() {
	  ODMEEditor.projectPanel.removeCurrentNode();
  }
}
