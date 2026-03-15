package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import org.springframework.stereotype.Component;

/**
 * Pipeline metrics wrapper.
 */
@Component
public class WrapperPipelineStats extends AbstractPipelineStats {

    public WrapperPipelineStats(final ServiceStatsCollector collector) {
        super(
                collector.counter(MetricName.PIPELINE_RECEIVED_EVENTS),
                collector.latency(MetricName.PIPELINE_LATENCY),
                collector.counter(MetricName.PIPELINE_FORWARDED_EVENTS)
        );
    }
}
