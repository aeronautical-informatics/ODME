package odme.domain.validation;

import odme.domain.model.SESTree;

/**
 * Contract for validating a System Entity Structure.
 *
 * <p>Validators check structural and semantic rules. Multiple validators
 * can be composed to check different aspects (schema compliance,
 * SES well-formedness rules, ODD completeness, etc.).</p>
 */
public interface SESValidator {

    /**
     * Validates the given SES tree.
     *
     * @param tree the SES to validate (must not be null)
     * @return a {@link ValidationResult} containing all findings; never null
     */
    ValidationResult validate(SESTree tree);
}
