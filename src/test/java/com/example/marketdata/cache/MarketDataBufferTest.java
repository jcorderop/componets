
package com.example.marketdata.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataBufferTest {

    @Test
    void newBufferIsEmpty() {
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        assertTrue(buffer.isEmpty());
    }

    @Test
    void putMakesBufferNonEmpty() {
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        buffer.put("k1", "v1");
        assertFalse(buffer.isEmpty());
    }

    @Test
    void releaseBufferReturnsSnapshotAndClears() {
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        buffer.put("k1", "v1");
        buffer.put("k2", "v2");

        Map<String, String> snapshot = buffer.releaseBuffer();

        assertEquals(2, snapshot.size());
        assertEquals("v1", snapshot.get("k1"));
        assertEquals("v2", snapshot.get("k2"));

        assertTrue(buffer.isEmpty());
    }

    @Test
    void subsequentReleasesSeeOnlyNewEntries() {
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        buffer.put("k1", "v1");

        Map<String, String> first = buffer.releaseBuffer();
        assertEquals(1, first.size());

        buffer.put("k2", "v2");
        Map<String, String> second = buffer.releaseBuffer();
        assertEquals(1, second.size());
        assertEquals("v2", second.get("k2"));
    }
}
