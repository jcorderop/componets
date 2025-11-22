package com.example.adapter.hazelcast;

import com.example.cache.MarketDataBuffer;
import com.example.hazelcastclient.model.IJsonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HazelcastBufferThrottle<T extends IJsonDto> {

    private final MarketDataBuffer<T> marketDataBuffer;
    private final HazelcastCacheAdapter<T> hazelcastCacheAdapter;

    @Scheduled(fixedRateString = "${marketdata.throttle.interval-ms:30000}")
    public void runThrottled() {
        if (marketDataBuffer.isEmpty()) {
            return;
        }

        Map<String, T> batch = marketDataBuffer.releaseBuffer();

        log.info("Flushing {} elements from throttled cache", batch.size());
        hazelcastCacheAdapter.send(batch);
    }
}