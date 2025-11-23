
package com.example.marketdata.service;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.consumer.AbstractMarketDataConsumer;
import com.example.marketdata.model.MarketDataEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Verifies that the handler dispatches every inbound event to all registered consumers.
 */
class ConsumersHandlerServiceTest {

    @Test
    void onEventForwardsEventToAllConsumers() {
        AbstractMarketDataConsumer c1 = mock(AbstractMarketDataConsumer.class);
        AbstractMarketDataConsumer c2 = mock(AbstractMarketDataConsumer.class);

        // given
        ConsumersHandlerService service = new ConsumersHandlerService(List.of(c1, c2));

        MarketDataEvent event = MarketDataMessage.builder()
                .source("demo")
                .symbol("XYZ")
                .price(1.0)
                .size(1)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        // when
        service.onEvent(event);

        // then
        verify(c1).enqueue(event);
        verify(c2).enqueue(event);
    }
}
