package com.example.marketdata.processor;

import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processor that batches market data events and forwards them through the configured
 * {@link BaseAdapter}. The adapter module is selected via Spring configuration/profile so
 * the processor remains vendor-neutral while still honoring the common retry/backoff
 * behaviour defined in {@link AbstractMarketDataProcessor}.
 */
@Slf4j
@Component
@ConditionalOnBean(BaseAdapter.class)
@ConditionalOnProperty(prefix = "marketdata.processors.adapter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdapterBackedMarketDataProcessor<T> extends AbstractMarketDataProcessor {

    private final BaseAdapter<T> baseAdapter;

    public AdapterBackedMarketDataProcessor(final MarketDataProcessorProperties props,
                                            final ProcessorStatsRegistry processorStatsRegistry,
                                            final BaseAdapter<T> baseAdapter) {
        super(props, processorStatsRegistry);
        this.baseAdapter = baseAdapter;
        log.info("Created adapter-backed processor with adapter {}", baseAdapter.getClass().getSimpleName());
    }

    @Override
    public String getProcessorName() {
        return getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Adapter-backed processor processing batch of size {}", batch.size());
        Map<String, T> batchMap = batch.stream()
                .collect(Collectors.toMap(MarketDataEvent::getCacheId, e -> cast(e), (oldVal, newVal) -> newVal));
        baseAdapter.send(batchMap);
    }

    @SuppressWarnings("unchecked")
    private T cast(MarketDataEvent event) {
        return (T) event;
    }
}
