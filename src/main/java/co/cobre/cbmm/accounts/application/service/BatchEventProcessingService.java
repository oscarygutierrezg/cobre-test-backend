package co.cobre.cbmm.accounts.application.service;

import co.cobre.cbmm.accounts.application.dto.BatchProcessingResponseDTO;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.domain.exception.FileParsingException;
import co.cobre.cbmm.accounts.ports.in.ProcessCBMMEventPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for batch processing of CBMM events from files
 * Supports JSON and JSONL (JSON Lines) formats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchEventProcessingService {

    private final ProcessCBMMEventPort processCBMMEventPort;
    private final ObjectMapper objectMapper;

    /**
     * Parse event file and extract CBMM events
     * Supports:
     * - JSON array: [{"event_id": "..."}, ...]
     * - JSONL: One JSON object per line
     */
    public List<CBMMEventDTO> parseEventFile(MultipartFile file) {
        log.info("Parsing event file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

        try {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isEmpty()) {
                throw new IllegalArgumentException("File name is required");
            }

            // Determine file format
            if (filename.endsWith(".json")) {
                return parseJsonArrayFile(file);
            } else if (filename.endsWith(".jsonl") || filename.endsWith(".ndjson")) {
                return parseJsonLinesFile(file);
            } else {
                throw new IllegalArgumentException(
                    "Unsupported file format. Supported formats: .json, .jsonl, .ndjson"
                );
            }
        } catch (Exception e) {
            log.error("Error parsing event file: {}", e.getMessage(), e);
            throw new FileParsingException("Failed to parse event file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON array file format
     * Example: [{"event_id": "...", "event_type": "...", ...}, ...]
     */
    private List<CBMMEventDTO> parseJsonArrayFile(MultipartFile file) throws Exception {
        log.debug("Parsing JSON array file");

        CBMMEventDTO[] events = objectMapper.readValue(
            file.getInputStream(),
            CBMMEventDTO[].class
        );

        List<CBMMEventDTO> eventList = List.of(events);
        log.info("Parsed {} events from JSON array file", eventList.size());

        return eventList;
    }

    /**
     * Parse JSON Lines (JSONL) file format
     * Example:
     * {"event_id": "...", "event_type": "...", ...}
     * {"event_id": "...", "event_type": "...", ...}
     */
    private List<CBMMEventDTO> parseJsonLinesFile(MultipartFile file) throws Exception {
        log.debug("Parsing JSON Lines file");

        List<CBMMEventDTO> events = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream()))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    CBMMEventDTO event = objectMapper.readValue(line, CBMMEventDTO.class);
                    events.add(event);
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {}", lineNumber, e.getMessage());
                    throw new FileParsingException(
                        "Invalid JSON at line " + lineNumber + ": " + e.getMessage(), e
                    );
                }
            }
        }

        log.info("Parsed {} events from JSON Lines file", events.size());
        return events;
    }

    /**
     * Process batch of events concurrently
     * Uses Virtual Threads for high throughput
     */
    public BatchProcessingResponseDTO processBatch(List<CBMMEventDTO> events) {
        String batchId = "batch_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime startTime = LocalDateTime.now();

        log.info("Processing batch {} with {} events", batchId, events.size());

        // Process events concurrently using CompletableFuture
        List<CompletableFuture<BatchProcessingResponseDTO.EventProcessingResult>> futures =
            events.stream()
                .map(event -> CompletableFuture.supplyAsync(
                    () -> processEvent(event)
                ))
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        List<BatchProcessingResponseDTO.EventProcessingResult> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        LocalDateTime endTime = LocalDateTime.now();

        // Count successes and failures
        long successCount = results.stream().filter(BatchProcessingResponseDTO.EventProcessingResult::success).count();
        long failureCount = results.stream().filter(r -> !r.success()).count();

        BatchProcessingResponseDTO response = BatchProcessingResponseDTO.success(
            batchId,
            events.size(),
            (int) successCount,
            (int) failureCount,
            startTime,
            endTime,
            results
        );

        log.info("Batch {} completed: {} successful, {} failed, duration: {}ms",
            batchId, successCount, failureCount, response.durationMs());

        return response;
    }

    /**
     * Process single event and return result
     */
    private BatchProcessingResponseDTO.EventProcessingResult processEvent(CBMMEventDTO event) {
        try {
            log.debug("Processing event: {}", event.eventId());

            processCBMMEventPort.processCBMMEvent(event);

            return new BatchProcessingResponseDTO.EventProcessingResult(
                event.eventId(),
                true,
                "Event processed successfully",
                null
            );
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.eventId(), e.getMessage());

            return new BatchProcessingResponseDTO.EventProcessingResult(
                event.eventId(),
                false,
                "Event processing failed",
                e.getMessage()
            );
        }
    }
}

