package odme.jtreetograph;

import java.io.File;
import java.util.ArrayList;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.view.mxGraph;

import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphVariables {

	public static int nodeNumber = 1;
	
	public static int behaviourNodeNumber = 1;
    public static mxGraph graph;
    // ProjectPane Related
    public static String newFileName = ODMEEditor.projName;
    public static String projectFileNameGraph = newFileName;
    public static mxCell selectedNodeCellForVariableUpdate = null;
    /**
     * To add node automatically from console window using text command (add node
     * Scenario) <code>currentSelectedCell</code> variable keep track of the last
     * selected cell using mouse.
     */
    public static mxCell currentSelectedCell = null;
    public static int uniformityNodeNumber = 0;
    public static boolean connectedToRoot = false;
    public static ArrayList<String> pathToRoot = new ArrayList<String>();
    public static mxCell firstAddedCellForSubTreeDeletion = null;
    public static Object parent;
    public static int treeSyncNodeCount = 0;
    public static int firstAddedCellForSubTree = 0;
    public static String subtreeCheckLabel = null;

    public static mxCell subtreeCheckCell = null;
    public static mxCell subtreeSyncCell = null;
    public static String addedCellNameSync = null;
    public static File ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
    		ODMEEditor.fileLocation, ODMEEditor.projName, projectFileNameGraph));

    public static mxGraphComponent graphComponent = null;

    public static mxGraphComponent behaviourGraphComponent = null;
    public static mxUndoManager undoManager;

    public static mxCell lastAddedCell = null;
    public static ArrayList<String> path = new ArrayList<String>();
    public static ArrayList<String> behaviourPath = new ArrayList<>();
    // used in next function
    public static String[] nodesToSelectedNode;
    public static int totalNodes;
    public static int nodeReached;
    public static mxCell lastNodeInPath;
    public static String[] nodeNamesForGraphSync = new String[100];
    
    public static String[] variableList = new String[100];
}
