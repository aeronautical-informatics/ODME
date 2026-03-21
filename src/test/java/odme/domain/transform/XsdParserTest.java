package odme.domain.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XsdParser}.
 */
class XsdParserTest {

    private XsdParser parser;

    @BeforeEach
    void setUp() {
        parser = new XsdParser();
    }

    private InputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    // ── Simple XSD ───────────────────────────────────────────────────────────

    @Test
    void readXsd_simpleElement_returnsNodeRow() throws Exception {
        String xsd = "<?xml version=\"1.0\"?>"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "  <xs:element name=\"Vehicle\">"
                + "    <xs:complexType/>"
                + "  </xs:element>"
                + "</xs:schema>";

        List<String[]> rows = parser.readXsd(toStream(xsd));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0].trim()).isEqualTo("Vehicle");
        assertThat(rows.get(0)[1]).isEqualTo("Node");
    }

    // ── Nested elements ──────────────────────────────────────────────────────

    @Test
    void readXsd_nestedElements_returnsMultipleRows() throws Exception {
        String xsd = "<?xml version=\"1.0\"?>"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "  <xs:element name=\"Vehicle\">"
                + "    <xs:complexType>"
                + "      <xs:sequence>"
                + "        <xs:element name=\"Engine\">"
                + "          <xs:complexType/>"
                + "        </xs:element>"
                + "      </xs:sequence>"
                + "    </xs:complexType>"
                + "  </xs:element>"
                + "</xs:schema>";

        List<String[]> rows = parser.readXsd(toStream(xsd));

        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);

        List<String> names = rows.stream()
                .map(r -> r[0].trim())
                .toList();
        assertThat(names).contains("Vehicle", "Engine");
    }

    // ── Attributes with restrictions ─────────────────────────────────────────

    @Test
    void readXsd_attributeWithRestriction_extractsMinMax() throws Exception {
        String xsd = "<?xml version=\"1.0\"?>"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "  <xs:element name=\"Sensor\">"
                + "    <xs:complexType>"
                + "      <xs:attribute name=\"temperature\">"
                + "        <xs:simpleType>"
                + "          <xs:restriction base=\"xs:int\">"
                + "            <xs:minInclusive value=\"-40\"/>"
                + "            <xs:maxInclusive value=\"150\"/>"
                + "          </xs:restriction>"
                + "        </xs:simpleType>"
                + "      </xs:attribute>"
                + "    </xs:complexType>"
                + "  </xs:element>"
                + "</xs:schema>";

        List<String[]> rows = parser.readXsd(toStream(xsd));

        // Should have the node row and a variable row
        assertThat(rows.size()).isGreaterThanOrEqualTo(2);

        // Find the variable row
        String[] varRow = rows.stream()
                .filter(r -> "Variable".equals(r[1]))
                .findFirst()
                .orElse(null);

        assertThat(varRow).isNotNull();
        assertThat(varRow[0]).contains("temperature");
        assertThat(varRow[2]).isEqualTo("int");
        assertThat(varRow[3]).isEqualTo("-40");
        assertThat(varRow[4]).isEqualTo("150");
    }

    // ── Indent calculation ───────────────────────────────────────────────────

    @Test
    void indentStr_singleSpace_indentsCorrectly() {
        String result = parser.indentStr("hello", 3);
        assertThat(result).isEqualTo("   hello");
    }

    @Test
    void indentStr_customIndent_indentsCorrectly() {
        String result = parser.indentStr("node", 2, "  ");
        assertThat(result).isEqualTo("    node");
    }

    @Test
    void indentStr_zeroLevel_noIndentation() {
        String result = parser.indentStr("root", 0);
        assertThat(result).isEqualTo("root");
    }

    // ── getNodeHeaders ───────────────────────────────────────────────────────

    @Test
    void readXsd_nodeHeaders_haveFiveElements() throws Exception {
        String xsd = "<?xml version=\"1.0\"?>"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "  <xs:element name=\"Root\">"
                + "    <xs:complexType/>"
                + "  </xs:element>"
                + "</xs:schema>";

        List<String[]> rows = parser.readXsd(toStream(xsd));
        String[] nodeRow = rows.get(0);

        assertThat(nodeRow).hasSize(5);
        assertThat(nodeRow[1]).isEqualTo("Node");
        // Remaining fields should be null for node headers
        assertThat(nodeRow[2]).isNull();
        assertThat(nodeRow[3]).isNull();
        assertThat(nodeRow[4]).isNull();
    }
}
