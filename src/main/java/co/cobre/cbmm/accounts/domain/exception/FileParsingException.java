package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when file parsing fails
 */
public class FileParsingException extends RuntimeException {

    public FileParsingException(String message) {
        super(message);
    }

    public FileParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

