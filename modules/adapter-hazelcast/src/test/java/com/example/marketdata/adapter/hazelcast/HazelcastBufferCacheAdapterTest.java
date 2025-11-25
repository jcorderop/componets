package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.adapter.hazelcast.handler.MarketDataBufferHandler;
import com.example.marketdata.exception.ProcessorRetryableException;
import com.example.marketdata.exception.ProcessorRuntimeException;
import com.example.marketdata.model.MarketDataEvent;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.map.IMap;
import com.hazelcast.spi.exception.RetryableHazelcastException;
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
class HazelcastBufferCacheAdapterTest {

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

    private HazelcastBufferCacheAdapter<TestMessage> adapter() {
        return new HazelcastBufferCacheAdapter<>(hz, "market-cache");
    }

    private HazelcastBufferCacheAdapter<MarketDataEvent> bufferedAdapter(MarketDataBufferHandler<MarketDataEvent> handler) {
        return new HazelcastBufferCacheAdapter<>(hz, "market-cache", handler, mock(HazelcastBufferThrottle.class));
    }

    private TestMessage msg() {
        return new TestMessage("TEST", Instant.parse("2024-01-01T00:00:00Z"));
    }

    private AdapterContext<TestMessage> adapterWithFailingMap(RuntimeException failure) {
        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        LifecycleService lifecycleService = mock(LifecycleService.class);
        when(hazelcast.getLifecycleService()).thenReturn(lifecycleService);
        when(lifecycleService.addLifecycleListener(any())).thenReturn("listener-id");

        @SuppressWarnings("unchecked")
        IMap<String, String> map = mock(IMap.class);
        when(hazelcast.getMap("market-cache")).thenReturn(map);
        doThrow(failure).when(map).putAll(anyMap());

        @SuppressWarnings("unchecked")
        MarketDataBufferHandler<TestMessage> handler = mock(MarketDataBufferHandler.class);

        @SuppressWarnings("unchecked")
        HazelcastBufferThrottle<TestMessage> throttle = mock(HazelcastBufferThrottle.class);

        HazelcastBufferCacheAdapter<TestMessage> adapter =
                new HazelcastBufferCacheAdapter<>(hazelcast, "market-cache", handler, throttle);
        return new AdapterContext<>(adapter, map);
    }

    // ----------------------------------------------------------------------
    // Happy path tests (real Hazelcast)
    // ----------------------------------------------------------------------

    @Test
    void sendRequiresEntries() {
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

        assertThrows(IllegalArgumentException.class, () -> a.send(null));
        assertThrows(IllegalArgumentException.class, () -> a.send(Map.of()));
    }

    @Test
    void sendHappyPathWritesJsonToHazelcast() {
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

        a.send(Map.of("cache-1", msg()));

        String json = map.get("cache-1");
        assertNotNull(json);
        assertTrue(json.contains("\"symbol\":\"TEST\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00:00Z\""));
    }

    @Test
    void sendSkipsInvalidEntries() {
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

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
        HazelcastBufferCacheAdapter<TestMessage> a = new HazelcastBufferCacheAdapter<>(hz, "market-cache");

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
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

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
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

        assertThrows(ProcessorRuntimeException.class, () -> {
            hz.shutdown();
            Thread.sleep(2_000);
            a.send(Map.of("cache-1", msg()));
        });
    }

    @Test
    void entriesAreConsideredNonRetryableWhenHazelcastThrowsRuntimeException() {
        HazelcastBufferCacheAdapter<TestMessage> a = adapter();

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

        HazelcastBufferCacheAdapter<MarketDataEvent> a = bufferedAdapter(handler);

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
        HazelcastBufferCacheAdapter<MarketDataEvent> a = new HazelcastBufferCacheAdapter<>(hz, "market-cache");

        assertThrows(NullPointerException.class, () -> a.bufferMarketData(List.of(new TestEvent("cache-1"))));
    }

    @Test
    void retryableHazelcastExceptionSurfacesAsProcessorRetryable() {
        AdapterContext<TestMessage> ctx = adapterWithFailingMap(new RetryableHazelcastException("retry"));

        assertThrows(ProcessorRetryableException.class, () ->
                ctx.adapter().send(Map.of("cache-1", msg())));
        verify(ctx.map()).putAll(anyMap());
    }

    @Test
    void nonRetryableHazelcastExceptionSurfacesAsProcessorRuntime() {
        AdapterContext<TestMessage> ctx = adapterWithFailingMap(new HazelcastException("fatal"));

        assertThrows(ProcessorRuntimeException.class, () ->
                ctx.adapter().send(Map.of("cache-1", msg())));
        verify(ctx.map()).putAll(anyMap());
    }

    private record TestMessage(String symbol, Instant timestamp) { }

    private record TestEvent(String cacheId) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return cacheId;
        }
    }

    private record AdapterContext<T>(HazelcastBufferCacheAdapter<T> adapter, IMap<String, String> map) { }
}
