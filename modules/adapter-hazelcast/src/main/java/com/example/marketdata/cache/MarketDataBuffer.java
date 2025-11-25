package com.example.marketdata.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe buffer that temporarily stores the latest market data entries before they
 * are flushed to an external cache or adapter.
 */
@Component
public class MarketDataBuffer <T> {

    // Atomic drain of the entire buffer without losing messages, and without locking
    private final AtomicReference<ConcurrentHashMap<String, T>> ref = new AtomicReference<>(new ConcurrentHashMap<>());

    public void put(String key, T value) {
        ref.get().put(key, value);
    }

    public boolean isEmpty() {
        return ref.get().isEmpty();
    }

    public Map<String, T> releaseBuffer() {
        ConcurrentHashMap<String, T> current = ref.getAndSet(new ConcurrentHashMap<>());
        return new HashMap<>(current);
    }
}