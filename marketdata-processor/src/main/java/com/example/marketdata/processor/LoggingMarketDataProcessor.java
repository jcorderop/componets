package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Processor that simply logs each batch for inspection, useful for local debugging.
 * <p>
 * Activated when {@code marketdata.processors.logging.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Component
@ConditionalOnProperty(
        prefix = "marketdata.processors.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class LoggingMarketDataProcessor extends AbstractMarketDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(LoggingMarketDataProcessor.class);

    public LoggingMarketDataProcessor(final MarketDataProcessorProperties props,
                                     final ProcessorStatsRegistry processorStatsRegistry) {
        super(props, processorStatsRegistry);
        log.info("Created Logging processor");
    }

    @Override
    public String getProcessorName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Logging processor processing batch of size {}", batch.size());
        if (log.isDebugEnabled()) {
            batch.forEach(e -> log.debug("Event: {}", e));
        }
    }

}
