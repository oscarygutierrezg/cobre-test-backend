package co.cobre.cbmm.accounts.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for batch processing response
 */
public record BatchProcessingResponseDTO(
    String batchId,
    int totalEvents,
    int successfulEvents,
    int failedEvents,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long durationMs,
    List<EventProcessingResult> results
) {
    public record EventProcessingResult(
        String eventId,
        boolean success,
        String message,
        String errorDetails
    ) {}

    public static BatchProcessingResponseDTO success(
        String batchId,
        int totalEvents,
        int successfulEvents,
        int failedEvents,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<EventProcessingResult> results
    ) {
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        return new BatchProcessingResponseDTO(
            batchId,
            totalEvents,
            successfulEvents,
            failedEvents,
            startTime,
            endTime,
            durationMs,
            results
        );
    }
}

