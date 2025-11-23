package com.example.marketdata.consumer;

import com.example.marketdata.exception.ConsumerRetryableException;
import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataConsumerBatchProcessor;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public abstract class AbstractMarketDataConsumer
        implements MarketDataConsumerBatchProcessor, SmartLifecycle {

    private final BlockingQueue<MarketDataEvent> queue;
    private final int batchSize;
    private final long pollTimeoutMillis;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("md-consumer-" + getConsumerName());
                t.setDaemon(true);
                return t;
            });

    private volatile boolean running = false;

    protected AbstractMarketDataConsumer(final MarketDataConsumerProperties props) {
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        this.batchSize = props.getBatchSize();
        this.pollTimeoutMillis = props.getPollTimeoutMillis();
    }

    public void enqueue(final MarketDataEvent message) {
        boolean offered = queue.offer(message);
        if (!offered) {
            log.warn("Queue full for consumer {} - dropping message: {}", getConsumerName(), message);
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::runLoop);
        log.info("Started MarketDataConsumer {}", getConsumerName());
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
        log.info("Stopped MarketDataConsumer {}", getConsumerName());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // If you care about startup order, adjust.
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void runLoop() {
        final List<MarketDataEvent> batch = new ArrayList<>(batchSize);

        try {
            while (running && !Thread.currentThread().isInterrupted()) {

                MarketDataEvent first = queue.poll(pollTimeoutMillis, TimeUnit.MILLISECONDS);

                if (first == null) {
                    continue;
                }

                batch.clear();
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);

                boolean processed = false;
                while (!processed && running && !Thread.currentThread().isInterrupted()) {
                    processed = executeProcessor(batch);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Consumer {} interrupted", getConsumerName());
        }
    }

    private boolean executeProcessor(List<MarketDataEvent> batch) {
        boolean processed = false;
        try {
            processBatch(batch);
            processed = true;
        } catch (ConsumerRetryableException e) {
            log.warn("Retryable error in consumer {}: {}. Will retry batch.", getConsumerName(), e.getMessage(), e);
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.info("Consumer {} interrupted during retry sleep", getConsumerName());
            }
        } catch (Exception e) {
            // Non-retryable: log and drop this batch, continue with next
            log.error("Non-retryable error in consumer {}: {}. Dropping batch.", getConsumerName(), e.getMessage(), e);
            processed = true;
        }
        return processed;
    }

}
