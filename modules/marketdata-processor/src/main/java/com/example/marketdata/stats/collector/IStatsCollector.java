package com.example.marketdata.stats.collector;

import com.example.marketdata.stats.snapshot.StatsSnapshot;

/**
 * Interface for collecting statistics.
 * Provides snapshot and reset capabilities for reporting.
 */
public interface IStatsCollector {

    /**
     * Create an immutable snapshot of current statistics and reset all metrics.
     * @return snapshot containing only non-zero metrics
     */
    StatsSnapshot snapshotAndReset();
}
