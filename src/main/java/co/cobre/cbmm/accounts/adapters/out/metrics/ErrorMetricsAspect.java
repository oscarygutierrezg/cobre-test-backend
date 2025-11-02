package co.cobre.cbmm.accounts.adapters.out.metrics;

import co.cobre.cbmm.accounts.domain.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to automatically capture and record metrics for all exceptions
 * Integrates with ErrorMetricsService to send metrics to OpenTelemetry
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorMetricsAspect {

    private final ErrorMetricsService errorMetricsService;

    /**
     * Capture all exceptions in use cases
     */
    @AfterThrowing(
        pointcut = "execution(* co.cobre.cbmm.accounts.application.usecase..*(..))",
        throwing = "exception"
    )
    public void recordUseCaseError(JoinPoint joinPoint, Throwable exception) {
        String operation = joinPoint.getSignature().toShortString();
        String errorType = "usecase_error";

        errorMetricsService.recordError(errorType, operation, exception);

        // Record specific error types
        recordSpecificErrorMetrics(exception, operation);

        log.info("Captured use case error: operation={}, exception={}",
            operation, exception.getClass().getSimpleName());
    }

    /**
     * Capture all exceptions in adapters
     */
    @AfterThrowing(
        pointcut = "execution(* co.cobre.cbmm.accounts.adapters..*(..))",
        throwing = "exception"
    )
    public void recordAdapterError(JoinPoint joinPoint, Throwable exception) {
        String operation = joinPoint.getSignature().toShortString();
        String errorType = "adapter_error";

        errorMetricsService.recordError(errorType, operation, exception);

        // Record specific error types
        recordSpecificErrorMetrics(exception, operation);

        log.debug("Captured adapter error: operation={}, exception={}",
            operation, exception.getClass().getSimpleName());
    }

    /**
     * Capture all exceptions in domain services
     */
    @AfterThrowing(
        pointcut = "execution(* co.cobre.cbmm.accounts.domain.service..*(..))",
        throwing = "exception"
    )
    public void recordDomainServiceError(JoinPoint joinPoint, Throwable exception) {
        String operation = joinPoint.getSignature().toShortString();
        String errorType = "domain_service_error";

        errorMetricsService.recordError(errorType, operation, exception);

        // Record specific error types
        recordSpecificErrorMetrics(exception, operation);

        log.debug("Captured domain service error: operation={}, exception={}",
            operation, exception.getClass().getSimpleName());
    }

    /**
     * Record specific error metrics based on exception type
     */
    private void recordSpecificErrorMetrics(Throwable exception, String operation) {
        if (exception instanceof OptimisticLockingFailureException) {
            // Extract entity type from exception message if possible
            String entityType = extractEntityType(exception.getMessage());
            errorMetricsService.recordOptimisticLockingFailure(entityType, operation);

        } else if (exception instanceof DuplicateEventException de) {
            errorMetricsService.recordDuplicateEventError(de.getEventId());

        } else if (exception instanceof InsufficientBalanceException) {
            InsufficientBalanceException ibe = (InsufficientBalanceException) exception;
            errorMetricsService.recordInsufficientBalanceError(
                ibe.getAccountNumber(),
                "UNKNOWN" // Currency not available in exception
            );

        } else if (exception instanceof EventPersistenceException) {
            errorMetricsService.recordPersistenceError(
                "CBMMEvent",
                operation,
                exception.getCause()
            );

        } else if (exception instanceof AccountNotFoundException) {
            errorMetricsService.recordError("account_not_found", operation, exception);

        } else if (exception instanceof InvalidCurrencyException) {
            errorMetricsService.recordValidationError("invalid_currency", "currency");
        }
    }

    /**
     * Extract entity type from exception message
     */
    private String extractEntityType(String message) {
        if (message == null) {
            return "Unknown";
        }

        // Try to extract entity class name from Hibernate exception message
        if (message.contains("AccountEntity")) {
            return "Account";
        } else if (message.contains("TransactionEntity")) {
            return "Transaction";
        } else if (message.contains("CBMMEventEntity")) {
            return "CBMMEvent";
        }

        return "Unknown";
    }
}

