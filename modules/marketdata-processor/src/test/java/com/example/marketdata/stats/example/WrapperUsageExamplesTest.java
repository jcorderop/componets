package com.example.marketdata.stats.example;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import com.example.marketdata.stats.wrapper.IWrapperStats;
import com.example.marketdata.stats.wrapper.WrapperConsumedKafkaStats;
import com.example.marketdata.stats.wrapper.WrapperDispatchedZmqStats;
import com.example.marketdata.stats.wrapper.WrapperPipelineStats;
import com.example.marketdata.stats.wrapper.WrapperStoragePostgresStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapperUsageExamplesTest {

    @Test
    void standaloneWrapperUsageExample() {
        ServiceStatsCollector stats = new ServiceStatsCollector("example");

        WrapperConsumedKafkaStats consumedKafka = new WrapperConsumedKafkaStats(stats);
        WrapperPipelineStats pipeline = new WrapperPipelineStats(stats);
        WrapperDispatchedZmqStats dispatchedZmq = new WrapperDispatchedZmqStats(stats);
        WrapperStoragePostgresStats storagePostgres = new WrapperStoragePostgresStats(stats);

        consumedKafka.addConsumed(1);
        pipeline.addReceived(1);
        pipeline.recordLatency(18);
        pipeline.addForwarded(1);
        dispatchedZmq.addDispatched(1);
        dispatchedZmq.recordLatency(12);
        dispatchedZmq.setQueueSizeMax(4);
        storagePostgres.addStored(1);
        storagePostgres.recordLatency(25);

        StatsSnapshot snapshot = stats.snapshotAndReset();

        assertEquals(1L, snapshot.counters().get(MetricName.CONSUMED_KAFKA_EVENTS));
        assertEquals(1L, snapshot.counters().get(MetricName.PIPELINE_RECEIVED_EVENTS));
        assertEquals(1L, snapshot.counters().get(MetricName.PIPELINE_FORWARDED_EVENTS));
        assertEquals(1L, snapshot.counters().get(MetricName.DISPATCHED_EVENTS));
        assertEquals(1L, snapshot.counters().get(MetricName.STORAGE_POSTGRES_EVENTS));
        assertEquals(4L, snapshot.gauges().get(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE));
    }

    @Test
    void wrappersShareCommonIWrapperStatsTypeForSpringStyleExchange() {
        ServiceStatsCollector stats = new ServiceStatsCollector("example");

        List<IWrapperStats> wrappers = List.of(
                new WrapperConsumedKafkaStats(stats),
                new WrapperPipelineStats(stats),
                new WrapperDispatchedZmqStats(stats),
                new WrapperStoragePostgresStats(stats)
        );

        assertEquals(4, wrappers.size());
        assertTrue(wrappers.stream().allMatch(IWrapperStats.class::isInstance));
    }
}
