package odme.domain.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link XmlToPythonTranslator}.
 */
class XmlToPythonTranslatorTest {

    private XmlToPythonTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new XmlToPythonTranslator();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    // ── translate (simple) ────────────────────────────────────────────────────

    @Test
    void translate_simpleElement_producesOutput() throws Exception {
        Document doc = parseXml("<root><greeting>hello</greeting></root>");
        String result = translator.translate(doc);

        assertThat(result).startsWith("# Translated XML to Python");
        assertThat(result).contains("data = {");
        assertThat(result).contains("'root':");
        // Single same-named leaf children are rendered as a list
        assertThat(result).contains("'greeting'");
    }

    @Test
    void translate_leafWithNoContentOrAttributes_appearsInOutput() throws Exception {
        Document doc = parseXml("<root><empty/></root>");
        String result = translator.translate(doc);

        // Single leaf child without attributes is rendered in a list
        assertThat(result).contains("'empty'");
    }

    // ── translate (nested) ────────────────────────────────────────────────────

    @Test
    void translate_nestedElements_producesNestedStructure() throws Exception {
        // Use different tag names for children so they don't collapse into a list
        Document doc = parseXml(
                "<vehicle>"
                + "  <engine>"
                + "    <cylinders>4</cylinders>"
                + "  </engine>"
                + "  <body>"
                + "    <color>red</color>"
                + "  </body>"
                + "</vehicle>");
        String result = translator.translate(doc);

        assertThat(result).contains("'vehicle': {");
        assertThat(result).contains("'engine'");
        assertThat(result).contains("'body'");
    }

    // ── translate (attributes with min/max) ───────────────────────────────────

    @Test
    void translate_leafWithAttributes_producesNestedDict() throws Exception {
        Document doc = parseXml(
                "<root><speed name=\"velocity\" min=\"0\" max=\"100\"/></root>");
        String result = translator.translate(doc);

        assertThat(result).contains("'velocity': {");
        assertThat(result).contains("'min': 0,");
        assertThat(result).contains("'max': 100,");
    }

    // ── translate (same-named leaf children => list) ──────────────────────────

    @Test
    void translate_sameNamedLeafChildren_producesList() throws Exception {
        Document doc = parseXml(
                "<fruits>"
                + "  <item>apple</item>"
                + "  <item>banana</item>"
                + "</fruits>");
        String result = translator.translate(doc);

        // All children have same tag and are leaves, so they form a list
        assertThat(result).contains("[");
    }

    // ── translate (null/empty) ────────────────────────────────────────────────

