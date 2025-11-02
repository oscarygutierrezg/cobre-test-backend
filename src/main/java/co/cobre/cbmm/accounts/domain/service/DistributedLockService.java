package co.cobre.cbmm.accounts.domain.service;

import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for managing distributed locks using Redis with OpenTelemetry metrics
 * Prevents race conditions when updating account balances
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;
    private final ErrorMetricsService errorMetricsService;

    private static final String ACCOUNT_LOCK_PREFIX = "account:lock:";
    private static final long DEFAULT_WAIT_TIME = 5L;
    private static final long DEFAULT_LEASE_TIME = 10L;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * Execute an operation with a distributed lock on an account
     *
     * @param accountId The account ID to lock
     * @param operation The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     * @throws IllegalStateException if lock cannot be acquired
     */
    public <T> T executeWithLock(String accountId, Supplier<T> operation) {
        String lockKey = ACCOUNT_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            log.debug("Attempting to acquire lock for account: {}", accountId);

            boolean isLocked = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);

            if (!isLocked) {
                log.error("Failed to acquire lock for account: {}", accountId);

                // Record metric
                errorMetricsService.recordDistributedLockFailure(lockKey, "timeout");

                throw new IllegalStateException("Could not acquire lock for account: " + accountId +
                    ". Another transaction may be in progress.");
            }

            log.debug("Lock acquired for account: {}", accountId);

            try {
                return operation.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Lock released for account: {}", accountId);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock on account: {}", accountId, e);

            // Record metric
            errorMetricsService.recordDistributedLockFailure(lockKey, "interrupted");

            throw new IllegalStateException("Lock acquisition interrupted for account: " + accountId, e);
        }
    }
}

