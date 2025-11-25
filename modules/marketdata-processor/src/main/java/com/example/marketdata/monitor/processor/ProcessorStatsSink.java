package com.example.marketdata.monitor.processor;

import java.util.List;

/**
 * Destination for processor statistics snapshots. Implementations can persist, log or export the
 * collected metrics to external systems.
 */
public interface ProcessorStatsSink {
    void publish(List<ProcessorStatsSnapshot> snapshots);
}
