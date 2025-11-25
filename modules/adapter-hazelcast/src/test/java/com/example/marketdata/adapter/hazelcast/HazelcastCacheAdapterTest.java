package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.adapter.hazelcast.handler.MarketDataBufferHandler;
import com.example.marketdata.exception.ProcessorRetryableException;
import com.example.marketdata.exception.ProcessorRuntimeException;
import com.example.marketdata.model.MarketDataEvent;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Exercises Hazelcast adapter validation, shadow cache behavior, and retry classification.
 */
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

    private HazelcastCacheAdapter<TestMessage> adapter() {
        return new HazelcastCacheAdapter<>(hz, "market-cache");
    }

    private HazelcastCacheAdapter<MarketDataEvent> bufferedAdapter(MarketDataBufferHandler<MarketDataEvent> handler) {
        return new HazelcastCacheAdapter<>(hz, "market-cache", handler, mock(HazelcastBufferThrottle.class));
    }

    private TestMessage msg() {
        return new TestMessage("TEST", Instant.parse("2024-01-01T00:00:00Z"));
    }

    // ----------------------------------------------------------------------
    // Happy path tests (real Hazelcast)
    // ----------------------------------------------------------------------

    @Test
    void sendRequiresEntries() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        assertThrows(IllegalArgumentException.class, () -> a.send(null));
        assertThrows(IllegalArgumentException.class, () -> a.send(Map.of()));
    }

    @Test
    void sendHappyPathWritesJsonToHazelcast() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        a.send(Map.of("cache-1", msg()));

        String json = map.get("cache-1");
        assertNotNull(json);
        assertTrue(json.contains("\"symbol\":\"TEST\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00:00Z\""));
    }

    @Test
    void sendSkipsInvalidEntries() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        a.send(Map.of(
                "", msg(),
                "valid", msg()
        ));

        assertFalse(map.containsKey(""));
        assertTrue(map.containsKey("valid"));
    }

    // ----------------------------------------------------------------------
    // Exception simulation WITHOUT mocks
    // ----------------------------------------------------------------------

    @Test
    void hazelcastInstanceNotActiveExceptionBecomesProcessorRuntimeException() throws InterruptedException {
        HazelcastCacheAdapter<TestMessage> a = new HazelcastCacheAdapter<>(hz, "market-cache");

        shutdown();
        Thread.sleep(2_000);

        assertThrows(ProcessorRuntimeException.class, () ->
                a.send(Map.of("cache-1", msg())));
    }

    // ----------------------------------------------------------------------
    // Shadow cache and retry classification (with mocks)
    // ----------------------------------------------------------------------

    @Test
    void cachedEntriesRemainAvailableAfterErrors() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        Map<String, TestMessage> entries = Map.of(
                "cache-1", msg(),
                "cache-2", msg()
        );

        Assertions.assertAll(() -> {
            try {
                a.send(entries);
            } catch (ProcessorRuntimeException | ProcessorRetryableException e) {
                // ignore
            }
        });

        a.send(entries);

        assertEquals(2, map.size());
    }

    @Test
    void entriesAreConsideredRetryableWhenHazelcastSaysSo() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        assertThrows(ProcessorRetryableException.class, () -> {
            hz.shutdown();
            a.send(Map.of("cache-1", msg()));
        });
    }

    @Test
    void entriesAreConsideredNonRetryableWhenHazelcastThrowsRuntimeException() {
        HazelcastCacheAdapter<TestMessage> a = adapter();

        assertThrows(ProcessorRuntimeException.class, () -> {
            hz.shutdown();
            a.send(Map.of(
                    "cache-1", msg(),
                    "cache-2", msg()
            ));
        });
    }

    @Test
    void bufferMarketDataDelegatesToHandler() {
        @SuppressWarnings("unchecked")
        MarketDataBufferHandler<MarketDataEvent> handler = mock(MarketDataBufferHandler.class);

        HazelcastCacheAdapter<MarketDataEvent> a = bufferedAdapter(handler);

        List<MarketDataEvent> batch = List.of(new TestEvent("cache-1"), new TestEvent("cache-2"));

        a.bufferMarketData(batch);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, MarketDataEvent>> captor = ArgumentCaptor.forClass(Map.class);
        verify(handler).handleAll(captor.capture());

        Map<String, MarketDataEvent> captured = captor.getValue();
        assertEquals(2, captured.size());
        assertTrue(captured.containsKey("cache-1"));
        assertTrue(captured.containsKey("cache-2"));
    }

    @Test
    void bufferMarketDataRequiresHandler() {
        HazelcastCacheAdapter<MarketDataEvent> a = new HazelcastCacheAdapter<>(hz, "market-cache");

        assertThrows(NullPointerException.class, () -> a.bufferMarketData(List.of(new TestEvent("cache-1"))));
    }

    private record TestMessage(String symbol, Instant timestamp) { }

    private record TestEvent(String cacheId) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return cacheId;
        }
    }
}
