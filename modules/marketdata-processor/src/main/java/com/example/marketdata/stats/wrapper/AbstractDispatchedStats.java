package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.metric.ICounterMetric;
import com.example.marketdata.stats.metric.IGaugeMetric;
import com.example.marketdata.stats.metric.ILatencyMetric;

/**
 * Base API for dispatched-stage metrics.
 */
public abstract class AbstractDispatchedStats implements IWrapperStats {

    private final ICounterMetric dispatched;
    private final ILatencyMetric latency;
    private final ICounterMetric dropped;
    private final IGaugeMetric queueSize;

    protected AbstractDispatchedStats(
            final ICounterMetric dispatched,
            final ILatencyMetric latency,
            final ICounterMetric dropped,
            final IGaugeMetric queueSize
    ) {
        this.dispatched = dispatched;
        this.latency = latency;
        this.dropped = dropped;
        this.queueSize = queueSize;
    }

    public void addDispatched(final long delta) {
        dispatched.add(delta);
    }

    public void recordLatency(final long latencyMs) {
        latency.record(latencyMs);
    }

    public void addDropped(final long delta) {
        dropped.add(delta);
    }

    public void setQueueSizeMax(final long queueDepth) {
        queueSize.setMax(queueDepth);
    }
}
