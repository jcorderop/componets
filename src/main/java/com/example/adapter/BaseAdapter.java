package com.example.adapter;


import java.util.Map;

public interface BaseAdapter <T> {

    /**
     * Serialize the provided payloads to JSON and update the configured Hazelcast cache.
     *
     * @param entries map of cache IDs to JSON-serializable DTOs to store
     */
    void send(Map<String, T> entries);
}
