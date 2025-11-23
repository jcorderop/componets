package com.example.marketdata.consumer;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.exception.ConsumerRetryableException;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.consumer.ConsumerStatsRegistry;
import com.example.marketdata.monitor.consumer.ConsumerStatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractMarketDataConsumerTest {

    private final TestConsumerStatsRegistry statsRegistry = new TestConsumerStatsRegistry();

    @AfterEach
    void tearDown() {
        statsRegistry.reset();
    }

    @Test
    void enqueueDropsNullAndEventsWhenNotRunning() {
        MarketDataConsumerProperties props = baseProps();
        TestConsumer consumer = new TestConsumer(props, statsRegistry, 0);

        consumer.enqueue(null);
        consumer.enqueue(sampleEvent());

        assertThat(statsRegistry.enqueues).isEqualTo(2);
        assertThat(statsRegistry.drops).isEqualTo(2);
    }

    @Test
    void enqueueDropsWhenQueueIsFull() throws Exception {
        MarketDataConsumerProperties props = baseProps();
        props.setQueueCapacity(1);
        TestConsumer consumer = new TestConsumer(props, statsRegistry, 0);

        BlockingQueue<MarketDataEvent> queue = queueFor(consumer);
        queue.add(sampleEvent());

        setRunning(consumer, true);

        consumer.enqueue(sampleEvent());

        assertThat(statsRegistry.enqueues).isEqualTo(1);
        assertThat(statsRegistry.drops).isEqualTo(1);
    }

    @Test
    void retriesAreTrackedUntilBatchSucceeds() throws InterruptedException {
        MarketDataConsumerProperties props = baseProps();
        props.setInitialRetryBackoffMillis(1);
        props.setMaxRetryBackoffMillis(2);
        props.setRetryBackoffMultiplier(1.5);

        CountDownLatch processed = new CountDownLatch(1);
        TestConsumer consumer = new TestConsumer(props, statsRegistry, 2, processed);

        consumer.start();
        try {
            consumer.enqueue(sampleEvent());

            assertThat(processed.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(statsRegistry.batchProcessed).isEqualTo(1);
            assertThat(statsRegistry.drops).isZero();
        } finally {
            consumer.stop();
        }
    }

    private MarketDataConsumerProperties baseProps() {
        MarketDataConsumerProperties props = new MarketDataConsumerProperties();
        props.setQueueCapacity(10);
        props.setBatchSize(1);
        props.setPollTimeoutMillis(1);
        return props;
    }

    private MarketDataEvent sampleEvent() {
        return MarketDataMessage.builder()
                .source("demo")
                .symbol("XYZ")
                .price(1.0)
                .size(1)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<MarketDataEvent> queueFor(AbstractMarketDataConsumer consumer) throws Exception {
        Field queueField = AbstractMarketDataConsumer.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        return (BlockingQueue<MarketDataEvent>) queueField.get(consumer);
    }

    private void setRunning(AbstractMarketDataConsumer consumer, boolean running) throws Exception {
        Field runningField = AbstractMarketDataConsumer.class.getDeclaredField("running");
        runningField.setAccessible(true);
        runningField.set(consumer, running);
    }

    private static class TestConsumer extends AbstractMarketDataConsumer {

        private final AtomicInteger attempt = new AtomicInteger();
        private final int failCount;
        private final CountDownLatch latch;

        TestConsumer(MarketDataConsumerProperties props,
                     ConsumerStatsRegistry consumerStatsRegistry,
                     int failCount) {
            this(props, consumerStatsRegistry, failCount, new CountDownLatch(0));
        }

        TestConsumer(MarketDataConsumerProperties props,
                     ConsumerStatsRegistry consumerStatsRegistry,
                     int failCount,
                     CountDownLatch latch) {
            super(props, consumerStatsRegistry);
            this.failCount = failCount;
            this.latch = latch;
        }

        @Override
        public String getConsumerName() {
            return "testConsumer";
        }

        @Override
        public void processBatch(List<MarketDataEvent> batch) {
            int currentAttempt = attempt.incrementAndGet();
            if (currentAttempt <= failCount) {
                throw new ConsumerRetryableException("retry " + currentAttempt);
            }
            latch.countDown();
        }
    }

    private static class TestConsumerStatsRegistry implements ConsumerStatsRegistry {

        private int enqueues;
        private int drops;
        private int batchProcessed;

        @Override
        public void recordEnqueue(String consumer) {
            enqueues++;
        }

        @Override
        public void recordDrops(String consumer, int dropCount) {
            drops += dropCount;
        }

        @Override
        public void recordBatchProcessed(String consumer, int batchSize, long durationMillis) {
            batchProcessed++;
        }

        @Override
        public void recordQueueSize(String consumer, int queueSize) {
            // not required for these tests
        }

        @Override
        public List<ConsumerStatsSnapshot> snapshotAndReset() {
            return Collections.emptyList();
        }

        void reset() {
            enqueues = 0;
            drops = 0;
            batchProcessed = 0;
        }
    }

    @Test
    void nonRetryableErrorDropsBatch() throws InterruptedException {
        MarketDataConsumerProperties props = baseProps();
        CountDownLatch latch = new CountDownLatch(1);

        // Consumer that always throws a RuntimeException
        AbstractMarketDataConsumer consumer = new AbstractMarketDataConsumer(props, statsRegistry) {
            @Override
            public String getConsumerName() {
                return "nonRetryableConsumer";
            }

            @Override
            public void processBatch(List<MarketDataEvent> batch) {
                throw new RuntimeException("non-retryable failure");
            }
        };

        consumer.start();
        try {
            consumer.enqueue(sampleEvent());
            // Give the consumer loop some time
            TimeUnit.MILLISECONDS.sleep(100);

            assertThat(statsRegistry.drops).isEqualTo(1);
            assertThat(statsRegistry.batchProcessed).isZero();
        } finally {
            consumer.stop();
        }
    }
}
