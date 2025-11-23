package com.example.marketdata.consumer;

import com.example.marketdata.adapter.hazelcast.HazelcastBufferThrottle;
import com.example.marketdata.adapter.hazelcast.handler.MarketDataBufferHandler;
import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import com.example.marketdata.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consumer that batches market data events and stages them for Hazelcast updates via the
 * buffer handler and throttle.
 * <p>
 * Enabled when {@code marketdata.consumers.hazelcast.enabled=true}. Inherits batching and
 * backoff tuning from {@code marketdata.default.*} and relies on {@code marketdata.throttle.interval-ms}
 * to schedule buffer flushes.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "marketdata.consumers.hazelcast",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class HazelcastMarketDataConsumer<T> extends AbstractMarketDataConsumer {

    final MarketDataBufferHandler<T> marketDataBufferHandler;
    final HazelcastBufferThrottle<T> hazelcastBufferThrottle;

    public HazelcastMarketDataConsumer(final MarketDataConsumerProperties props,
                                       final ConsumerStatsRegistry consumerStatsRegistry,
                                       final MarketDataBufferHandler<T> marketDataBufferHandler,
                                       final HazelcastBufferThrottle<T> hazelcastBufferThrottle) {
        super(props, consumerStatsRegistry);
        this.marketDataBufferHandler = marketDataBufferHandler;
        this.hazelcastBufferThrottle = hazelcastBufferThrottle;
        log.info("Created Hazelcast consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        log.info("Hazelcast consumer processing batch of size {}", batch.size());

        @SuppressWarnings("unchecked")
        Map<String, T> batchMap = batch.stream()
                .collect(Collectors.toMap(
                        MarketDataEvent::getCacheId,
                        e -> (T) e,
                        (oldVal, newVal) -> newVal
                ));

        marketDataBufferHandler.handleAll(batchMap);
    }
}
