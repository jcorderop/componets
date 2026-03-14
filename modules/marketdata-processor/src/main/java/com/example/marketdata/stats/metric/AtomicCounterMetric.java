package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.LongAdder;

public class AtomicCounterMetric implements ICounterMetric {

    private final LongAdder counter = new LongAdder();

    @Override
    public void add(final long delta) {
        counter.add(delta);
    }

    @Override
    public long sumThenReset() {
        return counter.sumThenReset();
    }
}
