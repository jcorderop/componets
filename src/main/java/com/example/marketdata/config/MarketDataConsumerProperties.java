package com.example.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
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
public class MarketDataConsumerProperties {

    /**
     * Max number of messages in each consumer queue.
     */
    private int queueCapacity = 1_000_000;

    /**
     * Batch size used by consumers when draining the queue.
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
}
