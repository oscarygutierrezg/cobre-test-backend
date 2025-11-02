package co.cobre.cbmm.accounts.unit.adapters.in.rest;

import co.cobre.cbmm.accounts.adapters.in.rest.BatchEventController;
import co.cobre.cbmm.accounts.adapters.in.rest.GlobalExceptionHandler;
import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.application.dto.BatchProcessingResponseDTO;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.application.service.BatchEventProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BatchEventControllerUnitTest {

    @Mock
    private BatchEventProcessingService batchEventProcessingService;

    @Mock
    private ErrorMetricsService errorMetricsService;

    @InjectMocks
    private BatchEventController batchEventController;

    private MockMvc mockMvc;
    private List<CBMMEventDTO> sampleEvents;
    private BatchProcessingResponseDTO successResponse;

    @BeforeEach
    void setUp() {
        // Setup MockMvc with GlobalExceptionHandler to catch exceptions
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(errorMetricsService);
        mockMvc = MockMvcBuilders.standaloneSetup(batchEventController)
            .setControllerAdvice(globalExceptionHandler)
            .build();

        // Create sample events
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

        CBMMEventDTO event1 = new CBMMEventDTO(
            "cbmm_20250101_000001",
            "cross_border_money_movement",
            ZonedDateTime.now(),
            origin,
            destination
        );

        sampleEvents = Collections.singletonList(event1);

        LocalDateTime now = LocalDateTime.now();
        successResponse = new BatchProcessingResponseDTO(
            "batch-123",
            1,
            1,
            0,
            now,
            now.plusSeconds(1),
            1000L,
            Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Given valid file with events, when uploadEventFile, then process successfully")
    void givenValidFileWithEvents_whenUploadEventFile_thenProcessSuccessfully() throws Exception {
        // Arrange
        String jsonContent = "[{\"event_id\":\"cbmm_20250101_000001\"}]";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes()
        );

        when(batchEventProcessingService.parseEventFile(any())).thenReturn(sampleEvents);
        when(batchEventProcessingService.processBatch(sampleEvents)).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.batchId").value("batch-123"))
            .andExpect(jsonPath("$.totalEvents").value(1))
            .andExpect(jsonPath("$.successfulEvents").value(1))
            .andExpect(jsonPath("$.failedEvents").value(0));

        verify(batchEventProcessingService).parseEventFile(any());
        verify(batchEventProcessingService).processBatch(sampleEvents);
    }

    @Test
    @DisplayName("Given empty file, when uploadEventFile, then return 400")
    void givenEmptyFile_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.json",
            MediaType.APPLICATION_JSON_VALUE,
            new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());

        verify(batchEventProcessingService, never()).parseEventFile(any());
        verify(batchEventProcessingService, never()).processBatch(any());
    }

    @Test
    @DisplayName("Given file with no valid events, when uploadEventFile, then return 400")
    void givenFileWithNoValidEvents_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange
        String jsonContent = "[]";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes()
        );

        when(batchEventProcessingService.parseEventFile(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());

        verify(batchEventProcessingService).parseEventFile(any());
        verify(batchEventProcessingService, never()).processBatch(any());
    }

    @Test
    @DisplayName("Given file exceeding size limit, when uploadEventFile, then return 400")
    void givenFileExceedingSizeLimit_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange - Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.json",
            MediaType.APPLICATION_JSON_VALUE,
            largeContent
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(largeFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("File Size Exceeded"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());

        verify(batchEventProcessingService, never()).parseEventFile(any());
        verify(batchEventProcessingService, never()).processBatch(any());
    }

    @Test
    @DisplayName("Given batch with partial failures, when uploadEventFile, then return success with failure details")
    void givenBatchWithPartialFailures_whenUploadEventFile_thenReturnSuccessWithFailureDetails() throws Exception {
        // Arrange
        String jsonContent = "[{\"event_id\":\"cbmm_1\"},{\"event_id\":\"cbmm_2\"}]";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes()
        );

        List<BatchProcessingResponseDTO.EventProcessingResult> results = List.of(
            new BatchProcessingResponseDTO.EventProcessingResult("cbmm_1", true, "Success", null),
            new BatchProcessingResponseDTO.EventProcessingResult("cbmm_2", false, "Processing failed", "Error details")
        );

        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResponseDTO partialResponse = new BatchProcessingResponseDTO(
            "batch-456",
            2,
            1,
            1,
            now,
            now.plusSeconds(2),
            2000L,
            results
        );

        when(batchEventProcessingService.parseEventFile(any())).thenReturn(sampleEvents);
        when(batchEventProcessingService.processBatch(any())).thenReturn(partialResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value("batch-456"))
            .andExpect(jsonPath("$.totalEvents").value(2))
            .andExpect(jsonPath("$.successfulEvents").value(1))
            .andExpect(jsonPath("$.failedEvents").value(1));

        verify(batchEventProcessingService).parseEventFile(any());
        verify(batchEventProcessingService).processBatch(any());
    }

    @Test
    @DisplayName("Given JSONL file format, when uploadEventFile, then process successfully")
    void givenJsonlFileFormat_whenUploadEventFile_thenProcessSuccessfully() throws Exception {
        // Arrange
        String jsonlContent = "{\"event_id\":\"cbmm_1\"}\n{\"event_id\":\"cbmm_2\"}";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.jsonl",
            "application/x-ndjson",
            jsonlContent.getBytes()
        );

        when(batchEventProcessingService.parseEventFile(any())).thenReturn(sampleEvents);
        when(batchEventProcessingService.processBatch(sampleEvents)).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value("batch-123"));

        verify(batchEventProcessingService).parseEventFile(any());
        verify(batchEventProcessingService).processBatch(sampleEvents);
    }
}

