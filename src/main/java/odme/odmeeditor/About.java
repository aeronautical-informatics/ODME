package odme.odmeeditor;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;



public class About extends JPanel {

	private static final long serialVersionUID = 1L;
	private JFrame frame = new JFrame();
    private Map<String, String> about_data = new HashMap<String, String>();
    
    public About() {
        super(new BorderLayout());
        
        about_data.put("Name:", "Operation Domain Modeling Environment");
        about_data.put("Version:", "2022.01");
        about_data.put("<html>Developer:<br></html>",
        		"<html>---, <br>--- <br> ---</html>");
        about_data.put("<html>Supervisor:<br></html>",
        		"<html>---, <br>---</html>");
    }

    public void aboutGUI() {
    	
    	JPanel panelTop = new JPanel();
        JPanel panelCenter = new JPanel();
        panelCenter.setLayout(null);
        JPanel panelBottom = new JPanel();
    	
        int i=30;
    	for (Map.Entry<String, String> entry : about_data.entrySet()) {
    	    String key = entry.getKey();
    	    String value = entry.getValue();
    	    
    	    JLabel label_key = new JLabel(key);
    	    label_key.setBounds(20, i, 120, i);
    	    
    	    JLabel label_value = new JLabel(value);
    	    label_value.setBounds(130, i, 300, i);
    	    
    	    panelCenter.add(label_key);
    	    panelCenter.add(label_value);
    	    
    	    i += 25;
    	}

        panelTop.setBorder(new EtchedBorder());
        panelCenter.setBorder(new EtchedBorder());
        panelBottom.setBorder(new EtchedBorder());

        int width = 450;
        int height = 300;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;

        frame.setBounds(x, y, width, height);
        frame.setTitle("About Editor");
        frame.setResizable(false);
        frame.pack();
        frame.setSize(width, height);
        frame.setVisible(true);
        frame.add(panelTop, BorderLayout.NORTH);
        frame.add(panelCenter, BorderLayout.CENTER);
        frame.add(panelBottom, BorderLayout.SOUTH);
    }
}
