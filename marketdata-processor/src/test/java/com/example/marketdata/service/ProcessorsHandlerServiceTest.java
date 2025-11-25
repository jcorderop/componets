
package com.example.marketdata.service;

import com.example.marketdata.processor.AbstractMarketDataProcessor;
import com.example.marketdata.model.MarketDataEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that the handler dispatches every inbound event to all registered processors.
 */
class ProcessorsHandlerServiceTest {

    @Test
    void onEventForwardsEventToAllProcessors() {
        AbstractMarketDataProcessor c1 = mock(AbstractMarketDataProcessor.class);
        AbstractMarketDataProcessor c2 = mock(AbstractMarketDataProcessor.class);

        // given
        ProcessorsHandlerService service = new ProcessorsHandlerService(List.of(c1, c2));

        MarketDataEvent event = () -> "XYZ";

        // when
        service.onEvent(event);

        // then
        verify(c1).enqueue(event);
        verify(c2).enqueue(event);
    }
}
