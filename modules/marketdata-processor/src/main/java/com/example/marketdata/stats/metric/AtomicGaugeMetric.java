package com.example.marketdata.stats.metric;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AtomicGaugeMetric implements IGaugeMetric {

    private final AtomicLong value = new AtomicLong();

    @Override
    public void setMax(final long value) {
        if (value >= 0) {
            this.value.updateAndGet(previous -> Math.max(previous, value));
        } else {
            log.warn("Invalid value for gauge metric: [{}]", value);
        }
    }

    @Override
    public long getAndReset() {
        return value.getAndSet(0);
    }
}
