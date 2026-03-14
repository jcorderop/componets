package com.example.marketdata.stats.reporter;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsSnapshotTest {

    @Test
    void constructorWrapsMapsAsUnmodifiable() {
        StatsSnapshot snapshot = new StatsSnapshot(
                "test",
                new HashMap<>(Map.of("c", 1L)),
                new HashMap<>(Map.of("g", 2L)),
                new HashMap<>(Map.of("l", new StatsSnapshot.LatencySnapshot(3.0, 4.0)))
        );

        assertThrows(UnsupportedOperationException.class, () -> snapshot.counters().put("x", 1L));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.gauges().put("x", 1L));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.latencies().put("x", new StatsSnapshot.LatencySnapshot(1, 1)));
    }
}
