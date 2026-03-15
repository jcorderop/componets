package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * RFA-specific consumed metrics wrapper.
 */
public class WrapperConsumedRfaStats extends AbstractConsumedStats {

    public WrapperConsumedRfaStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_RFA_EVENTS));
    }
}
