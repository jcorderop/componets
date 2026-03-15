package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * ZMQ dispatched metrics wrapper.
 */
public class WrapperDispatchedZmqStats extends AbstractDispatchedStats {

    public WrapperDispatchedZmqStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.DISPATCHED_EVENTS),
                collector.latency(MetricName.DISPATCHED_ZMQ_LATENCY_MS),
                collector.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED),
                collector.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE)
        );
    }
}
