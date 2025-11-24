package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub processor illustrating how a ZeroMQ sink might process market data batches.
 * <p>
 * Activated when {@code marketdata.processors.zmq.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.processors.zmq",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ZMQMarketDataProcessor extends AbstractMarketDataProcessor {

    public ZMQMarketDataProcessor(final MarketDataProcessorProperties props,
                                 final ProcessorStatsRegistry processorStatsRegistry) {
        super(props, processorStatsRegistry);
        log.info("Created ZMQ processor");
    }

    @Override
    public String getProcessorName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("ZMQ processor processing batch of size {}", batch.size());
    }
}
