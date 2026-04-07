package ses;

import org.junit.Test;
import static org.junit.Assert.*;

public class SerializerTest {

    // -------------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------------

    @Test
    public void roundTrip_simpleTree_preservesStructure() throws Exception {
        // TODO: build an EntityStructure with a few nodes, serialize to JSON,
        // deserialize back, and assert name/type/children match the original
    }

    @Test
    public void roundTrip_preservesNodeTypes() throws Exception {
        // TODO: build a tree with all four node types (ENTITY, ASPECT,
        // MULTI_ASPECT, SPECIALIZATION), round-trip, assert types survive
    }

    // -------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------

    @Test
    public void roundTrip_variableWithBounds_boundsPreserved() throws Exception {
        // TODO: add an INT variable with lowerBound and upperBound,
        // round-trip, assert bounds are preserved
    }

    @Test
    public void roundTrip_variableWithoutBounds_boundsAbsentInJson() throws Exception {
        // TODO: add a BOOLEAN variable (no bounds), serialize, assert
        // lowerBound and upperBound keys are absent from the JSON output
    }

    @Test
    public void roundTrip_variableDefaultValue_preserved() throws Exception {
        // TODO: set a defaultValue on a variable, round-trip, assert it survives
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    public void serialize_emptyTree_rootOnly() throws Exception {
        // TODO: serialize an EntityStructure with just a root node and no children,
        // assert children/variables/constraints are empty arrays in the output
    }

    @Test
    public void deserialize_unknownNodeType_throwsException() throws Exception {
        // TODO: pass JSON with an unrecognised type value, assert an exception is thrown
    }
}