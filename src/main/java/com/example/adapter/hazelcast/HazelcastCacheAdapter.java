package com.example.adapter.hazelcast;

import com.example.adapter.BaseAdapter;
import com.example.hazelcastclient.model.IJsonDto;
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
public class HazelcastCacheAdapter<T extends IJsonDto> implements BaseAdapter<T> {

    private final HazelcastInstance hazelcastInstance;
    private final String cacheName;

    public HazelcastCacheAdapter(final HazelcastInstance hazelcastInstance,
                                 @Value("${marketdata.hazelcast.cache-name:default-name}") final String cacheName) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheName = cacheName;
    }
    @Override
    public void send(final Map<String, T> entries) {
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

    private Map<String, String> buildBatch(final Map<String, T> entries) {
        final Map<String, String> batch = new HashMap<>();
        entries.forEach((key, value) -> {
            try {
                if (!StringUtils.hasText(key)) {
                    throw new IllegalArgumentException("CacheId is required");
                }
                if (value == null) {
                    throw new IllegalArgumentException(key + " Payload is required");
                }

                String json = ((IJsonDto) value).toJson();
                batch.put(key, json);

            } catch (Exception e) {
                log.error("Skipping invalid entry '{}': {}", key, e.getMessage(), e);
            }
        });
        return batch;
    }
}
