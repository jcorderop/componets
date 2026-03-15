package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.metric.ICounterMetric;

/**
 * Base API for consumed-event wrappers.
 * <p>
 * Concrete wrappers bind this abstraction to one consumed source metric
 * (Kafka/FIX/RFA/BPIPE).
 * </p>
 */
public abstract class AbstractConsumedStats implements IWrapperStats {

    private final ICounterMetric consumed;

    protected AbstractConsumedStats(final ICounterMetric consumed) {
        this.consumed = consumed;
    }

    public void addConsumed(final long delta) {
        consumed.add(delta);
    }
}
