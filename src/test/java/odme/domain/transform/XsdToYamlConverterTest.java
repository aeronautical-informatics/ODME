package odme.domain.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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

    // ── Simple conversion ─────────────────────────────────────────────────────

    @Test
    void convert_singleNodeWithVariable_producesValidYaml() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Vehicle", "Node", null, null, null},
                new String[]{"    -speed", "Variable", "int", "0", "200"}
        );

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("Vehicle:");
        assertThat(yaml).contains("- speed:");
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

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("Root:");
        assertThat(yaml).contains("  Child:");
        assertThat(yaml).contains("    GrandChild:");
    }

    // ── Node without variables ───────────────────────────────────────────────

    @Test
    void convert_nodeWithoutVariables_producesNodeOnly() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"Engine", "Node", null, null, null});

        String yaml = converter.convert(rows);

        assertThat(yaml).isEqualTo("Engine:\n");
    }

    // ── Variables with and without bounds ─────────────────────────────────────

    @Test
    void convert_variableWithoutBounds_omitsMinMax() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Config", "Node", null, null, null},
                new String[]{"    -label", "Variable", "string", null, null}
        );

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("- label:");
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

        String yaml = converter.convert(rows);

        assertThat(yaml).doesNotContain("min:");
        assertThat(yaml).doesNotContain("max:");
    }

    @Test
    void convert_variableWithOnlyMin_omitsMax() {
        List<String[]> rows = Arrays.asList(
                new String[]{"Sensor", "Node", null, null, null},
                new String[]{"    -temp", "Variable", "double", "0.0", null}
        );

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("min: 0.0");
        assertThat(yaml).doesNotContain("max:");
    }

    // ── Empty rows ────────────────────────────────────────────────────────────

    @Test
    void convert_emptyList_producesEmptyString() {
        String yaml = converter.convert(List.of());

        assertThat(yaml).isEmpty();
    }

    @Test
    void convert_rowWithBlankName_isSkipped() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"   ", "Node", null, null, null});

        String yaml = converter.convert(rows);

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

        String yaml = converter.convert(rows);

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

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("- temperature:");
        assertThat(yaml).contains("type: double");
        assertThat(yaml).contains("min: -40");
        assertThat(yaml).contains("max: 150");
        assertThat(yaml).contains("- humidity:");
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

        String yaml = converter.convert(rows);

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

        String yaml = converter.convert(rows);

        assertThat(yaml).contains("Vehicle:");
        assertThat(yaml).contains("- speed:");
        assertThat(yaml).contains("  Engine:");
        assertThat(yaml).contains("- rpm:");
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
}
