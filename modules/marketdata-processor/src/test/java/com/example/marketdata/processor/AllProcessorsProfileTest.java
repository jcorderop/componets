package com.example.marketdata.processor;

import com.example.marketdata.service.ProcessorsHandlerService;
import com.hazelcast.core.Hazelcast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms that enabling the "all-processors" profile activates every conditional processor
 * and wires the handler with the complete list.
 */
@SpringBootTest()
@ActiveProfiles("all-processors")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AllProcessorsProfileTest {

    @Autowired
    private List<AbstractMarketDataProcessor> processors;

    @Autowired
    private ProcessorsHandlerService processorsHandlerService;

    @Test
    void allConditionalProcessorsLoadUnderDedicatedProfile() {
        // given

        // when
        // then
        assertNotNull(processorsHandlerService, "ProcessorsHandlerService should be present");
        assertEquals(6, processors.size(), "All six conditional processors should load");

        assertTrue(processors.stream().anyMatch(HazelcastMarketDataBufferProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(KafkaMarketDataProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(PostgresMarketDataProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(ZMQMarketDataProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(OracleMarketDataProcessor.class::isInstance));
        assertTrue(processors.stream().anyMatch(LoggingMarketDataProcessor.class::isInstance));
    }

    @AfterAll
    static void shutdownHazelcast() {
        Hazelcast.shutdownAll();
    }

    @SpringBootConfiguration
    @ComponentScan("com.example.marketdata")
    static class TestConfig {
    }
}
