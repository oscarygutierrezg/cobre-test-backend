package co.cobre.cbmm.accounts.unit.adapters.in.rest;

import co.cobre.cbmm.accounts.adapters.in.rest.GlobalExceptionHandler;
import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GlobalExceptionHandler.
 * Asegura cobertura completa de todos los manejadores de excepciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Tests Unitarios")
class GlobalExceptionHandlerUnitTest {

    @Mock
    private ErrorMetricsService errorMetricsService;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(errorMetricsService);
    }

    @Test
    @DisplayName("handleInvalidCurrency() - Debe retornar BAD_REQUEST con mensaje de error")
    void handleInvalidCurrency_ShouldReturnBadRequestWithErrorMessage() {
        // Given
        String errorMessage = "Invalid currency: XYZ";
        InvalidCurrencyException exception = new InvalidCurrencyException(errorMessage);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleInvalidCurrency(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Invalid Currency", errorResponse.error());
        assertEquals(errorMessage, errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("invalid_currency", "currency");
    }

    @Test
    @DisplayName("handleAccountNotFound() - Debe retornar NOT_FOUND con mensaje de error")
    void handleAccountNotFound_ShouldReturnNotFoundWithErrorMessage() {
        // Given
        String accountNumber = "ACC123456";
        AccountNotFoundException exception = new AccountNotFoundException(accountNumber);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleAccountNotFound(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(404, errorResponse.status());
        assertEquals("Account Not Found", errorResponse.error());
        assertTrue(errorResponse.message().contains(accountNumber));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordError(eq("account_not_found"), eq("rest_api"), any());
    }

    @Test
    @DisplayName("handleIllegalArgument() - Debe retornar BAD_REQUEST con mensaje de error")
    void handleIllegalArgument_ShouldReturnBadRequestWithErrorMessage() {
        // Given
        String errorMessage = "Page number must not be negative";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleIllegalArgument(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Invalid Request", errorResponse.error());
        assertEquals(errorMessage, errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordError(eq("illegal_argument"), eq("rest_api"), any());
    }

    @Test
    @DisplayName("handleFileParsingException() - Debe retornar BAD_REQUEST con mensaje de error")
    void handleFileParsingException_ShouldReturnBadRequestWithErrorMessage() {
        // Given
        String errorMessage = "Failed to parse JSON file";
        FileParsingException exception = new FileParsingException(errorMessage);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleFileParsingException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("File Parsing Error", errorResponse.error());
        assertEquals(errorMessage, errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("file_parsing_error", "file_content");
    }

    @Test
    @DisplayName("handleMissingServletRequestParameterException() - Debe retornar BAD_REQUEST con detalles del parámetro")
    void handleMissingServletRequestParameterException_ShouldReturnBadRequestWithParameterDetails() {
        // Given
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException(
            "page", "int"
        );

        // When
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler
            .handleMissingServletRequestParameterException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("status"));
        assertEquals("Missing Required Parameter", responseBody.get("error"));
        assertNotNull(responseBody.get("errors"));
        assertNotNull(responseBody.get("timestamp"));

        verify(errorMetricsService).recordValidationError("missing_parameter", "page");
    }

    @Test
    @DisplayName("handleValidationExceptions() - Debe retornar BAD_REQUEST con errores de validación")
    void handleValidationExceptions_ShouldReturnBadRequestWithValidationErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("account", "accountNumber", "must not be null");
        FieldError fieldError2 = new FieldError("account", "currency", "must not be blank");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler
            .handleValidationExceptions(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("status"));
        assertEquals("Validation Failed", responseBody.get("error"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) responseBody.get("errors");
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertTrue(errors.containsKey("accountNumber"));
        assertTrue(errors.containsKey("currency"));

        verify(errorMetricsService, times(2)).recordValidationError(eq("field_validation"), anyString());
    }

    @Test
    @DisplayName("handleGenericException() - Debe retornar INTERNAL_SERVER_ERROR para excepciones genéricas")
    void handleGenericException_ShouldReturnInternalServerErrorForGenericExceptions() {
        // Given
        String errorMessage = "Unexpected error occurred";
        RuntimeException exception = new RuntimeException(errorMessage);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.status());
        assertEquals("Internal Server Error", errorResponse.error());
        assertTrue(errorResponse.message().contains(errorMessage));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordError(eq("generic_exception"), eq("rest_api"), any());
    }

    @Test
    @DisplayName("handleOptimisticLockingFailure() - Debe retornar CONFLICT con mensaje apropiado")
    void handleOptimisticLockingFailure_ShouldReturnConflictWithAppropriateMessage() {
        // Given
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException(
            "Row was updated or deleted by another transaction"
        );

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleOptimisticLockingFailure(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(409, errorResponse.status());
        assertEquals("Concurrent Modification", errorResponse.error());
        assertEquals("The resource was modified by another transaction. Please retry.", errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordOptimisticLockingFailure("Unknown", "rest_api");
    }

    @Test
    @DisplayName("handleDuplicateEvent() - Debe retornar CONFLICT con mensaje de evento duplicado")
    void handleDuplicateEvent_ShouldReturnConflictWithDuplicateEventMessage() {
        // Given
        String eventId = "cbmm_001";
        DuplicateEventException exception = new DuplicateEventException(eventId);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleDuplicateEvent(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(409, errorResponse.status());
        assertEquals("Duplicate Event", errorResponse.error());
        assertTrue(errorResponse.message().contains(eventId));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordDuplicateEventError(eventId);
    }

    @Test
    @DisplayName("handleInsufficientBalance() - Debe retornar BAD_REQUEST con mensaje de saldo insuficiente")
    void handleInsufficientBalance_ShouldReturnBadRequestWithInsufficientBalanceMessage() {
        // Given
        String accountNumber = "ACC123";
        InsufficientBalanceException exception = new InsufficientBalanceException(accountNumber, new BigDecimal("100.00"), new BigDecimal("50.00"));

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleInsufficientBalance(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Insufficient Balance", errorResponse.error());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordInsufficientBalanceError(accountNumber, "UNKNOWN");
    }

    @Test
    @DisplayName("handleInactiveAccount() - Debe retornar BAD_REQUEST con mensaje de cuenta inactiva")
    void handleInactiveAccount_ShouldReturnBadRequestWithInactiveAccountMessage() {
        // Given
        String accountNumber = "ACC456";
        InactiveAccountException exception = new InactiveAccountException(accountNumber);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleInactiveAccount(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Inactive Account", errorResponse.error());
        assertTrue(errorResponse.message().contains(accountNumber));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("inactive_account", accountNumber);
    }

    @Test
    @DisplayName("handleEventPersistence() - Debe retornar INTERNAL_SERVER_ERROR")
    void handleEventPersistence_ShouldReturnInternalServerError() {
        // Given
        String eventId = "cbmm_001";
        EventPersistenceException exception = new EventPersistenceException(eventId, new Exception("Database error"));

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleEventPersistence(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.status());
        assertEquals("Event Persistence Error", errorResponse.error());
        assertTrue(errorResponse.message().contains(eventId));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordPersistenceError(eq("CBMMEvent"), eq("persist"), any());
    }

    @Test
    @DisplayName("handleInvalidFile() - Debe retornar BAD_REQUEST con mensaje de archivo inválido")
    void handleInvalidFile_ShouldReturnBadRequestWithInvalidFileMessage() {
        // Given
        String errorMessage = "File cannot be empty";
        InvalidFileException exception = new InvalidFileException(errorMessage);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleInvalidFile(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Invalid File", errorResponse.error());
        assertEquals(errorMessage, errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("invalid_file", "file");
    }

    @Test
    @DisplayName("handleEmptyBatch() - Debe retornar BAD_REQUEST con mensaje de batch vacío")
    void handleEmptyBatch_ShouldReturnBadRequestWithEmptyBatchMessage() {
        // Given
        EmptyBatchException exception = new EmptyBatchException("No valid events found in the batch");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleEmptyBatch(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("Empty Batch", errorResponse.error());
        assertNotNull(errorResponse.message());
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("empty_batch", "batch");
    }

    @Test
    @DisplayName("handleFileSizeExceeded() - Debe retornar BAD_REQUEST con mensaje de tamaño excedido")
    void handleFileSizeExceeded_ShouldReturnBadRequestWithFileSizeExceededMessage() {
        // Given
        long fileSize = 15 * 1024 * 1024; // 15 MB
        long maxSize = 10 * 1024 * 1024; // 10 MB
        FileSizeExceededException exception = new FileSizeExceededException(fileSize, maxSize);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleFileSizeExceeded(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.status());
        assertEquals("File Size Exceeded", errorResponse.error());
        assertTrue(errorResponse.message().contains("15.00 MB"));
        assertTrue(errorResponse.message().contains("10.00 MB"));
        assertNotNull(errorResponse.timestamp());

        verify(errorMetricsService).recordValidationError("file_size_exceeded", "file_size");
    }

    @Test
    @DisplayName("ErrorResponse record - Debe tener todos los campos correctos")
    void errorResponse_ShouldHaveAllFieldsCorrect() {
        // Given
        InvalidCurrencyException exception = new InvalidCurrencyException("Test");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler
            .handleInvalidCurrency(exception);

        // Then
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);

        // Verify all record fields are accessible
        assertEquals(400, errorResponse.status());
        assertEquals("Invalid Currency", errorResponse.error());
        assertEquals("Test", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }
}

