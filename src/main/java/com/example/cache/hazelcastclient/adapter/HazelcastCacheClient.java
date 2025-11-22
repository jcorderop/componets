package com.example.cache.hazelcastclient.adapter;

import com.example.cache.hazelcastclient.model.IJsonDto;

import java.util.Map;

public interface HazelcastCacheClient {

    /**
     * Serialize the provided payloads to JSON and update the configured Hazelcast cache.
     *
     * @param entries map of cache IDs to JSON-serializable DTOs to store
     */
    void update(Map<String, IJsonDto> entries);
}
