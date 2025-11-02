package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when a currency is invalid or not supported
 */
public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String message) {
        super(message);
    }
}

