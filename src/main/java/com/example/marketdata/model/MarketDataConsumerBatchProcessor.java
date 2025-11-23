package com.example.marketdata.model;

import java.util.List;

public interface MarketDataConsumerBatchProcessor extends MarketDataConsumer{
    void processBatch(final List<MarketDataEvent> events);
}

