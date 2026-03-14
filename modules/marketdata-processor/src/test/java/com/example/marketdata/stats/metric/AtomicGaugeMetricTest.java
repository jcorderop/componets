package com.example.marketdata.stats.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicGaugeMetricTest {

    @Test
    void setMaxKeepsHighestValueSeen() {
        AtomicGaugeMetric metric = new AtomicGaugeMetric();

        metric.setMax(10);
        metric.setMax(7);
        metric.setMax(15);
        metric.setMax(14);

        assertEquals(15, metric.getAndReset());
    }

    @Test
    void getAndResetClearsCurrentMax() {
        AtomicGaugeMetric metric = new AtomicGaugeMetric();

        metric.setMax(5);

        assertEquals(5, metric.getAndReset());
        assertEquals(0, metric.getAndReset());

        metric.setMax(3);
        assertEquals(3, metric.getAndReset());
    }
}
