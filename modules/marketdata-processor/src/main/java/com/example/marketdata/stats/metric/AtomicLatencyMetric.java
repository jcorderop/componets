package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLatencyMetric implements ILatencyMetric {

    private final LongAdder count = new LongAdder();
    private final LongAdder latency = new LongAdder();
    private final AtomicLong max = new AtomicLong();

    @Override
    public void record(final long latency) {
        this.count.increment();
        this.latency.add(latency);
        this.max.updateAndGet(prev -> Math.max(prev, latency));
    }

    @Override
    public LatencyValues snapshotAndReset() {
        final long capturedCount = count.sumThenReset();
        final long capturedMax = max.getAndSet(0);
        final var capturedAvg = capturedCount > 0 ? (double) latency.sum() / capturedCount : 0.0;
        return new LatencyValues(capturedAvg, capturedMax);
    }
}
