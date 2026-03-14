package com.example.marketdata.stats.metric;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicLatencyMetric implements ILatencyMetric {

    private static final class State {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong total = new AtomicLong();
        private final AtomicLong max = new AtomicLong();

        void record(long latency) {
            count.incrementAndGet();
            total.addAndGet(latency);
            max.accumulateAndGet(latency, Math::max);
        }
    }

    private final AtomicReference<State> current = new AtomicReference<>(new State());

    @Override
    public void record(final long latency) {
        current.get().record(latency);
    }

    @Override
    public LatencyValues snapshotAndReset() {
        State captured = current.getAndSet(new State());

        long capturedCount = captured.count.get();
        long capturedTotal = captured.total.get();
        long capturedMax = captured.max.get();

        double avg = capturedCount > 0 ? (double) capturedTotal / capturedCount : 0.0;
        return new LatencyValues(avg, capturedMax);
    }
}