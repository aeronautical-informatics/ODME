package odme.odmeeditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * <h1>XMLViewer</h1>
 * <p>
 * This class inherits JInternalFrame because it is using within another frame.
 * XMLViewer is used to display XML instance of the created SES model. XML
 * Schema of the model is also displayed in this viewer. Console window also use
 * this viewer to display errors.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class XMLViewer extends JInternalFrame {

	private static final long serialVersionUID = 1L;
	public RSyntaxTextArea textArea;
    private JPanel cp;
    private RTextScrollPane sp;

    public XMLViewer() {
        cp = new JPanel(new BorderLayout());
        textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        textArea.setCodeFoldingEnabled(true);
        sp = new RTextScrollPane(textArea);
        cp.add(sp);
        setContentPane(cp);
        setTitle("XML Viewer");
    }
}
