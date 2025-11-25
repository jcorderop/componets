package com.example.marketdata.adapter.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lightweight placeholder adapter that would publish market data to Kafka when enabled.
 * It implements the base adapter contract so the processor module can remain vendor-neutral
 * and simply depend on this bean when the corresponding profile/property is active.
 */
@Slf4j
@Component
public class KafkaProducerAdapter<T> implements IKafkaAdapter<T> {

    @Override
    public void send(Map<String, T> entries) {
        log.info("Sending {} entries to Kafka placeholder adapter", entries.size());
    }
}
