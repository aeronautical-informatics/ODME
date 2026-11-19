package odme.domain.transform;

import odme.sampling.ScenarioParser;
import odme.sampling.model.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XsdToYamlConverter}.
 */
class XsdToYamlConverterTest {

    private XsdToYamlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new XsdToYamlConverter();
    }

    private String convertAndParse(List<String[]> rows) {
        String yaml = converter.convert(rows);
        new org.yaml.snakeyaml.Yaml().load(yaml);
        return yaml;
    }

    // ── Simple conversion ─────────────────────────────────────────────────────

    @Test
    void convert_singleNodeWithVariable_producesValidYaml() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Vehicle", "Node", null, null, null},
                new String[]{"    -speed", "Variable", "int", "0", "200"}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("Vehicle:");
        assertThat(yaml).contains("speed:");
        assertThat(yaml).contains("type: int");
        assertThat(yaml).contains("min: 0");
        assertThat(yaml).contains("max: 200");
    }

    // ── Indentation ──────────────────────────────────────────────────────────

    @Test
    void convert_nestedNodes_producesCorrectIndentation() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Root", "Node", null, null, null},
                new String[]{"  Child", "Node", null, null, null},
                new String[]{"    GrandChild", "Node", null, null, null}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("Root:");
        assertThat(yaml).contains("  Child:");
        assertThat(yaml).contains("    GrandChild:");
    }

    // ── Node without variables ───────────────────────────────────────────────

    @Test
    void convert_nodeWithoutVariables_producesNodeOnly() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"Engine", "Node", null, null, null});

        String yaml = convertAndParse(rows);

        assertThat(yaml).isEqualTo("Engine:\n");
    }

    // ── Variables with and without bounds ─────────────────────────────────────

    @Test
    void convert_variableWithoutBounds_omitsMinMax() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Config", "Node", null, null, null},
                new String[]{"    -label", "Variable", "string", null, null}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("label:");
        assertThat(yaml).contains("type: string");
        assertThat(yaml).doesNotContain("min:");
        assertThat(yaml).doesNotContain("max:");
    }

    @Test
    void convert_variableWithEmptyBounds_omitsMinMax() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Config", "Node", null, null, null},
                new String[]{"    -active", "Variable", "boolean", "", ""}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).doesNotContain("min:");
        assertThat(yaml).doesNotContain("max:");
    }

    @Test
    void convert_variableWithOnlyMin_omitsMax() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Sensor", "Node", null, null, null},
                new String[]{"    -temp", "Variable", "double", "0.0", null}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("min: 0.0");
        assertThat(yaml).doesNotContain("max:");
    }

    // ── Empty rows ────────────────────────────────────────────────────────────

    @Test
    void convert_emptyList_producesEmptyString() {
        String yaml = convertAndParse(List.of());

        assertThat(yaml).isEmpty();
    }

    @Test
    void convert_rowWithBlankName_isSkipped() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"   ", "Node", null, null, null});

        String yaml = convertAndParse(rows);

        assertThat(yaml).isEmpty();
    }

    // ── countLeadingSpaces ───────────────────────────────────────────────────

    @Test
    void countLeadingSpaces_returnsCorrectCount() {
        assertThat(XsdToYamlConverter.countLeadingSpaces("  hello")).isEqualTo(2);
        assertThat(XsdToYamlConverter.countLeadingSpaces("hello")).isEqualTo(0);
        assertThat(XsdToYamlConverter.countLeadingSpaces("    x")).isEqualTo(4);
    }

    // ── getIndent ────────────────────────────────────────────────────────────

    @Test
    void getIndent_producesCorrectSpaces() {
        assertThat(XsdToYamlConverter.getIndent(0)).isEmpty();
        assertThat(XsdToYamlConverter.getIndent(1)).isEqualTo("  ");
        assertThat(XsdToYamlConverter.getIndent(3)).isEqualTo("      ");
    }

    @Test
    void getIndent_negativeLevel_producesEmpty() {
        assertThat(XsdToYamlConverter.getIndent(-1)).isEmpty();
    }

    // ── Sibling nodes at same level ─────────────────────────────────────────

    @Test
    void convert_siblingNodes_sameIndentLevel() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Root", "Node", null, null, null},
                new String[]{"  ChildA", "Node", null, null, null},
                new String[]{"  ChildB", "Node", null, null, null}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("Root:");
        assertThat(yaml).contains("  ChildA:");
        assertThat(yaml).contains("  ChildB:");
    }

    // ── Node with multiple variables ────────────────────────────────────────

    @Test
    void convert_nodeWithMultipleVariables_allRendered() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Sensor", "Node", null, null, null},
                new String[]{"    -temperature", "Variable", "double", "-40", "150"},
                new String[]{"    -humidity", "Variable", "int", "0", "100"}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("temperature:");
        assertThat(yaml).contains("type: double");
        assertThat(yaml).contains("min: -40");
        assertThat(yaml).contains("max: 150");
        assertThat(yaml).contains("humidity:");
        assertThat(yaml).contains("type: int");
        assertThat(yaml).contains("min: 0");
        assertThat(yaml).contains("max: 100");
    }

    // ── Pop back to parent level ────────────────────────────────────────────

    @Test
    void convert_deepThenShallow_indentStackPopsCorrectly() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Root", "Node", null, null, null},
                new String[]{"  Child", "Node", null, null, null},
                new String[]{"    GrandChild", "Node", null, null, null},
                new String[]{"  Sibling", "Node", null, null, null}  // pops back to child level
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("Root:");
        assertThat(yaml).contains("  Child:");
        assertThat(yaml).contains("    GrandChild:");
        assertThat(yaml).contains("  Sibling:");
    }

    // ── Mixed nodes and variables ────────────────────────────────────────────

    @Test
    void convert_mixedNodesAndVariables_correctStructure() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Vehicle", "Node", null, null, null},
                new String[]{"    -speed", "Variable", "int", "0", "200"},
                new String[]{"  Engine", "Node", null, null, null},
                new String[]{"      -rpm", "Variable", "int", "0", "8000"}
        );

        String yaml = convertAndParse(rows);

        assertThat(yaml).contains("Vehicle:");
        assertThat(yaml).contains("speed:");
        assertThat(yaml).contains("  Engine:");
        assertThat(yaml).contains("rpm:");
    }

    // ── countLeadingSpaces edge cases ───────────────────────────────────────

    @Test
    void countLeadingSpaces_emptyString_returnsZero() {
        assertThat(XsdToYamlConverter.countLeadingSpaces("")).isEqualTo(0);
    }

    @Test
    void countLeadingSpaces_allSpaces_returnsLength() {
        assertThat(XsdToYamlConverter.countLeadingSpaces("     ")).isEqualTo(5);
    }

    @Test
    void countLeadingSpaces_tabsNotCounted_returnsZero() {
        assertThat(XsdToYamlConverter.countLeadingSpaces("\thello")).isEqualTo(0);
    }

    // ── Valid YAML ───────────────────────────────────────────────────────────

    @Test
    void convert_variablesBelowIndentedNodes_parsesAsYamlUnderTheirNode() {
        // Shape produced by XsdParser: variable rows are less indented than their node row.
        List<String[]> rows = Arrays.asList(
                new String[]{"Domain", "Node", null, null, null, null},
                new String[]{"  TimeOfDay", "Node", null, null, null, null},
                new String[]{"    Dawn", "Node", null, null, null, "ODD time of day: dawn"},
                new String[]{"   -sun_elevation_deg", "Variable", "double", "0", "10", null},
                new String[]{"    Dusk", "Node", null, null, null, null},
                new String[]{"   -sun_elevation_deg", "Variable", "double", "0", "10", "narrowed: \"low sun\""},
                new String[]{"  Camera", "Node", null, null, null, null},
                new String[]{"  -degradation", "Variable", "boolean", null, null, "with / without"},
                new String[]{"  -mode", "Variable", null, null, null, null}
        );

        String yaml = convertAndParse(rows);
        Map<String, Object> root = new org.yaml.snakeyaml.Yaml().load(yaml);

        Map<?, ?> domain = (Map<?, ?>) root.get("Domain");
        Map<?, ?> dawn = (Map<?, ?>) ((Map<?, ?>) domain.get("TimeOfDay")).get("Dawn");
        assertThat(dawn.get("comment")).isEqualTo("ODD time of day: dawn");
        Map<?, ?> elevation = (Map<?, ?>) dawn.get("sun_elevation_deg");
        assertThat(elevation.get("type")).isEqualTo("double");
        assertThat(elevation.get("min")).isEqualTo(0);
        assertThat(elevation.get("max")).isEqualTo(10);
        Map<?, ?> dusk = (Map<?, ?>) ((Map<?, ?>) domain.get("TimeOfDay")).get("Dusk");
        assertThat(((Map<?, ?>) dusk.get("sun_elevation_deg")).get("comment")).isEqualTo("narrowed: \"low sun\"");
        Map<?, ?> camera = (Map<?, ?>) domain.get("Camera");
        assertThat(((Map<?, ?>) camera.get("degradation")).get("type")).isEqualTo("boolean");
        assertThat(((Map<?, ?>) camera.get("mode")).get("type")).isEqualTo("string");
    }
    @Test
    void convert_nestedXsd_roundTripsAllVariableTypes(@TempDir Path directory) throws Exception {
        String xsd = """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Domain">
                    <xs:complexType><xs:sequence>
                      <xs:element name="Aircraft">
                        <xs:complexType><xs:sequence>
                          <xs:element name="Sensor">
                            <xs:complexType>
                              <xs:attribute name="count"><xs:simpleType><xs:restriction base="xs:int">
                                <xs:minInclusive value="0"/><xs:maxInclusive value="10"/>
                              </xs:restriction></xs:simpleType></xs:attribute>
                              <xs:attribute name="distance"><xs:simpleType><xs:restriction base="xs:double">
                                <xs:minInclusive value="0.5"/><xs:maxInclusive value="12.5"/>
                              </xs:restriction></xs:simpleType></xs:attribute>
                              <xs:attribute name="enabled" type="xs:boolean"/>
                              <xs:attribute name="label" type="xs:string">
                                <xs:annotation><xs:documentation>Sensor: "primary"</xs:documentation></xs:annotation>
                              </xs:attribute>
                              <xs:attribute name="procedure"/>
                            </xs:complexType>
                          </xs:element>
                          <xs:element name="EmptyNode">
                            <xs:annotation><xs:documentation>No variables here.</xs:documentation></xs:annotation>
                            <xs:complexType/>
                          </xs:element>
                        </xs:sequence></xs:complexType>
                      </xs:element>
                    </xs:sequence></xs:complexType>
                  </xs:element>
                </xs:schema>
                """;
        List<String[]> rows = new XsdParser().readXsd(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        String yaml = convertAndParse(rows);
        Path exported = directory.resolve("model.yaml");
        Files.writeString(exported, yaml);

        List<Parameter> parameters = new ScenarioParser().parse(exported.toString()).getParameters();

        assertThat(parameters).extracting(Parameter::getName).containsExactly(
                "Aircraft_Sensor_count", "Aircraft_Sensor_distance", "Aircraft_Sensor_enabled", "Aircraft_Sensor_label",
                "Aircraft_Sensor_procedure");
        assertThat(parameters).extracting(Parameter::getType)
                .containsExactly("int", "double", "boolean", "string", "string");
        assertThat(parameters.get(0).getMin()).isEqualTo(0.0);
        assertThat(parameters.get(0).getMax()).isEqualTo(10.0);
        assertThat(parameters.get(1).getMin()).isEqualTo(0.5);
        assertThat(parameters.get(1).getMax()).isEqualTo(12.5);
        Map<?, ?> root = new org.yaml.snakeyaml.Yaml().load(yaml);
        Map<?, ?> aircraft = (Map<?, ?>) ((Map<?, ?>) root.get("Domain")).get("Aircraft");
        assertThat(((Map<?, ?>) aircraft.get("EmptyNode")).get("comment")).isEqualTo("No variables here.");
        Map<?, ?> label = (Map<?, ?>) ((Map<?, ?>) aircraft.get("Sensor")).get("label");
        assertThat(label.get("comment")).isEqualTo("Sensor: \"primary\"");
    }

    @Test
    void convert_annotationEscapesPreserveText() {
        String annotation = "first line\nsecond line\twith \"quotes\" and \\slashes";
        String yaml = convertAndParse(Arrays.asList(
                new String[]{"Domain", "Node", null, null, null, annotation},
                new String[]{"  -flag", "Variable", "boolean", null, null, "null"}));
        Map<?, ?> root = new org.yaml.snakeyaml.Yaml().load(yaml);
        Map<?, ?> domain = (Map<?, ?>) root.get("Domain");

        assertThat(domain.get("comment")).isEqualTo(annotation);
        assertThat(((Map<?, ?>) domain.get("flag")).get("comment")).isEqualTo("null");
    }

    @Test
    void convert_annotatedSpecializationPreservesCategoricalOptions(@TempDir Path directory) throws Exception {
        String xsd = """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Domain"><xs:complexType><xs:sequence>
                    <xs:element name="Weather">
                      <xs:annotation><xs:documentation>Weather decision</xs:documentation></xs:annotation>
                      <xs:complexType><xs:choice>
                        <xs:element name="Rain">
                          <xs:annotation><xs:documentation>Rain option</xs:documentation></xs:annotation>
                          <xs:complexType/>
                        </xs:element>
                        <xs:element name="Snow"><xs:complexType/></xs:element>
                      </xs:choice></xs:complexType>
                    </xs:element>
                  </xs:sequence></xs:complexType></xs:element>
                </xs:schema>
                """;
        List<String[]> rows = new XsdParser().readXsd(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        Path yaml = directory.resolve("weather.yaml");
        Files.writeString(yaml, convertAndParse(rows));

        List<Parameter> parameters = new ScenarioParser().parse(yaml.toString()).getParameters();

        assertThat(parameters).hasSize(1);
        assertThat(parameters.get(0).getName()).isEqualTo("Weather");
        assertThat(parameters.get(0).getType()).isEqualTo("categorical");
        assertThat(parameters.get(0).getOptions()).containsExactly("Rain", "Snow");
    }
}
