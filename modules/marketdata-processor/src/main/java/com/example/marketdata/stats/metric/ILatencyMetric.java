package com.example.marketdata.stats.metric;

public interface ILatencyMetric {
    void record(long latency);
    LatencyValues snapshotAndReset();
    record LatencyValues(double avg, long max) {}
}
