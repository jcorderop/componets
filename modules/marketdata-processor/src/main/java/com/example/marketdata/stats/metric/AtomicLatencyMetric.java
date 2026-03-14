package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLatencyMetric implements ILatencyMetric {

    private final LongAdder count = new LongAdder();
    private final LongAdder total = new LongAdder();
    private final AtomicLong max = new AtomicLong();

    @Override
    public void record(long micros) {
        count.increment();
        total.add(micros);
        max.updateAndGet(prev -> Math.max(prev, micros));
    }

    @Override
    public double avg() {
        long c = count.sum();
        return c > 0 ? (double) total.sum() / c : 0.0;
    }

    @Override
    public long max() {
        return max.get();
    }

    @Override
    public void reset() {
        count.reset();
        total.reset();
        max.set(0);
    }

    // Internal accessors for snapshot export
    @Override
    public long count() {
        return count.sum();
    }

    @Override
    public long total() {
        return total.sum();
    }

    @Override
    public LatencyValues snapshotAndReset() {
        // Atomically capture and reset all values
        long capturedCount = count.sumThenReset();
        long capturedTotal = total.sumThenReset();
        long capturedMax = max.getAndSet(0);
        return new LatencyValues(capturedCount, capturedTotal, capturedMax);
    }
}
