package odme.domain.coverage;

import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ODD coverage analysis — the core metric for EASA AI Learning Assurance.
 */
class ODDCoverageAnalyzerTest {

    private ODDCoverageAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ODDCoverageAnalyzer();
    }

    /**
     * Builds a simple SES:
     *   Aircraft
     *   └── PropulsionDec (Aspect)
     *       ├── Electric (Entity - leaf)
     *       └── Combustion (Entity - leaf)
     */
    private SESTree buildSES() {
        SESTree ses = new SESTree("ses-1", "UAM_ODD");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode propDec = new SESNode("prop_dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        SESNode combustion = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);

        ses.setRoot(aircraft);
        ses.addNode("aircraft", propDec);
        ses.addNode("prop_dec", electric);
        ses.addNode("prop_dec", combustion);
        return ses;
    }

    @Test
    void noScenarios_zeroCoverage() {
        SESTree ses = buildSES();
        ODDCoverageReport report = analyzer.analyze(ses, List.of());

        assertThat(report.coveragePercent()).isEqualTo(0.0);
        assertThat(report.coveredLeafNodes()).isEqualTo(0);
        assertThat(report.totalLeafNodes()).isEqualTo(2);
    }

    @Test
    void oneScenario_coversHalfTheLeaves() {
        SESTree ses = buildSES();

        // PES with only Electric
        PESTree pes1 = new PESTree("pes-1", "ElectricScenario", "ses-1");
        SESNode aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode electric = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        aircraft.addChild(electric);
        pes1.setRoot(aircraft);

        ODDCoverageReport report = analyzer.analyze(ses, List.of(pes1));

        assertThat(report.coveredLeafNodes()).isEqualTo(1);
        assertThat(report.totalLeafNodes()).isEqualTo(2);
        assertThat(report.coveragePercent()).isEqualTo(50.0);
        assertThat(report.isFullyCovered()).isFalse();
    }

    @Test
    void twoScenarios_coversAllLeaves() {
        SESTree ses = buildSES();

        PESTree pes1 = new PESTree("pes-1", "Electric", "ses-1");
        SESNode a1 = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode e = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        a1.addChild(e);
        pes1.setRoot(a1);

        PESTree pes2 = new PESTree("pes-2", "Combustion", "ses-1");
        SESNode a2 = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode c = new SESNode("combustion", "Combustion", SESNodeType.ENTITY);
        a2.addChild(c);
        pes2.setRoot(a2);

        ODDCoverageReport report = analyzer.analyze(ses, List.of(pes1, pes2));

        assertThat(report.coveragePercent()).isEqualTo(100.0);
        assertThat(report.isFullyCovered()).isTrue();
        assertThat(report.uncoveredNodes()).isEmpty();
    }

    @Test
    void report_identifiesUncoveredNodes() {
        SESTree ses = buildSES();

        PESTree pes1 = new PESTree("pes-1", "Electric", "ses-1");
        SESNode a = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode e = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        a.addChild(e);
        pes1.setRoot(a);

        ODDCoverageReport report = analyzer.analyze(ses, List.of(pes1));

        assertThat(report.uncoveredNodes()).hasSize(1);
        assertThat(report.uncoveredNodes().get(0).getName()).isEqualTo("Combustion");
    }

    @Test
    void nodeFrequency_countsAppearancesAcrossScenarios() {
        SESTree ses = buildSES();

        // Two scenarios, both covering Electric
        PESTree pes1 = buildPESWithElectric("pes-1");
        PESTree pes2 = buildPESWithElectric("pes-2");

        ODDCoverageReport report = analyzer.analyze(ses, List.of(pes1, pes2));

        assertThat(report.nodeFrequency().get("electric")).isEqualTo(2);
    }

    private PESTree buildPESWithElectric(String id) {
        PESTree pes = new PESTree(id, "ElectricScenario_" + id, "ses-1");
        SESNode a = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        SESNode e = new SESNode("electric", "Electric", SESNodeType.ENTITY);
        a.addChild(e);
        pes.setRoot(a);
        return pes;
    }
}
