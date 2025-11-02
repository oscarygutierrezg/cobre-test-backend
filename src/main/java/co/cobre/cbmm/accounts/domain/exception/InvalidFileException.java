package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when an invalid file is uploaded
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}

