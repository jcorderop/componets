package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * Oracle storage metrics wrapper.
 */
public class WrapperStorageOracleStats extends AbstractStorageStats {

    public WrapperStorageOracleStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.STORAGE_ORACLE_EVENTS),
                collector.latency(MetricName.STORAGE_ORACLE_LATENCY_MS),
                collector.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED)
        );
    }
}
