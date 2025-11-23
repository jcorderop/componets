package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.model.IJsonDto;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class HazelcastCacheAdapter<T extends IJsonDto> implements BaseAdapter<T> {

    private final HazelcastInstance hazelcastInstance;
    private final String cacheName;

    // Secondary local cache with latest values (JSON already serialized)
    private final ConcurrentMap<String, String> latestValues = new ConcurrentHashMap<>();

    public HazelcastCacheAdapter(final HazelcastInstance hazelcastInstance,
                                 @Value("${marketdata.hazelcast.cache-name:default-name}")
                                 final String cacheName) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheName = cacheName;

        // Listen to Hazelcast lifecycle events (client reconnected, started, etc.)
        createHazelcastListener(hazelcastInstance);
    }

    private void createHazelcastListener(HazelcastInstance hazelcastInstance) {

        hazelcastInstance.getLifecycleService()
                .addLifecycleListener(new LifecycleListener() {
            @Override
            public void stateChanged(LifecycleEvent event) {
                LifecycleState state = event.getState();
                log.info("Hazelcast lifecycle event: {}", state);

                // Adjust depending on whether you use client or member
                if (state == LifecycleState.CLIENT_CONNECTED || state == LifecycleState.STARTED || state == LifecycleState.MERGED) {
                    // Cluster is up/connected again → resend everything we have
                    resendAllFromLocalCache();
                }
                // You can log disconnections if you want:
                if (state == LifecycleState.CLIENT_DISCONNECTED || state == LifecycleState.SHUTDOWN) {
                    log.warn("Hazelcast disconnected or shut down; will keep buffering latest values locally");
                }
            }
        });
    }

    @Override
    public void send(final Map<String, T> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }

        Map<String, String> batch = buildBatch(entries);
        if (batch.isEmpty()) {
            log.error("No entries to update in Hazelcast cache {}", cacheName);
            return;
        }

        // Always update local secondary cache first (latest view)
        latestValues.putAll(batch);

        try {
            final IMap<String, String> cache = hazelcastInstance.getMap(cacheName);
            cache.putAll(batch);
            log.debug("Updated Hazelcast cache {} with {} entries", cacheName, batch.size());
        } catch (Exception e) {
            // If Hazelcast is down, we just keep local latestValues
            log.error("Failed to send batch to Hazelcast cache {}. Kept {} entries in local cache for retry.",
                    cacheName, batch.size(), e);
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

                String json = value.toJson();
                batch.put(key, json);

            } catch (Exception e) {
                log.error("Skipping invalid entry '{}': {}", key, e.getMessage(), e);
            }
        });
        return batch;
    }

    /**
     * Resend *all* latest values from the local cache to Hazelcast, typically after reconnect.
     */
    private void resendAllFromLocalCache() {
        Map<String, String> snapshot = new HashMap<>(latestValues);
        if (snapshot.isEmpty()) {
            log.info("Local shadow cache is empty; nothing to resend to Hazelcast {}", cacheName);
            return;
        }

        try {
            log.info("Resending {} entries from local shadow cache to Hazelcast {}", snapshot.size(), cacheName);
            IMap<String, String> cache = hazelcastInstance.getMap(cacheName);
            cache.putAll(snapshot);
            log.info("Resend to Hazelcast cache {} completed", cacheName);
        } catch (Exception e) {
            log.error("Failed to resend local shadow cache to Hazelcast {}", cacheName, e);
        }
    }
}
