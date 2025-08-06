package odme.odmeeditor;

// import static odme.odmeeditor.XmlUtils.sesview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.xml.sax.SAXException;

import odme.jtreetograph.JtreeToGraphConvert;
import javax.swing.JFileChooser;

/**
 * ODDManager is a JPanel that shows the current ODD in a table and provides functionalities to manipulate
 * the data and the ODD themselves and also export them in Yaml and Xml
 *
 * it has two "mode"s ; "generate OD" and "ODD Manager" for the first one you can only edit and optionally
 * save the currently open project's ODD and perhaps give it a different name
 *
 * with ODD Manager mode, you can read all the ODD's that you've previously saved and delete or export them
 * in other formats if you want
 *
 * ODD's are saved and consumed in java's native .ser format which easily uses their table's model to build
 * a serialized format out of it. you can then read .ser files and convert them to EditableDataModel Object
 * and feed this object to the JTable which is showing all the data ; the advantage of this method is that
 * there's zero Xml/Yaml/Json overhead , + it's easier to maintain and it's java's native therefore it provides
 * superior performance compared to xml or other schemas (also people on reddit highly recommend this format for
 * such use cases; if that counts!)
 *
 * ODD .ser files are all saved in %PROJECT_ROOT%\odd\ (which you can see in the project's root directory)
 * @author Roy
 * */
public class ODDManager extends JPanel{

	static final long serialVersionUID=1l;

	// table headers
	private final static String[] nodeHeaders = {
			"Component Name",
			"Type",
			"Data-Type",
			"Lower Bound Value",
			"Upper Bound Value",
			"Comments"
	};

	private static String getStateXsdFilePath() {
		return ODMEEditor.fileLocation+System.getProperty("file.separator")+
				ODMEEditor.projName+
				System.getProperty("file.separator")+
				"xsdfromxml.xsd";
	}

	public static final String getODDsPath() {
		return ODMEEditor.fileLocation
				+System.getProperty("file.separator")
				+"odd";
	}

	/*
	 *     NOTE:
	 *     these vars are written so (indents) to
	 * mention how elements are structured
	 */
	private JPanel btnsPanel;
	private JButton saveBtn;
	private JButton deleteBtn;
	private JButton exportYamlBtn;
	private JButton exportXmlBtn;
	private JLabel currentODDLabel;

	private JPanel oddsPanel;
	private ODDListView oddListView;
	private JPanel jtPanel;
	private JScrollPane jsp;
	private JTable jt;

	private String currentProjName;
	private String mode;
	// private JButton openBtn;

	public ODDManager() {
		this("Generate OD");
		this.currentProjName=ODMEEditor.projName;
		this.mode=mode;
	}

	public ODDManager(String mode){
		super();
		File f=new File(getODDsPath());
		if(!f.exists())
			f.mkdirs();
		this.setLayout(new FlowLayout());
		if (!mode.equals("Generate OD") && !mode.equals("ODD Manager"))
			throw new IllegalArgumentException("Only Allowed modes for ODD Manager are 'ODD Manager' and 'Generate OD'");
		init(mode);
	}

	private void init(String mode){
		this.currentODDLabel=new JLabel();
		this.currentODDLabel.setFont(new Font("Serif", Font.BOLD, 20));

		this.jt=new JTable();
		this.jtPanel=new JPanel();
		this.oddsPanel=new JPanel();

		// making the table scrollable
		jsp=new JScrollPane(jt);
		this.add(jsp);
		jtPanel.add(this.jsp);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		initBtnsPanel(mode);
		this.add(btnsPanel);
		this.add(currentODDLabel);

		// CAVEAT: if the mode is "Generate ODD" this still gets generated, but only
		// for the purpose of backwards-compatibility and preventing future 'null'-issues
		this.oddListView=new ODDListView(this);
		if (mode.equals("ODD Manager")) {
			// initOpenBtn(); // deprecated
			this.oddsPanel.add(this.oddListView);
			this.oddsPanel.add(jtPanel);
			this.add(oddsPanel);
		}
		else this.add(jtPanel);
		populateInitialTable(); // out of the current xsd open
		reDoTableInitials();
		updateCurrentODD(ODMEEditor.projName);
	}

