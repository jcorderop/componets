package com.example.marketdata.stats.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicCounterMetricTest {

    @Test
    void sumThenResetReturnsAccumulatedValueAndResets() {
        AtomicCounterMetric metric = new AtomicCounterMetric();

        metric.add(3);
        metric.add(7);

        assertEquals(10, metric.sumThenReset());
        assertEquals(0, metric.sumThenReset());
    }
}
