package com.example.demo;

import com.example.marketdata.model.MarketDataEvent;

import java.time.Instant;

/**
 * Simple market data payload used by the demo to drive processor pipelines and cache updates.
 */
public class MarketDataMessage implements MarketDataEvent {
    private String source;       // e.g. "Bloomberg", "Reuters"
    private String symbol;       // e.g. "EURUSD", "AAPL"
    private double price;
    private long size;
    private Instant timestamp;   // event time

    public MarketDataMessage() {
    }

    public MarketDataMessage(String source, String symbol, double price, long size, Instant timestamp) {
        this.source = source;
        this.symbol = symbol;
        this.price = price;
        this.size = size;
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getCacheId() {
        return symbol;
    }
}
