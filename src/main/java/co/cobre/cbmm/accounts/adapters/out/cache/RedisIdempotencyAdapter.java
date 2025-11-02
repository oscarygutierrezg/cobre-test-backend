package co.cobre.cbmm.accounts.adapters.out.cache;

import co.cobre.cbmm.accounts.ports.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis adapter implementing IdempotencyPort
 * Uses Redisson for distributed cache operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private final RedissonClient redissonClient;

    private static final String EVENT_KEY_PREFIX = "cbmm:event:processed:";

    @Override
    public boolean isEventProcessed(String eventId) {
        String key = EVENT_KEY_PREFIX + eventId;
        RBucket<String> bucket = redissonClient.getBucket(key);

        boolean exists = bucket.isExists();
        log.debug("Checking if event {} is processed: {}", eventId, exists);

        return exists;
    }

    @Override
    public void markEventAsProcessed(String eventId, long ttlSeconds) {
        String key = EVENT_KEY_PREFIX + eventId;
        RBucket<String> bucket = redissonClient.getBucket(key);

        // Store event ID with timestamp
        String value = String.format("processed_at:%d", System.currentTimeMillis());
        bucket.set(value, Duration.ofSeconds(ttlSeconds));

        log.info("Marked event {} as processed with TTL {} seconds", eventId, ttlSeconds);
    }
}

