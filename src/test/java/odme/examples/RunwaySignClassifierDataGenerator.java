package odme.examples;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.core.FlagVariables;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates serialized .ssdvar, .ssdbeh, .ssdcon, and .ssdflag files
 * for the RunwaySignClassifier example project.
 *
 * TreePath elements must be DefaultMutableTreeNode (not String) because
 * JtreeToGraphConvert.rootToEndVariableAddition casts them.
 */
public class RunwaySignClassifierDataGenerator {

    private static final String EXAMPLE_DIR =
            "examples/RunwaySignClassifier/RunwaySignClassifier";

    // Cache nodes by their full path string so we can reuse them
    private final Map<String, DefaultMutableTreeNode> nodeCache = new HashMap<>();

    public static void main(String[] args) throws Exception {
        new RunwaySignClassifierDataGenerator().generate();
    }

    @Test
    public void generateSerializedFiles() throws Exception {
        generate();
    }

    private void generate() throws Exception {
        Path projectRoot = findProjectRoot();
        String basePath = projectRoot.resolve(EXAMPLE_DIR).toString();

        // Build the full tree structure first
        DefaultMutableTreeNode root = buildTree();

        Multimap<TreePath, String> varMap = buildVarMap(root);
        Multimap<TreePath, String> behavioursList = ArrayListMultimap.create();
        Multimap<TreePath, String> constraintsList = ArrayListMultimap.create();
        FlagVariables flags = new FlagVariables();
        flags.nodeNumber = 36;
        flags.uniformityNodeNumber = 0;

        serialize(basePath + ".ssdvar", varMap);
        serialize(basePath + ".ssdbeh", behavioursList);
        serialize(basePath + ".ssdcon", constraintsList);
        serialize(basePath + ".ssdflag", flags);

        System.out.println("All files generated successfully.");
        System.out.println("varMap entries: " + varMap.size() + " across " + varMap.keySet().size() + " nodes");
    }

    /**
     * Build the JTree structure matching RunwaySignClassifier.xml exactly.
     * Each DefaultMutableTreeNode has proper parent-child relationships.
     */
    private DefaultMutableTreeNode buildTree() {
        DefaultMutableTreeNode root = node("RunwaySignClassifier");

        DefaultMutableTreeNode pSpec = node("pSpec");
        root.add(pSpec);

        // Environment branch
        DefaultMutableTreeNode env = node("Environment");
        pSpec.add(env);

        DefaultMutableTreeNode airportDec = node("AirportDec");
        env.add(airportDec);
        airportDec.add(node("KSFO"));
        airportDec.add(node("KBOS"));
        airportDec.add(node("KSAN"));

        DefaultMutableTreeNode weatherDec = node("WeatherDec");
        env.add(weatherDec);
        weatherDec.add(node("FairWeather"));
        weatherDec.add(node("RainyWeather"));
        weatherDec.add(node("SnowyWeather"));
        weatherDec.add(node("FoggyWeather"));

        DefaultMutableTreeNode timeOfDayDec = node("TimeOfDayDec");
        env.add(timeOfDayDec);
        timeOfDayDec.add(node("Morning"));
        timeOfDayDec.add(node("Afternoon"));
        timeOfDayDec.add(node("Dusk"));
        timeOfDayDec.add(node("Dawn"));

        // Sensor branch
        DefaultMutableTreeNode sensor = node("Sensor");
        pSpec.add(sensor);

        DefaultMutableTreeNode distanceDec = node("DistanceDec");
        sensor.add(distanceDec);
        distanceDec.add(node("DS10"));
        distanceDec.add(node("DS12"));
        distanceDec.add(node("DS14"));

        DefaultMutableTreeNode elevationDec = node("ElevationDec");
        sensor.add(elevationDec);
        elevationDec.add(node("EL10"));
        elevationDec.add(node("EL13"));
        elevationDec.add(node("EL16"));

        DefaultMutableTreeNode lateralOffsetDec = node("LateralOffsetDec");
        sensor.add(lateralOffsetDec);
        lateralOffsetDec.add(node("LO00"));
        lateralOffsetDec.add(node("LO07"));
        lateralOffsetDec.add(node("LO14"));

        // SystemArchitecture branch
        DefaultMutableTreeNode sysArch = node("SystemArchitecture");
        pSpec.add(sysArch);

        DefaultMutableTreeNode dnnDec = node("DNNComponentDec");
        sysArch.add(dnnDec);
        dnnDec.add(node("FasterRCNN"));
        dnnDec.add(node("YOLOv2"));

        DefaultMutableTreeNode sysArchDec = node("SysArchDec");
        sysArch.add(sysArchDec);
        sysArchDec.add(node("SafetyMonitor"));

        return root;
    }

