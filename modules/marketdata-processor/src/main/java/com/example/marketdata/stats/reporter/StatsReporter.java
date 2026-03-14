package com.example.marketdata.stats.reporter;

import com.example.marketdata.stats.collector.IStatsCollector;
import com.example.marketdata.stats.sink.IStatsSink;
import com.example.marketdata.stats.snapshot.StatsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring-managed statistics reporter that periodically snapshots and publishes metrics.
 * Runs every minute (configurable) using Spring's @Scheduled annotation.
 * After publishing, resets all metrics for the next reporting window.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsReporter {

    private final IStatsCollector collector;
    private final List<IStatsSink> sinks;

    @Value("${stats.reporter.fixed-rate-millis:60000}")
    private long fixedRateMillis;

    /**
     * Automatically triggered at configured interval (default: 1 minute) by Spring scheduler.
     * Takes a snapshot of all metrics, publishes to all configured sinks, and resets metrics.
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
