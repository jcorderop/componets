package com.example.marketdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MarketDataEvent {
    @JsonIgnore
    String getCacheId();
}
