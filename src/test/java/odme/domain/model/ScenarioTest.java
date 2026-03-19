package odme.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link Scenario} lifecycle state machine.
 *
 * <p>The EASA AI Learning Assurance workflow requires that scenarios pass through
 * defined lifecycle stages. These tests verify the transition rules.</p>
 */
class ScenarioTest {

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    void newScenario_isInDraftStatus() {
        Scenario s = new Scenario("s-001", "ClearWeather_LowAltitude", "ses-001");
        assertThat(s.getStatus()).isEqualTo(ScenarioStatus.DRAFT);
    }

    @Test
    void newScenario_isDraftTherforeEditable() {
        Scenario s = new Scenario("s-001", "Test", null);
        assertThat(s.getStatus().isEditable()).isTrue();
    }

    @Test
    void newScenario_isNotApproved() {
        Scenario s = new Scenario("s-001", "Test", null);
        assertThat(s.getStatus().isApproved()).isFalse();
    }

    // ── DRAFT → IN_REVIEW ────────────────────────────────────────────────────

    @Test
    void submitForReview_fromDraft_transitionsToInReview() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.submitForReview("u.durak");

        assertThat(s.getStatus()).isEqualTo(ScenarioStatus.IN_REVIEW);
    }

    @Test
    void submitForReview_fromInReview_throwsException() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.submitForReview("user1");

        assertThatThrownBy(() -> s.submitForReview("user2"))
            .isInstanceOf(IllegalStateException.class);
    }

    // ── IN_REVIEW → APPROVED ─────────────────────────────────────────────────

    @Test
    void approve_fromInReview_transitionsToApproved() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.submitForReview("author");
        s.approve("reviewer");

        assertThat(s.getStatus()).isEqualTo(ScenarioStatus.APPROVED);
        assertThat(s.getStatus().isApproved()).isTrue();
    }

    @Test
    void approve_fromDraft_throwsException() {
        Scenario s = new Scenario("s-001", "Test", null);

        assertThatThrownBy(() -> s.approve("reviewer"))
            .isInstanceOf(IllegalStateException.class);
    }

    // ── Return to DRAFT ───────────────────────────────────────────────────────

    @Test
    void returnToDraft_fromInReview_resetsStatus() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.submitForReview("author");
        s.returnToDraft("reviewer", "Needs more detail on wind conditions");

        assertThat(s.getStatus()).isEqualTo(ScenarioStatus.DRAFT);
        assertThat(s.getStatus().isEditable()).isTrue();
    }

    // ── Audit history ────────────────────────────────────────────────────────

    @Test
    void lifecycle_actions_recordedInHistory() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.submitForReview("author");
        s.approve("reviewer");

        assertThat(s.getHistory()).hasSize(2);
        assertThat(s.getHistory().get(0).action()).contains("SUBMITTED");
        assertThat(s.getHistory().get(1).action()).contains("APPROVED");
    }

    // ── Deprecation ───────────────────────────────────────────────────────────

    @Test
    void deprecate_marksAsDeprecated() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.deprecate("admin", "Superseded by v2");

        assertThat(s.getStatus()).isEqualTo(ScenarioStatus.DEPRECATED);
    }

    @Test
    void returnToDraft_fromDeprecated_throwsException() {
        Scenario s = new Scenario("s-001", "Test", null);
        s.deprecate("admin", "Old");

        assertThatThrownBy(() -> s.returnToDraft("user", "Try again"))
            .isInstanceOf(IllegalStateException.class);
    }
}
