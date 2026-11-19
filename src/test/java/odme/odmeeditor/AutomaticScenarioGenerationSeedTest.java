package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import odme.core.EditorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.TreePath;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomaticScenarioGenerationSeedTest {
    @TempDir
    Path directory;

    @Test
    void sameSeedProducesIdenticalScenarioFilesAcrossProjects() throws Exception {
        Path first = createProject("first");
        Path replay = createProject("replay");
        EditorContext context = EditorContext.getInstance();
        String oldLocation = context.getFileLocation();
        String oldProject = context.getProjName();
        String oldMode = context.getToolMode();
        try {
            context.setProjName("Demo");
            context.setToolMode("ses");
            context.setFileLocation(first.getParent().toString());
            var expected = AutomaticScenarioGeneration.generateAll("Seeded", 3, 123L);
            context.setFileLocation(replay.getParent().toString());
            var actual = AutomaticScenarioGeneration.generateAll("Seeded", 3, 123L);

            assertEquals(123L, expected.seed());
            assertEquals(6, expected.createdCount());
            assertEquals(expected.createdScenarioNames(), actual.createdScenarioNames());
            for (String scenario : expected.createdScenarioNames()) {
                try (var files = Files.list(first.resolve(scenario))) {
                    for (Path file : files.toList()) {
                        assertArrayEquals(Files.readAllBytes(file),
                                Files.readAllBytes(replay.resolve(scenario).resolve(file.getFileName())),
                                scenario + "/" + file.getFileName());
                    }
                }
            }
        } finally {
            context.setFileLocation(oldLocation);
            context.setProjName(oldProject);
            context.setToolMode(oldMode);
        }
    }

    private Path createProject(String run) throws Exception {
        Path project = Files.createDirectories(directory.resolve(run).resolve("Demo"));
        Files.writeString(project.resolve("Demo.xml"), "<Scenario><ModeSpec><A> </A><B> </B></ModeSpec></Scenario>");
        var variables = ArrayListMultimap.<TreePath, String>create();
        variables.put(new TreePath("Scenario"), "Speed,double,50,0,100");
        variables.put(new TreePath(new String[]{"Scenario", "ModeSpec", "A"}), "Flag,boolean,false");
        variables.put(new TreePath(new String[]{"Scenario", "ModeSpec", "B"}), "Flag,boolean,false");
        var constraints = ArrayListMultimap.<TreePath, String>create();
        constraints.put(new TreePath("Scenario"), "@Speed > 20");
        try (var stream = new ObjectOutputStream(Files.newOutputStream(project.resolve("Demo.ssdvar")))) {
            stream.writeObject(variables);
        }
        try (var stream = new ObjectOutputStream(Files.newOutputStream(project.resolve("Demo.ssdcon")))) {
            stream.writeObject(constraints);
        }
        return project;
    }
}
