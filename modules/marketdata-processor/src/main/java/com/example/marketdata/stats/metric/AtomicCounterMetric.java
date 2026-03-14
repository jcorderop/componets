package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.LongAdder;

public class AtomicCounterMetric implements ICounterMetric {

    private final LongAdder counter = new LongAdder();

    @Override
    public void add(long delta) {
        counter.add(delta);
    }

    @Override
    public long value() {
        return counter.sum();
    }

    @Override
    public void reset() {
        counter.reset();
    }

    @Override
    public long sumThenReset() {
        return counter.sumThenReset();
    }
}
