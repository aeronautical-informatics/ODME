package odme.core;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstraintSchemaExportTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {
            "@a > 5 and @b < 3",
            "@a > 5 && @b < 3",
            "if (@a >= 5) then (@b <= 3) else true()",
            "@a div 2 = 3 or @b mod 3 = 1"
    })
    void schemaExportPreservesComparisonAndBooleanText(String constraint) throws Exception {
        EditorContext context = EditorContext.getInstance();
        String oldLocation = context.getFileLocation();
        String oldProject = context.getProjName();
        String oldMode = context.getToolMode();
        try {
            context.setFileLocation(directory.toString());
            context.setProjName("Model");
            context.setToolMode("ses");
            Path project = Files.createDirectories(directory.resolve("Model"));
            Files.writeString(project.resolve("outputgraphxmlforxsd.xml"),
                    "<?xml version=\"1.0\"?>\n<Root>\n<" + constraint + "Con/>\n</Root>\n");
            new FileConvertion().xmlToXSDConversion();
            var xsd = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(project.resolve("xsdfromxml.xsd").toFile());
            var assertion = (org.w3c.dom.Element) xsd.getElementsByTagName("xs:assert").item(0);
            assertEquals(constraint, assertion.getAttribute("test"));

            Files.writeString(directory.resolve("xmlforxsd.xml"),
                    "<?xml version=\"1.0\"?>\n<entity name=\"Root\" id=\"keep-this\">\n</entity>\n");
            new FileConvertion().addConstraintToSESStructure(new String[]{"entity"}, constraint);
            var intermediate = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(directory.resolve("testcon.xml").toFile());
            assertEquals(constraint, intermediate.getDocumentElement().getAttribute("constraint"));
            assertEquals("keep-this", intermediate.getDocumentElement().getAttribute("id"));
        } finally {
            context.setFileLocation(oldLocation);
            context.setProjName(oldProject);
            context.setToolMode(oldMode);
        }
    }
}
