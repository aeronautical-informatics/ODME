package odme.domain.prune;

import odme.domain.model.SESNodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NamingConventions}.
 */
class NamingConventionsTest {

    // ── isEntity ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "isEntity(''{0}'') = true")
    @ValueSource(strings = {"Vehicle", "Aircraft", "Engine", "SomeRandomName"})
    void isEntity_entityNames_returnsTrue(String name) {
        assertThat(NamingConventions.isEntity(name)).isTrue();
    }

    @ParameterizedTest(name = "isEntity(''{0}'') = false")
    @ValueSource(strings = {"VehicleDec", "AircraftSpec", "WheelMAsp"})
    void isEntity_nonEntityNames_returnsFalse(String name) {
        assertThat(NamingConventions.isEntity(name)).isFalse();
    }

    // ── isAspect ─────────────────────────────────────────────────────────────

    @Test
    void isAspect_decSuffix_returnsTrue() {
        assertThat(NamingConventions.isAspect("VehicleDec")).isTrue();
    }

    @Test
    void isAspect_noSuffix_returnsFalse() {
        assertThat(NamingConventions.isAspect("Vehicle")).isFalse();
    }

    @Test
    void isAspect_null_returnsFalse() {
        assertThat(NamingConventions.isAspect(null)).isFalse();
    }

    // ── isSpecialization ─────────────────────────────────────────────────────

    @Test
    void isSpecialization_specSuffix_returnsTrue() {
        assertThat(NamingConventions.isSpecialization("PowerSpec")).isTrue();
    }

    @Test
    void isSpecialization_noSuffix_returnsFalse() {
        assertThat(NamingConventions.isSpecialization("Power")).isFalse();
    }

    @Test
    void isSpecialization_null_returnsFalse() {
        assertThat(NamingConventions.isSpecialization(null)).isFalse();
    }

    // ── isMultiAspect ────────────────────────────────────────────────────────

    @Test
    void isMultiAspect_maspSuffix_returnsTrue() {
        assertThat(NamingConventions.isMultiAspect("WheelMAsp")).isTrue();
    }

    @Test
    void isMultiAspect_noSuffix_returnsFalse() {
        assertThat(NamingConventions.isMultiAspect("Wheel")).isFalse();
    }

    @Test
    void isMultiAspect_null_returnsFalse() {
        assertThat(NamingConventions.isMultiAspect(null)).isFalse();
    }

    // ── addSuffix ────────────────────────────────────────────────────────────

    @Test
    void addSuffix_entity_doesNotAddSuffix() {
        assertThat(NamingConventions.addSuffix("Vehicle", SESNodeType.ENTITY))
                .isEqualTo("Vehicle");
    }

    @Test
    void addSuffix_aspect_addsDec() {
        assertThat(NamingConventions.addSuffix("Vehicle", SESNodeType.ASPECT))
                .isEqualTo("VehicleDec");
    }

    @Test
    void addSuffix_specialization_addsSpec() {
        assertThat(NamingConventions.addSuffix("Power", SESNodeType.SPECIALIZATION))
                .isEqualTo("PowerSpec");
    }

    @Test
    void addSuffix_multiAspect_addsMAsp() {
        assertThat(NamingConventions.addSuffix("Wheel", SESNodeType.MULTI_ASPECT))
                .isEqualTo("WheelMAsp");
    }

    @Test
    void addSuffix_alreadyHasSuffix_doesNotDouble() {
        assertThat(NamingConventions.addSuffix("VehicleDec", SESNodeType.ASPECT))
                .isEqualTo("VehicleDec");
    }

    @Test
    void addSuffix_null_returnsNull() {
        assertThat(NamingConventions.addSuffix(null, SESNodeType.ASPECT)).isNull();
    }

    // ── removeSuffix ─────────────────────────────────────────────────────────

    @ParameterizedTest(name = "removeSuffix(''{0}'') = ''{1}''")
    @CsvSource({
            "VehicleDec,  Vehicle",
            "PowerSpec,   Power",
            "WheelMAsp,   Wheel",
            "Aircraft,    Aircraft"
    })
    void removeSuffix_removesKnownSuffixes(String input, String expected) {
        assertThat(NamingConventions.removeSuffix(input.trim())).isEqualTo(expected.trim());
    }

    @Test
    void removeSuffix_null_returnsNull() {
        assertThat(NamingConventions.removeSuffix(null)).isNull();
    }

    // ── getBaseName ──────────────────────────────────────────────────────────

    @Test
    void getBaseName_isAliasForRemoveSuffix() {
        assertThat(NamingConventions.getBaseName("EngineDec"))
                .isEqualTo(NamingConventions.removeSuffix("EngineDec"));
    }

    // ── ensureSuffix ─────────────────────────────────────────────────────────

    @Test
    void ensureSuffix_aspectNode_appendsSuffix() {
        assertThat(NamingConventions.ensureSuffix("OldDec", "New"))
                .isEqualTo("NewDec");
    }

    @Test
    void ensureSuffix_specNode_appendsSuffix() {
        assertThat(NamingConventions.ensureSuffix("OldSpec", "New"))
                .isEqualTo("NewSpec");
    }

    @Test
    void ensureSuffix_multiAspNode_appendsSuffix() {
        assertThat(NamingConventions.ensureSuffix("OldMAsp", "New"))
                .isEqualTo("NewMAsp");
    }

    @Test
    void ensureSuffix_entityNode_noSuffixAdded() {
        assertThat(NamingConventions.ensureSuffix("OldEntity", "New"))
                .isEqualTo("New");
    }

    @Test
    void ensureSuffix_newAlreadyHasSuffix_doesNotDouble() {
        assertThat(NamingConventions.ensureSuffix("OldDec", "NewDec"))
                .isEqualTo("NewDec");
    }

    @Test
    void ensureSuffix_nullInputs_handledGracefully() {
        assertThat(NamingConventions.ensureSuffix(null, "New")).isEqualTo("New");
        assertThat(NamingConventions.ensureSuffix("OldDec", null)).isNull();
    }

    // ── classify ─────────────────────────────────────────────────────────────

    @Test
    void classify_delegatesToSESNodeType() {
        assertThat(NamingConventions.classify("VehicleDec")).isEqualTo(SESNodeType.ASPECT);
        assertThat(NamingConventions.classify("PowerSpec")).isEqualTo(SESNodeType.SPECIALIZATION);
        assertThat(NamingConventions.classify("WheelMAsp")).isEqualTo(SESNodeType.MULTI_ASPECT);
        assertThat(NamingConventions.classify("Aircraft")).isEqualTo(SESNodeType.ENTITY);
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    void isEntity_emptyString_returnsTrue() {
        // Empty string has no suffix, so classified as entity
        assertThat(NamingConventions.classify("")).isEqualTo(SESNodeType.ENTITY);
    }
}
