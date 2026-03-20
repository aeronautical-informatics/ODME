package odme.domain.enumeration;

import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for automated PES enumeration — implementing the scenario generation
 * methodology from Gupta et al. (2026).
 */
class ExhaustivePESEnumeratorTest {

    private ExhaustivePESEnumerator enumerator;

    @BeforeEach
    void setUp() {
        enumerator = new ExhaustivePESEnumerator();
    }

    @Test
    void emptySES_returnsEmpty() {
        SESTree ses = new SESTree("s1", "Empty");
        assertThat(enumerator.enumerateAll(ses)).isEmpty();
    }

    @Test
    void noSpecializations_returnsSinglePES() {
        SESTree ses = new SESTree("s1", "Simple");
        ses.setRoot(new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY));

        List<PESTree> result = enumerator.enumerateAll(ses);
        assertThat(result).hasSize(1);
    }

    @Test
    void oneSpecializationWithTwoChoices_returnsTwoPES() {
        SESTree ses = buildSESWithSpecialization();

        List<PESTree> result = enumerator.enumerateAll(ses);
        assertThat(result).hasSize(2);
    }

    @Test
    void twoSpecializationsWithTwoChoicesEach_returnsFourPES() {
        SESTree ses = buildSESWithTwoSpecializations();

        List<PESTree> result = enumerator.enumerateAll(ses);
        assertThat(result).hasSize(4);
    }

    @Test
    void enumeratedPES_linkedToSourceSES() {
        SESTree ses = buildSESWithSpecialization();

        List<PESTree> result = enumerator.enumerateAll(ses);
        for (PESTree pes : result) {
            assertThat(pes.getSourceSESId()).isEqualTo(ses.getId());
        }
    }

    @Test
    void enumeratedPES_eachHasUniqueId() {
        SESTree ses = buildSESWithSpecialization();
        List<PESTree> result = enumerator.enumerateAll(ses);

        long distinctIds = result.stream().map(PESTree::getId).distinct().count();
        assertThat(distinctIds).isEqualTo(result.size());
    }

    @Test
    void enumerateToCoverage_50percent_returnsOneScenario() {
        SESTree ses = buildSESWithSpecialization();

        // 2 leaves, 50% coverage → 1 scenario should suffice
        List<PESTree> result = enumerator.enumerateToCoverage(ses, 0.5);
        assertThat(result).hasSize(1);
    }

    @Test
    void enumerateToCoverage_100percent_returnsBothScenarios() {
        SESTree ses = buildSESWithSpecialization();

        List<PESTree> result = enumerator.enumerateToCoverage(ses, 1.0);
        assertThat(result).hasSize(2);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /**
     * Aircraft
     * └── PowerSpec (Specialization)
     *     ├── Electric (Entity)
     *     └── Combustion (Entity)
     */
    private SESTree buildSESWithSpecialization() {
        SESTree ses = new SESTree("ses-spec", "UAM");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode powerSpec = new SESNode("power_spec", "PowerSpec", SESNodeType.SPECIALIZATION);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        SESNode combustion = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);

        ses.setRoot(aircraft);
        ses.addNode("aircraft", powerSpec);
        ses.addNode("power_spec", electric);
        ses.addNode("power_spec", combustion);
        return ses;
    }

    /**
     * Aircraft
     * ├── PowerSpec (Specialization) → {Electric, Combustion}
     * └── WeatherSpec (Specialization) → {Clear, Cloudy}
     */
    private SESTree buildSESWithTwoSpecializations() {
        SESTree ses = new SESTree("ses-two-spec", "UAM2");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode powerSpec = new SESNode("power_spec", "PowerSpec", SESNodeType.SPECIALIZATION);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        SESNode combustion = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);
        SESNode weatherSpec = new SESNode("weather_spec", "WeatherSpec", SESNodeType.SPECIALIZATION);
        SESNode clear = new SESNode("clear", "Clear", SESNodeType.ENTITY);
        SESNode cloudy = new SESNode("cloudy", "Cloudy", SESNodeType.ENTITY);

        ses.setRoot(aircraft);
        ses.addNode("aircraft", powerSpec);
        ses.addNode("power_spec", electric);
        ses.addNode("power_spec", combustion);
        ses.addNode("aircraft", weatherSpec);
        ses.addNode("weather_spec", clear);
        ses.addNode("weather_spec", cloudy);

        return ses;
    }
}
