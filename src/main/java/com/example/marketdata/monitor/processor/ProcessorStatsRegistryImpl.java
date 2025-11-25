package com.example.marketdata.monitor.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe implementation of {@link ProcessorStatsRegistry} that aggregates metrics for each
 * processor in memory using per-processor buckets. Each snapshot resets the bucket to start a new
 * reporting window while retaining a consistent window start time for the captured metrics.
 */
@Slf4j
@Component
public class ProcessorStatsRegistryImpl implements ProcessorStatsRegistry {

    /**
     * A StatsBucket holds counters for a single processor.
     */
    private static class StatsBucket {
        final long windowStartMillis = System.currentTimeMillis();

        final LongAdder eventsEnqueued = new LongAdder();
        final LongAdder eventsProcessed = new LongAdder();
        final LongAdder eventsDropped = new LongAdder();

        final LongAdder totalLatencyMillis = new LongAdder();

        final AtomicLong minLatencyMillis = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxLatencyMillis = new AtomicLong(Long.MIN_VALUE);

        final AtomicReference<Integer> queueSize = new AtomicReference<>(0);
    }

    private final ConcurrentHashMap<String, StatsBucket> buckets = new ConcurrentHashMap<>();

    private StatsBucket getBucket(String processor) {
        return buckets.computeIfAbsent(processor, k -> new StatsBucket());
    }

    private void safeUpdate(String processorName, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while updating processor stats for {}", processorName, e);
                return;
            }
            log.warn("Failed to update processor stats for {}", processorName, e);
        }
    }

    @Override
    public void recordEnqueue(String processorName) {
        safeUpdate(processorName, () -> getBucket(processorName).eventsEnqueued.increment());
    }

    @Override
    public void recordDrop(String processorName) {
        recordDrops(processorName, 1);
    }

    @Override
    public void recordDrops(String processorName, int dropCount) {
        if (dropCount <= 0) {
            return;
        }
        safeUpdate(processorName, () -> getBucket(processorName).eventsDropped.add(dropCount));
    }

    @Override
    public void recordBatchProcessed(String processorName, int batchSize, long durationMillis) {
        safeUpdate(processorName, () -> {
            StatsBucket b = getBucket(processorName);

            b.eventsProcessed.add(batchSize);
            b.totalLatencyMillis.add(durationMillis);

            b.minLatencyMillis.accumulateAndGet(durationMillis, Math::min);
            b.maxLatencyMillis.accumulateAndGet(durationMillis, Math::max);
        });
    }

    @Override
    public void recordQueueSize(String processorName, int queueSize) {
        safeUpdate(processorName, () -> getBucket(processorName).queueSize.set(queueSize));
    }

    @Override
    public List<ProcessorStatsSnapshot> snapshotAndReset() {
        long now = System.currentTimeMillis();
        List<ProcessorStatsSnapshot> snapshots = new ArrayList<>();

        // Swap buckets atomically
        for (String processor : buckets.keySet()) {
            try {
                StatsBucket old = buckets.replace(processor, new StatsBucket());
                if (old == null) continue;

                long eventsProcessed = old.eventsProcessed.sum();
                long totalLatency = old.totalLatencyMillis.sum();

                double avg = eventsProcessed > 0
                        ? ((double) totalLatency / eventsProcessed)
                        : 0.0;

                snapshots.add(ProcessorStatsSnapshot.builder()
                        .processorName(processor)
                        .windowStartMillis(old.windowStartMillis)
                        .windowEndMillis(now)
                        .eventsEnqueued(old.eventsEnqueued.sum())
                        .eventsProcessed(eventsProcessed)
                        .eventsDropped(old.eventsDropped.sum())
                        .minLatencyMillis(old.minLatencyMillis.get() == Long.MAX_VALUE ? 0 : old.minLatencyMillis.get())
                        .maxLatencyMillis(old.maxLatencyMillis.get() == Long.MIN_VALUE ? 0 : old.maxLatencyMillis.get())
                        .avgLatencyMillis(avg)
                        .queueSizeAtSnapshot(old.queueSize.get())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to snapshot stats for {}", processor, e);
            }
        }

        return snapshots;
    }
}

