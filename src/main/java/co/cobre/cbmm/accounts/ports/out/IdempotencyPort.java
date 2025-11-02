package co.cobre.cbmm.accounts.ports.out;

/**
 * Driven port for idempotency checks using distributed cache
 */
public interface IdempotencyPort {

    /**
     * Check if an event has already been processed
     *
     * @param eventId the event ID
     * @return true if already processed, false otherwise
     */
    boolean isEventProcessed(String eventId);

    /**
     * Mark an event as processed
     *
     * @param eventId the event ID
     * @param ttlSeconds time to live in seconds
     */
    void markEventAsProcessed(String eventId, long ttlSeconds);
}

