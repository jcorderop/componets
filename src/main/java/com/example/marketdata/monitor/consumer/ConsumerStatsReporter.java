package com.example.marketdata.monitor.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsumerStatsReporter {

    private final ConsumerStatsRegistry consumerStatsRegistry;
    private final List<ConsumerStatsSink> sinks;

    @Scheduled(fixedRateString = "${marketdata.stats.interval-ms:60000}")
    public void publishStats() {
        List<ConsumerStatsSnapshot> snapshots = consumerStatsRegistry.snapshotAndReset();
        for (ConsumerStatsSink sink : sinks) {
            sink.publish(snapshots);
        }
    }
}

