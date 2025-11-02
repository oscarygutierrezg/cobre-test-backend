package co.cobre.cbmm.accounts.application.usecase;

import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.application.service.AsyncAccountProcessingService;
import co.cobre.cbmm.accounts.domain.exception.DuplicateEventException;
import co.cobre.cbmm.accounts.domain.exception.EventPersistenceException;
import co.cobre.cbmm.accounts.ports.in.ProcessCBMMEventPort;
import co.cobre.cbmm.accounts.ports.out.CBMMEventRepositoryPort;
import co.cobre.cbmm.accounts.ports.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for processing CBMM events from Kafka
 * Uses Virtual Threads for parallel processing of origin and destination accounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessCBMMEventUseCase implements ProcessCBMMEventPort {

    private final IdempotencyPort idempotencyPort;
    private final AsyncAccountProcessingService asyncAccountProcessingService;
    private final CBMMEventRepositoryPort cbmmEventRepositoryPort;

    private static final long EVENT_TTL_SECONDS = 86400;

    @Override
    @Transactional
    public void processCBMMEvent(CBMMEventDTO event) {
        log.info("Processing CBMM event: {} - Type: {}", event.eventId(), event.eventType());

        if (idempotencyPort.isEventProcessed(event.eventId())) {
            log.warn("Event {} already processed, skipping", event.eventId());
            throw new DuplicateEventException(event.eventId());
        }

        try {
            cbmmEventRepositoryPort.save(event);
            log.info("CBMM event {} persisted with PENDING status", event.eventId());
        } catch (Exception e) {
            log.error("Failed to persist CBMM event {}: {}", event.eventId(), e.getMessage());
            throw EventPersistenceException.forEvent(event.eventId(), e);
        }

        try {
            cbmmEventRepositoryPort.updateStatus(event.eventId(), "PROCESSING", 0);

            validateEvent(event);

            log.debug("Starting parallel processing with Virtual Threads for event: {}", event.eventId());

            CompletableFuture<UUID> originFuture = asyncAccountProcessingService.processOriginAccountAsync(event);
            CompletableFuture<UUID> destinationFuture = asyncAccountProcessingService.processDestinationAccountAsync(event);

            CompletableFuture.allOf(originFuture, destinationFuture).join();

            UUID originAccountId = originFuture.join();
            UUID destinationAccountId = destinationFuture.join();

            cbmmEventRepositoryPort.updateStatus(event.eventId(), "COMPLETED", 0);
            idempotencyPort.markEventAsProcessed(event.eventId(), EVENT_TTL_SECONDS);

            log.info("CBMM event {} processed successfully with Virtual Threads. Origin: {}, Destination: {}",
                event.eventId(), originAccountId, destinationAccountId);

        } catch (Exception e) {
            log.error("Error processing CBMM event {}: {}", event.eventId(), e.getMessage(), e);

            try {
                cbmmEventRepositoryPort.updateStatus(event.eventId(), "FAILED", 0);
            } catch (Exception updateEx) {
                log.error("Failed to update event status to FAILED: {}", updateEx.getMessage());
            }

            throw e;
        }
    }

    private void validateEvent(CBMMEventDTO event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (event.origin() == null) {
            throw new IllegalArgumentException("Origin account information is required");
        }
        if (event.destination() == null) {
            throw new IllegalArgumentException("Destination account information is required");
        }
        if (event.origin().amount() == null || event.origin().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Origin amount must be positive");
        }
        if (event.destination().amount() == null || event.destination().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Destination amount must be positive");
        }
    }
}

