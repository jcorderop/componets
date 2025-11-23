package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.consumers.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class LoggingMarketDataConsumer extends AbstractMarketDataConsumer {

    public LoggingMarketDataConsumer(final MarketDataConsumerProperties props,
                                     final ConsumerStatsRegistry consumerStatsRegistry) {
        super(props, consumerStatsRegistry);
        log.info("Created Logging consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        try {
            log.info("Logging consumer processing batch of size {}", batch.size());
            //} catch (IOException | TimeoutException e) {
            // transient problem → retry
            //    throw new ConsumerRetryableException("Temporary failure talking to service", e);
        } catch (Exception e) {
            throw e;
        }
    }

}
