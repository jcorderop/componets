package com.example.marketdata.service;

import com.example.marketdata.consumer.AbstractMarketDataConsumer;
import com.example.marketdata.model.MarketDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates incoming market data events and dispatches them to all configured consumers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataHandlerService {

    private final List<AbstractMarketDataConsumer> consumers;

    public void onEvent(final MarketDataEvent event) {
        log.info("Received event {}", event);
        for (AbstractMarketDataConsumer consumer : consumers) {
            consumer.enqueue(event);
        }
    }
}
