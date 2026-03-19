package odme.domain.validation;

import odme.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link SESStructureValidator} — verifies SES well-formedness rules.
 */
class SESStructureValidatorTest {

    private SESStructureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SESStructureValidator();
    }

    @Test
    void emptyTree_producesWarning() {
        SESTree tree = new SESTree("t1", "Empty");
        ValidationResult result = validator.validate(tree);

        assertThat(result.isValid()).isTrue(); // warning, not error
        assertThat(result.getWarnings()).isNotEmpty();
    }

    @Test
    void simpleEntityRoot_isValid() {
        SESTree tree = new SESTree("t1", "Simple");
        tree.setRoot(new SESNode("root", "Aircraft", SESNodeType.ENTITY));

        ValidationResult result = validator.validate(tree);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void aspectWithEntityChild_isValid() {
        SESTree tree = new SESTree("t1", "Test");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        SESNode aspect = new SESNode("dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.setRoot(root);
        tree.addNode("root", aspect);
        tree.addNode("dec", engine);

        ValidationResult result = validator.validate(tree);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void aspectWithNoChildren_producesWarning() {
        SESTree tree = new SESTree("t1", "Test");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        SESNode aspect = new SESNode("dec", "PropulsionDec", SESNodeType.ASPECT);
        tree.setRoot(root);
        tree.addNode("root", aspect);

        ValidationResult result = validator.validate(tree);
        assertThat(result.getWarnings()).anyMatch(w -> w.code().equals("ASPECT_NO_CHILDREN"));
    }

    @Test
    void multiAspectWithOneEntityChild_isValid() {
        SESTree tree = new SESTree("t1", "Test");
        SESNode root = new SESNode("root", "UAV", SESNodeType.ENTITY);
        SESNode masp = new SESNode("masp", "RotorMAsp", SESNodeType.MULTI_ASPECT);
        SESNode rotor = new SESNode("rotor", "Rotor", SESNodeType.ENTITY);
        tree.setRoot(root);
        tree.addNode("root", masp);
        tree.addNode("masp", rotor);

        ValidationResult result = validator.validate(tree);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void multiAspectWithTwoEntityChildren_producesError() {
        SESTree tree = new SESTree("t1", "Test");
        SESNode root = new SESNode("root", "UAV", SESNodeType.ENTITY);
        SESNode masp = new SESNode("masp", "RotorMAsp", SESNodeType.MULTI_ASPECT);
        SESNode rotor1 = new SESNode("rotor1", "Rotor1", SESNodeType.ENTITY);
        SESNode rotor2 = new SESNode("rotor2", "Rotor2", SESNodeType.ENTITY);
        tree.setRoot(root);
        tree.addNode("root", masp);
        tree.addNode("masp", rotor1);
        tree.addNode("masp", rotor2);

        ValidationResult result = validator.validate(tree);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(e -> e.code().equals("MULTI_ASPECT_ENTITY_COUNT"));
    }

    @Test
    void specializationWithSingleChild_producesWarning() {
        SESTree tree = new SESTree("t1", "Test");
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        SESNode spec = new SESNode("spec", "PowerSpec", SESNodeType.SPECIALIZATION);
        SESNode electric = new SESNode("elec", "Electric", SESNodeType.ENTITY);
        tree.setRoot(root);
        tree.addNode("root", spec);
        tree.addNode("spec", electric);

        ValidationResult result = validator.validate(tree);
        assertThat(result.getWarnings())
            .anyMatch(w -> w.code().equals("SPECIALIZATION_FEW_CHILDREN"));
    }
}
