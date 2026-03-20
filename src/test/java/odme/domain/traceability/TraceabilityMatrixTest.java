package odme.domain.traceability;

import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TraceabilityMatrixTest {

    private SESTree ses;
    private PESTree pes1;
    private PESTree pes2;
    private Scenario scenario1;
    private Scenario scenario2;

    @BeforeEach
    void setUp() {
        // Build SES: Aircraft → PropulsionDec → {Electric, Combustion}
        ses = new SESTree("ses-1", "UAM_ODD");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode propDec = new SESNode("prop_dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        SESNode combustion = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);
        ses.setRoot(aircraft);
        ses.addNode("aircraft", propDec);
        ses.addNode("prop_dec", electric);
        ses.addNode("prop_dec", combustion);

        // Scenario 1: Electric scenario
        scenario1 = new Scenario("s1", "ElectricScenario", "ses-1");
        pes1 = new PESTree("pes-1", "ElectricScenario", "ses-1");
        SESNode a1 = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode e1 = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        a1.addChild(e1);
        pes1.setRoot(a1);

        // Scenario 2: Combustion scenario
        scenario2 = new Scenario("s2", "CombustionScenario", "ses-1");
        pes2 = new PESTree("pes-2", "CombustionScenario", "ses-1");
        SESNode a2 = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode c2 = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);
        a2.addChild(c2);
        pes2.setRoot(a2);
    }

    @Test
    void fullCoverage_noGaps() {
        TraceabilityMatrix matrix = new TraceabilityMatrix(
            ses, List.of(scenario1, scenario2), List.of(pes1, pes2));

        assertThat(matrix.getODDCoveragePercent()).isEqualTo(100.0);
        assertThat(matrix.getUncoveredODDElements()).isEmpty();
    }

    @Test
    void partialCoverage_reportsGap() {
        TraceabilityMatrix matrix = new TraceabilityMatrix(
            ses, List.of(scenario1), List.of(pes1));

        assertThat(matrix.getODDCoveragePercent()).isEqualTo(50.0);
        assertThat(matrix.getUncoveredODDElements()).hasSize(1);
        assertThat(matrix.getUncoveredODDElements().get(0).getName()).isEqualTo("Combustion");
    }

    @Test
    void entriesCount_matchesPESLeaves() {
        TraceabilityMatrix matrix = new TraceabilityMatrix(
            ses, List.of(scenario1, scenario2), List.of(pes1, pes2));

        // Each PES has 1 leaf matched to 1 SES leaf → 2 entries total
        assertThat(matrix.getEntries()).hasSize(2);
    }

    @Test
    void scenariosWithoutTestCase_allScenariosWhenNoTestsLinked() {
        TraceabilityMatrix matrix = new TraceabilityMatrix(
            ses, List.of(scenario1, scenario2), List.of(pes1, pes2));

        // No test cases assigned yet → both scenarios appear in "without tests" list
        assertThat(matrix.getScenariosWithoutTestCase()).hasSize(2);
    }

    @Test
    void summary_containsKeyMetrics() {
        TraceabilityMatrix matrix = new TraceabilityMatrix(
            ses, List.of(scenario1), List.of(pes1));

        String summary = matrix.getSummary();
        assertThat(summary).contains("50.0%");
        assertThat(summary).contains("1/2");
    }
}
