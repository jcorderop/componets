package com.example.demo;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.consumer.AbstractMarketDataConsumer;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PostgresMarketDataConsumer extends AbstractMarketDataConsumer {

    public PostgresMarketDataConsumer(final MarketDataConsumerProperties props) {
        super(props);
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void dequeueBatch(List<MarketDataEvent> batch) {
        log.info("PostgreSQL consumer processing batch of size {}", batch.size());
    }
}
