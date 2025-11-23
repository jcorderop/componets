
package com.example.marketdata.adapter.hazelcast.handler;

import com.example.marketdata.cache.MarketDataBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class MarketDataBufferHandlerTest {

    @Test
    void handleDelegatesToBuffer() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        handler.handle("k1", "v1");

        verify(buffer).put("k1", "v1");
    }

    @Test
    void handleAllCallsHandleForEachEntry() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        handler.handleAll(Map.of("k1", "v1", "k2", "v2"));

        verify(buffer).put("k1", "v1");
        verify(buffer).put("k2", "v2");
    }

    @Test
    void handleAllDoesNothingForNullOrEmpty() {
        @SuppressWarnings("unchecked")
        MarketDataBuffer<String> buffer = mock(MarketDataBuffer.class);

        MarketDataBufferHandler<String> handler = new MarketDataBufferHandler<>(buffer);

        handler.handleAll(null);
        handler.handleAll(Map.of());

        verifyNoInteractions(buffer);
    }
}
