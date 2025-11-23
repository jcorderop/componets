package com.example.marketdata.consumer;

import com.example.marketdata.adapter.hazelcast.HazelcastBufferThrottle;
import com.example.marketdata.adapter.hazelcast.handler.MarketDataBufferHandler;
import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.IJsonDto;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HazelcastMarketDataConsumer extends AbstractMarketDataConsumer {

    final MarketDataBufferHandler<IJsonDto> marketDataBufferHandler;
    final HazelcastBufferThrottle<IJsonDto> hazelcastBufferThrottle;

    public HazelcastMarketDataConsumer(final MarketDataConsumerProperties props,
                                       final MarketDataBufferHandler<IJsonDto> marketDataBufferHandler,
                                       final HazelcastBufferThrottle<IJsonDto> hazelcastBufferThrottle) {
        super(props);
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
        try {
            log.info("Hazelcast consumer processing batch of size {}", batch.size());
            final Map<String, IJsonDto> batchMap = batch.stream()
                    .collect(Collectors.toMap(
                            MarketDataEvent::getCacheId,
                            e -> (IJsonDto) e,
                            (oldVal, newVal) -> newVal
                    ));

            this.marketDataBufferHandler.handleAll(batchMap);
        //} catch (IOException | TimeoutException e) {
            // transient problem → retry
        //    throw new ConsumerRetryableException("Temporary failure talking to service", e);
        } catch (Exception e) {
            throw e;
        }
    }
}
