package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when a batch file contains no valid events
 */
public class EmptyBatchException extends RuntimeException {

    public EmptyBatchException(String message) {
        super(message);
    }
}

