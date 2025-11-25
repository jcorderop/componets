package com.example.marketdata.adapter.hazelcast;

import com.example.marketdata.adapter.BaseAdapter;
import com.example.marketdata.model.MarketDataEvent;

import java.util.List;

public interface IHazelcastBufferCacheAdapter<T> extends BaseAdapter<T> {

    void bufferMarketData(List<MarketDataEvent> batch);
}
