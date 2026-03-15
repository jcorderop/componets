package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * BPIPE-specific consumed metrics wrapper.
 */
@Component
public class WrapperConsumedBpipeStats extends AbstractConsumedStats {

    public WrapperConsumedBpipeStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_BPIPE_EVENTS));
    }
}
