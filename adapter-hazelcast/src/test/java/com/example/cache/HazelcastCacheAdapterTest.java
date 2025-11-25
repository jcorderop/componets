package com.example.cache;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.adapter.hazelcast.HazelcastCacheAdapter;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that exercise the Hazelcast cache adapter against a real cluster.
 */
class HazelcastCacheAdapterIT {

    private HazelcastInstance hazelcastInstance;
    private String clusterName;

    @BeforeEach
    void setUp() {
        clusterName = "test-cluster-" + UUID.randomUUID();
        Config config = new Config();
        config.setClusterName(clusterName);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    }

    @AfterEach
    void tearDown() {
        Hazelcast.shutdownAll();
        HazelcastClient.shutdownAll();
    }

    @Test
    void updateWritesJsonPayload_withRealHazelcast() {
        // given
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");

        MarketDataMessage message = new MarketDataMessage(
                "demo",
                "TEST",
                10.5,
                5,
                Instant.parse("2024-01-01T00:00:00Z")
        );

        // when
        cacheAdapter.send(Map.of("cache-1", message));

        // then
        IMap<String, String> map = hazelcastInstance.getMap("market-cache");
        String json = map.get("cache-1");

        // Assert on the stored JSON
        assertNotNull(json);
        assertTrue(json.contains("\"source\":\"demo\""));
    }


    @Test
    void updateRequiresCacheId() {
        // given
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");
        MarketDataMessage validMessage = new MarketDataMessage(
                "demo",
                "VALID",
                12.5,
                10,
                Instant.parse("2024-02-02T00:00:00Z")
        );
        MarketDataMessage invalidMessage = new MarketDataMessage(
                "demo",
                "INVALID",
                1.5,
                1,
                Instant.parse("2024-02-02T00:00:00Z")
        );

        // when
        cacheAdapter.send(Map.of("cache-valid", validMessage, "", invalidMessage));

        // then
        IMap<String, String> map = hazelcastInstance.getMap("market-cache");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("cache-valid"));
    }

    @Test
    void updateRequiresEntries() {
        // given
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.send(null));
        assertThrows(IllegalArgumentException.class, () -> cacheAdapter.send(Map.of()));
    }

    @Test
    void updateWritesMultipleEntries() {
        // given
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(hazelcastInstance, "market-cache");

        MarketDataMessage first = new MarketDataMessage(
                "demo",
                "FIRST",
                101.5,
                50,
                Instant.parse("2024-03-03T00:00:00Z")
        );
        MarketDataMessage second = new MarketDataMessage(
                "demo",
                "SECOND",
                202.5,
                25,
                Instant.parse("2024-03-03T00:00:00Z")
        );

        // when
        cacheAdapter.send(Map.of("cache-first", first, "cache-second", second));

        // then
        IMap<String, String> map = hazelcastInstance.getMap("market-cache");
        assertEquals(2, map.size());
        assertTrue(map.get("cache-first").contains("\"symbol\":\"FIRST\""));
        assertTrue(map.get("cache-second").contains("\"symbol\":\"SECOND\""));
    }

    @Test
    void resendAllFromLocalCacheAfterReconnect() throws InterruptedException {
        // given
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
        HazelcastCacheAdapter cacheAdapter = new HazelcastCacheAdapter(client, "market-cache");

        MarketDataMessage message = new MarketDataMessage(
                "demo",
                "RECONNECT",
                33.3,
                3,
                Instant.parse("2024-04-04T00:00:00Z")
        );

        // when
        cacheAdapter.send(Map.of("cache-reconnect", message));

        IMap<String, String> map = hazelcastInstance.getMap("market-cache");
        assertEquals(1, map.size());

        hazelcastInstance.shutdown();

        // start a new member for the same cluster; client should reconnect and resend cached values
        Config config = new Config();
        config.setClusterName(clusterName);
        HazelcastInstance newMember = Hazelcast.newHazelcastInstance(config);

        // then
        IMap<String, String> reconnectedMap = newMember.getMap("market-cache");
        boolean found = false;
        for (int i = 0; i < 40; i++) { // wait up to 10 seconds
            if (reconnectedMap.containsKey("cache-reconnect")) {
                found = true;
                break;
            }
            Thread.sleep(250);
        }

        assertTrue(found, "Entry should be resent after client reconnects");
        assertTrue(reconnectedMap.get("cache-reconnect").contains("\"RECONNECT\""));

        client.shutdown();
    }
}
