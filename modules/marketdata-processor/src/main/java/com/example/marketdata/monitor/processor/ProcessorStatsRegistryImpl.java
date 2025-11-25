package com.example.marketdata.monitor.processor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe implementation of {@link ProcessorStatsRegistry} that aggregates metrics for each
 * processor in memory using per-processor buckets.
 *
 * All updates are serialized through a single-threaded executor (backed by an internal queue),
 * so modifications and snapshots never run concurrently. This simplifies reasoning about
 * consistency at the cost of a small amount of extra indirection.
 */
@Slf4j
@Component
public class ProcessorStatsRegistryImpl implements ProcessorStatsRegistry {

    /**
     * A StatsBucket holds counters for a single processor.
     *
     * Note: counters are still LongAdder/Atomic* for cheap roll-up,
     * but they are only mutated from the stats thread.
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

    /**
     * Per-processor stats buckets. Only mutated from the stats executor thread.
     */
    private final ConcurrentHashMap<String, StatsBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Single-thread executor that processes all stat updates and snapshots.
     * This is effectively our "stats queue".
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "processor-stats-executor");
        t.setDaemon(true);
        return t;
    });

    private StatsBucket getBucket(String processor) {
        return buckets.computeIfAbsent(processor, k -> new StatsBucket());
    }

    /**
     * Enqueue a stats update action to be executed by the single stats thread.
     */
    private void safeUpdate(String processorName, Runnable action) {
        try {
            executor.execute(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    // We still treat interruption specially inside the stats thread
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while updating processor stats for {}", processorName, e);
                        return;
                    }
                    log.warn("Failed to update processor stats for {}", processorName, e);
                }
            });
        } catch (RejectedExecutionException e) {
            // Happens if executor is shutting down
            log.warn("Stats executor is shut down; dropping stats update for {}", processorName, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    @Override
    public void recordEnqueue(String processorName) {
        // Count how many events were received/attempted in this window.
        safeUpdate(processorName, () -> getBucket(processorName).eventsEnqueued.increment());
    }

    @Override
    public void recordDrop(String processorName) {
        //will do a safeUpdate via recordDrops
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
        // durationMillis = processing time for the whole batch
        // We roll it up as processing time per event in this window.
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
        try {
            // Submit snapshot task to the same single-thread executor and wait for result.
            Callable<List<ProcessorStatsSnapshot>> task = createSnapshotAndResetTask();
            return executor.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while snapshotting processor stats", e);
            return List.of();
        } catch (ExecutionException e) {
            log.warn("Failed to snapshot processor stats", e.getCause());
            return List.of();
        } catch (RejectedExecutionException e) {
            log.warn("Stats executor is shut down; cannot snapshot processor stats", e);
            return List.of();
        }
    }

    private Callable<List<ProcessorStatsSnapshot>> createSnapshotAndResetTask() {
        // Run the whole snapshot operation inside the stats executor thread
        Callable<List<ProcessorStatsSnapshot>> task = () -> {
            long now = System.currentTimeMillis();
            List<ProcessorStatsSnapshot> snapshots = new ArrayList<>();

            for (Map.Entry<String, StatsBucket> entry : buckets.entrySet()) {
                String processor = entry.getKey();
                // Replace the bucket with a new one for the next window
                StatsBucket old = buckets.replace(processor, new StatsBucket());
                if (old == null) {
                    continue;
                }

                long eventsProcessed = old.eventsProcessed.sum();
                long totalLatency = old.totalLatencyMillis.sum();

                double avg = eventsProcessed > 0
                        ? ((double) totalLatency / eventsProcessed)
                        : 0.0;

                long minLatency = old.minLatencyMillis.get();
                long maxLatency = old.maxLatencyMillis.get();

                snapshots.add(new ProcessorStatsSnapshot(
                        processor,
                        old.windowStartMillis,
                        now,
                        old.eventsEnqueued.sum(),
                        eventsProcessed,
                        old.eventsDropped.sum(),
                        minLatency == Long.MAX_VALUE ? 0 : minLatency,
                        maxLatency == Long.MIN_VALUE ? 0 : maxLatency,
                        avg,
                        old.queueSize.get()
                ));
            }

            return snapshots;
        };
        return task;
    }
}
