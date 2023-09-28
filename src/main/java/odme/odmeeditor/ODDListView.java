package odme.odmeeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/*
 * ODDListView lists all the ODD files and shows them in a selectable manner.
 * upon being clicked on, the filenames are reported to it's parent, so the parent
 * (which is ODDManager) can show the newly selected ODD file in it's table
 * 
 * also there's an "updateNames" method here, which the parent uses to tell ODDListView 
 * to try and list the odds again; this is useful when a new file is saved/deleted
 * and you want to show the user what are the new changes
 * */
public class ODDListView extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private ODDManager parent;
	private JLabel oddsListLabel;
	private JList<String> odds;
	
	public ODDListView(ODDManager parent) {
		this.parent=parent;
		this.setPreferredSize(new Dimension(200,600));
		this.setMinimumSize(new Dimension(100,600));
		init();
	}
	
	private void init() { // initialize Graphical part 
		
		odds=new JList<String>();
		odds.addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()
            		&& odds.getSelectedValuesList().size()>0)
                		parent.readODD(odds.getSelectedValuesList().get(0));
            }
        });
		oddsListLabel=new JLabel("ODD(s) List");
		updateNames();
		
		// pure graphical
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(oddsListLabel);
		this.oddsListLabel.setBorder(BorderFactory.createEtchedBorder(1));
		odds.setFont(new Font("Serif", Font.BOLD, 14));
		this.add(odds);
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.setBackground(Color.WHITE);
	}
	
	public void updateNames() { // called by the parent, when a new file is saved or deleted
		DefaultListModel<String> dlm=getFileNamesListModel();
		odds.setModel(dlm);
	}
	
	/**
	 * @author Roy
	 * Get the list of all ODD .ser files and return them in a format which can be consumed
	 * by JList
	 * */
	private DefaultListModel<String> getFileNamesListModel(){
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		File dir=new File(ODDManager.getODDsPath());
		File[] serFiles=dir.listFiles();
		
		// no ODD's added yet
		if(serFiles==null || serFiles.length==0) { 
			listModel.clear();
			return listModel;
		}
		
		// read the filenames and add them to listModel
		for (File ser:serFiles) // foreach loop in java
			if(ser.getAbsolutePath().endsWith(".ser"))
				listModel.addElement(ser.getName().replace(".ser", ""));
		return listModel;
	}	
	
}
