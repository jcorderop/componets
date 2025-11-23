package com.example.marketdata.monitor.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Component
public class ConsumerStatsRegistryImpl implements ConsumerStatsRegistry {

    /**
     * A StatsBucket holds counters for a single consumer.
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

    private StatsBucket getBucket(String consumer) {
        return buckets.computeIfAbsent(consumer, k -> new StatsBucket());
    }

    private void safeUpdate(String consumerName, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Failed to update consumer stats for {}", consumerName, e);
        }
    }

    @Override
    public void recordEnqueue(String consumerName) {
        safeUpdate(consumerName, () -> getBucket(consumerName).eventsEnqueued.increment());
    }

    @Override
    public void recordDrop(String consumerName) {
        recordDrops(consumerName, 1);
    }

    @Override
    public void recordDrops(String consumerName, int dropCount) {
        if (dropCount <= 0) {
            return;
        }
        safeUpdate(consumerName, () -> getBucket(consumerName).eventsDropped.add(dropCount));
    }

    @Override
    public void recordBatchProcessed(String consumerName, int batchSize, long durationMillis) {
        safeUpdate(consumerName, () -> {
            StatsBucket b = getBucket(consumerName);

            b.eventsProcessed.add(batchSize);
            b.totalLatencyMillis.add(durationMillis);

            b.minLatencyMillis.accumulateAndGet(durationMillis, Math::min);
            b.maxLatencyMillis.accumulateAndGet(durationMillis, Math::max);
        });
    }

    @Override
    public void recordQueueSize(String consumerName, int queueSize) {
        safeUpdate(consumerName, () -> getBucket(consumerName).queueSize.set(queueSize));
    }

    @Override
    public List<ConsumerStatsSnapshot> snapshotAndReset() {
        long now = System.currentTimeMillis();
        List<ConsumerStatsSnapshot> snapshots = new ArrayList<>();

        // Swap buckets atomically
        for (String consumer : buckets.keySet()) {
            try {
                StatsBucket old = buckets.replace(consumer, new StatsBucket());
                if (old == null) continue;

                long eventsProcessed = old.eventsProcessed.sum();
                long totalLatency = old.totalLatencyMillis.sum();

                double avg = eventsProcessed > 0
                        ? ((double) totalLatency / eventsProcessed)
                        : 0.0;

                snapshots.add(ConsumerStatsSnapshot.builder()
                        .consumerName(consumer)
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
                log.warn("Failed to snapshot stats for {}", consumer, e);
            }
        }

        return snapshots;
    }
}

