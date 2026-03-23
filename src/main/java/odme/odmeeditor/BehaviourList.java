package odme.odmeeditor;

//import static odme.behaviour.MainWindow

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import odme.behaviour.BehaviourToTree;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.mxgraph.util.svg.ParseException;
import odme.behaviour.MainWindow;
import odme.core.EditorContext;
import javax.swing.JOptionPane;

public class BehaviourList extends JPanel{

	private static final long serialVersionUID = 1L;
	private JTable table;
	private DefaultTableModel model;

	public void createScenarioListWindow() {
		List<String[]> dataList = getJsonData();
		List<String[]> dataList1 = new ArrayList<>();

		for(int i = 0; i<dataList.size(); i++) {
			String t = scenaiorsName(dataList.get(i));
			if(t!= null) {
				String[] temp = new String[] {t};
				dataList1.add(temp);
			}
		}


		model = new DefaultTableModel(new String[]{"Name"}, 0);
		for (String[] arr: dataList1) {

			model.addRow(arr);

		}

		if(dataList1.isEmpty()){
			model.addRow(new Object[]{"You don`t have any behaviour in scenarios."});
		}

		table = new JTable(model);
		table.setShowVerticalLines(true);
		table.setDefaultEditor(Object.class, null);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.setAutoCreateRowSorter(true);

		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {

					try {

						int row = table.getSelectedRow();
						String[] h =  dataList1.get(row);
						BehaviourToTree.selectedScenario = h[0];
						MainWindow window = new MainWindow();
					} catch (IndexOutOfBoundsException i){
						i.printStackTrace();
					} catch (Exception ex){
						ex.printStackTrace();
					}
				}
			}
		});
		JFrame frame = new JFrame("Behaviours List");
		JPanel panelCenter = new JPanel();
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(480, 200));

		panelCenter.add(scroll);

		panelCenter.setBorder(new EtchedBorder());

		int width = 500;
		int height = 250;
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screen.width - width) / 2;
		int y = (screen.height - height) / 2;

		frame.pack();

		frame.setBounds(x, y, width, height);
		frame.setSize(width, height);

		frame.add(panelCenter, BorderLayout.CENTER);

		frame.setResizable(false);
		frame.setVisible(true);
	}


	private List<String[]> getJsonData() {
		JSONParser jsonParser = new JSONParser();
		List<String[]> dataList = new ArrayList<String[]>();

		try {

			Path path = Path.of("").toAbsolutePath();
			FileReader reader = new FileReader(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");

			Object obj = null;
			try {
				obj = jsonParser.parse(reader);

			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return dataList;
			}
			//from here check these scenario have behaviour files
			JSONArray data = (JSONArray) obj;

			for (Object dtObj:data) {

				dataList.add(parseObject((JSONObject)dtObj));

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (ParseException e) {
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return dataList;
	}

	private String[] parseObject(JSONObject obj) {
		JSONObject dataObject = (JSONObject) obj.get("scenario");
		String name = (String) dataObject.get("name");
		String[] arr = {name};
		return arr;
	}
	private String scenaiorsName(String[] scenaiorsList) {
		String s = null;
		Path path = Path.of("").toAbsolutePath();
		for(String folder: scenaiorsList) {
			File f =  new File(EditorContext.getInstance().getProjectDir() + "/" + folder + "/" + EditorContext.getInstance().getProjName() + ".ssdbeh" );
			if(f.exists()) {
				s = folder;
			}
		}
		return s;
	}
}
