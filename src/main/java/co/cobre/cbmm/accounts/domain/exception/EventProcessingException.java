package co.cobre.cbmm.accounts.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when processing a CBMM event fails
 */
@Getter
public class EventProcessingException extends RuntimeException {

    private final String eventId;

    public EventProcessingException(String eventId, String message, Throwable cause) {
        super(String.format("Failed to process event %s: %s", eventId, message), cause);
        this.eventId = eventId;
    }

}

