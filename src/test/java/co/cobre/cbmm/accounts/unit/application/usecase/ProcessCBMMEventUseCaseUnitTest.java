package co.cobre.cbmm.accounts.unit.application.usecase;

import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.application.service.AsyncAccountProcessingService;
import co.cobre.cbmm.accounts.application.usecase.ProcessCBMMEventUseCase;
import co.cobre.cbmm.accounts.domain.exception.DuplicateEventException;
import co.cobre.cbmm.accounts.domain.exception.EventPersistenceException;
import co.cobre.cbmm.accounts.ports.out.CBMMEventRepositoryPort;
import co.cobre.cbmm.accounts.ports.out.IdempotencyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessCBMMEventUseCaseUnitTest {

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock
    private AsyncAccountProcessingService asyncAccountProcessingService;

    @Mock
    private CBMMEventRepositoryPort cbmmEventRepositoryPort;

    @InjectMocks
    private ProcessCBMMEventUseCase processCBMMEventUseCase;

    private CBMMEventDTO validEvent;
    private String eventId;

    @BeforeEach
    void setUp() {
        eventId = "cbmm_20250101_000001";

        CBMMEventDTO.AccountOperationDTO origin = new CBMMEventDTO.AccountOperationDTO(
            "ACC-ORIGIN-001",
            "USD",
            new BigDecimal("100.00")
        );

        CBMMEventDTO.AccountOperationDTO destination = new CBMMEventDTO.AccountOperationDTO(
            "ACC-DEST-001",
            "USD",
            new BigDecimal("100.00")
        );

        validEvent = new CBMMEventDTO(
            eventId,
            "cross_border_money_movement",
            ZonedDateTime.now(),
            origin,
            destination
        );
    }

    @Nested
    @DisplayName("Process CBMM event tests")
    class ProcessCBMMEventTests {

        @Test
        @DisplayName("Given valid event, when processCBMMEvent, then process successfully")
        void givenValidEvent_whenProcessCBMMEvent_thenProcessSuccessfully() {
            // Arrange
            UUID originAccountId = UUID.randomUUID();
            UUID destinationAccountId = UUID.randomUUID();

            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);
            doNothing().when(cbmmEventRepositoryPort).updateStatus(eq(eventId), anyString(), anyInt());

            when(asyncAccountProcessingService.processOriginAccountAsync(validEvent))
                .thenReturn(CompletableFuture.completedFuture(originAccountId));
            when(asyncAccountProcessingService.processDestinationAccountAsync(validEvent))
                .thenReturn(CompletableFuture.completedFuture(destinationAccountId));

            // Act
            processCBMMEventUseCase.processCBMMEvent(validEvent);

            // Assert
            verify(idempotencyPort).isEventProcessed(eventId);
            verify(cbmmEventRepositoryPort).save(validEvent);
            verify(cbmmEventRepositoryPort).updateStatus(eventId, "PROCESSING", 0);
            verify(asyncAccountProcessingService).processOriginAccountAsync(validEvent);
            verify(asyncAccountProcessingService).processDestinationAccountAsync(validEvent);
            verify(cbmmEventRepositoryPort).updateStatus(eventId, "COMPLETED", 0);
            verify(idempotencyPort).markEventAsProcessed(eq(eventId), anyLong());
        }

        @Test
        @DisplayName("Given already processed event, when processCBMMEvent, then throw DuplicateEventException")
        void givenAlreadyProcessedEvent_whenProcessCBMMEvent_thenThrowDuplicateEventException() {
            // Arrange
            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateEventException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(validEvent));

            verify(idempotencyPort).isEventProcessed(eventId);
            verify(cbmmEventRepositoryPort, never()).save(any());
            verify(asyncAccountProcessingService, never()).processOriginAccountAsync(any());
        }

        @Test
        @DisplayName("Given event persistence fails, when processCBMMEvent, then throw EventPersistenceException")
        void givenEventPersistenceFails_whenProcessCBMMEvent_thenThrowEventPersistenceException() {
            // Arrange
            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);
            doThrow(new RuntimeException("DB error")).when(cbmmEventRepositoryPort).save(validEvent);

            // Act & Assert
            assertThrows(EventPersistenceException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(validEvent));

            verify(cbmmEventRepositoryPort).save(validEvent);
            verify(asyncAccountProcessingService, never()).processOriginAccountAsync(any());
        }

        @Test
        @DisplayName("Given event with null eventId, when processCBMMEvent, then throw IllegalArgumentException")
        void givenEventWithNullEventId_whenProcessCBMMEvent_thenThrowIllegalArgumentException() {
            // Arrange
            CBMMEventDTO invalidEvent = new CBMMEventDTO(
                null,
                "cross_border_money_movement",
                ZonedDateTime.now(),
                validEvent.origin(),
                validEvent.destination()
            );

            when(idempotencyPort.isEventProcessed(null)).thenReturn(false);
            doNothing().when(cbmmEventRepositoryPort).updateStatus(isNull(), eq("PROCESSING"), anyInt());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with null origin, when processCBMMEvent, then throw IllegalArgumentException")
        void givenEventWithNullOrigin_whenProcessCBMMEvent_thenThrowIllegalArgumentException() {
            // Arrange
            CBMMEventDTO invalidEvent = new CBMMEventDTO(
                eventId,
                "cross_border_money_movement",
                ZonedDateTime.now(),
                null,
                validEvent.destination()
            );

            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);
            doNothing().when(cbmmEventRepositoryPort).updateStatus(eq(eventId), eq("PROCESSING"), anyInt());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with null destination, when processCBMMEvent, then throw IllegalArgumentException")
        void givenEventWithNullDestination_whenProcessCBMMEvent_thenThrowIllegalArgumentException() {
            // Arrange
            CBMMEventDTO invalidEvent = new CBMMEventDTO(
                eventId,
                "cross_border_money_movement",
                ZonedDateTime.now(),
                validEvent.origin(),
                null
            );

            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with zero origin amount, when processCBMMEvent, then throw IllegalArgumentException")
        void givenEventWithZeroOriginAmount_whenProcessCBMMEvent_thenThrowIllegalArgumentException() {
            // Arrange
            CBMMEventDTO.AccountOperationDTO invalidOrigin = new CBMMEventDTO.AccountOperationDTO(
                "ACC-ORIGIN-001",
                "USD",
                BigDecimal.ZERO
            );

            CBMMEventDTO invalidEvent = new CBMMEventDTO(
                eventId,
                "cross_border_money_movement",
                ZonedDateTime.now(),
                invalidOrigin,
                validEvent.destination()
            );

            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with negative destination amount, when processCBMMEvent, then throw IllegalArgumentException")
        void givenEventWithNegativeDestinationAmount_whenProcessCBMMEvent_thenThrowIllegalArgumentException() {
            // Arrange
            CBMMEventDTO.AccountOperationDTO invalidDestination = new CBMMEventDTO.AccountOperationDTO(
                "ACC-DEST-001",
                "USD",
                new BigDecimal("-100.00")
            );

            CBMMEventDTO invalidEvent = new CBMMEventDTO(
                eventId,
                "cross_border_money_movement",
                ZonedDateTime.now(),
                validEvent.origin(),
                invalidDestination
            );

            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> processCBMMEventUseCase.processCBMMEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given processing fails, when processCBMMEvent, then update status to FAILED")
        void givenProcessingFails_whenProcessCBMMEvent_thenUpdateStatusToFailed() {
            // Arrange
            when(idempotencyPort.isEventProcessed(eventId)).thenReturn(false);
            doNothing().when(cbmmEventRepositoryPort).updateStatus(eq(eventId), anyString(), anyInt());

            when(asyncAccountProcessingService.processOriginAccountAsync(validEvent))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing error")));

            // Act & Assert
            assertThrows(Exception.class,
                () -> processCBMMEventUseCase.processCBMMEvent(validEvent));

            verify(cbmmEventRepositoryPort).updateStatus(eventId, "FAILED", 0);
        }
    }
}

