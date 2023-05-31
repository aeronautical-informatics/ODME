package odme.core;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import odme.odmeeditor.ODMEEditor;

/**
 * <h1>EditorUndoableEditListener</h1>
 * <p>
 * This class implements the undo and redo options of the JTree.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class EditorUndoableEditListener implements UndoableEditListener {
    public void undoableEditHappened(UndoableEditEvent e) {
        // Remember the edit and update the menus
        ODMEEditor.undoJtree.addEdit(e.getEdit());
    }
}
