package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.consumer.ConsumerStatsSink;
import com.example.marketdata.monitor.consumer.ConsumerStatsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ConsumerStatsSinkLoggerService implements ConsumerStatsSink {

    @Override
    public void publish(List<ConsumerStatsSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            log.debug("No consumer stats to publish for the current interval");
            return;
        }

        for (ConsumerStatsSnapshot snapshot : snapshots) {
            log.info(
                    "Consumer stats [{}]: window={}..{}, enqueued={}, processed={}, dropped={}, " +
                            "latency_ms[min={}, max={}, avg={}], queueSize={}",
                    snapshot.consumerName(),
                    snapshot.windowStartMillis(),
                    snapshot.windowEndMillis(),
                    snapshot.eventsEnqueued(),
                    snapshot.eventsProcessed(),
                    snapshot.eventsDropped(),
                    snapshot.minLatencyMillis(),
                    snapshot.maxLatencyMillis(),
                    snapshot.avgLatencyMillis(),
                    snapshot.queueSizeAtSnapshot()
            );
        }
    }
}
