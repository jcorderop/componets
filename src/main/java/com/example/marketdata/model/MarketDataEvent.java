package com.example.marketdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MarketDataEvent extends IJsonDto {
    @JsonIgnore
    String getCacheId();
}
