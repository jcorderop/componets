package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * Hazelcast dispatched metrics wrapper.
 */
@Component
public class WrapperDispatchedHazelcastStats extends AbstractDispatchedStats {

    public WrapperDispatchedHazelcastStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS),
                collector.latency(MetricName.DISPATCHED_HAZELCAST_LATENCY_MS),
                collector.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED),
                collector.gauge(MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE)
        );
    }
}
