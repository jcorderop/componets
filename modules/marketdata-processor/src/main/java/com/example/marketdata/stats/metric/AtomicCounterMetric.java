package com.example.marketdata.stats.metric;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AtomicCounterMetric implements ICounterMetric {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public void add(final long delta) {
        if (delta >= 0) {
            counter.addAndGet(delta);
        } else {
            log.warn("Invalid value for gauge metric: [{}]", delta);
        }
    }

    @Override
    public long sumThenReset() {
        return counter.getAndSet(0);
    }
}