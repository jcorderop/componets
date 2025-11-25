package com.example.marketdata.processor;

import com.example.marketdata.adapter.hazelcast.IHazelcastBufferCacheAdapter;
import com.example.marketdata.config.MarketDataProcessorProperties;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.monitor.processor.ProcessorStatsRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HazelcastMarketDataBufferProcessorTest {

    @Test
    void processBatchDelegatesToCacheAdapter() {
        MarketDataProcessorProperties props = new MarketDataProcessorProperties();
        ProcessorStatsRegistry statsRegistry = mock(ProcessorStatsRegistry.class);

        @SuppressWarnings("unchecked")
        IHazelcastBufferCacheAdapter<MarketDataEvent> cacheAdapter = mock(IHazelcastBufferCacheAdapter.class);

        HazelcastMarketDataBufferProcessor<MarketDataEvent> processor =
                new HazelcastMarketDataBufferProcessor<>(props, statsRegistry, cacheAdapter);

        List<MarketDataEvent> batch = List.of(new TestEvent("cache-1"), new TestEvent("cache-2"));

        processor.processBatch(batch);

        verify(cacheAdapter).bufferMarketData(batch);
    }

    @Test
    void processorNameMatchesClassName() {
        MarketDataProcessorProperties props = new MarketDataProcessorProperties();
        ProcessorStatsRegistry statsRegistry = mock(ProcessorStatsRegistry.class);

        @SuppressWarnings("unchecked")
        IHazelcastBufferCacheAdapter<MarketDataEvent> cacheAdapter = mock(IHazelcastBufferCacheAdapter.class);

        HazelcastMarketDataBufferProcessor<MarketDataEvent> processor =
                new HazelcastMarketDataBufferProcessor<>(props, statsRegistry, cacheAdapter);

        assertThat(processor.getProcessorName()).isEqualTo(HazelcastMarketDataBufferProcessor.class.getName());
    }

    private record TestEvent(String cacheId) implements MarketDataEvent {
        @Override
        public String getCacheId() {
            return cacheId;
        }
    }
}
