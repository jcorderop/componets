package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.processor.ProcessorStatsSink;
import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simple {@link ProcessorStatsSink} that renders each snapshot to the application logs. It is
 * useful for diagnostics or environments where a lightweight reporting mechanism is sufficient.
 */
@Slf4j
@Service
public class ProcessorStatsSinkLoggerService implements ProcessorStatsSink {

    @Override
    public void publish(List<ProcessorStatsSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            log.debug("No processor stats to publish for the current interval");
            return;
        }

        for (ProcessorStatsSnapshot snapshot : snapshots) {
            log.info(
                    "Processor stats [{}]: window={}..{}, enqueued={}, processed={}, dropped={}, " +
                            "latency_ms[min={}, max={}, avg={}], queueSize={}",
                    snapshot.processorName(),
                    snapshot.windowStartMillis(),
                    snapshot.windowEndMillis(),
                    snapshot.eventsEnqueued(),
                    snapshot.eventsProcessed(),
                    snapshot.eventsDropped(),
                    snapshot.minLatencyMillis(),
                    snapshot.maxLatencyMillis(),
                    snapshot.avgLatencyMillis(),
                    snapshot.queueSizeAtSnapshot()
            );
        }
    }
}
