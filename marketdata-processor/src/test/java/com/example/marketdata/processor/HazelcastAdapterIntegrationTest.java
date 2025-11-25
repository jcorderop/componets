package com.example.marketdata.processor;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.adapter.hazelcast.HazelcastCacheAdapter;
import com.example.marketdata.adapter.hazelcast.config.HazelcastConfiguration;
import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistryImpl;
import com.example.marketdata.service.ProcessorsHandlerService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test that wires the Hazelcast adapter into the adapter-backed processor and
 * verifies that batches are delivered to a Hazelcast map.
 */
@SpringBootTest(
        classes = HazelcastAdapterIntegrationTest.TestConfig.class,
        properties = {
                "marketdata.adapters.hazelcast.enabled=true",
                "marketdata.hazelcast.cache-name=integration-cache",
                "marketdata.processors.adapter.enabled=true",
                "marketdata.default.batch-size=1",
                "marketdata.default.queue-capacity=10",
                "marketdata.default.poll-timeout-millis=25"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HazelcastAdapterIntegrationTest {

    private static final String CACHE_NAME = "integration-cache";

    @Autowired
    private ProcessorsHandlerService processorsHandlerService;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private BaseAdapter<MarketDataMessage> baseAdapter;

    @Test
    void adapterBackedProcessorWritesToHazelcast() throws Exception {
        MarketDataMessage event = new MarketDataMessage(
                "feed-A",
                "EURUSD",
                1.1234,
                1000,
                Instant.now()
        );

        processorsHandlerService.onEvent(event);

        IMap<String, String> cache = hazelcastInstance.getMap(CACHE_NAME);
        awaitCachePopulation(cache, event.getCacheId());

        String storedJson = cache.get(event.getCacheId());
        assertTrue(storedJson.contains("\"symbol\":\"EURUSD\""),
                "Hazelcast map should contain serialized market data entry");
        assertTrue(baseAdapter instanceof HazelcastCacheAdapter,
                "BaseAdapter bean should be provided by Hazelcast module");
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
            AdapterBackedMarketDataProcessor.class,
            ProcessorsHandlerService.class,
            MarketDataProcessorProperties.class,
            ProcessorStatsRegistryImpl.class,
            HazelcastConfiguration.class,
            HazelcastCacheAdapter.class
    })
    static class TestConfig {
    }
}
