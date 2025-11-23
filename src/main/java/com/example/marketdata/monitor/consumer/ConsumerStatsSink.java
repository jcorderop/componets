package com.example.marketdata.monitor.consumer;

import java.util.List;

/**
 * Destination for consumer statistics snapshots. Implementations can persist, log or export the
 * collected metrics to external systems.
 */
public interface ConsumerStatsSink {
    void publish(List<ConsumerStatsSnapshot> snapshots);
}
