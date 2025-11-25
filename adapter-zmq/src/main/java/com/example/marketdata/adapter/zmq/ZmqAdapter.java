package com.example.marketdata.adapter.zmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Placeholder adapter illustrating how a ZeroMQ publisher would integrate with the
 * shared processor pipeline while honoring the {@link BaseAdapter} contract.
 */
@Slf4j
@Component
public class ZmqAdapter<T> implements IZmqAdapter<T> {
    @Override
    public void send(Map<String, T> entries) {
        log.info("Sending {} entries to ZeroMQ placeholder adapter", entries.size());
    }
}
