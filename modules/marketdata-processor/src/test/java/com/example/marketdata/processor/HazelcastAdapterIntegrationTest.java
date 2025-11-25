package com.example.marketdata.processor;

import com.example.marketdata.adapter.hazelcast.HazelcastBufferCacheAdapter;
import com.example.marketdata.adapter.hazelcast.HazelcastBufferThrottle;
import com.example.marketdata.adapter.hazelcast.config.HazelcastConfiguration;
import com.example.marketdata.adapter.hazelcast.handler.MarketDataBufferHandler;
import com.example.marketdata.cache.MarketDataBuffer;
import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistryImpl;
import com.example.marketdata.service.ProcessorsHandlerService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test that wires the Hazelcast processor and verifies that batches are delivered
 * to a Hazelcast map.
 */

@EnableAutoConfiguration
@SpringBootTest(
        classes = HazelcastAdapterIntegrationTest.TestConfig.class,
        properties = {
                "marketdata.processors.hazelcast.enabled=true",
                "marketdata.default.batch-size=1",
                "marketdata.default.queue-capacity=10",
                "marketdata.default.poll-timeout-millis=25",
                "marketdata.hazelcast.cache-name=integration-cache"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HazelcastAdapterIntegrationTest {

    private record TestMarketDataMessage(String source,
                                         String symbol,
                                         double price,
                                         long size,
                                         Instant timestamp) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return symbol;
        }
    }

    private static final String CACHE_NAME = "integration-cache";

    @Autowired
    private ProcessorsHandlerService processorsHandlerService;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private HazelcastBufferThrottle<TestMarketDataMessage> hazelcastBufferThrottle;

    @Test
    void hazelcastProcessorWritesToHazelcast() throws Exception {
        TestMarketDataMessage event = new TestMarketDataMessage(
                "feed-A",
                "EURUSD",
                1.1234,
                1000,
                Instant.now()
        );

        processorsHandlerService.onEvent(event);
        Thread.sleep(100);
        hazelcastBufferThrottle.runThrottled();

        IMap<String, String> cache = hazelcastInstance.getMap(CACHE_NAME);
        awaitCachePopulation(cache, event.getCacheId());

        String storedJson = cache.get(event.getCacheId());
        assertTrue(storedJson.contains("\"symbol\":\"EURUSD\""),
                "Hazelcast map should contain serialized market data entry");
    }

    private void awaitCachePopulation(IMap<String, String> cache, String key) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            if (cache.containsKey(key)) {
                return;
            }
            Thread.sleep(25);
        }
        fail("Timed out waiting for Hazelcast cache to receive entry for key " + key);
    }

    @TestConfiguration
    @Import({
            HazelcastMarketDataBufferProcessor.class,
            ProcessorsHandlerService.class,
            MarketDataProcessorProperties.class,
            ProcessorStatsRegistryImpl.class,
            HazelcastConfiguration.class,
            HazelcastBufferCacheAdapter.class,
            MarketDataBuffer.class,
            MarketDataBufferHandler.class,
            HazelcastBufferThrottle.class
    })
    static class TestConfig {
    }
}
