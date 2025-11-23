package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub consumer representing a PostgreSQL destination for processed market data batches.
 * <p>
 * Activated when {@code marketdata.consumers.postgres.enabled=true} and inherits queue,
 * batch, and retry tuning from {@code marketdata.default.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.consumers.postgres",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PostgresMarketDataConsumer extends AbstractMarketDataConsumer {

    public PostgresMarketDataConsumer(final MarketDataConsumerProperties props,
                                      final ConsumerStatsRegistry consumerStatsRegistry) {
        super(props, consumerStatsRegistry);
        log.info("Created Postgres consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Postgres consumer processing batch of size {}", batch.size());
    }
}
