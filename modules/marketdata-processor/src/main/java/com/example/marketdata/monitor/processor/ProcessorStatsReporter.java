package com.example.marketdata.monitor.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Coordinates the periodic publication of processor metrics by gathering snapshots from the
 * {@link ProcessorStatsRegistry} and forwarding them to every configured {@link ProcessorStatsSink}.
 * Any individual sink failure is logged and ignored so the scheduled task keeps running.
 * <p>
 * Scheduling is driven by {@code marketdata.stats.interval-ms} (default {@code 60000}) via
 * the {@link org.springframework.scheduling.annotation.Scheduled} annotation.
 */
@Slf4j
@Component
public class ProcessorStatsReporter {

    private final ProcessorStatsRegistry processorStatsRegistry;
    private final List<ProcessorStatsSink> sinks;

    public ProcessorStatsReporter(ProcessorStatsRegistry registry,
                                 List<ProcessorStatsSink> sinks) {
        this.processorStatsRegistry = registry;
        this.sinks = List.copyOf(sinks);
    }

    @Scheduled(fixedRateString = "${marketdata.stats.interval-ms:60000}")
    public void publishStats() {
        List<ProcessorStatsSnapshot> snapshots = processorStatsRegistry.snapshotAndReset();
        if (snapshots.isEmpty()) {
            log.debug("No processor stats snapshots to publish");
            return;
        }

        for (ProcessorStatsSink sink : sinks) {
            try {
                sink.publish(snapshots);
            } catch (Exception e) {
                String sinkName = sink.getClass().getSimpleName();

                if (shouldStopOn(e)) {
                    log.warn("Processor stats publishing interrupted while calling sink {}", sinkName, e);
                    return;
                }

                log.warn("Processor stats sink {} failed to publish stats", sinkName, e);
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

