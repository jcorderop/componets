package com.example.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Component
public class MarketDataBuffer <T> {

    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();

    public void put(String key, T value) {
        cache.put(key, value);
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * Returns a snapshot of the current contents and clears the cache.
     */
    public Map<String, T> releaseBuffer() {
        Map<String, T> snapshot = new HashMap<>(cache);
        cache.clear();
        return snapshot;
    }
}