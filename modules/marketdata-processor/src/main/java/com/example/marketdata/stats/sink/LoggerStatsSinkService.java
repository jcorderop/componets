package com.example.marketdata.stats.sink;

import com.example.marketdata.stats.reporter.StatsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Publishes statistics snapshots to logs.
 * Formats flat snapshot metrics in a readable way.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "stats.sink.logger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggerStatsSinkService implements IStatsSink {

    @Override
    public void publish(final StatsSnapshot snapshot) {
        if (snapshot.counters().isEmpty() && snapshot.gauges().isEmpty() && snapshot.latencies().isEmpty()) {
            log.info("=== Statistics Report: {} (empty) ===", snapshot.name());
            return;
        }

        log.info("=== Statistics Report: {} ===", snapshot.name());

        snapshot.counters().forEach((name, value) ->
                log.info("counter.{} = {}", name, value));

        snapshot.gauges().forEach((name, value) ->
                log.info("gauge.{} = {}", name, value));

        snapshot.latencies().forEach((name, latency) ->
                log.info("latency.{} = {{avg={} ms, max={} ms}}",
                        name,
                        String.format("%.2f", latency.avg()),
                        String.format("%.2f", latency.max())));
    }
}
