package com.example.marketdata.stats.reporter;

import com.example.marketdata.stats.collector.IStatsCollector;
import com.example.marketdata.stats.sink.IStatsSink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring-managed reporter that periodically snapshots and publishes statistics.
 * <p>
 * Scheduling interval is configured with {@code stats.reporter.fixed-rate-millis}
 * (default: {@code 60000} milliseconds).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsReporter {

    private final IStatsCollector collector;
    private final List<IStatsSink> sinks;

    /**
     * Scheduled task that captures a snapshot and publishes it to all configured sinks.
     * <p>
     * Failures in one sink do not prevent publishing to other sinks.
     * </p>
     */
    @Scheduled(fixedRateString = "${stats.reporter.fixed-rate-millis:60000}")
    public void report() {
        try {
            log.debug("Starting scheduled statistics report");
            StatsSnapshot snapshot = collector.snapshotAndReset();

            for (IStatsSink sink : sinks) {
                try {
                    sink.publish(snapshot);
                } catch (Exception e) {
                    // Log but don't fail other sinks
                    log.error("Error publishing to sink {}: {}", sink.getClass().getSimpleName(), e.getMessage(), e);
                }
            }

            log.debug("Completed scheduled statistics report");
        } catch (Exception e) {
            log.error("Error during stats reporting: {}", e.getMessage(), e);
        }
    }
}
