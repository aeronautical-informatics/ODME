package odme.examples;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.core.FlagVariables;

import javax.swing.tree.TreePath;
import java.io.*;

/**
 * Utility to generate empty .ssd* files for example projects.
 * Run as: mvn exec:java -Dexec.mainClass=odme.examples.GenerateExampleSsdFiles -Dexec.args=examples/RunwaySignClassifier/RunwaySignClassifier
 */
public class GenerateExampleSsdFiles {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GenerateExampleSsdFiles <basePath>");
            System.err.println("  e.g. examples/RunwaySignClassifier/RunwaySignClassifier");
            System.exit(1);
        }
        String basePath = args[0];

        Multimap<TreePath, String> emptyMap = ArrayListMultimap.create();

        // .ssdvar
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(basePath + ".ssdvar"))) {
            oos.writeObject(emptyMap);
        }
        System.out.println("Created " + basePath + ".ssdvar");

        // .ssdbeh
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(basePath + ".ssdbeh"))) {
            oos.writeObject(emptyMap);
        }
        System.out.println("Created " + basePath + ".ssdbeh");

        // .ssdcon
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(basePath + ".ssdcon"))) {
            oos.writeObject(emptyMap);
        }
        System.out.println("Created " + basePath + ".ssdcon");

        // .ssdflag
        FlagVariables flags = new FlagVariables();
        flags.nodeNumber = 0;
        flags.uniformityNodeNumber = 0;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(basePath + ".ssdflag"))) {
            oos.writeObject(flags);
        }
        System.out.println("Created " + basePath + ".ssdflag");
    }
}