	/**
	 * @author Roy
	 * initialize the buttons and call their corresponding init* methods
	 * which in turn will initialize that button and assing a listnere to it
	 * (so it can act on clicks) and adds it to the panel
	 * */
	private void initBtnsPanel(String mode) {
		this.btnsPanel=new JPanel();
		btnsPanel.add(currentODDLabel);
		initSaveBtn();
		if (mode.equals("ODD Manager")) {
			initExportXmlBtn();
			initExportYamlBtn();
			initDeleteBtn();
		}
		btnsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	}

	/**
	 * @author Roy
	 * update the name of currently open ODD
	 * */
	private void updateCurrentODD(String newODD){
		this.currentODDLabel.setText(newODD);
		this.currentProjName=newODD;
	}


	// ======================================================= FUNCTIONALITIES

	/**
	 * @author Roy
	 * Use JavaSE's classes to serialize the table's data model and write it to
	 * a file
	 * */
	private void exportSerialized(File f) throws IOException{
		FileOutputStream fos=new FileOutputStream(f);
		ObjectOutputStream oos=new ObjectOutputStream(fos);
		EditableDataModel edm=(EditableDataModel)jt.getModel();
		oos.writeObject(edm);
		oos.close();
		fos.close();
	}

	/**
	 * @author Roy
	 * Use JavaSE's classes to deserialize a file (specified by the path) into JTable's
	 * data model and return it so it can be assigned to JTable
	 * */
	private EditableDataModel importSerialized(String path) throws IOException, ClassNotFoundException{
		EditableDataModel dtm=null;
		FileInputStream fis=new FileInputStream(path);
		ObjectInputStream in=new ObjectInputStream(fis);
		dtm=(EditableDataModel) in.readObject();
		in.close();
		fis.close();
		return dtm;
	}

	/**
	 * @author Roy
	 * used by ODDListView in order to inform ODDManager when a new file is selected
	 * */
	public void readODD(String oddName){

		if(oddName==currentProjName)return; // it's the same file - don't read again

		String oddPath=getODDsPath()+System.getProperty("file.separator")+oddName+".ser";
		EditableDataModel dtm=null;
		try {

			// there's most likely a permission problem if this error happens
			if(!new File(oddPath).exists())
				throw new IOException("'"+oddPath+"' does not exist!");
			updateCurrentODD(oddName); // remember which file is being read

			dtm=importSerialized(oddPath);
			if(dtm == null) {
				JOptionPane.showMessageDialog(
						null,"Unable to Read ODD File",
						"Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			jt.setModel(dtm);
			reDoTableInitials(); // gui stuff
		} catch (ClassNotFoundException | IOException e) {
			JOptionPane.showMessageDialog(
					null, "Unable to Read ODD File","Error"
					,JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * save the current table's DataModel as a serialized object in a file
	 * with a custom name (asked from user)
	 * @author Roy
	 * */
	private void saveSer() {
		File fOut=null;
		String filename=JOptionPane.showInputDialog(null, "enter ODD's name (without the '.ser' part):",this.currentProjName);
		if(filename==null) return; // canceled
		String outPath=getODDsPath()+System.getProperty("file.separator")+filename+".ser";
		try{

			fOut=new File(outPath);
			if(fOut.exists()) { // bug fix - DO NOT REMOVE (I forgot what the bug was tho)
				fOut.delete();
				fOut.createNewFile();
			}

			exportSerialized(fOut);
			reDoTableInitials();
			this.updateCurrentODD(filename);
			oddListView.updateNames(); // tell the listview to re-read the filenames

			if(mode=="Generate OD")javax.swing.JOptionPane.showMessageDialog(null,filename+" saved.");
		}catch(IOException ioe) {
			ioe.printStackTrace();
			// javax.swing.JOptionPane.showMessageDialog(null,"Could not write the serialized Object");
		}
	}

	private void deleteCurrentODD() {
		File fdel=null;
		String delPath=getODDsPath()+System.getProperty("file.separator")+currentProjName+".ser";
		fdel=new File(delPath);
		fdel.delete(); // if the file doesn't exist - this method causes no errors
		javax.swing.JOptionPane.showMessageDialog(null,currentProjName+" Deleted");
		this.oddListView.updateNames();

	}

	// ======================================================= BTNs

	private void initExportXmlBtn() {
		this.exportXmlBtn=new JButton("Export XML");
		btnsPanel.add(exportXmlBtn);
		exportXmlBtn.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				exportMachineReadable();
			}
		});
	}

	private void initExportYamlBtn() {
		this.exportYamlBtn=new JButton("Export YAML");
		btnsPanel.add(exportYamlBtn);
		exportYamlBtn.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				exportYaml();
			}
		});
	}

	private void initSaveBtn() {
		this.saveBtn=new JButton("Save");
		btnsPanel.add(saveBtn);
		saveBtn.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				saveSer();
			}
		});
	}

	private void initDeleteBtn() {
		this.deleteBtn=new JButton("Delete");
		btnsPanel.add(deleteBtn);
		deleteBtn.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				deleteCurrentODD();
			}
		});
	}
