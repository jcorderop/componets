package com.example.marketdata.stats.metric;

public interface IGaugeMetric {
    void setMax(long value);
    long getAndReset();
}
