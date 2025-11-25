package com.example.marketdata.service;

import com.example.marketdata.processor.AbstractMarketDataProcessor;
import com.example.marketdata.model.MarketDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates incoming market data events and dispatches them to all configured processors.
 */
@Service
public class ProcessorsHandlerService {

    private static final Logger log = LoggerFactory.getLogger(ProcessorsHandlerService.class);

    private final List<AbstractMarketDataProcessor> processors;

    public ProcessorsHandlerService(List<AbstractMarketDataProcessor> processors) {
        this.processors = List.copyOf(processors);
    }

    public void onEvent(final MarketDataEvent event) {
        log.debug("Received event {}", event);
        for (AbstractMarketDataProcessor processor : processors) {
            processor.enqueue(event);
        }
    }
}
