package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.exception.ConsumerRetryableException;
import com.example.marketdata.model.MarketDataConsumerBatchProcessor;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Base class for market data consumers that manages queueing, lifecycle hooks, batch
 * processing, and retry/backoff behavior while delegating actual batch handling to
 * subclasses.
 * <p>
 * Reads {@code marketdata.default.*} properties via {@link com.example.marketdata.config.MarketDataConsumerProperties}
 * to size queues and tune batching/backoff:
 * <ul>
 *     <li>{@code queue-capacity} – queue depth used for {@link java.util.concurrent.ArrayBlockingQueue}.</li>
 *     <li>{@code batch-size} – maximum items pulled from the queue before processing.</li>
 *     <li>{@code poll-timeout-millis} – wait time before flushing a partial batch.</li>
 *     <li>{@code initial-retry-backoff-millis}, {@code max-retry-backoff-millis},
 *     {@code retry-backoff-multiplier} – govern exponential retry delays when batch processing fails.</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractMarketDataConsumer
        implements MarketDataConsumerBatchProcessor, SmartLifecycle {

    private final MarketDataConsumerProperties props;
    private final BlockingQueue<MarketDataEvent> queue;
    private final ExecutorService consumerExecutor;
    private final ConsumerStatsRegistry consumerStatsRegistry;

    private volatile boolean running = false;


    protected AbstractMarketDataConsumer(final MarketDataConsumerProperties props,
                                         final ConsumerStatsRegistry consumerStatsRegistry) {
        this.props = props;
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        this.consumerExecutor = Executors.newSingleThreadExecutor(r ->
                new Thread(r, getConsumerName() + "-consumer-thread"));
        this.consumerStatsRegistry = consumerStatsRegistry;
    }

    // ------------------------------------------------------------------------
    // SmartLifecycle
    // ------------------------------------------------------------------------

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        log.info("Starting market data consumer {}", getConsumerName());
        consumerExecutor.submit(this::runLoop);
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        log.info("Stopping market data consumer {}", getConsumerName());
        running = false;
        consumerExecutor.shutdownNow();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    // ------------------------------------------------------------------------
    // Public API for producers (e.g. ConsumersHandlerService)
    // ------------------------------------------------------------------------

    public void enqueue(final MarketDataEvent event) {
        consumerStatsRegistry.recordEnqueue(getConsumerName());
        if (event == null) {
            log.warn("Ignoring null event for consumer {}", getConsumerName());
            consumerStatsRegistry.recordDrop(getConsumerName());
            return;
        }

        if (!running) {
            log.warn("Consumer {} is not running; dropping event {}", getConsumerName(), event);
            consumerStatsRegistry.recordDrop(getConsumerName());
            return;
        }

        boolean offered = queue.offer(event);
        if (!offered) {
            log.error("Queue is full for consumer {} (capacity={}); dropping event {}",
                    getConsumerName(), props.getQueueCapacity(), event);
            consumerStatsRegistry.recordDrop(getConsumerName());
        }
    }

    // ------------------------------------------------------------------------
    // Main consumer loop
    // ------------------------------------------------------------------------

    private void runLoop() {
        final int batchSize = props.getBatchSize();
        final long pollTimeoutMillis = props.getPollTimeoutMillis();

        final List<MarketDataEvent> batch = new ArrayList<>(batchSize);

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                consumerStatsRegistry.recordQueueSize(getConsumerName(), queue.size());

                MarketDataEvent first = queue.poll(pollTimeoutMillis, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }

                batch.clear();
                batch.add(first);

                queue.drainTo(batch, batchSize - 1);

                executeProcessor(batch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Consumer {} interrupted", getConsumerName());
        } finally {
            log.info("Exiting consumer loop for {}", getConsumerName());
        }
    }

    private void executeProcessor(final List<MarketDataEvent> batch) {
        // Retry loop for this batch with progressive backoff
        boolean processed = false;
        long backoff = props.getInitialRetryBackoffMillis();
        final long maxBackoff = props.getMaxRetryBackoffMillis();
        final double multiplier = props.getRetryBackoffMultiplier();
        final long startNanos = System.nanoTime();

        while (!processed && running && !Thread.currentThread().isInterrupted()) {
            try {
                processBatch(batch);
                processed = true; // success -> exit retry loop

                long elapsedMillis =
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

                // stats: batch processed successfully
                consumerStatsRegistry.recordBatchProcessed(
                        getConsumerName(),
                        batch.size(),
                        elapsedMillis
                );

            } catch (ConsumerRetryableException e) {
                log.warn("Retryable error in consumer {}: {}. Will retry batch after {} ms.",
                        getConsumerName(), e.getMessage(), backoff, e);

                // Sleep with current backoff
                if (backoff > 0) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("Consumer {} interrupted during retry sleep", getConsumerName());
                        // allow outer loop to exit
                        break;
                    }
                }

                // Increase backoff for next retry
                if (multiplier > 1.0 && maxBackoff > 0) {
                    backoff = Math.min((long) (backoff * multiplier), maxBackoff);
                }

            } catch (Exception e) {
                // Non-retryable: log and drop this batch, continue with next
                log.error("Non-retryable error in consumer {}: {}. Dropping batch.",
                        getConsumerName(), e.getMessage(), e);
                consumerStatsRegistry.recordDrops(getConsumerName(), batch.size());
                processed = true;
            }
        }
    }

    // ------------------------------------------------------------------------
    // To be implemented by concrete consumers
    // ------------------------------------------------------------------------

    public abstract String getConsumerName();

    @Override
    public abstract void processBatch(List<MarketDataEvent> batch);
}
