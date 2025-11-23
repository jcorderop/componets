
package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.cache.MarketDataBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Verifies the throttle drains the buffer and forwards batches to the cache adapter.
 */
class HazelcastBufferThrottleTest {

    @Test
    void runThrottledDoesNothingWhenBufferEmpty() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);
        when(buffer.isEmpty()).thenReturn(true);

        @SuppressWarnings("unchecked")
        HazelcastCacheAdapter<String> adapter = mock(HazelcastCacheAdapter.class);

        HazelcastBufferThrottle<String> throttle = new HazelcastBufferThrottle<>(buffer, adapter);

        // when
        throttle.runThrottled();

        // then
        verify(buffer).isEmpty();
        verifyNoMoreInteractions(buffer, adapter);
    }

    @Test
    void runThrottledFlushesToHazelcastWhenNotEmpty() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);
        when(buffer.isEmpty()).thenReturn(false);
        when(buffer.releaseBuffer()).thenReturn(Map.of("k1", "v1", "k2", "v2"));

        @SuppressWarnings("unchecked")
        HazelcastCacheAdapter<String> adapter = mock(HazelcastCacheAdapter.class);

        HazelcastBufferThrottle<String> throttle = new HazelcastBufferThrottle<>(buffer, adapter);

        // when
        throttle.runThrottled();

        // then
        verify(buffer).isEmpty();
        verify(buffer).releaseBuffer();
        verify(adapter).send(Map.of("k1", "v1", "k2", "v2"));
    }

    @Test
    void runThrottledCatchesExceptionFromAdapter() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);
        when(buffer.isEmpty()).thenReturn(false);
        when(buffer.releaseBuffer()).thenReturn(Map.of("k1", "v1"));

        @SuppressWarnings("unchecked")
        HazelcastCacheAdapter<String> adapter = mock(HazelcastCacheAdapter.class);
        doThrow(new RuntimeException("boom")).when(adapter).send(anyMap());

        HazelcastBufferThrottle<String> throttle = new HazelcastBufferThrottle<>(buffer, adapter);

        // when
        throttle.runThrottled();

        // then
        verify(buffer).releaseBuffer();
        verify(adapter).send(anyMap());
    }
}
