package com.example.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties that control processor queue sizing, batching, and retry behavior
 * for processing inbound market data.
 * <p>
 * Properties (all under {@code marketdata.default}) and how they are applied:
 * <ul>
 *     <li>{@code queue-capacity} (default: {@code 1000000}) – maximum number of events the
 *     processor queue can hold before producers start dropping new events.</li>
 *     <li>{@code batch-size} (default: {@code 1000}) – number of events drained from the
 *     queue before invoking {@link com.example.marketdata.model.MarketDataProcessorBatchProcessor#processBatch}.</li>
 *     <li>{@code poll-timeout-millis} (default: {@code 10}) – how long the processor waits for
 *     additional events before flushing a partially full batch.</li>
 *     <li>{@code initial-retry-backoff-millis} (default: {@code 1000}) – first sleep duration
 *     when a batch fails with a retryable exception.</li>
 *     <li>{@code max-retry-backoff-millis} (default: {@code 10000}) – upper bound for the retry
 *     backoff between successive failures of the same batch.</li>
 *     <li>{@code retry-backoff-multiplier} (default: {@code 2.0}) – multiplier applied after each
 *     retry to achieve exponential backoff.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "marketdata.default")
/*
marketdata.default.queue-capacity=100000
marketdata.default.batch-size=500
marketdata.default.poll-timeout-millis=10
marketdata.default.initial-retry-backoff-millis=1000
marketdata.default.max-retry-backoff-millis=10000
marketdata.default.retry-backoff-multiplier=2.0
 */
public class MarketDataProcessorProperties {

    /**
     * Max number of messages in each processor queue.
     */
    private int queueCapacity = 1_000_000;

    /**
     * Batch size used by processors when draining the queue.
     */
    private int batchSize = 1_000;

    /**
     * Max wait in ms before flushing a partially-full batch.
     */
    private long pollTimeoutMillis = 10;

    /**
     * Sleep time in ms between retries when a batch fails with a retryable exception.
     */
    private long initialRetryBackoffMillis = 1_000;

    /**
     * Maximum backoff in ms when repeatedly failing the same batch.
     */
    private long maxRetryBackoffMillis = 10_000;

    /**
     * Multiplier applied to the backoff after each retry (e.g. 2.0 = exponential backoff).
     */
    private double retryBackoffMultiplier = 2.0;

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getPollTimeoutMillis() {
        return pollTimeoutMillis;
    }

    public void setPollTimeoutMillis(long pollTimeoutMillis) {
        this.pollTimeoutMillis = pollTimeoutMillis;
    }

    public long getInitialRetryBackoffMillis() {
        return initialRetryBackoffMillis;
    }

    public void setInitialRetryBackoffMillis(long initialRetryBackoffMillis) {
        this.initialRetryBackoffMillis = initialRetryBackoffMillis;
    }

    public long getMaxRetryBackoffMillis() {
        return maxRetryBackoffMillis;
    }

    public void setMaxRetryBackoffMillis(long maxRetryBackoffMillis) {
        this.maxRetryBackoffMillis = maxRetryBackoffMillis;
    }

    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }
}
