package odme.domain.transform;

import odme.domain.model.SESNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XmlTransformRules}.
 */
class XmlTransformRulesTest {

    private XmlTransformRules rules;

    @BeforeEach
    void setUp() {
        rules = new XmlTransformRules();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // applyModifyXmlOutputRules
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ModifyXmlOutputRules {

        @Test
        void dropsLineEndingWithStart() {
            List<String> input = List.of("<mxGraphModel>", "<somestart>", "</mxGraphModel>");
            List<String> result = rules.applyModifyXmlOutputRules(input);

            assertThat(result).doesNotContain("<somestart>");
        }

        @Test
        void expandsSelfClosingTag() {
            List<String> input = List.of("<Engine/>");
            List<String> result = rules.applyModifyXmlOutputRules(input);

            assertThat(result).containsExactly("<Engine>", "</Engine>");
        }

        @Test
        void selfClosingTagWithWhitespace_isCompacted() {
            List<String> input = List.of("<My Node/>");
            List<String> result = rules.applyModifyXmlOutputRules(input);

            // Whitespace in tag name is stripped
            assertThat(result.get(0)).isEqualTo("<MyNode>");
            assertThat(result.get(1)).isEqualTo("</MyNode>");
        }

        @Test
        void normalLinePassesThrough() {
            List<String> input = List.of("<Vehicle>", "</Vehicle>");
            List<String> result = rules.applyModifyXmlOutputRules(input);

            assertThat(result).containsExactly("<Vehicle>", "</Vehicle>");
        }

        @Test
        void emptyInput_returnsEmptyOutput() {
            List<String> result = rules.applyModifyXmlOutputRules(Collections.emptyList());
            assertThat(result).isEmpty();
        }

        @Test
        void mixedLines_processedCorrectly() {
            List<String> input = List.of(
                    "<Root>",
                    "<somestart>",
                    "<Child/>",
                    "</Root>"
            );
            List<String> result = rules.applyModifyXmlOutputRules(input);

            assertThat(result).containsExactly("<Root>", "<Child>", "</Child>", "</Root>");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // applyModifyXmlOutputFixForSameNameNode
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ModifyXmlOutputFixForSameNameNode {

        @Test
        void selfClosingTag_notSpecialSuffix_expanded() {
            List<String> input = List.of("<Engine/>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<Engine>", "</Engine>");
        }

        @Test
        void selfClosingTag_refNodeSuffix_keptAsIs() {
            List<String> input = List.of("<VehicleRefNode/>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<VehicleRefNode/>");
        }

        @Test
        void selfClosingTag_varSuffix_keptAsIs() {
            List<String> input = List.of("<speedVar/>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<speedVar/>");
        }

        @Test
        void selfClosingTag_behaviourSuffix_keptAsIs() {
            List<String> input = List.of("<runBehaviour/>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<runBehaviour/>");
        }

        @Test
        void selfClosingTag_conSuffix_keptAsIs() {
            List<String> input = List.of("<limitCon/>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<limitCon/>");
        }

        @Test
        void normalLine_passesThrough() {
            List<String> input = List.of("<Vehicle>", "</Vehicle>");
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(input);

            assertThat(result).containsExactly("<Vehicle>", "</Vehicle>");
        }

        @Test
        void emptyInput_returnsEmpty() {
            List<String> result = rules.applyModifyXmlOutputFixForSameNameNode(Collections.emptyList());
            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // applyXsdTransformRules
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class XsdTransformRules {

        @Test
        void xmlDeclaration_isReplaced() {
            List<String> input = List.of("<?xml version=\"1.0\"?>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).containsExactly(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        }

        @Test
        void entityOpenTag_firstEntity_hasNamespaces() {
            List<String> input = List.of("<Vehicle>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).startsWith("<entity xmlns:");
            assertThat(result.get(0)).contains("name=\"Vehicle\"");
        }

        @Test
        void entityOpenTag_secondEntity_noNamespaces() {
            List<String> input = List.of("<Vehicle>", "<Engine>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("xmlns:");
            assertThat(result.get(1)).isEqualTo("<entity name=\"Engine\">");
        }

        @Test
        void aspectOpenTag_Dec_suffix() {
            List<String> input = List.of("<propulsionDec>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).isEqualTo("<aspect name=\"propulsionDec\">");
        }

        @Test
        void multiAspectOpenTag_MAsp_suffix() {
            List<String> input = List.of("<rotorMAsp>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).isEqualTo("<multiAspect name=\"rotorMAsp\">");
        }

        @Test
        void specializationOpenTag_Spec_suffix() {
            List<String> input = List.of("<powerSpec>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).isEqualTo("<specialization name=\"powerSpec\">");
        }

        @Test
        void seqOpenTag_isSkipped() {
            List<String> input = List.of("<itemSeq>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).isEmpty();
        }

        // ── Closing tags ─────────────────────────────────────────────────────

        @Test
        void closingTag_Dec_becomesAspect() {
            List<String> input = List.of("</propulsionDec>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).containsExactly("</aspect>");
        }

        @Test
        void closingTag_MAsp_becomesMultiAspect() {
            List<String> input = List.of("</rotorMAsp>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).containsExactly("</multiAspect>");
        }

        @Test
        void closingTag_Spec_becomesSpecialization() {
            List<String> input = List.of("</powerSpec>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).containsExactly("</specialization>");
        }

        @Test
        void closingTag_entity_becomesEntity() {
            List<String> input = List.of("</Vehicle>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).containsExactly("</entity>");
        }

        @Test
        void closingTag_Seq_isSkipped() {
            List<String> input = List.of("</itemSeq>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).isEmpty();
        }

        // ── Self-closing tags ────────────────────────────────────────────────

        @Test
        void selfClosing_Var_stringType_producesVarElement() {
            List<String> input = List.of("<name,string,defaultValVar/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<var name=\"name\"");
            assertThat(result.get(0)).contains("type=\"string\"");
            assertThat(result.get(0)).contains("default=\"defaultVal\"");
        }

        @Test
        void selfClosing_Var_booleanType_producesVarElement() {
            List<String> input = List.of("<active,boolean,trueVar/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("type=\"boolean\"");
        }

        @Test
        void selfClosing_Var_intType_producesVarWithBounds() {
            List<String> input = List.of("<speed,int,50,0,100Var/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("name=\"speed\"");
            assertThat(result.get(0)).contains("type=\"int\"");
            assertThat(result.get(0)).contains("default=\"50\"");
            assertThat(result.get(0)).contains("lower=\"0\"");
            assertThat(result.get(0)).contains("upper=\"100\"");
        }

        @Test
        void selfClosing_Behaviour_producesBehaviourElement() {
            List<String> input = List.of("<runModelBehaviour/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<behaviour name=\"runModel\"");
        }

        @Test
        void selfClosing_RefNode_entity_producesEntityRef() {
            List<String> input = List.of("<EngineRefNode/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<entity name=\"Engine\"");
            assertThat(result.get(0)).contains("ref=\"Engine\"");
        }

        @Test
        void selfClosing_RefNode_Dec_producesAspectRef() {
            List<String> input = List.of("<propulsionDecRefNode/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<aspect name=\"propulsionDec\"");
            assertThat(result.get(0)).contains("ref=\"propulsionDec\"");
        }

        @Test
        void selfClosing_RefNode_MAsp_producesMultiAspectRef() {
            List<String> input = List.of("<rotorMAspRefNode/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<multiAspect name=\"rotorMAsp\"");
        }

        @Test
        void selfClosing_RefNode_Spec_producesSpecializationRef() {
            List<String> input = List.of("<powerSpecRefNode/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<specialization name=\"powerSpec\"");
        }

        @Test
        void selfClosing_otherTag_isIgnored() {
            // Not ending with Var, Behaviour, or RefNode - silently ignored
            List<String> input = List.of("<SomeOther/>");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).isEmpty();
        }

        // ── Complete document ────────────────────────────────────────────────

        @Test
        void completeDocument_transformsCorrectly() {
            List<String> input = Arrays.asList(
                    "<?xml version=\"1.0\"?>",
                    "<Aircraft>",
                    "<propulsionDec>",
                    "<Engine>",
                    "</Engine>",
                    "</propulsionDec>",
                    "</Aircraft>"
            );
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result.get(0)).contains("<?xml");
            assertThat(result.get(1)).contains("<entity");
            assertThat(result.get(1)).contains("xmlns:");
            assertThat(result.get(2)).isEqualTo("<aspect name=\"propulsionDec\">");
            assertThat(result.get(3)).isEqualTo("<entity name=\"Engine\">");
            assertThat(result.get(4)).isEqualTo("</entity>");
            assertThat(result.get(5)).isEqualTo("</aspect>");
            assertThat(result.get(6)).isEqualTo("</entity>");
        }

        @Test
        void emptyInput_returnsEmpty() {
            List<String> result = rules.applyXsdTransformRules(Collections.emptyList());
            assertThat(result).isEmpty();
        }

        @Test
        void nonTagLines_areIgnored() {
            List<String> input = List.of("just some text", "another line");
            List<String> result = rules.applyXsdTransformRules(input);

            assertThat(result).isEmpty();
        }

        @Test
        void decOpenTag_doesNotFlipFirstEntityFlag() {
            // Dec/MAsp/Spec open tags should not flip isFirstEntity
            // So the first actual entity after them still gets namespaces
            List<String> input = Arrays.asList(
                    "<propulsionDec>",
                    "<Engine>"
            );
            List<String> result = rules.applyXsdTransformRules(input);

            // Engine is the first entity, should have xmlns
            assertThat(result.get(1)).contains("xmlns:");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // classifyNode
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ClassifyNode {

        @Test
        void entityName_returnsEntity() {
            assertThat(rules.classifyNode("Vehicle")).isEqualTo(SESNodeType.ENTITY);
        }

        @Test
        void decSuffix_returnsAspect() {
            assertThat(rules.classifyNode("propulsionDec")).isEqualTo(SESNodeType.ASPECT);
        }

        @Test
        void maspSuffix_returnsMultiAspect() {
            assertThat(rules.classifyNode("rotorMAsp")).isEqualTo(SESNodeType.MULTI_ASPECT);
        }

        @Test
        void specSuffix_returnsSpecialization() {
            assertThat(rules.classifyNode("powerSpec")).isEqualTo(SESNodeType.SPECIALIZATION);
        }

        @Test
        void tildeInName_returnsSpecialization() {
            assertThat(rules.classifyNode("power~type")).isEqualTo(SESNodeType.SPECIALIZATION);
        }

        @Test
        void nullName_returnsEntity() {
            assertThat(rules.classifyNode(null)).isEqualTo(SESNodeType.ENTITY);
        }

        @Test
        void blankName_returnsEntity() {
            assertThat(rules.classifyNode("  ")).isEqualTo(SESNodeType.ENTITY);
        }
    }
}
