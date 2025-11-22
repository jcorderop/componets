package com.example.demo;

import com.example.marketdata.model.MarketDataEvent;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataMessage implements MarketDataEvent {
    private String source;       // e.g. "Bloomberg", "Reuters"
    private String symbol;       // e.g. "EURUSD", "AAPL"
    private double price;
    private long size;
    private Instant timestamp;   // event time
}