    private DefaultMutableTreeNode node(String name) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(name);
        nodeCache.put(name, n);
        return n;
    }

    /**
     * Get a TreePath for a named leaf node by walking up its parent chain.
     * This produces TreePath with DefaultMutableTreeNode elements.
     */
    private TreePath treePath(String leafName) {
        DefaultMutableTreeNode leaf = nodeCache.get(leafName);
        if (leaf == null) {
            throw new IllegalArgumentException("No node named: " + leafName);
        }
        // getPath() returns root-to-leaf array of TreeNode
        javax.swing.tree.TreeNode[] nodes = leaf.getPath();
        return new TreePath(nodes);
    }

    private Multimap<TreePath, String> buildVarMap(DefaultMutableTreeNode root) {
        Multimap<TreePath, String> varMap = ArrayListMultimap.create();

        // Variable format: "name,type,default,lowerBound,upperBound,comment"
        // (6 comma-separated fields, matching TreePopup.java line 142-144)

        // --- Sensor > DistanceDec (from paper Table II) ---
        addVar(varMap, "DS10", "distance_m,double,11.0,10.0,12.0,Camera-to-sign distance in meters");
        addVar(varMap, "DS12", "distance_m,double,13.0,12.0,14.0,Camera-to-sign distance in meters");
        addVar(varMap, "DS14", "distance_m,double,15.0,14.0,16.0,Camera-to-sign distance in meters");

        // --- Sensor > ElevationDec ---
        addVar(varMap, "EL10", "elevation_m,double,1.15,1.0,1.3,Camera height above ground level");
        addVar(varMap, "EL13", "elevation_m,double,1.45,1.3,1.6,Camera height above ground level");
        addVar(varMap, "EL16", "elevation_m,double,1.75,1.6,1.9,Camera height above ground level");

        // --- Sensor > LateralOffsetDec ---
        addVar(varMap, "LO00", "lateral_offset_m,double,0.35,0.0,0.7,Camera lateral offset from sign centerline");
        addVar(varMap, "LO07", "lateral_offset_m,double,1.05,0.7,1.4,Camera lateral offset from sign centerline");
        addVar(varMap, "LO14", "lateral_offset_m,double,1.7,1.4,2.0,Camera lateral offset from sign centerline");

        // --- Environment > WeatherDec (visibility per RVR/ICAO standards) ---
        addVar(varMap, "FairWeather",
                "visibility_m,double,10000.0,5000.0,15000.0,Meteorological visibility in meters",
                "precipitation_mm_h,double,0.0,0.0,0.0,Precipitation rate mm/h");
        addVar(varMap, "RainyWeather",
                "visibility_m,double,3000.0,1000.0,5000.0,Reduced visibility due to rain",
                "precipitation_mm_h,double,5.0,1.0,20.0,Rain intensity mm/h");
        addVar(varMap, "SnowyWeather",
                "visibility_m,double,2000.0,500.0,5000.0,Reduced visibility due to snow",
                "precipitation_mm_h,double,3.0,0.5,10.0,Snowfall rate mm/h water equivalent");
        addVar(varMap, "FoggyWeather",
                "visibility_m,double,800.0,200.0,2000.0,Fog visibility per CAT II/III minima",
                "precipitation_mm_h,double,0.0,0.0,0.0,No precipitation in fog");

        // --- Environment > TimeOfDayDec (sun elevation & illuminance) ---
        addVar(varMap, "Morning",
                "sun_elevation_deg,double,15.0,5.0,30.0,Sun angle above horizon",
                "illuminance_lux,double,10000.0,1000.0,25000.0,Ambient light level");
        addVar(varMap, "Afternoon",
                "sun_elevation_deg,double,45.0,30.0,70.0,Sun angle above horizon",
                "illuminance_lux,double,50000.0,25000.0,100000.0,Peak daylight illuminance");
        addVar(varMap, "Dusk",
                "sun_elevation_deg,double,-3.0,-6.0,0.0,Civil twilight sun angle",
                "illuminance_lux,double,400.0,40.0,1000.0,Low-light twilight conditions");
        addVar(varMap, "Dawn",
                "sun_elevation_deg,double,2.0,-6.0,10.0,Pre-sunrise sun angle",
                "illuminance_lux,double,500.0,40.0,5000.0,Early morning light level");

        // --- Environment > AirportDec (ICAO airport data) ---
        addVar(varMap, "KSFO",
                "icao_code,string,KSFO,KSFO,KSFO,San Francisco International",
                "latitude_deg,double,37.6213,37.6213,37.6213,Airport latitude",
                "longitude_deg,double,-122.379,-122.379,-122.379,Airport longitude",
                "elevation_ft,double,13.0,13.0,13.0,Airport elevation AMSL");
        addVar(varMap, "KBOS",
                "icao_code,string,KBOS,KBOS,KBOS,Boston Logan International",
                "latitude_deg,double,42.3656,42.3656,42.3656,Airport latitude",
                "longitude_deg,double,-71.0096,-71.0096,-71.0096,Airport longitude",
                "elevation_ft,double,20.0,20.0,20.0,Airport elevation AMSL");
        addVar(varMap, "KSAN",
                "icao_code,string,KSAN,KSAN,KSAN,San Diego International",
                "latitude_deg,double,32.7336,32.7336,32.7336,Airport latitude",
                "longitude_deg,double,-117.19,-117.19,-117.19,Airport longitude",
                "elevation_ft,double,17.0,17.0,17.0,Airport elevation AMSL");

        // --- SystemArchitecture > DNNComponentDec (DNN parameters from paper) ---
        addVar(varMap, "FasterRCNN",
                "backbone,string,ResNet50,ResNet50,ResNet50,CNN backbone architecture",
                "input_resolution_px,int,800,600,1200,Input image size in pixels",
                "confidence_threshold,double,0.95,0.5,0.99,Detection confidence threshold",
                "iou_threshold,double,0.95,0.3,0.99,IoU threshold for NMS",
                "num_layers,int,50,50,50,ResNet depth");
        addVar(varMap, "YOLOv2",
                "backbone,string,DarkNet19,DarkNet19,DarkNet19,CNN backbone architecture",
                "input_resolution_px,int,448,224,448,Input image size in pixels",
                "confidence_threshold,double,0.95,0.5,0.99,Detection confidence threshold",
                "anchor_h_px,int,15,15,15,Anchor box height in pixels",
                "num_layers,int,19,19,19,DarkNet depth");

        // --- SystemArchitecture > SysArchDec > SafetyMonitor ---
        addVar(varMap, "SafetyMonitor",
                "iou_divergence_threshold,double,0.32,0.1,0.5,IoU threshold for output inhibition",
                "availability_target_pct,double,95.0,90.0,99.0,System availability target percent");

        return varMap;
    }

    private void addVar(Multimap<TreePath, String> map, String nodeName, String... vars) {
        TreePath tp = treePath(nodeName);
        for (String v : vars) {
            map.put(tp, v);
        }
    }

    private void serialize(String path, Object obj) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(obj);
        }
        System.out.println("Written: " + path);
    }

    private Path findProjectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        while (dir != null) {
            if (dir.resolve("pom.xml").toFile().exists()
                    && dir.resolve("examples/RunwaySignClassifier").toFile().exists()) {
                return dir;
            }
            dir = dir.getParent();
        }
        return Paths.get("/Users/umutdurak/Code/ODME");
    }
}
