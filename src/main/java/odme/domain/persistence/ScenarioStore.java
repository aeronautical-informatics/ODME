package odme.domain.persistence;

import odme.domain.model.Scenario;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Contract for persisting and retrieving {@link Scenario} objects.
 *
 * <p>Replaces the ad-hoc {@code scenarios.json} read/write pattern in the
 * original codebase with a proper repository interface.</p>
 */
public interface ScenarioStore {

    /**
     * Loads all scenarios for a given project directory.
     *
     * @param projectDirectory the directory containing the scenarios file
     * @return list of all scenarios (may be empty, never null)
     * @throws IOException if reading fails
     */
    List<Scenario> loadAll(Path projectDirectory) throws IOException;

    /**
     * Saves all scenarios for a given project directory (overwrites existing).
     *
     * @param scenarios        the complete list of scenarios to persist
     * @param projectDirectory the directory containing the scenarios file
     * @throws IOException if writing fails
     */
    void saveAll(List<Scenario> scenarios, Path projectDirectory) throws IOException;

    /**
     * Finds a scenario by its unique ID.
     *
     * @param id               the scenario ID to search for
     * @param projectDirectory the project directory to search in
     * @return the scenario if found
     * @throws IOException if reading fails
     */
    Optional<Scenario> findById(String id, Path projectDirectory) throws IOException;
}
