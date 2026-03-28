package odme.domain.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link YamlToPythonTranslator}.
 */
class YamlToPythonTranslatorTest {

    private YamlToPythonTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new YamlToPythonTranslator();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // translate
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void translate_nullInput_throwsIllegalArgument() {
        assertThatThrownBy(() -> translator.translate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void translate_simpleMap_producesValidPython() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Aircraft");
        data.put("speed", "200");

        String result = translator.translate(data);

        assertThat(result).startsWith("# Translated YAML to Python");
        assertThat(result).contains("config = {");
        assertThat(result).contains("'name': 'Aircraft'");
        assertThat(result).contains("'speed': 200");
    }

    @Test
    void translate_nestedMap_producesNestedDict() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("type", "jet");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("engine", inner);

        String result = translator.translate(data);

        assertThat(result).contains("'engine': {");
        assertThat(result).contains("'type': 'jet'");
    }

    @Test
    void translate_list_producesListLiteral() {
        List<Object> data = Arrays.asList("one", "two", "three");

        String result = translator.translate(data);

        assertThat(result).contains("config = [");
        assertThat(result).contains("'one'");
        assertThat(result).contains("'two'");
        assertThat(result).contains("'three'");
    }

    @Test
    void translate_scalarString_producesQuotedValue() {
        String result = translator.translate("hello");

        assertThat(result).contains("config = 'hello'");
    }

    @Test
    void translate_scalarNumber_producesUnquotedValue() {
        String result = translator.translate("42");

        assertThat(result).contains("config = 42");
    }

    @Test
    void translate_emptyMap_producesCurlyBraces() {
        String result = translator.translate(new LinkedHashMap<>());

        // Empty map goes through handleMap which produces "{\n\n}"
        assertThat(result).contains("config = {");
        assertThat(result).contains("}");
    }

    @Test
    void translate_mapWithListValues_producesCorrectStructure() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", Arrays.asList("a", "b"));

        String result = translator.translate(data);

        assertThat(result).contains("'items': [");
        assertThat(result).contains("'a'");
        assertThat(result).contains("'b'");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // formatPythonValue
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class FormatPythonValue {

        @Test
        void null_returnsNone() {
            assertThat(translator.formatPythonValue(null)).isEqualTo("None");
        }

        @Test
        void emptyMap_returnsCurlyBraces() {
            assertThat(translator.formatPythonValue(new HashMap<>())).isEqualTo("{}");
        }

        @Test
        void nonEmptyMap_isSingleQuoted() {
            Map<String, String> m = Map.of("k", "v");
            // non-empty maps are not handled as scalars; they go through toString
            String result = translator.formatPythonValue(m);
            assertThat(result).startsWith("'");
        }

        @Test
        void emptyString_returnsNone() {
            assertThat(translator.formatPythonValue("")).isEqualTo("None");
        }

        @Test
        void blankString_returnsNone() {
            assertThat(translator.formatPythonValue("   ")).isEqualTo("None");
        }

        @Test
        void numericString_returnsUnquoted() {
            assertThat(translator.formatPythonValue("42")).isEqualTo("42");
        }

        @Test
        void floatString_returnsUnquoted() {
            assertThat(translator.formatPythonValue("3.14")).isEqualTo("3.14");
        }

        @Test
        void nonNumericString_returnsSingleQuoted() {
            assertThat(translator.formatPythonValue("hello")).isEqualTo("'hello'");
        }

        @Test
        void stringWithApostrophe_escapesIt() {
            assertThat(translator.formatPythonValue("it's")).isEqualTo("'it\\'s'");
        }

        @Test
        void integerNumber_returnsToString() {
            assertThat(translator.formatPythonValue(42)).isEqualTo("42");
        }

        @Test
        void doubleNumber_returnsToString() {
            assertThat(translator.formatPythonValue(3.14)).isEqualTo("3.14");
        }

        @Test
        void booleanTrue_returnsTrue() {
            assertThat(translator.formatPythonValue(true)).isEqualTo("True");
        }

        @Test
        void booleanFalse_returnsFalse() {
            assertThat(translator.formatPythonValue(false)).isEqualTo("False");
        }

        @Test
        void otherObject_returnsSingleQuoted() {
            Object obj = new Object() {
                @Override
                public String toString() {
                    return "customObj";
                }
            };
            assertThat(translator.formatPythonValue(obj)).isEqualTo("'customObj'");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // cleanYamlData
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class CleanYamlData {

        @Test
        void map_keysAreTrimmed() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("  key  ", "value");

            @SuppressWarnings("unchecked")
            Map<String, Object> cleaned = (Map<String, Object>) translator.cleanYamlData(data);

            assertThat(cleaned).containsKey("key");
        }

        @Test
        void map_leadingHyphenRemoved() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("- item", "value");

            @SuppressWarnings("unchecked")
            Map<String, Object> cleaned = (Map<String, Object>) translator.cleanYamlData(data);

            assertThat(cleaned).containsKey("item");
        }

        @Test
        void list_itemsCleaned() {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("- key", "val");
            List<Object> data = new ArrayList<>();
            data.add(inner);

            @SuppressWarnings("unchecked")
            List<Object> cleaned = (List<Object>) translator.cleanYamlData(data);

            assertThat(cleaned).hasSize(1);
            @SuppressWarnings("unchecked")
            Map<String, Object> cleanedInner = (Map<String, Object>) cleaned.get(0);
            assertThat(cleanedInner).containsKey("key");
        }

        @Test
        void scalar_returnedAsIs() {
            assertThat(translator.cleanYamlData("hello")).isEqualTo("hello");
            assertThat(translator.cleanYamlData(42)).isEqualTo(42);
        }

        @Test
        void nestedMaps_cleanedRecursively() {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("- nested", "val");
            Map<String, Object> outer = new LinkedHashMap<>();
            outer.put("- outer", inner);

            @SuppressWarnings("unchecked")
            Map<String, Object> cleaned = (Map<String, Object>) translator.cleanYamlData(outer);

            assertThat(cleaned).containsKey("outer");
            @SuppressWarnings("unchecked")
            Map<String, Object> cleanedInner = (Map<String, Object>) cleaned.get("outer");
            assertThat(cleanedInner).containsKey("nested");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // handleMap — edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void handleMap_keysWithLeadingHyphen_areStripped() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("-item", "value");

        String result = translator.translate(data);

        assertThat(result).contains("'item': 'value'");
    }

    @Test
    void handleMap_multipleEntries_separatedByCommas() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("a", "1");
        data.put("b", "2");

        String result = translator.translate(data);

        assertThat(result).contains("'a': 1");
        assertThat(result).contains("'b': 2");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // handleList — edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void handleList_emptyList_producesEmptyBrackets() {
        List<Object> data = new ArrayList<>();

        String result = translator.translate(data);

        assertThat(result).contains("config = [");
        assertThat(result).contains("]");
    }

    @Test
    void handleList_singleItem_noTrailingComma() {
        List<Object> data = List.of("only");

        String result = translator.translate(data);

        assertThat(result).contains("'only'");
        // Single item should not have comma after it
        assertThat(result).doesNotContain("'only',");
    }

    @Test
    void handleList_mixedTypes_allFormatted() {
        List<Object> data = Arrays.asList("text", "42", true, null);

        String result = translator.translate(data);

        assertThat(result).contains("'text'");
        assertThat(result).contains("42");
        assertThat(result).contains("True");
        assertThat(result).contains("None");
    }
}
