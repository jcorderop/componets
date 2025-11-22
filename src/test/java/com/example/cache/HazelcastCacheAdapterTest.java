package com.example.cache;

import com.example.demo.MarketDataMessage;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HazelcastCacheAdapterTest {

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private IMap<String, String> map;

    @Test
    void updateWritesJsonPayload() {
        when(hazelcastInstance.getMap("market-cache")).thenReturn(map);

        MarketDataMessage message = MarketDataMessage.builder()
                .source("demo")
                .symbol("TEST")
                .price(10.5)
                .size(5)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        cacheAdapter.update(Map.of("cache-1", message));

        verify(map).putAll(Map.of("cache-1", "{\"source\":\"demo\",\"symbol\":\"TEST\",\"price\":10.5,\"size\":5,\"timestamp\":\"2024-01-01T00:00:00Z\"}"));
    }

    @Test
    void updateRequiresPayload() {
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.update(Map.of("cache-1", null)));
    }

    @Test
    void updateRequiresCacheId() {
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.update(Map.of("", new MarketDataMessage())));
    }

    @Test
    void updateRequiresEntries() {
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.update(null));
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.update(Map.of()));
    }
}
