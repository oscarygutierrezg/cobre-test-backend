package co.cobre.cbmm.accounts.adapters.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

/**
 * Configuration for Spring Retry with metrics listener
 */
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class RetryConfig {

    private final RetryMetricsListener retryMetricsListener;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(5000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Register metrics listener
        retryTemplate.setListeners(new org.springframework.retry.RetryListener[]{retryMetricsListener});

        return retryTemplate;
    }
}

