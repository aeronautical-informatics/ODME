package odme.odmeeditor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioListGeneratedScenarioTest {

    @Test
    void recognizesAutomaticScenarioByLegacyPrefix() {
        assertThat(ScenarioList.isGeneratedScenarioRecord("AutoScenario_001", null)).isTrue();
    }

    @Test
    void recognizesCartesianScenarioByDefaultPrefix() {
        assertThat(ScenarioList.isGeneratedScenarioRecord("CartesianScenario_001", null)).isTrue();
    }

    @Test
    void recognizesGeneratedScenarioByRemarksEvenWithCustomPrefix() {
        assertThat(ScenarioList.isGeneratedScenarioRecord(
                "FuelingBatch_001",
                "Automatically generated specialization combination: cloudsSpec=Clear "
                        + "Cartesian variable combination 1 of 4 using 2 level(s) per sampled variable."
        )).isTrue();
    }

    @Test
    void ignoresManuallyCreatedScenario() {
        assertThat(ScenarioList.isGeneratedScenarioRecord("Scenario_day", "Created manually")).isFalse();
    }
}
