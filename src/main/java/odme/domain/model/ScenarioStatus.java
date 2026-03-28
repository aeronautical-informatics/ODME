package odme.domain.model;

/**
 * Lifecycle status of a scenario within the EASA AI Learning Assurance workflow.
 *
 * <p>Scenarios in an AI Learning Assurance context (EASA AI Roadmap 2.0, EUROCAE ED-324)
 * must be traceable through a review and approval process before being used as
 * test evidence. This enum captures that lifecycle.</p>
 */
public enum ScenarioStatus {

    /**
     * Scenario is being actively authored. Not yet ready for review.
     * This is the default state when a scenario is first created.
     */
    DRAFT("Draft", "Being authored, not yet submitted for review"),

    /**
     * Scenario has been submitted for peer review.
     * No further edits should be made without resetting to DRAFT.
     */
    IN_REVIEW("In Review", "Submitted for peer review"),

    /**
     * Scenario has been reviewed and approved for use in testing.
     * This status is required before a scenario can be used as assurance evidence.
     */
    APPROVED("Approved", "Approved for use in testing and evidence"),

    /**
     * Scenario has been superseded by a newer version or is no longer valid.
     */
    DEPRECATED("Deprecated", "Superseded or no longer valid");

    private final String displayName;
    private final String description;

    ScenarioStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }

    public String getDescription() { return description; }

    /** Whether this scenario may be edited. Only DRAFT scenarios are editable. */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /** Whether this scenario qualifies as assurance evidence. Only APPROVED scenarios do. */
    public boolean isApproved() {
        return this == APPROVED;
    }

    @Override
    public String toString() { return displayName; }
}
