package odme.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SESNodeType} — the authoritative type system replacing
 * scattered magic-string checks across the legacy codebase.
 */
class SESNodeTypeTest {

    // ── fromLabel ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "fromLabel(''{0}'') = {1}")
    @CsvSource({
        "AircraftDec,         ASPECT",
        "EngineDec,           ASPECT",
        "PropulsionMAsp,      MULTI_ASPECT",
        "RotorMAsp,           MULTI_ASPECT",
        "PowerSpec,           SPECIALIZATION",
        "Electric~Gas,        SPECIALIZATION",
        "Aircraft,            ENTITY",
        "Engine,              ENTITY",
        "SomeRandomName,      ENTITY",
    })
    void fromLabel_recognisesAllSuffixes(String label, SESNodeType expected) {
        assertThat(SESNodeType.fromLabel(label.trim())).isEqualTo(expected);
    }

    @Test
    void fromLabel_null_returnsEntity() {
        assertThat(SESNodeType.fromLabel(null)).isEqualTo(SESNodeType.ENTITY);
    }

    @Test
    void fromLabel_blank_returnsEntity() {
        assertThat(SESNodeType.fromLabel("   ")).isEqualTo(SESNodeType.ENTITY);
    }

    @Test
    void fromLabel_maspTakesPriorityOverDec() {
        // "SomeMAsp" ends with "MAsp" — should be MULTI_ASPECT, not ASPECT
        assertThat(SESNodeType.fromLabel("SomeMAsp")).isEqualTo(SESNodeType.MULTI_ASPECT);
    }

    // ── toLabel ───────────────────────────────────────────────────────────────

    @Test
    void toLabel_entity_addsNoSuffix() {
        assertThat(SESNodeType.ENTITY.toLabel("Aircraft")).isEqualTo("Aircraft");
    }

    @Test
    void toLabel_aspect_addsDec() {
        assertThat(SESNodeType.ASPECT.toLabel("Propulsion")).isEqualTo("PropulsionDec");
    }

    @Test
    void toLabel_multiAspect_addsMAsp() {
        assertThat(SESNodeType.MULTI_ASPECT.toLabel("Rotor")).isEqualTo("RotorMAsp");
    }

    @Test
    void toLabel_specialization_addsSpec() {
        assertThat(SESNodeType.SPECIALIZATION.toLabel("Power")).isEqualTo("PowerSpec");
    }

    // ── Roundtrip ─────────────────────────────────────────────────────────────

    @Test
    void toLabel_then_fromLabel_roundtrips() {
        for (SESNodeType type : SESNodeType.values()) {
            String label = type.toLabel("TestNode");
            assertThat(SESNodeType.fromLabel(label))
                .as("Roundtrip for %s", type)
                .isEqualTo(type);
        }
    }

    // ── fromXmlTag ────────────────────────────────────────────────────────────

    @Test
    void fromXmlTag_recognisesAllTags() {
        assertThat(SESNodeType.fromXmlTag("entity")).isEqualTo(SESNodeType.ENTITY);
        assertThat(SESNodeType.fromXmlTag("aspect")).isEqualTo(SESNodeType.ASPECT);
        assertThat(SESNodeType.fromXmlTag("multiAspect")).isEqualTo(SESNodeType.MULTI_ASPECT);
        assertThat(SESNodeType.fromXmlTag("specialization")).isEqualTo(SESNodeType.SPECIALIZATION);
    }

    @Test
    void fromXmlTag_caseInsensitive() {
        assertThat(SESNodeType.fromXmlTag("ENTITY")).isEqualTo(SESNodeType.ENTITY);
        assertThat(SESNodeType.fromXmlTag("Aspect")).isEqualTo(SESNodeType.ASPECT);
    }

    @Test
    void fromXmlTag_unknown_returnsEntity() {
        assertThat(SESNodeType.fromXmlTag("unknown")).isEqualTo(SESNodeType.ENTITY);
    }
}
