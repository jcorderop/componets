package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

/**
 * BPIPE-specific consumed metrics wrapper.
 */
public class WrapperConsumedBpipeStats extends AbstractConsumedStats {

    public WrapperConsumedBpipeStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_BPIPE_EVENTS));
    }
}
