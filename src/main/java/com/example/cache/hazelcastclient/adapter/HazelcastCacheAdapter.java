package com.example.cache.hazelcastclient.adapter;

import com.example.cache.hazelcastclient.model.IJsonDto;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class HazelcastCacheAdapter implements HazelcastCacheClient {

    private final HazelcastInstance hazelcastInstance;
    private final String cacheName;

    public HazelcastCacheAdapter(final HazelcastInstance hazelcastInstance,
                                 @Value("${marketdata.hazelcast.cache-name:market-data-cache}") final String cacheName) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheName = cacheName;
    }

    @Override
    public void update(final Map<String, IJsonDto> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }

        Map<String, String> batch = buildBatch(entries);
        if (batch.isEmpty()) {
            log.error("No entries to update in Hazelcast cache {}", cacheName);
        } else {
            final IMap<String, String> cache = hazelcastInstance.getMap(cacheName);
            cache.putAll(batch);
            log.debug("Updated Hazelcast cache {} with {} entries", cacheName, batch.size());
        }
    }

    private Map<String, String> buildBatch(final Map<String, IJsonDto> entries) {
        final Map<String, String> batch = new HashMap<>();
        entries.forEach((key, value) -> {
            try {
                if (!StringUtils.hasText(key)) {
                    throw new IllegalArgumentException("CacheId is required");
                }
                if (value == null) {
                    throw new IllegalArgumentException(key + " Payload is required");
                }

                String json = value.toJson();
                batch.put(key, json);

            } catch (Exception e) {
                log.error("Skipping invalid entry '{}': {}", key, e.getMessage(), e);
            }
        });
        return batch;
    }
}
