package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.exception.ProcessorRetryableException;
import com.example.marketdata.model.MarketDataProcessorBatchProcessor;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Base class for market data processors that manages queueing, lifecycle hooks, batch
 * processing, and retry/backoff behavior while delegating actual batch handling to
 * subclasses.
 * <p>
 * Reads {@code marketdata.default.*} properties via {@link com.example.marketdata.config.MarketDataProcessorProperties}
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
public abstract class AbstractMarketDataProcessor
        implements MarketDataProcessorBatchProcessor, SmartLifecycle {

    private final MarketDataProcessorProperties props;
    private final BlockingQueue<MarketDataEvent> queue;
    private final ExecutorService processorExecutor;
    private final ProcessorStatsRegistry processorStatsRegistry;

    private volatile boolean running = false;


    protected AbstractMarketDataProcessor(final MarketDataProcessorProperties props,
                                         final ProcessorStatsRegistry processorStatsRegistry) {
        this.props = props;
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        this.processorExecutor = Executors.newSingleThreadExecutor(r ->
                new Thread(r, getProcessorName() + "-processor-thread"));
        this.processorStatsRegistry = processorStatsRegistry;
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
        log.info("Starting market data processor {}", getProcessorName());
        processorExecutor.submit(this::runLoop);
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        log.info("Stopping market data processor {}", getProcessorName());
        running = false;
        processorExecutor.shutdownNow();
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

    @PreDestroy
    public void destroy() {
        stop();
        try {
            processorExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------------
    // Public API for producers (e.g. ProcessorsHandlerService)
    // ------------------------------------------------------------------------

    public void enqueue(final MarketDataEvent event) {
        processorStatsRegistry.recordEnqueue(getProcessorName());
        if (event == null) {
            log.warn("Ignoring null event for processor {}", getProcessorName());
            processorStatsRegistry.recordDrop(getProcessorName());
            return;
        }

        if (!running) {
            log.warn("Processor {} is not running; dropping event {}", getProcessorName(), event);
            processorStatsRegistry.recordDrop(getProcessorName());
            return;
        }

        boolean offered = queue.offer(event);
        if (!offered) {
            log.error("Queue is full for processor {} (capacity={}); dropping event {}",
                    getProcessorName(), props.getQueueCapacity(), event);
            processorStatsRegistry.recordDrop(getProcessorName());
        }
    }

    // ------------------------------------------------------------------------
    // Main processor loop
    // ------------------------------------------------------------------------

    private void runLoop() {
        final int batchSize = props.getBatchSize();
        final long pollTimeoutMillis = props.getPollTimeoutMillis();

        final List<MarketDataEvent> batch = new ArrayList<>(batchSize);

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                processorStatsRegistry.recordQueueSize(getProcessorName(), queue.size());

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
            log.info("Processor {} interrupted", getProcessorName());
        } finally {
            log.info("Exiting processor loop for {}", getProcessorName());
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
                processorStatsRegistry.recordBatchProcessed(
                        getProcessorName(),
                        batch.size(),
                        elapsedMillis
                );

            } catch (ProcessorRetryableException e) {
                log.warn("Retryable error in processor {}: {}. Will retry batch after {} ms.",
                        getProcessorName(), e.getMessage(), backoff, e);

                // Sleep with current backoff
                if (backoff > 0) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("Processor {} interrupted during retry sleep", getProcessorName());
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
                log.error("Non-retryable error in processor {}: {}. Dropping batch.",
                        getProcessorName(), e.getMessage(), e);
                processorStatsRegistry.recordDrops(getProcessorName(), batch.size());
                processed = true;
            }
        }
    }

    // ------------------------------------------------------------------------
    // To be implemented by concrete processors
    // ------------------------------------------------------------------------

    public abstract String getProcessorName();

    @Override
    public abstract void processBatch(List<MarketDataEvent> batch);
}
