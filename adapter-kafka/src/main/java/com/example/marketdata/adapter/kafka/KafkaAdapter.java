package com.example.marketdata.adapter.kafka;

import com.example.marketdata.adapter.BaseAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lightweight placeholder adapter that would publish market data to Kafka when enabled.
 * It implements {@link BaseAdapter} so that the processor module can remain vendor-neutral
 * and simply depend on this bean when the corresponding profile/property is active.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "marketdata.adapters.kafka", name = "enabled", havingValue = "true")
public class KafkaAdapter<T> implements BaseAdapter<T> {

    @Override
    public void send(Map<String, T> entries) {
        log.info("Sending {} entries to Kafka placeholder adapter", entries.size());
    }
}
