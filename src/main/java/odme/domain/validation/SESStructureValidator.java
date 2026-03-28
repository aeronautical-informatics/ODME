package odme.domain.validation;

import odme.domain.model.SESNode;
import odme.domain.model.SESNodeType;
import odme.domain.model.SESTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the structural well-formedness rules of a System Entity Structure.
 *
 * <p>SES well-formedness rules (from Zeigler 1984, formalized in Karmokar et al. 2019):</p>
 * <ol>
 *   <li>The tree must have exactly one root.</li>
 *   <li>An ASPECT node must have at least one child ENTITY node.</li>
 *   <li>A MULTI_ASPECT node must have exactly one child ENTITY node (the template).</li>
 *   <li>A SPECIALIZATION node must have at least two children (otherwise use the entity directly).</li>
 *   <li>Entity names must be unique within the tree (for a given scope).</li>
 * </ol>
 */
public class SESStructureValidator implements SESValidator {

    private static final Logger log = LoggerFactory.getLogger(SESStructureValidator.class);

    @Override
    public ValidationResult validate(SESTree tree) {
        ValidationResult result = ValidationResult.empty();

        if (tree.isEmpty()) {
            result.addWarning("SES_EMPTY", "The SES tree has no root node");
            return result;
        }

        SESNode root = tree.getRoot().orElseThrow();
        validateNode(root, result);

        log.debug("Validated SES '{}': {}", tree.getName(), result);
        return result;
    }

    private void validateNode(SESNode node, ValidationResult result) {
        switch (node.getType()) {
            case ASPECT -> validateAspect(node, result);
            case MULTI_ASPECT -> validateMultiAspect(node, result);
            case SPECIALIZATION -> validateSpecialization(node, result);
            case ENTITY -> { /* entity nodes have no structural constraints on children */ }
        }

        // Recurse
        for (SESNode child : node.getChildren()) {
            validateNode(child, result);
        }
    }

    private void validateAspect(SESNode node, ValidationResult result) {
        if (node.getChildren().isEmpty()) {
            result.addWarning("ASPECT_NO_CHILDREN",
                "Aspect node '" + node.getName() + "' has no children. " +
                "An aspect should decompose into at least one entity.",
                node.getId());
        }
        boolean hasEntityChild = node.getChildren().stream()
            .anyMatch(c -> c.getType() == SESNodeType.ENTITY);
        if (!node.getChildren().isEmpty() && !hasEntityChild) {
            result.addError("ASPECT_NO_ENTITY_CHILD",
                "Aspect node '" + node.getName() + "' has no direct entity children. " +
                "Aspects must decompose into entities.",
                node.getId());
        }
    }

    private void validateMultiAspect(SESNode node, ValidationResult result) {
        long entityChildren = node.getChildren().stream()
            .filter(c -> c.getType() == SESNodeType.ENTITY)
            .count();
        if (entityChildren != 1) {
            result.addError("MULTI_ASPECT_ENTITY_COUNT",
                "Multi-aspect node '" + node.getName() + "' must have exactly one entity child " +
                "(the template entity), but has " + entityChildren + ".",
                node.getId());
        }
    }

    private void validateSpecialization(SESNode node, ValidationResult result) {
        if (node.getChildren().size() < 2) {
            result.addWarning("SPECIALIZATION_FEW_CHILDREN",
                "Specialization node '" + node.getName() + "' has fewer than 2 children. " +
                "A specialization with a single child is equivalent to the child entity directly.",
                node.getId());
        }
    }
}
