package odme.odmeeditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import odme.jtreetograph.JtreeToGraphAdd;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;



public class Console extends JInternalFrame {

	private static final long serialVersionUID = 1L;
	public static RSyntaxTextArea consoleText;
    private JPanel cp;

    public Console() {

        cp = new JPanel(new BorderLayout());

        consoleText = new RSyntaxTextArea(20, 60);
        consoleText.append(">>");
        consoleText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        consoleText.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(consoleText);
        cp.add(sp);
        setContentPane(cp);
        setTitle("Console");

        consoleText.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
            	consoleKeyPressedAction(e);
            }
        });
    }
    
    private void consoleKeyPressedAction(KeyEvent e) {
    	Robot robot = null;
        try {
            robot = new Robot();
        } 
        catch (AWTException e3) {
            e3.printStackTrace();
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {

            try {
                int end = consoleText.getDocument().getLength();
                int start = Utilities.getRowStart(consoleText, end);

                while (start == end) {
                    end--;
                    start = Utilities.getRowStart(consoleText, end);
                }

                consoleText.append("\n\n>>");

                String text = consoleText.getText(start, end - start);

                // use the last input here
                if (text.startsWith(">>add")) {
                    String[] arrOfStr = text.split(" ");
                    for (String a : arrOfStr) {
                        text = a;
                    }

                    JtreeToGraphAdd.addNodeFromConsole(text);

                } 
                else if (text.equals(">>clear")) {
                    consoleText.setText(">>");
                }

                robot.keyPress(KeyEvent.VK_SHIFT);
                robot.keyPress(KeyEvent.VK_BACK_SPACE);
                robot.keyRelease(KeyEvent.VK_BACK_SPACE);
                robot.keyRelease(KeyEvent.VK_SHIFT);
            } 
            catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }
    
    /**
     * Takes string as an argument and add that as an output in the console window.
     *
     * @param str
     */
    public static void addConsoleOutput(String str) {
        consoleText.append(str);
        consoleText.append("\n");
    }
}
