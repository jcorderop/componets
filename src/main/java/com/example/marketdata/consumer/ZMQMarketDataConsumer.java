package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub consumer illustrating how a ZeroMQ sink might process market data batches.
 * <p>
 * Activated when {@code marketdata.consumers.zmq.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.consumers.zmq",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ZMQMarketDataConsumer extends AbstractMarketDataConsumer {

    public ZMQMarketDataConsumer(final MarketDataConsumerProperties props,
                                 final ConsumerStatsRegistry consumerStatsRegistry) {
        super(props, consumerStatsRegistry);
        log.info("Created ZMQ consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("ZMQ consumer processing batch of size {}", batch.size());
    }
}
