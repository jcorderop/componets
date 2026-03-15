package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * Kafka dispatched metrics wrapper.
 */
@Component
public class WrapperDispatchedKafkaStats extends AbstractDispatchedStats {

    public WrapperDispatchedKafkaStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.DISPATCHED_KAFKA_EVENTS),
                collector.latency(MetricName.DISPATCHED_KAFKA_LATENCY_MS),
                collector.counter(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED),
                collector.gauge(MetricName.DISPATCHED_KAFKA_QUEUE_SIZE)
        );
    }
}
