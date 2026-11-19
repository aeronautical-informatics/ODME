package odme.odmeeditor;

import odme.core.EditorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticScenarioGenerationSpecializationTest {
    @TempDir
    Path directory;

    @Test
    void nestedSpecializationOnlyExpandsInItsSelectedBranch() throws Exception {
        checkCombinations("<OuterSpec><A><InnerSpec><X> </X><Y> </Y></InnerSpec></A><B> </B></OuterSpec>", 3);
    }

    @Test
    void independentSpecializationsStillFormACrossProduct() throws Exception {
        checkCombinations("<ChoicesDec><FirstSpec><A> </A><B> </B></FirstSpec>"
                + "<SecondSpec><X> </X><Y> </Y><Z> </Z></SecondSpec></ChoicesDec>", 6);
    }

    @Test
    void nestedAndIndependentSpecializationsHaveNoDuplicates() throws Exception {
        checkCombinations("<OuterSpec><A><InnerSpec><X> </X><Y> </Y></InnerSpec></A><B> </B></OuterSpec>"
                + "<OtherSpec><P> </P><Q> </Q></OtherSpec>", 6);
    }

    @Test
    void threeLevelsAndRepeatedSpecializationNamesUseTheirFullPaths() throws Exception {
        checkCombinations("<OuterSpec><A><InnerSpec><X><DeepSpec><P> </P><Q> </Q></DeepSpec></X>"
                + "<Y> </Y></InnerSpec></A><B><InnerSpec><X> </X><Y> </Y></InnerSpec></B></OuterSpec>", 5);
    }

    @Test
    void noSpecializationsProducesOnePes() throws Exception {
        checkCombinations("<Aircraft> </Aircraft>", 1);
    }

    @Test
    void previewSaturatesAndEmptySpecializationStillFailsClearly() throws Exception {
        String oldLocation = EditorContext.getInstance().getFileLocation();
        String oldProject = EditorContext.getInstance().getProjName();
        try {
            Path project = Files.createDirectories(directory.resolve("Demo"));
            EditorContext.getInstance().setFileLocation(directory.toString());
            EditorContext.getInstance().setProjName("Demo");
            StringBuilder tree = new StringBuilder("<Scenario>");
            for (int index = 0; index < 15; index++) {
                tree.append("<Choice").append(index).append("Spec><A> </A><B> </B></Choice")
                        .append(index).append("Spec>");
            }
            Files.writeString(project.resolve("Demo.xml"), tree + "</Scenario>");
            assertEquals(10001, AutomaticScenarioGeneration.inspectProject().totalCombinations());
            Files.writeString(project.resolve("Demo.xml"), "<Scenario><EmptySpec> </EmptySpec></Scenario>");
            assertTrue(assertThrows(IllegalStateException.class,
                    () -> AutomaticScenarioGeneration.generateAll("Empty", 1))
                    .getMessage().contains("Specialization node has no options: EmptySpec"));
        } finally {
            EditorContext.getInstance().setFileLocation(oldLocation);
            EditorContext.getInstance().setProjName(oldProject);
        }
    }

    private void checkCombinations(String tree, int expectedCount) throws Exception {
        Path project = Files.createDirectories(directory.resolve("Demo"));
        Files.writeString(project.resolve("Demo.xml"), "<Scenario>" + tree + "</Scenario>");
        EditorContext context = EditorContext.getInstance();
        String oldLocation = context.getFileLocation();
        String oldProject = context.getProjName();
        String oldMode = context.getToolMode();
        try {
            context.setFileLocation(directory.toString());
            context.setProjName("Demo");
            context.setToolMode("ses");
            assertEquals(expectedCount, AutomaticScenarioGeneration.inspectProject().totalCombinations());
            var cartesian = AutomaticScenarioGeneration.inspectCartesianProject(2);
            assertEquals(expectedCount, cartesian.structuralCombinationCount());
            assertEquals(expectedCount, cartesian.projectedScenarioModels());

            var lhs = AutomaticScenarioGeneration.generateAll("Lhs", 1);
            var product = AutomaticScenarioGeneration.generateCartesian("Cartesian", 2);
            assertEquals(expectedCount, lhs.structuralCombinationCount());
            assertEquals(expectedCount, lhs.createdCount());
            assertEquals(expectedCount, product.createdCount());
            for (var result : new AutomaticScenarioGeneration.GenerationResult[]{lhs, product}) {
                var trees = new HashSet<String>();
                for (String scenario : result.createdScenarioNames()) {
                    trees.add(Files.readString(project.resolve(scenario).resolve("Demo.xml")));
                }
                assertEquals(expectedCount, trees.size());
            }
        } finally {
            context.setFileLocation(oldLocation);
            context.setProjName(oldProject);
            context.setToolMode(oldMode);
        }
    }
}