// ======================================================= LEGACY - WORKS FINE, DON'T MODIFY UNLESS NECESSARY!
// Modified to generate properly structured YAML

	public static String currentXsdToYaml() {
		return xsdToYaml(getStateXsdFilePath());
	}

	public static String xsdToYaml(String path) {
		StringBuilder curr = new StringBuilder();
		List<String[]> xsd;
		try {
			xsd = readXsd(path);
			// Stack holds pair: [xsdIndent, yamlIndent]
			Deque<int[]> indentStack = new ArrayDeque<>();
			indentStack.push(new int[]{-1, -1});

			for (String[] row : xsd) {
				int xsdIndent = countLeadingSpaces(row[0]) / 2;
				String componentName = row[0].replaceAll("^\\s*-?", "").trim();
				if (componentName.isEmpty()) {
					continue;  // Skip empty names
				}
				String type = row[1].trim();

				// Pop until the current xsdIndent is greater than the one at stack top.
				while (!indentStack.isEmpty() && indentStack.peek()[0] >= xsdIndent) {
					indentStack.pop();
				}

				int parentYamlIndent = indentStack.isEmpty() ? -1 : indentStack.peek()[1];
				int currentYamlIndent = parentYamlIndent + 1;

				if (type.equals("Node")) {
					// For nodes, output a mapping key with a colon.
					curr.append(getIndent(currentYamlIndent))
							.append(componentName)
							.append(":\n");
					indentStack.push(new int[]{xsdIndent, currentYamlIndent});
				} else if (type.equals("Variable")) {
					// For variables, output as list item.
					curr.append(getIndent(currentYamlIndent))
							.append("- ").append(componentName).append(":\n")
							.append(getIndent(currentYamlIndent + 1))
							.append("type: ").append(row[2]).append("\n");

					if (!row[3].isEmpty()) {
						curr.append(getIndent(currentYamlIndent + 1))
								.append("min: ").append(row[3]).append("\n");
					}
					if (!row[4].isEmpty()) {
						curr.append(getIndent(currentYamlIndent + 1))
								.append("max: ").append(row[4]).append("\n");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return curr.toString();
	}

	private static int countLeadingSpaces(String s) {
		return (int) s.chars().takeWhile(c -> c == ' ').count();
	}

	private static String getIndent(int level) {
		return "  ".repeat(Math.max(0, level));
	}

	/**
	 * @author Roy
	 * #ROY - adding new Functionality
	 * */
	private void exportMachineReadable() {

		ODMEEditor.saveFunc(false);
		ODMEEditor.updateState();
		JtreeToGraphConvert.convertTreeToXML();
		String xmlContent=XmlUtils.readFile(
				ODMEEditor.fileLocation,
				ODMEEditor.projName,
				"xmlforxsd.xml"
		);
		ODMEEditor.chooseAndSaveFile(xmlContent,ODMEEditor.projName+".xml", null); // uncomment for production
	}

	public static void chooseAndSaveFile(String content,String suggestedPath,String ext) {
		FileWriter fw=null;
		try{
			fw=new FileWriter(getOpenedFile(suggestedPath));
			fw.write(content);
			javax.swing.JOptionPane.showMessageDialog(null,"File Saved Successfully.");
		}catch(IOException ioe) {ioe.printStackTrace();}

		// handle leakage and canceling
		try { if(fw!=null) fw.close(); }
		catch(IOException ioe1) { ioe1.printStackTrace(); }
	}

	public static File getOpenedFile(String path) {
		return getOpenedFile(path,null);
	}

	public static File getOpenedFile(String path,String ext) {
		JFileChooser jfc=new JFileChooser();

		// XXX making ext ineffective for now (due to BUGS)
		if (ext!=null)
			jfc.setFileFilter(new FileNameExtensionFilter(ext,ext));

		jfc.setSelectedFile(new File(path));
		int res=jfc.showOpenDialog(null);
		if(res==JFileChooser.APPROVE_OPTION)
			return jfc.getSelectedFile();
		return null;
	}

	/**
	 * @author Roy
	 * #ROY - adding new Functionality : use it for a Human Readable Yaml, which is NOT
	 * convertable to xsd/xml
	 **/
	private void exportYaml() {
		// javax.swing.JOptionPane.showMessageDialog(null,ODDManager.getODDsPath());
		ODMEEditor.saveFunc(false);
		ODMEEditor.updateState();
		JtreeToGraphConvert.convertTreeToXML();
		String yamlContent=currentXsdToYaml();
		ODMEEditor.chooseAndSaveFile(yamlContent,ODMEEditor.projName+".yaml", null); // uncomment for production
	}

	private void reDoTableInitials() { // this method just does some bugfix (don't ask)

		jt.setPreferredScrollableViewportSize(new Dimension(1024,600));
		jt.getColumnModel().getColumn(0).setPreferredWidth(200);
		// getColumnModel().getColumn(1).setPreferredWidth();
		jt.setFillsViewportHeight(true);
		jt.setFont(new Font("Arial", Font.PLAIN, 20));
		jt.setRowHeight(30);
		jt.setShowVerticalLines(true);
		jt.setSelectionBackground(new Color(217, 237, 146));
		jt.setSelectionForeground(new Color(188, 71, 73));
	}

	/**
	 * use readCurrentXsd to read the current xsd and turn it into
	 * suitable DataModel for JTable and then assign it to JTable
	 * */
	private void populateInitialTable() {

		EditableDataModel dtm=new EditableDataModel(nodeHeaders,0);
		List<String[]> data=null;
		try{
			data=readCurrentXsd();

			// caveat: if user hasn't saved the whole
			// progress yet, "data" may be null
			for(int i=0;i<data.size();i++)
				dtm.insertRow(dtm.getRowCount(),data.get(i));

			// adjusting table size and font etc.
			jt.setModel(dtm);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	// ======================================================= Reading inital xsd - new format with indents

	private static List<String[]> readCurrentXsd() throws SAXException, IOException, ParserConfigurationException{
		return readXsd(getStateXsdFilePath());
	}

	/**
	 * @author Roy
	 * read current project's xsd and parse it (manually) to extract the data you want (that is; only the
	 * columns like lowerBound upperBound etc.) and return a String[] list (list of arrays) so it can
	 * be assigned to JTable
	 */
	private static List<String[]> readXsd(String tablePath) throws SAXException, IOException, ParserConfigurationException {

		// preparing the Document Object Model (DOM)
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		org.w3c.dom.Document doc = db.parse(new File(tablePath));
		doc.getDocumentElement().normalize();

		// source of data for later consumption by the JTable
		List<String[]> dataSource=new ArrayList<String[]>();
		List<Element> processed=new ArrayList<Element>();

		// kick start processing in a recursive manner (xsd in and of itself is recursive!)
		NodeList nodes=doc.getElementsByTagName("xs:element");
		processNodeList(nodes,dataSource,processed,0);

		return dataSource;
	}

	/**
	 * @author Roy
	 * processes xs:element tags at one specific level of depth and recursively calls itself
	 * if that tag has xs:element child tags, until all xs:element tags are processed
	 * */
	private static void processNodeList(NodeList nodes,List<String[]> src,List<Element> processed,int indent) {
		Element curr=null;
		NodeList kids=null;
		for (int i=0;i<nodes.getLength();i++) {
			curr=(Element)nodes.item(i);
			if(!processed.contains(curr)) {
				processElementTag(curr, src, indent);
				kids=curr.getElementsByTagName("xs:element");
				if(kids.getLength()>0)
					processNodeList(kids, src,processed,indent+1);
				processed.add(curr);
			}
		}
	}


	/**
	 * @author Roy
	 * process one Element Tag, without touching it's children
	 * */
	private static void processElementTag(Element e,List<String[]> src,int indent) {
		src.add(getNodeHeaders(e,indent)); // adding headers

		// processing xs:attribute tags under the current xs:element tag (which is e)
		NodeList attrs=e.getElementsByTagName("xs:attribute");
		indent+=2;
		Element curr=null;
		String firstParentName=null;
		for(int i=0;i<attrs.getLength();i++) {
			curr=(Element)attrs.item(i);
			firstParentName=findFirstParent(curr, "xs:element");
			if(firstParentName.equals(e.getAttribute("name")) &&
					(!curr.hasAttribute("use") || !curr.getAttribute("use").equals("optional"))
			)src.add(processAttributeTag(curr, indent));
		}
	}

	/**
	 * @author Roy
	 * find the first parent tag of Element e, that has the tagName as its...well, tag name!
	 * */
	private static String findFirstParent(Element e,String tagName) {
		Node curr=(Node)e;
		Node n=null;
		do {
			n=curr.getParentNode();
			curr=n;
		}while(n!=null && !n.getNodeName().equals(tagName));
		if(n==null)return "[NO_PARENT]";
		return ((Element)n).getAttribute("name");
	}

	/**
	 * @author Roy
	 * get a node's attributes in a consumable form (based on the child tags)
	 * */
	private static String[] processAttributeTag(Element e,int indent) {

		String[] r=new String[6];
		r[0]=indentStr("-"+e.getAttribute("name"),indent);
		r[1]="Variable";
		NodeList nl=e.getElementsByTagName("*");
		Element temp;
		for(int i=0;i<nl.getLength();i++) {
			temp=(Element)nl.item(i);
			switch(temp.getNodeName()) {
				case "xs:restriction":r[2]=temp.getAttribute("base").replace("xs:", "");break;
				case "xs:minInclusive":r[3]=temp.getAttribute("value");break;
				case "xs:maxInclusive":r[4]=temp.getAttribute("value");break;
				default:break;
			}
		}
		return r;
	}

	/**
	 * @author Roy
	 * get node name and the fact that it is a node
	 * */
	private static String[] getNodeHeaders(Element e,int indent) {
		String[] r=new String[5];
		r[0]=indentStr(e.getAttribute("name"),indent,"  ");
		r[1]="Node";
		return r;
	}

	/**
	 * @author Roy
	 * puts whitespaces at the start of a String
	 * */
	private static String indentStr(String s,int level) {
		return indentStr(s,level," ");
	}

	/**
	 * @author Roy
	 * puts whitespaces at the start of a String
	 * */
	private static String indentStr(String s,int level,String indentStr) {
		for (int i=0;i<level;i++)
			s=indentStr+s;
		return s;
	}

}

