package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Placeholder consumer that would forward batches to Kafka when enabled via configuration.
 * <p>
 * Activated when {@code marketdata.consumers.kafka.enabled=true} and inherits queue, batch,
 * and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.consumers.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KafkaMarketDataConsumer extends AbstractMarketDataConsumer {

    public KafkaMarketDataConsumer(final MarketDataConsumerProperties props,
                                   final ConsumerStatsRegistry consumerStatsRegistry) {
        super(props, consumerStatsRegistry);
        log.info("Created Kafka consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Kafka consumer processing batch of size {}", batch.size());
    }
}
