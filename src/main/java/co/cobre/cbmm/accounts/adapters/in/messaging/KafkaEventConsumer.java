package co.cobre.cbmm.accounts.adapters.in.messaging;

import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.domain.exception.DuplicateEventException;
import co.cobre.cbmm.accounts.domain.exception.EventProcessingException;
import co.cobre.cbmm.accounts.ports.in.ProcessCBMMEventPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for CBMM events with OpenTelemetry metrics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final ProcessCBMMEventPort processCBMMEventPort;
    private final ObjectMapper objectMapper;
    private final ErrorMetricsService errorMetricsService;

    @KafkaListener(
        topics = "${spring.kafka.topics.cbmm-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCBMMEvent(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        log.info("Received message from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        log.debug("Message content: {}", message);

        try {
            // Parse JSON to DTO
            CBMMEventDTO event = objectMapper.readValue(message, CBMMEventDTO.class);

            log.info("Parsed CBMM event: {}", event.eventId());

            // Process event
            processCBMMEventPort.processCBMMEvent(event);

            // Acknowledge message after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("Successfully processed and acknowledged CBMM event: {}", event.eventId());

        } catch (DuplicateEventException e) {
            log.warn("Duplicate event detected: {}", e.getMessage());

            // Record metric
            errorMetricsService.recordDuplicateEventError(e.getEventId());

            // Acknowledge duplicate events to avoid reprocessing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing CBMM event from topic {}: {}", topic, e.getMessage(), e);

            // Record metric
            errorMetricsService.recordKafkaError(topic, "unknown", e);

            // Don't acknowledge - message will be retried based on Kafka configuration
            throw new EventProcessingException("unknown", e.getMessage(), e);
        }
    }
}

