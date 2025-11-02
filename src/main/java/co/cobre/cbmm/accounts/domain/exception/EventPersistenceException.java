package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when an event fails to persist
 */
public class EventPersistenceException extends RuntimeException {

    public EventPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static EventPersistenceException forEvent(String eventId, Throwable cause) {
        return new EventPersistenceException("Failed to persist event: " + eventId, cause);
    }
}

