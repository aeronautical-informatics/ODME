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
}
