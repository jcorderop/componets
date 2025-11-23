package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.cache.MarketDataBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HazelcastBufferThrottle<T> {

    private final MarketDataBuffer<T> marketDataBuffer;
    private final HazelcastCacheAdapter<T> hazelcastCacheAdapter;

    @Scheduled(fixedRateString = "${marketdata.throttle.interval-ms:5000}")
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