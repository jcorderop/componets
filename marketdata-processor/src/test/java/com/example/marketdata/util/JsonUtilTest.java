
package com.example.marketdata.util;

import com.example.marketdata.model.MarketDataEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures JSON serialization of domain messages preserves key fields and timestamps.
 */
class JsonUtilTest {

    private record TestMarketDataMessage(String source,
                                         String symbol,
                                         double price,
                                         long size,
                                         Instant timestamp) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return symbol;
        }
    }

    @Test
    void toJsonSerializesPojo() {
        // given
        TestMarketDataMessage msg = new TestMarketDataMessage(
                "demo",
                "BOND1",
                100.5,
                10,
                Instant.parse("2024-01-01T00:00:00Z")
        );

        // when
        String json = JsonUtil.toJson(msg);

        // then
        assertNotNull(json);
        assertTrue(json.contains("\"symbol\":\"BOND1\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00:00Z\""));
    }
}
