package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.cache.MarketDataBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduled throttle that periodically drains the in-memory market data buffer into
 * the Hazelcast cache adapter, protecting downstream systems from bursts.
 *
 * <p>Properties:
 * <ul>
 *     <li>{@code marketdata.throttle.interval-ms} (default: {@code 30000}) – interval in milliseconds
 *     between buffer flushes.</li>
 * </ul>
 */
@Slf4j
@Component
public class HazelcastBufferThrottle<T> {

    private final MarketDataBuffer<T> marketDataBuffer;
    private final HazelcastBufferCacheAdapter<T> hazelcastCacheAdapter;

    public HazelcastBufferThrottle(MarketDataBuffer<T> marketDataBuffer,
                                   HazelcastBufferCacheAdapter<T> hazelcastCacheAdapter) {
        this.marketDataBuffer = marketDataBuffer;
        this.hazelcastCacheAdapter = hazelcastCacheAdapter;
    }

    @Scheduled(fixedRateString = "${marketdata.throttle.interval-ms:30000}")
    public void runThrottled() {
        log.debug("Running throttled cache flush");
        if (marketDataBuffer.isEmpty()) {
            log.info("No elements in buffer to flush");
            return;
        }

        Map<String, T> batch = marketDataBuffer.releaseBuffer();

        log.info("Flushing [{}] elements from throttled cache", batch.size());
        try {
            hazelcastCacheAdapter.send(batch);
        } catch (Exception e) {
            log.error("Error flushing {} elements to Hazelcast; they will be lost or need recomputation", batch.size(), e);
        }
    }
}