package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.snapshot.StatsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes statistics snapshots to logs.
 * Formats the hierarchical structure in a readable way.
 */
@Slf4j
@Component
public class LoggerStatsSink implements IStatsSink {

    @Override
    public void publish(StatsSnapshot snapshot) {
        log.info("=== Statistics Report: {} ===", snapshot.name());
        printSnapshot(snapshot, "");
    }

    private void printSnapshot(StatsSnapshot snapshot, String indent) {
        // Print counters
        snapshot.counters().forEach((name, value) ->
                log.info("{}counter.{} = {}", indent, name, value));

        // Print gauges
        snapshot.gauges().forEach((name, value) ->
                log.info("{}gauge.{} = {}", indent, name, value));

        // Print latencies
        snapshot.latencies().forEach((name, latency) ->
                log.info("{}latency.{} = {{count={}, avg={} µs, max={} µs}}",
                        indent, name, latency.count(),
                        String.format("%.2f", latency.avgMicros()),
                        latency.maxMicros()));

        // Print children recursively
        snapshot.children().forEach((name, child) -> {
            log.info("{}{}:", indent, name);
            printSnapshot(child, indent + "  ");
        });
    }
}
