package com.example.demo;

import com.example.cache.hazelcastclient.adapter.HazelcastCacheClient;
import com.example.cache.hazelcastclient.model.IJsonDto;
import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.consumer.AbstractMarketDataConsumer;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
public class HazelcastMarketDataConsumer extends AbstractMarketDataConsumer {

    private final HazelcastCacheClient cacheClient;
    private final String cacheId;
    private final String cacheName;

    public HazelcastMarketDataConsumer(final MarketDataConsumerProperties props,
                                       final HazelcastCacheClient cacheClient,
                                       @Value("${marketdata.hazelcast.cache-name:market-data-cache}") final String cacheName,
                                       @Value("${marketdata.hazelcast.cache-id:market-data-cache}") final String cacheId) {
        super(props);
        this.cacheClient = cacheClient;
        this.cacheId = cacheId;
        this.cacheName = cacheName;
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void dequeueBatch(final List<MarketDataEvent> batch) {
        log.info("Hazelcast consumer writing batch of size {} to cache {}", batch.size(), cacheName);
        Map<String, IJsonDto> payloads = new HashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            payloads.put(cacheId + ":" + i, batch.get(i));
        }
        cacheClient.update(payloads);
    }
}