    @Test
    void translate_nullDocument_throwsIllegalArgument() {
        assertThatThrownBy(() -> translator.translate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── formatValue ──────────────────────────────────────────────────────────

    @Test
    void formatValue_integer_returnsUnquotedNumber() {
        assertThat(translator.formatValue("42")).isEqualTo("42");
    }

    @Test
    void formatValue_double_returnsUnquotedNumber() {
        assertThat(translator.formatValue("3.14")).isEqualTo("3.14");
    }

    @Test
    void formatValue_string_returnsSingleQuoted() {
        assertThat(translator.formatValue("hello")).isEqualTo("'hello'");
    }

    @Test
    void formatValue_stringWithApostrophe_escapesIt() {
        assertThat(translator.formatValue("it's")).isEqualTo("'it\\'s'");
    }

    // ── removeExtension ──────────────────────────────────────────────────────

    @Test
    void removeExtension_removesFileExtension() {
        assertThat(XmlToPythonTranslator.removeExtension("model.xml")).isEqualTo("model");
    }

    @Test
    void removeExtension_noExtension_returnsOriginal() {
        assertThat(XmlToPythonTranslator.removeExtension("model")).isEqualTo("model");
    }

    @Test
    void removeExtension_multiDot_removesOnlyLast() {
        assertThat(XmlToPythonTranslator.removeExtension("my.model.xml")).isEqualTo("my.model");
    }

    // ── hasExtension ─────────────────────────────────────────────────────────

    @Test
    void hasExtension_matchingExtension_returnsTrue() {
        assertThat(XmlToPythonTranslator.hasExtension("model.xml", "xml")).isTrue();
    }

    @Test
    void hasExtension_caseInsensitive() {
        assertThat(XmlToPythonTranslator.hasExtension("model.XML", "xml")).isTrue();
    }

    @Test
    void hasExtension_noMatch_returnsFalse() {
        assertThat(XmlToPythonTranslator.hasExtension("model.json", "xml")).isFalse();
    }

    @Test
    void hasExtension_multipleExtensions_matchesAny() {
        assertThat(XmlToPythonTranslator.hasExtension("file.xsd", "xml", "xsd")).isTrue();
    }

    // ── formatXml ───────────────────────────────────────────────────────────

    @Test
    void formatXml_producesIndentedOutput() throws Exception {
        Document doc = parseXml("<root><child>text</child></root>");
        String result = translator.formatXml(doc);

        assertThat(result).contains("<?xml");
        assertThat(result).contains("<root>");
        assertThat(result).contains("<child>");
    }

    // ── translate edge cases ────────────────────────────────────────────────

    @Test
    void translate_leafNodeWithEmptyTextAndNoAttributes_producesNone() throws Exception {
        // A single empty element with no children, no text, no attributes
        Document doc = parseXml("<root/>");
        String result = translator.translate(doc);

        assertThat(result).contains("'root': None");
    }

    @Test
    void translate_multipleGroupsOfChildren_producesNestedDict() throws Exception {
        // Multiple children with different tag names, some repeated
        Document doc = parseXml(
                "<root>"
                + "  <a>1</a>"
                + "  <a>2</a>"
                + "  <b>3</b>"
                + "</root>");
        String result = translator.translate(doc);

        // Multiple groups: 'a' appears twice -> rendered as list within a dict
        assertThat(result).contains("'root': {");
        assertThat(result).contains("'a': [");
        assertThat(result).contains("'b'");
    }

    @Test
    void translate_nameAttributeUsedAsKey() throws Exception {
        Document doc = parseXml(
                "<root>"
                + "  <item name=\"first\">value1</item>"
                + "  <other name=\"second\">value2</other>"
                + "</root>");
        String result = translator.translate(doc);

        // "name" attribute is used as key instead of tag name
        assertThat(result).contains("'first'");
        assertThat(result).contains("'second'");
    }

    @Test
    void translate_negativeNumber_formattedCorrectly() throws Exception {
        // Use two children with different tag names to avoid list rendering
        Document doc = parseXml("<root><val>-42</val><other>text</other></root>");
        String result = translator.translate(doc);

        assertThat(result).contains("-42");
    }

    // ── formatValue edge cases ──────────────────────────────────────────────

    @Test
    void formatValue_negativeInteger_returnsUnquotedNumber() {
        assertThat(translator.formatValue("-5")).isEqualTo("-5");
    }

    @Test
    void formatValue_zero_returnsUnquotedNumber() {
        assertThat(translator.formatValue("0")).isEqualTo("0");
    }

    @Test
    void formatValue_emptyString_returnsSingleQuoted() {
        assertThat(translator.formatValue("")).isEqualTo("''");
    }

    // ── getAttributeValue ───────────────────────────────────────────────────

    @Test
    void getAttributeValue_nullAttributes_returnsDefault() {
        String result = translator.getAttributeValue(null, "name", "fallback");
        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void getAttributeValue_missingAttribute_returnsDefault() throws Exception {
        Document doc = parseXml("<root attr1=\"val1\"/>");
        String result = translator.getAttributeValue(
                doc.getDocumentElement().getAttributes(), "missing", "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    void getAttributeValue_presentAttribute_returnsValue() throws Exception {
        Document doc = parseXml("<root name=\"myName\"/>");
        String result = translator.getAttributeValue(
                doc.getDocumentElement().getAttributes(), "name", "default");
        assertThat(result).isEqualTo("myName");
    }

    // ── allChildrenAreLeafNodes ─────────────────────────────────────────────

    @Test
    void allChildrenAreLeafNodes_emptyList_returnsTrue() {
        assertThat(translator.allChildrenAreLeafNodes(java.util.Collections.emptyList())).isTrue();
    }

    @Test
    void allChildrenAreLeafNodes_textOnlyLeaves_returnsTrue() throws Exception {
        Document doc = parseXml("<root><a/><b/></root>");
        java.util.List<org.w3c.dom.Node> nodes = new java.util.ArrayList<>();
        org.w3c.dom.NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                nodes.add(children.item(i));
            }
        }
        assertThat(translator.allChildrenAreLeafNodes(nodes)).isTrue();
    }

    @Test
    void allChildrenAreLeafNodes_withNestedElement_returnsFalse() throws Exception {
        Document doc = parseXml("<root><a><nested/></a></root>");
        java.util.List<org.w3c.dom.Node> nodes = new java.util.ArrayList<>();
        org.w3c.dom.NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                nodes.add(children.item(i));
            }
        }
        assertThat(translator.allChildrenAreLeafNodes(nodes)).isFalse();
    }

    @Test
    void allChildrenAreLeafNodes_withAttributes_returnsFalse() throws Exception {
        Document doc = parseXml("<root><a key=\"val\"/></root>");
        java.util.List<org.w3c.dom.Node> nodes = new java.util.ArrayList<>();
        org.w3c.dom.NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                nodes.add(children.item(i));
            }
        }
        assertThat(translator.allChildrenAreLeafNodes(nodes)).isFalse();
    }
}
