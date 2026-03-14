package com.example.marketdata.stats.metric;

public interface ICounterMetric {
    void add(long delta);
    long value();
    void reset();

    /**
     * Atomically get the current value and reset to zero.
     * Thread-safe for concurrent updates during snapshot.
     */
    long sumThenReset();
}
