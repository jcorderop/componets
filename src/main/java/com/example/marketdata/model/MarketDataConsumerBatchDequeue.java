package com.example.marketdata.model;

import java.util.List;

public interface MarketDataConsumerBatchDequeue extends MarketDataConsumer{
    void dequeueBatch(final List<MarketDataEvent> events);
}

