package com.example.marketdata.consumer;

import com.example.marketdata.service.ConsumersHandlerService;
import com.hazelcast.core.Hazelcast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms that enabling the "all-consumers" profile activates every conditional consumer
 * and wires the handler with the complete list.
 */
@SpringBootTest
@ActiveProfiles("all-consumers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AllConsumersProfileTest {

    @Autowired
    private List<AbstractMarketDataConsumer> consumers;

    @Autowired
    private ConsumersHandlerService consumersHandlerService;

    @Test
    void allConditionalConsumersLoadUnderDedicatedProfile() {
        // given

        // when
        // then
        assertNotNull(consumersHandlerService, "ConsumersHandlerService should be present");
        assertEquals(6, consumers.size(), "All six conditional consumers should load");

        assertTrue(consumers.stream().anyMatch(HazelcastMarketDataConsumer.class::isInstance));
        assertTrue(consumers.stream().anyMatch(KafkaMarketDataConsumer.class::isInstance));
        assertTrue(consumers.stream().anyMatch(PostgresMarketDataConsumer.class::isInstance));
        assertTrue(consumers.stream().anyMatch(ZMQMarketDataConsumer.class::isInstance));
        assertTrue(consumers.stream().anyMatch(OracleMarketDataConsumer.class::isInstance));
        assertTrue(consumers.stream().anyMatch(LoggingMarketDataConsumer.class::isInstance));
    }

    @AfterAll
    static void shutdownHazelcast() {
        Hazelcast.shutdownAll();
    }
}
