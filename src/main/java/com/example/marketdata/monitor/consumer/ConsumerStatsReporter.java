package com.example.marketdata.monitor.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Coordinates the periodic publication of consumer metrics by gathering snapshots from the
 * {@link ConsumerStatsRegistry} and forwarding them to every configured {@link ConsumerStatsSink}.
 * Any individual sink failure is logged and ignored so the scheduled task keeps running.
 * <p>
 * Scheduling is driven by {@code marketdata.stats.interval-ms} (default {@code 60000}) via
 * the {@link org.springframework.scheduling.annotation.Scheduled} annotation.
 */
@Component
@Slf4j
public class ConsumerStatsReporter {

    private final ConsumerStatsRegistry consumerStatsRegistry;
    private final List<ConsumerStatsSink> sinks;

    public ConsumerStatsReporter(ConsumerStatsRegistry registry,
                                 List<ConsumerStatsSink> sinks) {
        this.consumerStatsRegistry = registry;
        this.sinks = List.copyOf(sinks);
    }

    @Scheduled(fixedRateString = "${marketdata.stats.interval-ms:60000}")
    public void publishStats() {
        List<ConsumerStatsSnapshot> snapshots = consumerStatsRegistry.snapshotAndReset();
        if (snapshots.isEmpty()) {
            log.debug("No consumer stats snapshots to publish");
            return;
        }

        for (ConsumerStatsSink sink : sinks) {
            try {
                sink.publish(snapshots);
            } catch (Exception e) {
                String sinkName = sink.getClass().getSimpleName();

                if (shouldStopOn(e)) {
                    log.warn("Consumer stats publishing interrupted while calling sink {}", sinkName, e);
                    return;
                }

                log.warn("Consumer stats sink {} failed to publish stats", sinkName, e);
            }
        }
    }

    boolean shouldStopOn(Exception e) {
        // Direct InterruptedException
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }

        // Wrapped InterruptedException
        if (e.getCause() instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }

        // Thread already interrupted
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }

        return false;
    }
}

