package odme.domain.persistence;

import odme.domain.model.SESTree;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Contract for persisting and loading a {@link SESTree}.
 *
 * <p>Implementations must be format-agnostic from the domain's perspective.
 * Current implementations: {@link XmlSESSerializer} (XML format aligned with ses.xsd).</p>
 */
public interface SESSerializer {

    /**
     * Writes the given SES tree to the specified file path.
     *
     * @param tree the SES model to persist
     * @param path the target file (will be created or overwritten)
     * @throws IOException if writing fails
     */
    void write(SESTree tree, Path path) throws IOException;

    /**
     * Reads an SES tree from the specified file path.
     *
     * @param path the source file
     * @return the loaded SES tree
     * @throws IOException              if reading fails
     * @throws IllegalArgumentException if the file content is invalid
     */
    SESTree read(Path path) throws IOException;
}
