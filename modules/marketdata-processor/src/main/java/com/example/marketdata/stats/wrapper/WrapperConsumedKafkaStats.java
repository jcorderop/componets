package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * Kafka-specific consumed metrics wrapper.
 */
@Component
public class WrapperConsumedKafkaStats extends AbstractConsumedStats {

    public WrapperConsumedKafkaStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_KAFKA_EVENTS));
    }
}
