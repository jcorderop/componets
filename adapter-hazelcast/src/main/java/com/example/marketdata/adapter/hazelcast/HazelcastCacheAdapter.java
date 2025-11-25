package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.exception.ProcessorRetryableException;
import com.example.marketdata.exception.ProcessorRuntimeException;
import com.example.marketdata.util.JsonUtil;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.map.IMap;
import com.hazelcast.spi.exception.RetryableHazelcastException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapter that serializes market data payloads to JSON and persists them into a Hazelcast
 * map, while keeping a local shadow cache to support reconnection scenarios.
 * <p>
 * Uses {@code marketdata.hazelcast.cache-name} (default {@code default-name}) to determine which
 * Hazelcast map to update and retain a matching shadow cache for reconnection flows.
 */
@Component
@ConditionalOnProperty(prefix = "marketdata.adapters.hazelcast", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HazelcastCacheAdapter<T> implements BaseAdapter<T> {

    private static final Logger log = LoggerFactory.getLogger(HazelcastCacheAdapter.class);

    private final HazelcastInstance hazelcastInstance;
    private final String cacheName;

    // Secondary local cache with latest values (JSON already serialized)
    // this cache will have the latest updates and will be resent when Hazelcast is reconnected.
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
                        log.info("Hazelcast lifecycle event '{}' for cache {}", state, cacheName);

                        // Consider both client and member lifecycle states
                        if (state == LifecycleState.CLIENT_CONNECTED
                                || state == LifecycleState.STARTED
                                || state == LifecycleState.MERGED) {
                            // Cluster is up/connected again → resend everything we have
                            resendAllFromLocalCache();
                        }

                        if (state == LifecycleState.CLIENT_DISCONNECTED
                                || state == LifecycleState.SHUTDOWN) {
                            log.warn(
                                    "Hazelcast connection lost for cache {}. {} buffered entries in shadow cache will be resent on reconnect.",
                                    cacheName, latestValues.size()
                            );
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
        int droppedEntries = entries.size() - batch.size();

        if (batch.isEmpty()) {
            log.info("Received {} entries for Hazelcast cache {}, but none were valid; skipping update", entries.size(), cacheName);
            return;
        }

        if (droppedEntries > 0) {
            log.warn("{} entries skipped for Hazelcast cache {} due to validation errors; proceeding with {} valid entries",
                    droppedEntries, cacheName, batch.size());
        }

        // Always update local secondary cache first (latest view)
        // in case of reconnection this cache will be full re-send
        latestValues.putAll(batch);

        try {
            final IMap<String, String> cache = hazelcastInstance.getMap(cacheName);
            cache.putAll(batch);
            log.info("Updated Hazelcast cache {} with {} entries; dropped {} invalid entries",
                    cacheName, batch.size(), droppedEntries);

        } catch (HazelcastException e) {
            // Hazelcast-specific error → decide retryable vs not
            if (isRetryableHazelcastException(e)) {
                log.warn("Retryable Hazelcast error while updating cache {}: {}. " +
                                "Entries remain in local shadow cache for retry.",
                        cacheName, e.getMessage(), e);
                throw new ProcessorRetryableException(
                        "Retryable Hazelcast error while updating cache " + cacheName, e);
            } else {
                log.error("Non-retryable Hazelcast error while updating cache {}. " +
                                "Entries remain in local shadow cache, but this batch will be dropped by the processor.",
                        cacheName, e);
                throw new ProcessorRuntimeException("Non-retryable Hazelcast error while updating cache " + cacheName, e);
            }

        } catch (RuntimeException e) {
            // Any other runtime exception in the adapter is treated as non-retryable
            log.error("Unexpected runtime error while updating Hazelcast cache {}. " +
                    "Entries remain in local shadow cache.", cacheName, e);
            throw new ProcessorRuntimeException("Unexpected error while updating cache " + cacheName, e);
        }
    }

    /**
     * Check if the exception (or any cause) is a retryable Hazelcast exception.
     */
    private boolean isRetryableHazelcastException(Throwable t) {
        while (t != null) {
            if (t instanceof RetryableHazelcastException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private Map<String, String> buildBatch(Map<String, T> entries) {
        Map<String, String> batch = new HashMap<>();
        entries.forEach((key, value) -> {
            try {
                if (!StringUtils.hasText(key)) {
                    throw new IllegalArgumentException("CacheId is required");
                }
                if (value == null) {
                    throw new IllegalArgumentException(key + " Payload is required");
                }
                String json = JsonUtil.toJson(value);
                batch.put(key, json);
            } catch (Exception e) {
                log.warn("Skipping invalid Hazelcast entry for cache {} and key '{}': {}", cacheName, key, e.getMessage(), e);
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
