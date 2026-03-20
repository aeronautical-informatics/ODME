package odme.domain.enumeration;

import odme.domain.model.PESTree;
import odme.domain.model.SESTree;

import java.util.List;

/**
 * Enumerates Possible Entity Structure (PES) instances from a System Entity Structure (SES).
 *
 * In the SES formalism, a PES is derived by selecting exactly one specialization
 * from each specialization node. The full set of valid PES instances represents
 * the complete space of possible system configurations — i.e., the full ODD.
 *
 * Implementations vary in strategy:
 * - {@link ExhaustivePESEnumerator}: generate all valid PES instances
 * - Coverage-guided enumeration (Phase 4 extension): minimize scenarios for target coverage
 */
public interface PESEnumerator {

    /**
     * Enumerates all valid PES instances from the given SES.
     *
     * @param ses the source System Entity Structure
     * @return all possible PES trees; empty if the SES is empty
     */
    List<PESTree> enumerateAll(SESTree ses);

    /**
     * Enumerates the minimum set of PES instances that achieves the target ODD coverage.
     *
     * @param ses            the source SES
     * @param targetCoverage target coverage fraction (0.0–1.0)
     * @return minimal scenario set achieving target coverage
     */
    List<PESTree> enumerateToCoverage(SESTree ses, double targetCoverage);
}
