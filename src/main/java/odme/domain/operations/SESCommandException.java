package odme.domain.operations;

/**
 * Thrown when an SES command cannot be executed or undone.
 */
public class SESCommandException extends Exception {

    public SESCommandException(String message) {
        super(message);
    }

    public SESCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
