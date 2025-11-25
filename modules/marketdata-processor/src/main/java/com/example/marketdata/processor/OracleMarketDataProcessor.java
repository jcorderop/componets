package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub processor representing an Oracle-backed sink for market data batches.
 * <p>
 * Activated when {@code marketdata.processors.oracle.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.processors.oracle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class OracleMarketDataProcessor extends AbstractMarketDataProcessor {

    public OracleMarketDataProcessor(final MarketDataProcessorProperties props,
                                    final ProcessorStatsRegistry processorStatsRegistry) {
        super(props, processorStatsRegistry);
        log.info("Created Oracle processor");
    }

    @Override
    public String getProcessorName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Oracle processor processing batch of size {}", batch.size());
    }
}
