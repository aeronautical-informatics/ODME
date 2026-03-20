package odme.domain.traceability;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Exports a {@link TraceabilityMatrix} to an external format for review.
 */
public interface TraceabilityExporter {
    /**
     * @param matrix the matrix to export
     * @param path   destination file
     * @throws IOException on write failure
     */
    void export(TraceabilityMatrix matrix, Path path) throws IOException;
}
