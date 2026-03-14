package com.example.marketdata.stats.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicLatencyMetricTest {

    @Test
    void snapshotAndResetReturnsAvgAndMax() {
        AtomicLatencyMetric metric = new AtomicLatencyMetric();

        metric.record(10);
        metric.record(30);

        ILatencyMetric.LatencyValues values = metric.snapshotAndReset();
        assertEquals(20.0, values.avg(), 1e-9);
        assertEquals(30.0, values.max(), 1e-9);
    }

    @Test
    void snapshotAndResetResetsInternalState() {
        AtomicLatencyMetric metric = new AtomicLatencyMetric();

        metric.record(10);
        metric.snapshotAndReset();

        ILatencyMetric.LatencyValues valuesAfterReset = metric.snapshotAndReset();
        assertEquals(0.0, valuesAfterReset.avg(), 1e-9);
        assertEquals(0.0, valuesAfterReset.max(), 1e-9);
    }
}
