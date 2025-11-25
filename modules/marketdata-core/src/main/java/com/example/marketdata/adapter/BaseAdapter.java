package com.example.marketdata.adapter;


import java.util.Map;

/**
 * Generic contract for adapters capable of delivering market data payloads to external
 * destinations such as caches or message brokers.
 */
public interface BaseAdapter <T> {

    /**
     * Deliver the provided payloads to the adapter's destination.
     *
     * @param entries map of cache IDs to DTOs to store or publish
     */
    void send(Map<String, T> entries);
}
