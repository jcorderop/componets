package com.example.marketdata.processor;

import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.exception.ProcessorRetryableException;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractMarketDataProcessorTest {

    private final TestProcessorStatsRegistry statsRegistry = new TestProcessorStatsRegistry();

    @AfterEach
    void tearDown() {
        statsRegistry.reset();
    }

    @Test
    void enqueueDropsNullAndEventsWhenNotRunning() {
        // given
        MarketDataProcessorProperties props = baseProps();
        TestProcessor processor = new TestProcessor(props, statsRegistry, 0);

        // when
        processor.enqueue(null);
        processor.enqueue(sampleEvent());

        // then
        assertThat(statsRegistry.enqueues).isEqualTo(2);
        assertThat(statsRegistry.drops).isEqualTo(2);
    }

    @Test
    void enqueueDropsWhenQueueIsFull() throws Exception {
        // given
        MarketDataProcessorProperties props = baseProps();
        props.setQueueCapacity(1);
        TestProcessor processor = new TestProcessor(props, statsRegistry, 0);

        BlockingQueue<MarketDataEvent> queue = queueFor(processor);
        queue.add(sampleEvent());

        setRunning(processor, true);

        // when
        processor.enqueue(sampleEvent());

        // then
        assertThat(statsRegistry.enqueues).isEqualTo(1);
        assertThat(statsRegistry.drops).isEqualTo(1);
    }

    @Test
    void retriesAreTrackedUntilBatchSucceeds() throws InterruptedException {
        // given
        MarketDataProcessorProperties props = baseProps();
        props.setInitialRetryBackoffMillis(1);
        props.setMaxRetryBackoffMillis(2);
        props.setRetryBackoffMultiplier(1.5);

        CountDownLatch processed = new CountDownLatch(1);
        TestProcessor processor = new TestProcessor(props, statsRegistry, 2, processed);

        // when
        processor.start();
        try {
            processor.enqueue(sampleEvent());

            // then
            assertThat(processed.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(statsRegistry.batchProcessed).isEqualTo(1);
            assertThat(statsRegistry.drops).isZero();
        } finally {
            processor.stop();
        }
    }

    private MarketDataProcessorProperties baseProps() {
        MarketDataProcessorProperties props = new MarketDataProcessorProperties();
        props.setQueueCapacity(10);
        props.setBatchSize(1);
        props.setPollTimeoutMillis(1);
        return props;
    }

    private MarketDataEvent sampleEvent() {
        return new TestEvent();
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<MarketDataEvent> queueFor(AbstractMarketDataProcessor processor) throws Exception {
        Field queueField = AbstractMarketDataProcessor.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        return (BlockingQueue<MarketDataEvent>) queueField.get(processor);
    }

    private void setRunning(AbstractMarketDataProcessor processor, boolean running) throws Exception {
        Field runningField = AbstractMarketDataProcessor.class.getDeclaredField("running");
        runningField.setAccessible(true);
        runningField.set(processor, running);
    }

    private static class TestEvent implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return "XYZ";
        }
    }

    private static class TestProcessor extends AbstractMarketDataProcessor {

        private final AtomicInteger attempt = new AtomicInteger();
        private final int failCount;
        private final CountDownLatch latch;

        TestProcessor(MarketDataProcessorProperties props,
                     ProcessorStatsRegistry processorStatsRegistry,
                     int failCount) {
            this(props, processorStatsRegistry, failCount, new CountDownLatch(0));
        }

        TestProcessor(MarketDataProcessorProperties props,
                     ProcessorStatsRegistry processorStatsRegistry,
                     int failCount,
                     CountDownLatch latch) {
            super(props, processorStatsRegistry);
            this.failCount = failCount;
            this.latch = latch;
        }

        @Override
        public String getProcessorName() {
            return "testProcessor";
        }

        @Override
        public void processBatch(List<MarketDataEvent> batch) {
            int currentAttempt = attempt.incrementAndGet();
            if (currentAttempt <= failCount) {
                throw new ProcessorRetryableException("retry " + currentAttempt);
            }
            latch.countDown();
        }
    }

    private static class TestProcessorStatsRegistry implements ProcessorStatsRegistry {

        private int enqueues;
        private int drops;
        private int batchProcessed;

        @Override
        public void recordEnqueue(String processor) {
            enqueues++;
        }

        @Override
        public void recordDrops(String processor, int dropCount) {
            drops += dropCount;
        }

        @Override
        public void recordBatchProcessed(String processor, int batchSize, long durationMillis) {
            batchProcessed++;
        }

        @Override
        public void recordQueueSize(String processor, int queueSize) {
            // not required for these tests
        }

        @Override
        public List<ProcessorStatsSnapshot> snapshotAndReset() {
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
        // given
        MarketDataProcessorProperties props = baseProps();
        CountDownLatch latch = new CountDownLatch(1);

        // Processor that always throws a RuntimeException
        AbstractMarketDataProcessor processor = new AbstractMarketDataProcessor(props, statsRegistry) {
            @Override
            public String getProcessorName() {
                return "nonRetryableProcessor";
            }

            @Override
            public void processBatch(List<MarketDataEvent> batch) {
                throw new RuntimeException("non-retryable failure");
            }
        };

        // when
        processor.start();
        try {
            processor.enqueue(sampleEvent());
            // Give the processor loop some time
            TimeUnit.MILLISECONDS.sleep(100);

            // then
            assertThat(statsRegistry.drops).isEqualTo(1);
            assertThat(statsRegistry.batchProcessed).isZero();
        } finally {
            processor.stop();
        }
    }
}
