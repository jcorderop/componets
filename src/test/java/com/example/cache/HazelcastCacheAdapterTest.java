package com.example.cache;

import com.example.marketdata.adapter.hazelcast.HazelcastCacheAdapter;
import com.example.demo.MarketDataMessage;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

class HazelcastCacheAdapterIT {

    private HazelcastInstance hazelcastInstance;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.setClusterName("test-cluster");
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    }

    @AfterEach
    void tearDown() {
        hazelcastInstance.shutdown();
    }

    @Test
    void updateWritesJsonPayload_withRealHazelcast() {
        HazelcastCacheAdapter cacheAdapter =
                new HazelcastCacheAdapter(hazelcastInstance, "market-cache");

        MarketDataMessage message = MarketDataMessage.builder()
                .source("demo")
                .symbol("TEST")
                .price(10.5)
                .size(5)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        cacheAdapter.send(Map.of("cache-1", message));

        IMap<String, String> map = hazelcastInstance.getMap("market-cache");
        String json = map.get("cache-1");

        // Assert on the stored JSON
        assertNotNull(json);
        assertTrue(json.contains("\"source\":\"demo\""));
    }


    @Test
    void updateRequiresCacheId() {
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        cacheAdapter.send(Map.of("key1", new MarketDataMessage()));
        MarketDataMessage message = MarketDataMessage.builder()
                .source("demo")
                .symbol("TEST")
                .price(10.5)
                .size(5)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        cacheAdapter.send(Map.of("", message));
    }

    @Test
    void updateRequiresEntries() {
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.send(null));
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.send(Map.of()));
    }
}
