package com.example.marketdata.consumer;

import com.example.marketdata.config.MarketDataConsumerProperties;
import com.example.marketdata.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaMarketDataConsumer extends AbstractMarketDataConsumer {

    public KafkaMarketDataConsumer(final MarketDataConsumerProperties props) {
        super(props);
        log.info("Created Kafka consumer");
    }

    @Override
    public String getConsumerName() {
        return this.getClass().getName();
    }

    @Override
    public void processBatch(List<MarketDataEvent> batch) {
        try {
            log.info("Kafka consumer processing batch of size {}", batch.size());
            //} catch (IOException | TimeoutException e) {
            // transient problem → retry
            //    throw new ConsumerRetryableException("Temporary failure talking to service", e);
        } catch (Exception e) {
            throw e;
        }
    }
}
