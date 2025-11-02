package co.cobre.cbmm.accounts.ports.out;

import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;

/**
 * Port for CBMM Event persistence operations (Driven Port)
 * Allows storing and retrieving CBMM events
 */
public interface CBMMEventRepositoryPort {

    /**
     * Save CBMM event
     *
     * @param event the event to save
     * @return the saved event
     */
    CBMMEventDTO save(CBMMEventDTO event);

    /**
     * Update event status
     *
     * @param eventId the event ID
     * @param status the new status
     * @param retryCount the retry count
     */
    void updateStatus(String eventId, String status, Integer retryCount);
}

