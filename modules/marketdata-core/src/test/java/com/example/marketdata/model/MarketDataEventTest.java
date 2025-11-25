package com.example.marketdata.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketDataEventTest {

    private record TestMarketDataEvent(String cacheId) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return cacheId;
        }
    }

    @Test
    void returnsProvidedCacheId() {
        MarketDataEvent event = new TestMarketDataEvent("ABC-123");
        assertEquals("ABC-123", event.getCacheId());
    }
}
