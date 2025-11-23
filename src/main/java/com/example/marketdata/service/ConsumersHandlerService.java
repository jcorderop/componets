package com.example.marketdata.service;

import com.example.marketdata.consumer.AbstractMarketDataConsumer;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates incoming market data events and dispatches them to all configured consumers.
 */
@Slf4j
@Service
public class ConsumersHandlerService {

    private final List<AbstractMarketDataConsumer> consumers;

    public ConsumersHandlerService(List<AbstractMarketDataConsumer> consumers) {
        this.consumers = List.copyOf(consumers);
    }

    public void onEvent(final MarketDataEvent event) {
        log.debug("Received event {}", event);
        for (AbstractMarketDataConsumer consumer : consumers) {
            consumer.enqueue(event);
        }
    }
}
