package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * FIX-specific consumed metrics wrapper.
 */
@Component
public class WrapperConsumedFixStats extends AbstractConsumedStats {

    public WrapperConsumedFixStats(final ServiceStatsCollector collector) {
        super(collector.counter(MetricName.CONSUMED_FIX_EVENTS));
    }
}
