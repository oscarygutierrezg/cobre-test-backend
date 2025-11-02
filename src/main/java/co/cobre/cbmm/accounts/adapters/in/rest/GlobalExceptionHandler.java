package co.cobre.cbmm.accounts.adapters.in.rest;

import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.exception.DuplicateEventException;
import co.cobre.cbmm.accounts.domain.exception.EmptyBatchException;
import co.cobre.cbmm.accounts.domain.exception.EventPersistenceException;
import co.cobre.cbmm.accounts.domain.exception.FileParsingException;
import co.cobre.cbmm.accounts.domain.exception.FileSizeExceededException;
import co.cobre.cbmm.accounts.domain.exception.InactiveAccountException;
import co.cobre.cbmm.accounts.domain.exception.InsufficientBalanceException;
import co.cobre.cbmm.accounts.domain.exception.InvalidCurrencyException;
import co.cobre.cbmm.accounts.domain.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers with OpenTelemetry metrics
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMetricsService errorMetricsService;

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrency(InvalidCurrencyException ex) {
        // Record metric
        errorMetricsService.recordValidationError("invalid_currency", "currency");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Currency",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }



    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        // Record metric
        errorMetricsService.recordError("account_not_found", "rest_api", ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Account Not Found",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        // Record metric
        errorMetricsService.recordError("illegal_argument", "rest_api", ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Request",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle File Parsing Exception
     */
    @ExceptionHandler(FileParsingException.class)
    public ResponseEntity<ErrorResponse> handleFileParsingException(FileParsingException ex) {
        // Record metric
        errorMetricsService.recordValidationError("file_parsing_error", "file_content");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "File Parsing Error",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        // Record metric
        errorMetricsService.recordValidationError("missing_parameter", ex.getParameterName());

        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getParameterName(), "Parameter '" + ex.getParameterName() + "' is required and was not provided");

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Missing Required Parameter");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);

            // Record metric for each validation error
            errorMetricsService.recordValidationError("field_validation", fieldName);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Record metric
        errorMetricsService.recordError("generic_exception", "rest_api", ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred: " + ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle Optimistic Locking Failures
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        // Record metric
        errorMetricsService.recordOptimisticLockingFailure("Unknown", "rest_api");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Concurrent Modification",
            "The resource was modified by another transaction. Please retry.",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle Duplicate Event Exception
     */
    @ExceptionHandler(DuplicateEventException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEvent(DuplicateEventException ex) {
        // Record metric
        errorMetricsService.recordDuplicateEventError(ex.getEventId());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Duplicate Event",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle Insufficient Balance Exception
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        // Record metric
        errorMetricsService.recordInsufficientBalanceError(ex.getAccountNumber(), "UNKNOWN");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Insufficient Balance",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle Inactive Account Exception
     */
    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ErrorResponse> handleInactiveAccount(InactiveAccountException ex) {
        // Record metric
        errorMetricsService.recordValidationError("inactive_account", ex.getAccountNumber());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Inactive Account",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle Event Persistence Exception
     */
    @ExceptionHandler(EventPersistenceException.class)
    public ResponseEntity<ErrorResponse> handleEventPersistence(EventPersistenceException ex) {
        // Record metric
        errorMetricsService.recordPersistenceError("CBMMEvent", "persist", ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Event Persistence Error",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle Invalid File Exception
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        // Record metric
        errorMetricsService.recordValidationError("invalid_file", "file");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid File",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle Empty Batch Exception
     */
    @ExceptionHandler(EmptyBatchException.class)
    public ResponseEntity<ErrorResponse> handleEmptyBatch(EmptyBatchException ex) {
        // Record metric
        errorMetricsService.recordValidationError("empty_batch", "batch");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Empty Batch",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle File Size Exceeded Exception
     */
    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeExceeded(FileSizeExceededException ex) {
        // Record metric
        errorMetricsService.recordValidationError("file_size_exceeded", "file_size");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "File Size Exceeded",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
    ) {}
}

