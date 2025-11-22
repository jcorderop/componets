package com.example.handler;

import com.example.cache.MarketDataBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataBufferHandler<T> {

    private final MarketDataBuffer<T> marketDataBuffer;

    /**
     * Add one item to the buffer.
     */
    public void handle(final String key, final T value) {
        if (key == null || key.isBlank()) {
            log.error("Ignoring market data: key is null/blank");
            return;
        }
        if (value == null) {
            log.error("Ignoring market data: value is null for key {}", key);
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
            return;
        }
        entries.forEach(this::handle);
    }
}
