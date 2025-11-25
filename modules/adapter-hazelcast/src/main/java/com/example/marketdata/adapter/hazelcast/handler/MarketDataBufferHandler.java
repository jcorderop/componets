package com.example.marketdata.adapter.hazelcast.handler;

import com.example.marketdata.cache.MarketDataBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates and routes market data entries into the shared buffer that backs Hazelcast
 * cache updates, preventing malformed records from being stored.
 */
@Slf4j
@Component
public class MarketDataBufferHandler<T> {

    private final MarketDataBuffer<T> marketDataBuffer;

    public MarketDataBufferHandler(MarketDataBuffer<T> marketDataBuffer) {
        this.marketDataBuffer = marketDataBuffer;
    }

    /**
     * Add one item to the buffer.
     */
    public void handle(final String key, final T value) {
        if (key == null || key.isBlank()) {
            log.warn("Ignoring market data: key is null/blank");
            return;
        }
        if (value == null) {
            log.warn("Ignoring market data: value is null for key {}", key);
            return;
        }

        marketDataBuffer.put(key, value);
        log.debug("Buffered market data for key {}", key);
    }

    /**
     * Add multiple items to the buffer.
     */
    public void handleAll(final Map<String, T> entries) {
        if (entries == null || entries.isEmpty()) {
            log.warn("Ignoring market data: no entries to buffer");
            return;
        }
        entries.forEach(this::handle);
    }
}
