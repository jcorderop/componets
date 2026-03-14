package com.example.marketdata.stats.metric;

public interface IGaugeMetric {
    void set(long value);
    long value();
    void reset();

    /**
     * Atomically get the current value and reset to zero.
     * Thread-safe for concurrent updates during snapshot.
     */
    long getAndReset();
}
