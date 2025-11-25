package com.example.marketdata.processor;

import com.example.marketdata.adapter.hazelcast.IHazelcastBufferCacheAdapter;
import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processor that batches market data events and stages them for Hazelcast updates via the
 * buffer handler and throttle.
 * <p>
 * Enabled when {@code marketdata.processors.hazelcast.enabled=true}. Inherits batching and
 * backoff tuning from {@code marketdata.default.*} and relies on {@code marketdata.throttle.interval-ms}
 * to schedule buffer flushes.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.processors.hazelcast",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class HazelcastMarketDataProcessor<T> extends AbstractMarketDataProcessor {

    final IHazelcastBufferCacheAdapter<T> hazelcastCacheAdapter;

    public HazelcastMarketDataProcessor(final MarketDataProcessorProperties props,
                                       final ProcessorStatsRegistry processorStatsRegistry,
                                       final IHazelcastBufferCacheAdapter<T> hazelcastCacheAdapter) {
        super(props, processorStatsRegistry);
        this.hazelcastCacheAdapter = hazelcastCacheAdapter;
        log.info("Created Hazelcast processor");
    }

    @Override
    public String getProcessorName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Hazelcast processor processing batch of size {}", batch.size());
        hazelcastCacheAdapter.bufferMarketData(batch);
    }
}
