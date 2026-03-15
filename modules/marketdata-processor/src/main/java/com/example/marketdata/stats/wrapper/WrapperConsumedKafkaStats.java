package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * Kafka-specific consumed metrics wrapper.
 */
public class WrapperConsumedKafkaStats extends AbstractConsumedStats {

    public WrapperConsumedKafkaStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_KAFKA_EVENTS));
    }
}
