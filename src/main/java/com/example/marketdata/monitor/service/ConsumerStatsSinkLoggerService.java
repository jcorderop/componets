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
        log.info("Publishing stats: {}", snapshots.toString());
    }
}
