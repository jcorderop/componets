
package com.example.marketdata.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataBufferTest {

    @Test
    void newBufferIsEmpty() {
        // given
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();

        // when

        // then
        assertTrue(buffer.isEmpty());
    }

    @Test
    void putMakesBufferNonEmpty() {
        // given
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();

        // when
        buffer.put("k1", "v1");

        // then
        assertFalse(buffer.isEmpty());
    }

    @Test
    void releaseBufferReturnsSnapshotAndClears() {
        // given
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        buffer.put("k1", "v1");
        buffer.put("k2", "v2");

        // when
        Map<String, String> snapshot = buffer.releaseBuffer();

        // then
        assertEquals(2, snapshot.size());
        assertEquals("v1", snapshot.get("k1"));
        assertEquals("v2", snapshot.get("k2"));

        assertTrue(buffer.isEmpty());
    }

    @Test
    void subsequentReleasesSeeOnlyNewEntries() {
        // given
        MarketDataBuffer<String> buffer = new MarketDataBuffer<>();
        buffer.put("k1", "v1");

        // when
        Map<String, String> first = buffer.releaseBuffer();
        assertEquals(1, first.size());

        buffer.put("k2", "v2");
        Map<String, String> second = buffer.releaseBuffer();

        // then
        assertEquals(1, second.size());
        assertEquals("v2", second.get("k2"));
    }
}
