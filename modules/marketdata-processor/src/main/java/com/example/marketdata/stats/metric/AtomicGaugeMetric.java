package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicGaugeMetric implements IGaugeMetric {

    private final AtomicLong value = new AtomicLong();

    @Override
    public void set(long value) {
        this.value.set(value);
    }

    @Override
    public long value() {
        return value.get();
    }

    @Override
    public void reset() {
        value.set(0);
    }

    @Override
    public long getAndReset() {
        return value.getAndSet(0);
    }
}
