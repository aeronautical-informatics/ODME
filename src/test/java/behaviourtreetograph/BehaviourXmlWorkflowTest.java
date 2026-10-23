package behaviourtreetograph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviourXmlWorkflowTest {

    @TempDir
    Path tempDir;

    @Test
    void exportsActionAttributesAndMergesTreeInsideBehaviourElement() throws Exception {
        Path graphXml = tempDir.resolve("behaviourxml.xml");
        Path behaviourXml = tempDir.resolve("OutputBehaviourxml.xml");
        Path scenarioXml = tempDir.resolve("xmlforxsd.xml");
        Path mergedXml = tempDir.resolve("MergedXMLforxsd.xml");

        Files.writeString(graphXml, """
                <start>
                  <Events>
                    <initializeRunwayDetection Attributes="source = probe"/>
                  </Events>
                </start>
                """);
        Files.writeString(scenarioXml, """
                <entity name="Scenario">
                  <entity name="Runway"/>
                </entity>
                """);

        BehaviourXmlWorkflow.exportBehaviourTree(graphXml, behaviourXml);
        BehaviourXmlWorkflow.mergeIntoScenario(scenarioXml, behaviourXml, mergedXml);

        Document behaviour = parse(behaviourXml);
        Element action = (Element) behaviour.getElementsByTagName("Action").item(0);
        assertThat(action.getAttribute("name")).isEqualTo("initializeRunwayDetection");
        assertThat(action.getAttribute("Attributes")).isEqualTo("source = probe");

        Document merged = parse(mergedXml);
        assertThat(merged.getDocumentElement().getTagName()).isEqualTo("entity");
        assertThat(merged.getElementsByTagName("behaviour").getLength()).isEqualTo(1);
        Element wrapper = (Element) merged.getElementsByTagName("behaviour").item(0);
        assertThat(wrapper.getAttribute("name")).isEqualTo("MainTree");
        assertThat(wrapper.getElementsByTagName("root").getLength()).isEqualTo(1);
        assertThat(wrapper.getElementsByTagName("Action").getLength()).isEqualTo(1);
        assertThat(merged.getDocumentElement().getElementsByTagName("root").getLength()).isEqualTo(1);
    }

    @Test
    void acceptsLegacyNestedStartWrapperWithoutDuplicatingTheTree() throws Exception {
        Path graphXml = tempDir.resolve("legacy-behaviourxml.xml");
        Path behaviourXml = tempDir.resolve("legacy-OutputBehaviourxml.xml");
        Files.writeString(graphXml, """
                <start><start><Events><monitorApproach/></Events></start></start>
                """);

        BehaviourXmlWorkflow.exportBehaviourTree(graphXml, behaviourXml);

        Document behaviour = parse(behaviourXml);
        assertThat(behaviour.getElementsByTagName("root").getLength()).isEqualTo(1);
        assertThat(behaviour.getElementsByTagName("BehaviourTree").getLength()).isEqualTo(1);
        assertThat(behaviour.getElementsByTagName("Action").getLength()).isEqualTo(1);
    }

    private Document parse(Path xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.toFile());
    }
}
