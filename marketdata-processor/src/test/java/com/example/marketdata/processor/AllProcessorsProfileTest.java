package com.example.marketdata.processor;

import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.service.ProcessorsHandlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms that enabling the "all-processors" profile activates every conditional processor
 * and wires the handler with the complete list.
 */
@SpringBootTest(classes = AllProcessorsProfileTest.TestConfig.class)
@ActiveProfiles("all-processors")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AllProcessorsProfileTest {

    @Autowired
    private List<AbstractMarketDataProcessor> processors;

    @Autowired
    private ProcessorsHandlerService processorsHandlerService;

    @Test
    void adapterAndLoggingProcessorsLoadUnderProfile() {
        assertNotNull(processorsHandlerService, "ProcessorsHandlerService should be present");
        assertEquals(2, processors.size(), "Adapter-backed and logging processors should load");

        assertTrue(processors.stream().anyMatch(AdapterBackedMarketDataProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(LoggingMarketDataProcessor.class::isInstance));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        BaseAdapter<Map<String, String>> testAdapter() {
            return entries -> new HashMap<>(entries);
        }
    }
}
