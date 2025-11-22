package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataConsumerBatchDequeue;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public abstract class AbstractMarketDataConsumer
        implements MarketDataConsumerBatchDequeue, SmartLifecycle {

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
        List<MarketDataEvent> batch = new ArrayList<>(batchSize);

        try {
            while (running && !Thread.currentThread().isInterrupted()) {

                MarketDataEvent first = queue.poll(pollTimeoutMillis, TimeUnit.MILLISECONDS);

                if (first == null) {
                    // No data; simply continue
                    continue;
                }

                batch.clear();
                batch.add(first);

                // Drain up to batchSize - 1 without waiting too long
                queue.drainTo(batch, batchSize - 1);

                try {
                    dequeueBatch(batch);
                } catch (Exception e) {
                    log.error("Error processing batch in consumer {}: {}", getConsumerName(), e.getMessage(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Consumer {} interrupted", getConsumerName());
        }
    }
}
