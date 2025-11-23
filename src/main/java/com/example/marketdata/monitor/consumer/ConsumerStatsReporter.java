package com.example.marketdata.monitor.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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
                throwIfInterrupted(e);
                String sinkName = sink.getClass().getSimpleName();
                // Log at warn to draw attention without spamming error-level logs every interval.
                log.warn("Consumer stats sink {} failed to publish stats", sinkName, e);
            }
        }
    }

    private static void throwIfInterrupted(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}

