
package com.example.marketdata.adapter.hazelcast.handler;

import com.example.marketdata.cache.MarketDataBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Ensures the buffer handler validates and forwards entries to the underlying buffer.
 */
class MarketDataBufferHandlerTest {

    @Test
    void handleDelegatesToBuffer() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        // given
        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        // when
        handler.handle("k1", "v1");

        // then
        verify(buffer).put("k1", "v1");
    }

    @Test
    void handleAllCallsHandleForEachEntry() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        // given
        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        // when
        handler.handleAll(Map.of("k1", "v1", "k2", "v2"));

        // then
        verify(buffer).put("k1", "v1");
        verify(buffer).put("k2", "v2");
    }

    @Test
    void handleAllDoesNothingForNullOrEmpty() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        // given
        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        // when
        handler.handleAll(null);
        handler.handleAll(Map.of());

        // then
        verifyNoInteractions(buffer);
    }

    @Test
    void handleSkipsBlankKeysAndNullValues() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        handler.handle("   ", "value");
        handler.handle("valid", null);

        verify(buffer, never()).put(any(), any());
    }
}
