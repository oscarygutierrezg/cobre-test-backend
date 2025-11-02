package co.cobre.cbmm.accounts.adapters.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for recording OpenTelemetry metrics for errors and exceptions
 * Implements best practices for metrics collection and reporting
 */
@Service
@Slf4j
public class ErrorMetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    private static final String ERROR_COUNTER_NAME = "cbmm.accounts.errors.total";
    private static final String RETRY_COUNTER_NAME = "cbmm.accounts.retries.total";
    private static final String LOCK_FAILURE_NAME = "cbmm.accounts.lock.failures.total";
    private static final String PERSISTENCE_ERROR_NAME = "cbmm.accounts.persistence.errors.total";

    public ErrorMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("ErrorMetricsService initialized with MeterRegistry: {}", meterRegistry.getClass().getSimpleName());
    }

    /**
     * Record a general error occurrence
     */
    public void recordError(String errorType, String operation, Throwable throwable) {
        String exceptionClass = throwable != null ? throwable.getClass().getSimpleName() : "UnknownException";

        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", errorType,
            "operation", operation,
            "exception", exceptionClass
        );

        counter.increment();

        log.debug("Recorded error metric: type={}, operation={}, exception={}",
            errorType, operation, exceptionClass);
    }

    /**
     * Record a retry attempt
     */
    public void recordRetry(String operation, int attemptNumber, boolean success) {
        Counter counter = getOrCreateCounter(
            RETRY_COUNTER_NAME,
            "operation", operation,
            "attempt", String.valueOf(attemptNumber),
            "success", String.valueOf(success)
        );

        counter.increment();

        log.debug("Recorded retry metric: operation={}, attempt={}, success={}",
            operation, attemptNumber, success);
    }

    /**
     * Record optimistic locking failure
     */
    public void recordOptimisticLockingFailure(String entityType, String operation) {
        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", "optimistic_locking_failure",
            "entity.type", entityType,
            "operation", operation
        );

        counter.increment();

        log.debug("Recorded optimistic locking failure: entity={}, operation={}",
            entityType, operation);
    }

    /**
     * Record distributed lock failure
     */
    public void recordDistributedLockFailure(String lockKey, String reason) {
        Counter counter = getOrCreateCounter(
            LOCK_FAILURE_NAME,
            "lock.key", lockKey,
            "reason", reason
        );

        counter.increment();

        log.debug("Recorded distributed lock failure: lockKey={}, reason={}", lockKey, reason);
    }

    /**
     * Record persistence error
     */
    public void recordPersistenceError(String entityType, String operation, Throwable throwable) {
        String exceptionClass = throwable != null ? throwable.getClass().getSimpleName() : "UnknownException";

        Counter counter = getOrCreateCounter(
            PERSISTENCE_ERROR_NAME,
            "entity.type", entityType,
            "operation", operation,
            "exception", exceptionClass
        );

        counter.increment();

        log.debug("Recorded persistence error: entity={}, operation={}, exception={}",
            entityType, operation, exceptionClass);
    }

    /**
     * Record Kafka processing error
     */
    public void recordKafkaError(String topic, String eventId, Throwable throwable) {
        String exceptionClass = throwable != null ? throwable.getClass().getSimpleName() : "UnknownException";

        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", "kafka_processing_error",
            "topic", topic,
            "exception", exceptionClass
        );

        counter.increment();

        log.debug("Recorded Kafka error: topic={}, eventId={}, exception={}",
            topic, eventId, exceptionClass);
    }

    /**
     * Record validation error
     */
    public void recordValidationError(String validationType, String field) {
        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", "validation_error",
            "validation.type", validationType,
            "field", field
        );

        counter.increment();

        log.debug("Recorded validation error: type={}, field={}", validationType, field);
    }

    /**
     * Record insufficient balance error
     */
    public void recordInsufficientBalanceError(String accountNumber, String currency) {
        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", "insufficient_balance",
            "currency", currency
        );

        counter.increment();

        log.debug("Recorded insufficient balance error: account={}, currency={}",
            accountNumber, currency);
    }

    /**
     * Record duplicate event error
     */
    public void recordDuplicateEventError(String eventId) {
        Counter counter = getOrCreateCounter(
            ERROR_COUNTER_NAME,
            "error.type", "duplicate_event",
            "idempotency", "violation"
        );

        counter.increment();

        log.debug("Recorded duplicate event error: eventId={}", eventId);
    }


    /**
     * Get or create a counter with specific tags
     */
    private Counter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);

        return errorCounters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .tags(tags)
                .description("Error counter for " + name)
                .register(meterRegistry)
        );
    }

    /**
     * Build cache key from metric name and tags
     */
    private String buildKey(String name, String... tags) {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            sb.append("|").append(tags[i]).append("=");
            if (i + 1 < tags.length) {
                sb.append(tags[i + 1]);
            }
        }
        return sb.toString();
    }
}

