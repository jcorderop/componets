package com.example.marketdata.stats.metric;

public interface ILatencyMetric {
    void record(long micros);
    double avg();
    long max();
    void reset();

    // Internal accessors for snapshot export
    long count();
    long total();

    /**
     * Atomically snapshot all values and reset to zero.
     * Thread-safe for concurrent updates during snapshot.
     */
    LatencyValues snapshotAndReset();

    /**
     * Immutable holder for latency snapshot values.
     */
    record LatencyValues(long count, long total, long max) {}
}
