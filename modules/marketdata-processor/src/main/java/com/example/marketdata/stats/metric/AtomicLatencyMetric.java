package com.example.marketdata.stats.metric;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
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
        if (latency >= 0) {
            current.get().record(latency);
        } else {
            log.warn("Invalid value for gauge metric: [{}]", latency);
        }
    }

    @Override
    public LatencyValues snapshotAndReset() {
        final State captured = current.getAndSet(new State());

        final long capturedCount = captured.count.get();
        final long capturedTotal = captured.total.get();
        final long capturedMax = captured.max.get();

        final double avg = capturedCount > 0 ? (double) capturedTotal / capturedCount : 0.0;
        return new LatencyValues(avg, capturedMax);
    }
}