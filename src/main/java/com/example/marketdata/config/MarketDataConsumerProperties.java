package com.example.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "marketdata.default")
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
}
