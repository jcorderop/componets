package com.example.marketdata.model;

import java.util.List;

/**
 * Extension of {@link MarketDataProcessor} that can process batches of events at once.
 */
public interface MarketDataProcessorBatchProcessor extends MarketDataProcessor{
    void processBatch(final List<MarketDataEvent> events);
}

