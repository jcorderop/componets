package com.example.marketdata.monitor.consumer;

import java.util.List;

public interface ConsumerStatsSink {
    void publish(List<ConsumerStatsSnapshot> snapshots);
}