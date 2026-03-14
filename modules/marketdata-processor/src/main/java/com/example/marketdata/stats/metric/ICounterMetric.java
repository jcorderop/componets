package com.example.marketdata.stats.metric;

public interface ICounterMetric {
    void add(long delta);
    long sumThenReset();
}
