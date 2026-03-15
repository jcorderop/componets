package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * Postgres storage metrics wrapper.
 */
public class WrapperStoragePostgresStats extends AbstractStorageStats {

    public WrapperStoragePostgresStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.STORAGE_POSTGRES_EVENTS),
                collector.latency(MetricName.STORAGE_POSTGRES_LATENCY_MS),
                collector.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED)
        );
    }
}
