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
@RequiredArgsConstructor
@Slf4j
public class ConsumerStatsReporter {

    private final ConsumerStatsRegistry consumerStatsRegistry;
    private final List<ConsumerStatsSink> sinks;

    @Scheduled(fixedRateString = "${marketdata.stats.interval-ms:60000}")
    public void publishStats() {
        List<ConsumerStatsSnapshot> snapshots = consumerStatsRegistry.snapshotAndReset();
        for (ConsumerStatsSink sink : sinks) {
            try {
                sink.publish(snapshots);
            } catch (Exception e) {
                // Don't allow a faulty sink to stop stats publication for others
                // or to break the scheduled task.
                String sinkName = sink.getClass().getSimpleName();
                if (throwIfInterrupted(e)) {
                    log.warn("Consumer stats publishing interrupted while calling sink {}", sinkName, e);
                    return;
                }
                // Log at warn to draw attention without spamming error-level logs every interval.
                log.warn("Consumer stats sink {} failed to publish stats", sinkName, e);
            }
        }
    }

    private static boolean throwIfInterrupted(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        return false;
    }
}

