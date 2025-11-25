package com.example.marketdata.adapter.zmq;

import com.example.marketdata.adapter.BaseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Placeholder adapter illustrating how a ZeroMQ publisher would integrate with the
 * shared processor pipeline while honoring the {@link BaseAdapter} contract.
 */
@Component
@ConditionalOnProperty(prefix = "marketdata.adapters.zmq", name = "enabled", havingValue = "true")
public class ZmqAdapter<T> implements BaseAdapter<T> {

    private static final Logger log = LoggerFactory.getLogger(ZmqAdapter.class);
    @Override
    public void send(Map<String, T> entries) {
        log.info("Sending {} entries to ZeroMQ placeholder adapter", entries.size());
    }
}
