package com.example.marketdata.stats.metric;

public interface IGaugeMetric {
    void set(long value);
    long value();

    /**
     * Reset to zero.
     *
     * Kept for explicit lifecycle control (e.g., tests or manual resets).
     * Snapshot pipelines should prefer {@link #getAndReset()} for atomic capture.
     */
    void reset();

    /**
     * Atomically get the current value and reset to zero.
     * Thread-safe for concurrent updates during snapshot.
     */
    long getAndReset();
}
