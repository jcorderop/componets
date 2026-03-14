package com.example.marketdata.stats.metric;

public interface IGaugeMetric {
    void set(long value);
    long getAndReset();
}
