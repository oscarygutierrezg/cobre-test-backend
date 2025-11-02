package co.cobre.cbmm.accounts.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when trying to process a duplicate event
 */
@Getter
public class DuplicateEventException extends RuntimeException {

    private final String eventId;

    public DuplicateEventException(String eventId) {
        super(String.format("Event %s has already been processed", eventId));
        this.eventId = eventId;
    }
}

