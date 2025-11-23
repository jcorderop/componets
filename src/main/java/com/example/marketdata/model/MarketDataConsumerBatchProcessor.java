package com.example.marketdata.model;

import java.util.List;

/**
 * Extension of {@link MarketDataConsumer} that can process batches of events at once.
 */
public interface MarketDataConsumerBatchProcessor extends MarketDataConsumer{
    void processBatch(final List<MarketDataEvent> events);
}

