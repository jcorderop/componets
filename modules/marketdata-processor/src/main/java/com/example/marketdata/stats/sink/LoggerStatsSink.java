package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes statistics snapshots to logs.
 * Formats flat snapshot metrics in a readable way.
 */
@Slf4j
@Component
public class LoggerStatsSink implements IStatsSink {

    @Override
    public void publish(StatsSnapshot snapshot) {
        log.info("=== Statistics Report: {} ===", snapshot.name());
        // Print counters
        snapshot.counters().forEach((name, value) ->
                log.info("counter.{} = {}", name, value));

        // Print gauges
        snapshot.gauges().forEach((name, value) ->
                log.info("gauge.{} = {}", name, value));

        // Print latencies
        snapshot.latencies().forEach((name, latency) ->
                log.info("latency.{} = {{count={}, avg={} µs, max={} µs}}",
                        name, latency.count(),
                        String.format("%.2f", latency.avgMicros()),
                        latency.maxMicros()));
    }

}
