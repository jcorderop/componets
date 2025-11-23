
package com.example.marketdata.util;

import com.example.demo.MarketDataMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures JSON serialization of domain messages preserves key fields and timestamps.
 */
class JsonUtilTest {

    @Test
    void toJsonSerializesPojo() {
        // given
        MarketDataMessage msg = MarketDataMessage.builder()
                .source("demo")
                .symbol("BOND1")
                .price(100.5)
                .size(10)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        // when
        String json = JsonUtil.toJson(msg);

        // then
        assertNotNull(json);
        assertTrue(json.contains("\"symbol\":\"BOND1\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01T00:00:00Z\""));
    }
}
