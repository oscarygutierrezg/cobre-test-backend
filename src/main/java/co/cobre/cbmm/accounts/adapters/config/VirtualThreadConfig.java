package co.cobre.cbmm.accounts.adapters.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for Virtual Threads (Project Loom) and Retry Logic
 * Enables lightweight concurrency for high-throughput operations
 * Enables retry for handling transient failures like optimistic locking
 */
@Configuration
@EnableAsync
@EnableRetry
public class VirtualThreadConfig {

    /**
     * Executor that uses Virtual Threads for async operations
     * Virtual threads are lightweight and can handle millions of concurrent operations
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Legacy thread pool executor (for comparison/fallback)
     */
    @Bean(name = "platformThreadExecutor")
    public Executor platformThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("platform-thread-");
        executor.initialize();
        return executor;
    }
}

