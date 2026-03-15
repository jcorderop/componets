package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * FIX-specific consumed metrics wrapper.
 */
public class WrapperConsumedFixStats extends AbstractConsumedStats {

    public WrapperConsumedFixStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_FIX_EVENTS));
    }
}
