package com.example.cache;

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
        final IMap<String, String> cache = hazelcastInstance.getMap(cacheName);
        cache.putAll(batch);
        log.debug("Updated Hazelcast cache {} with {} entries", cacheName, batch.size());
    }

    private Map<String, String> buildBatch(final Map<String, IJsonDto> entries) {
        Map<String, String> batch = new HashMap<>();
        entries.entrySet().stream()
                .filter(entry -> {
                    if (!StringUtils.hasText(entry.getKey())) {
                        throw new IllegalArgumentException("cacheId is required");
                    }
                    if (entry.getValue() == null) {
                        throw new IllegalArgumentException("payload is required");
                    }
                    return true;
                })
                .forEach(entry -> batch.put(entry.getKey(), entry.getValue().getJson()));
        return batch;
    }
}
