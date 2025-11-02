package co.cobre.cbmm.accounts.adapters.in.rest;

import co.cobre.cbmm.accounts.application.dto.BatchProcessingResponseDTO;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.application.service.BatchEventProcessingService;
import co.cobre.cbmm.accounts.domain.exception.EmptyBatchException;
import co.cobre.cbmm.accounts.domain.exception.FileSizeExceededException;
import co.cobre.cbmm.accounts.domain.exception.InvalidFileException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for batch event processing (Driving Adapter)
 * Allows uploading files with multiple CBMM events
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Batch Events", description = "Batch processing operations for CBMM events")
@RequiredArgsConstructor
@Slf4j
public class BatchEventController {

    private final BatchEventProcessingService batchEventProcessingService;

    @Operation(
        summary = "Upload and process batch of CBMM events",
        description = "Upload a file containing multiple CBMM events in JSON or JSONL format. " +
            "Supported formats: .json (array), .jsonl, .ndjson (JSON Lines). " +
            "Events are processed concurrently using Virtual Threads for high performance."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch processed successfully",
            content = @Content(schema = @Schema(implementation = BatchProcessingResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format or content"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during batch processing"
        )
    })
    @PostMapping(
        value = "/batch/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BatchProcessingResponseDTO> uploadEventFile(
        @Parameter(
            description = "File containing CBMM events (JSON array or JSON Lines format)",
            required = true
        )
        @RequestParam("file") MultipartFile file
    ) {
        log.info("Received batch upload request: file={}, size={} bytes",
            file.getOriginalFilename(), file.getSize());

        // Validate file
        if (file.isEmpty()) {
            throw new InvalidFileException("File cannot be empty");
        }

        // Validate file size (max 10MB)
        long maxSizeBytes = 10L * 1024 * 1024; // 10MB
        if (file.getSize() > maxSizeBytes) {
            throw new FileSizeExceededException(file.getSize(), maxSizeBytes);
        }

        // Parse events from file
        List<CBMMEventDTO> events = batchEventProcessingService.parseEventFile(file);

        // Validate event count
        if (events.isEmpty()) {
            throw new EmptyBatchException("File contains no valid events");
        }

        // Process batch
        BatchProcessingResponseDTO response = batchEventProcessingService.processBatch(events);

        log.info("Batch upload completed: batchId={}, total={}, success={}, failed={}",
            response.batchId(), response.totalEvents(),
            response.successfulEvents(), response.failedEvents());

        return ResponseEntity.ok(response);
    }
}

