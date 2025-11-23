package com.example.marketdata.adapter;


import java.util.Map;

/**
 * Generic contract for adapters capable of delivering market data payloads to external
 * destinations such as caches or message brokers.
 */
public interface BaseAdapter <T> {

    /**
     * Serialize the provided payloads to JSON and update the configured Hazelcast cache.
     *
     * @param entries map of cache IDs to JSON-serializable DTOs to store
     */
    void send(Map<String, T> entries);
}
