package com.example.marketdata.adapter.hazelcast;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.exception.ConsumerRetryableException;
import com.example.marketdata.exception.ConsumerRuntimeException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HazelcastCacheAdapterTest {

    private HazelcastInstance hz;
    private IMap<String, String> map;

    @BeforeEach
    void setup() {
        Config config = new Config();
        config.setClusterName("dev");
        hz = Hazelcast.newHazelcastInstance(config);
        map = hz.getMap("market-cache");
        map.clear();
    }

    @AfterEach
    void shutdown() {
        if (hz != null) {
            hz.shutdown();
        }
    }

    private HazelcastCacheAdapter<MarketDataMessage> adapter() {
        return new HazelcastCacheAdapter<>(hz, "market-cache");
    }

    private MarketDataMessage msg() {
        return MarketDataMessage.builder()
                .source("demo")
                .symbol("TEST")
                .price(10.5)
                .size(5)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ----------------------------------------------------------------------
    // Happy path tests (real Hazelcast)
    // ----------------------------------------------------------------------

    @Test
    void sendRequiresEntries() {
        // given
        HazelcastCacheAdapter<MarketDataMessage> a = adapter();

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> a.send(null));
        assertThrows(IllegalArgumentException.class, () -> a.send(Map.of()));
    }

    @Test
    void sendHappyPathWritesJsonToHazelcast() {
        // given
        HazelcastCacheAdapter<MarketDataMessage> a = adapter();

        // when
        a.send(Map.of("cache-1", msg()));

        // then
        String json = map.get("cache-1");
        assertNotNull(json);
        assertTrue(json.contains("\"symbol\":\"TEST\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00:00Z\""));
    }

    @Test
    void sendSkipsInvalidEntries() {
        // given
        HazelcastCacheAdapter<MarketDataMessage> a = adapter();

        // when
        a.send(Map.of(
                "", msg(),
                "valid", msg()
        ));

        // then
        assertFalse(map.containsKey(""));
        assertTrue(map.containsKey("valid"));
    }

    // ----------------------------------------------------------------------
    // Exception simulation WITHOUT mocks
    // ----------------------------------------------------------------------

    /**
     * How to generate a RetryableHazelcastException?
     *
     * Steps:
     *  1) Start two Hazelcast members
     *  2) Use one as client and kill the *other* member during putAll
     *  3) Hazelcast throws RetryableHazelcastException (cluster instability)
    */
    @Test
    void hazelcastInstanceNotActiveExceptionBecomesConsumerRuntimeException() throws InterruptedException {
        // given
        HazelcastCacheAdapter<MarketDataMessage> a =
                new HazelcastCacheAdapter<>(hz, "market-cache");

        shutdown();
        Thread.sleep(2_000);

        // Cluster instability begins → write will trigger RetryableHazelcastException
        // when
        // then
        assertThrows(ConsumerRuntimeException.class,
                () -> a.send(Map.of("k1", msg())));
    }

    /**
     * How to generate a NON-retryable HazelcastException?
     *
     * Steps:
     *  1) Shutdown the local Hazelcast instance BEFORE send()
     *  2) `send()` will attempt to call putAll() on a dead instance
     *  3) Hazelcast throws a non-retryable HazelcastException
     */
    @Test
    void nonRetryableHazelcastExceptionBecomesConsumerRuntimeException() {

        // given
        HazelcastCacheAdapter<MarketDataMessage> a =
                new HazelcastCacheAdapter<>(hz, "market-cache");

        // Kill Hazelcast → Local instance is dead → non-retryable exception on map.putAll()
        hz.shutdown();

        // when
        // then
        assertThrows(ConsumerRuntimeException.class,
                () -> a.send(Map.of("kX", msg())));
    }
}
