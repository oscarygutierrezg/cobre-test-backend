package co.cobre.cbmm.accounts.adapters.config;

import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

/**
 * Listener for Spring Retry to record metrics on retry attempts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryMetricsListener implements RetryListener {

    private final ErrorMetricsService errorMetricsService;

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        int retryCount = context.getRetryCount();
        String operationName = context.getAttribute("context.name") != null
            ? context.getAttribute("context.name").toString()
            : "unknown";

        log.debug("Retry attempt {} for operation: {}", retryCount, operationName);

        // Record retry metric
        errorMetricsService.recordRetry(operationName, retryCount, false);
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            // Success after retries
            int retryCount = context.getRetryCount();
            String operationName = context.getAttribute("context.name") != null
                ? context.getAttribute("context.name").toString()
                : "unknown";

            if (retryCount > 0) {
                log.info("Operation succeeded after {} retries: {}", retryCount, operationName);
                errorMetricsService.recordRetry(operationName, retryCount, true);
            }
        }
    }
}

