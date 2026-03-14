package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicGaugeMetric implements IGaugeMetric {

    private final AtomicLong value = new AtomicLong();

    @Override
    public void setMax(final long value) {
        this.value.updateAndGet(previous -> Math.max(previous, value));
    }

    @Override
    public long getAndReset() {
        return value.getAndSet(0);
    }
}
