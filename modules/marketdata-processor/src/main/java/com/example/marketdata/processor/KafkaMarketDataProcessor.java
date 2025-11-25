package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Placeholder processor that would forward batches to Kafka when enabled via configuration.
 * <p>
 * Activated when {@code marketdata.processors.kafka.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.processors.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KafkaMarketDataProcessor extends AbstractMarketDataProcessor {

    public KafkaMarketDataProcessor(final MarketDataProcessorProperties props,
                                   final ProcessorStatsRegistry processorStatsRegistry) {
        super(props, processorStatsRegistry);
        log.info("Created Kafka processor");
    }

    @Override
    public String getProcessorName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Kafka processor processing batch of size {}", batch.size());
    }
}
