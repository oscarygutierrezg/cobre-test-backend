package co.cobre.cbmm.accounts.unit.adapters.in.messaging;

import co.cobre.cbmm.accounts.adapters.in.messaging.KafkaEventConsumer;
import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.domain.exception.DuplicateEventException;
import co.cobre.cbmm.accounts.domain.exception.EventProcessingException;
import co.cobre.cbmm.accounts.ports.in.ProcessCBMMEventPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventConsumer Unit Tests")
class KafkaEventConsumerUnitTest {

    @Mock
    private ProcessCBMMEventPort processCBMMEventPort;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ErrorMetricsService errorMetricsService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaEventConsumer kafkaEventConsumer;

    private String validJsonMessage;
    private CBMMEventDTO validEvent;

    @BeforeEach
    void setUp() {
        validJsonMessage = """
            {
                "event_id": "cbmm_20250909_000123",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T15:32:10Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "COP",
                    "amount": 15000.50
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 880.25
                }
            }
            """;

        validEvent = new CBMMEventDTO(
            "cbmm_20250909_000123",
            "cross_border_money_movement",
            ZonedDateTime.parse("2025-09-09T15:32:10Z"),
            new CBMMEventDTO.AccountOperationDTO("ACC123456789", "COP", new BigDecimal("15000.50")),
            new CBMMEventDTO.AccountOperationDTO("ACC987654321", "USD", new BigDecimal("880.25"))
        );
    }

    @Test
    @DisplayName("Given valid message, when consumeCBMMEvent, then process successfully and acknowledge")
    void givenValidMessage_whenConsumeCBMMEvent_thenProcessSuccessfullyAndAcknowledge() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doNothing().when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 0, 123L, acknowledgment);

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(errorMetricsService);
    }

    @Test
    @DisplayName("Given duplicate event, when consumeCBMMEvent, then log warning and acknowledge")
    void givenDuplicateEvent_whenConsumeCBMMEvent_thenLogWarningAndAcknowledge() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doThrow(new DuplicateEventException("cbmm_20250909_000123"))
            .when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 0, 123L, acknowledgment);

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(acknowledgment).acknowledge();
        verify(errorMetricsService).recordDuplicateEventError("cbmm_20250909_000123");
    }

    @Test
    @DisplayName("Given invalid JSON, when consumeCBMMEvent, then throw EventProcessingException")
    void givenInvalidJson_whenConsumeCBMMEvent_thenThrowEventProcessingException() throws Exception {
        // Arrange
        String invalidJson = "{ invalid json }";
        when(objectMapper.readValue(invalidJson, CBMMEventDTO.class))
            .thenThrow(new RuntimeException("Invalid JSON format"));

        // Act & Assert
        assertThrows(EventProcessingException.class, () ->
            kafkaEventConsumer.consumeCBMMEvent(invalidJson, "cbmm-events", 0, 123L, acknowledgment)
        );

        verify(objectMapper).readValue(invalidJson, CBMMEventDTO.class);
        verify(processCBMMEventPort, never()).processCBMMEvent(any());
        verify(acknowledgment, never()).acknowledge();
        verify(errorMetricsService).recordKafkaError(eq("cbmm-events"), eq("unknown"), any(Exception.class));
    }

    @Test
    @DisplayName("Given processing error, when consumeCBMMEvent, then throw EventProcessingException and don't acknowledge")
    void givenProcessingError_whenConsumeCBMMEvent_thenThrowEventProcessingExceptionAndDontAcknowledge() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        RuntimeException processingError = new RuntimeException("Database connection failed");
        doThrow(processingError).when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act & Assert
        assertThrows(EventProcessingException.class, () ->
            kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 0, 123L, acknowledgment)
        );

        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(acknowledgment, never()).acknowledge();
        verify(errorMetricsService).recordKafkaError(eq("cbmm-events"), eq("unknown"), any(Exception.class));
    }

    @Test
    @DisplayName("Given null acknowledgment, when consumeCBMMEvent with valid message, then process without error")
    void givenNullAcknowledgment_whenConsumeCBMMEventWithValidMessage_thenProcessWithoutError() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doNothing().when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        assertDoesNotThrow(() ->
            kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 0, 123L, null)
        );

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verifyNoInteractions(errorMetricsService);
    }

    @Test
    @DisplayName("Given null acknowledgment and duplicate event, when consumeCBMMEvent, then handle without NPE")
    void givenNullAcknowledgmentAndDuplicateEvent_whenConsumeCBMMEvent_thenHandleWithoutNPE() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doThrow(new DuplicateEventException("cbmm_20250909_000123"))
            .when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        assertDoesNotThrow(() ->
            kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 0, 123L, null)
        );

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(errorMetricsService).recordDuplicateEventError("cbmm_20250909_000123");
    }

    @Test
    @DisplayName("Given message with different partition, when consumeCBMMEvent, then process successfully")
    void givenMessageWithDifferentPartition_whenConsumeCBMMEvent_thenProcessSuccessfully() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doNothing().when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events", 5, 999L, acknowledgment);

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Given event with different topic, when consumeCBMMEvent, then process successfully")
    void givenEventWithDifferentTopic_whenConsumeCBMMEvent_thenProcessSuccessfully() throws Exception {
        // Arrange
        when(objectMapper.readValue(validJsonMessage, CBMMEventDTO.class)).thenReturn(validEvent);
        doNothing().when(processCBMMEventPort).processCBMMEvent(any(CBMMEventDTO.class));

        // Act
        kafkaEventConsumer.consumeCBMMEvent(validJsonMessage, "cbmm-events-v2", 0, 123L, acknowledgment);

        // Assert
        verify(objectMapper).readValue(validJsonMessage, CBMMEventDTO.class);
        verify(processCBMMEventPort).processCBMMEvent(validEvent);
        verify(acknowledgment).acknowledge();
    }
}

