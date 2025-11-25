package com.example.marketdata.adapter.kafka;

import com.example.marketdata.adapter.BaseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lightweight placeholder adapter that would publish market data to Kafka when enabled.
 * It implements {@link BaseAdapter} so that the processor module can remain vendor-neutral
 * and simply depend on this bean when the corresponding profile/property is active.
 */
@Component
@ConditionalOnProperty(prefix = "marketdata.adapters.kafka", name = "enabled", havingValue = "true")
public class KafkaAdapter<T> implements BaseAdapter<T> {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdapter.class);
    @Override
    public void send(Map<String, T> entries) {
        log.info("Sending {} entries to Kafka placeholder adapter", entries.size());
    }
}
