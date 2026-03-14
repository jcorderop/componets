package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicCounterMetric implements ICounterMetric {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public void add(final long delta) {
        counter.addAndGet(delta);
    }

    @Override
    public long sumThenReset() {
        return counter.getAndSet(0);
    }
}