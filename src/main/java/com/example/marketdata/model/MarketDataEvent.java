package com.example.marketdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a single unit of market data that can be routed through consumer pipelines
 * and ultimately cached or published.
 */
public interface MarketDataEvent {
    @JsonIgnore
    String getCacheId();
}
