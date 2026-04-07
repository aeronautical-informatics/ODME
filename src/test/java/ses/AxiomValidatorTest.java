package ses;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class AxiomValidatorTest {

    // -------------------------------------------------------------------------
    // Axiom 1: Uniformity
    // -------------------------------------------------------------------------

    @Test
    public void uniformity_sameNameSameStructure_noViolation() {
        // TODO: build a tree where the same node name appears in two branches
        // with identical subtrees — should pass
    }

    @Test
    public void uniformity_sameNameDifferentStructure_violation() {
        // TODO: build a tree where the same node name appears in two branches
        // with different subtrees — should produce a Uniformity violation
    }

    // -------------------------------------------------------------------------
    // Axiom 2: Strict Hierarchy
    // -------------------------------------------------------------------------

    @Test
    public void strictHierarchy_nameRepeatedOnPath_violation() {
        // TODO: build a tree where a node name appears twice on the same root-to-leaf path
        // e.g. Vehicle → PowerSource → Vehicle — should produce a Strict Hierarchy violation
    }

    @Test
    public void strictHierarchy_sameNameInDifferentBranches_noViolation() {
        // TODO: build a tree where the same name appears in two separate branches
        // (not on the same path) — should pass strict hierarchy
    }

    // -------------------------------------------------------------------------
    // Axiom 3: Alternating Mode
    // -------------------------------------------------------------------------

    @Test
    public void alternatingMode_entityChildOfEntity_violation() {
        // TODO: build Entity → Entity directly — should produce an Alternating Mode violation
    }

    @Test
    public void alternatingMode_aspectChildOfEntity_noViolation() {
        // TODO: build Entity → Aspect — should pass
    }

    @Test
    public void alternatingMode_nonEntityChildOfAspect_violation() {
        // TODO: build Aspect → Aspect — should produce an Alternating Mode violation
    }

    // -------------------------------------------------------------------------
    // Axiom 4: Valid Brothers
    // -------------------------------------------------------------------------

    @Test
    public void validBrothers_siblingsWithSameName_violation() {
        // TODO: build a node with two children sharing the same name
        // — should produce a Valid Brothers violation
    }

    @Test
    public void validBrothers_siblingsWithDifferentNames_noViolation() {
        // TODO: build a node with two children with distinct names — should pass
    }

    // -------------------------------------------------------------------------
    // Axiom 5: Attached Variables
    // -------------------------------------------------------------------------

    @Test
    public void attachedVariables_duplicateVariableOnSameNode_violation() {
        // TODO: add two variables with the same name to one node
        // — should produce an Attached Variables violation
    }

    @Test
    public void attachedVariables_distinctVariableNames_noViolation() {
        // TODO: add two variables with different names to one node — should pass
    }

    // -------------------------------------------------------------------------
    // validate() — combines all axioms
    // -------------------------------------------------------------------------

    @Test
    public void validate_multipleViolations_allReported() {
        // TODO: build a tree that violates more than one axiom simultaneously
        // — validate() should return violations from all failing checks
    }

    @Test
    public void validate_validTree_noViolations() {
        // TODO: build a well-formed SES tree — validate() should return an empty list
    }
}
