package com.example.marketdata.stats.wrapper;

import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrapperStatsTest {

    @Test
    void consumedWrappersPublishToTheirDedicatedCounters() {
        ServiceStatsCollector collector = new ServiceStatsCollector("wrapper-test");

        new WrapperConsumedKafkaStats(collector).addConsumed(1);
        new WrapperConsumedFixStats(collector).addConsumed(2);
        new WrapperConsumedRfaStats(collector).addConsumed(3);
        new WrapperConsumedBpipeStats(collector).addConsumed(4);

        StatsSnapshot snapshot = collector.snapshotAndReset();

        assertEquals(1L, snapshot.counters().get(MetricName.CONSUMED_KAFKA_EVENTS));
        assertEquals(2L, snapshot.counters().get(MetricName.CONSUMED_FIX_EVENTS));
        assertEquals(3L, snapshot.counters().get(MetricName.CONSUMED_RFA_EVENTS));
        assertEquals(4L, snapshot.counters().get(MetricName.CONSUMED_BPIPE_EVENTS));
    }

    @Test
    void pipelineWrapperPublishesReceivedLatencyAndForwardedMetrics() {
        ServiceStatsCollector collector = new ServiceStatsCollector("wrapper-test");
        WrapperPipelineStats wrapper = new WrapperPipelineStats(collector);

        wrapper.addReceived(10);
        wrapper.recordLatency(15);
        wrapper.recordLatency(45);
        wrapper.addForwarded(8);

        StatsSnapshot snapshot = collector.snapshotAndReset();

        assertEquals(10L, snapshot.counters().get(MetricName.PIPELINE_RECEIVED_EVENTS));
        assertEquals(8L, snapshot.counters().get(MetricName.PIPELINE_FORWARDED_EVENTS));
        assertEquals(30.0, snapshot.latencies().get(MetricName.PIPELINE_LATENCY).avg(), 1e-9);
        assertEquals(45.0, snapshot.latencies().get(MetricName.PIPELINE_LATENCY).max(), 1e-9);
    }

    @Test
    void dispatchedWrappersPublishCounterLatencyDroppedAndQueueMetrics() {
        ServiceStatsCollector collector = new ServiceStatsCollector("wrapper-test");

        WrapperDispatchedZmqStats zmq = new WrapperDispatchedZmqStats(collector);
        WrapperDispatchedKafkaStats kafka = new WrapperDispatchedKafkaStats(collector);
        WrapperDispatchedHazelcastStats hazelcast = new WrapperDispatchedHazelcastStats(collector);

        zmq.addDispatched(1);
        zmq.recordLatency(20);
        zmq.addDropped(1);
        zmq.setQueueSizeMax(5);

        kafka.addDispatched(2);
        kafka.recordLatency(25);
        kafka.addDropped(2);
        kafka.setQueueSizeMax(6);

        hazelcast.addDispatched(3);
        hazelcast.recordLatency(30);
        hazelcast.addDropped(3);
        hazelcast.setQueueSizeMax(7);

        StatsSnapshot snapshot = collector.snapshotAndReset();

        assertEquals(1L, snapshot.counters().get(MetricName.DISPATCHED_EVENTS));
        assertEquals(2L, snapshot.counters().get(MetricName.DISPATCHED_KAFKA_EVENTS));
        assertEquals(3L, snapshot.counters().get(MetricName.DISPATCHED_HAZELCAST_EVENTS));

        assertEquals(1L, snapshot.counters().get(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED));
        assertEquals(2L, snapshot.counters().get(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED));
        assertEquals(3L, snapshot.counters().get(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED));

        assertEquals(20.0, snapshot.latencies().get(MetricName.DISPATCHED_ZMQ_LATENCY_MS).max(), 1e-9);
        assertEquals(25.0, snapshot.latencies().get(MetricName.DISPATCHED_KAFKA_LATENCY_MS).max(), 1e-9);
        assertEquals(30.0, snapshot.latencies().get(MetricName.DISPATCHED_HAZELCAST_LATENCY_MS).max(), 1e-9);

        assertEquals(5L, snapshot.gauges().get(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE));
        assertEquals(6L, snapshot.gauges().get(MetricName.DISPATCHED_KAFKA_QUEUE_SIZE));
        assertEquals(7L, snapshot.gauges().get(MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE));
    }


    @Test
    void dispatchedQueueWrapperRespectsGaugePeakAndResetsWithSnapshot() {
        ServiceStatsCollector collector = new ServiceStatsCollector("wrapper-test");
        WrapperDispatchedZmqStats zmq = new WrapperDispatchedZmqStats(collector);

        zmq.setQueueSizeMax(10);
        zmq.setQueueSizeMax(3); // lower values are ignored by setMax

        StatsSnapshot first = collector.snapshotAndReset();
        assertEquals(10L, first.gauges().get(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE));

        StatsSnapshot second = collector.snapshotAndReset();
        assertEquals(0L, second.gauges().get(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE));
    }

    @Test
    void storageWrappersPublishStoredLatencyAndDroppedMetrics() {
        ServiceStatsCollector collector = new ServiceStatsCollector("wrapper-test");

        WrapperStoragePostgresStats postgres = new WrapperStoragePostgresStats(collector);
        WrapperStorageOracleStats oracle = new WrapperStorageOracleStats(collector);

        postgres.addStored(10);
        postgres.recordLatency(100);
        postgres.addDropped(1);

        oracle.addStored(20);
        oracle.recordLatency(200);
        oracle.addDropped(2);

        StatsSnapshot snapshot = collector.snapshotAndReset();

        assertEquals(10L, snapshot.counters().get(MetricName.STORAGE_POSTGRES_EVENTS));
        assertEquals(20L, snapshot.counters().get(MetricName.STORAGE_ORACLE_EVENTS));

        assertEquals(1L, snapshot.counters().get(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED));
        assertEquals(2L, snapshot.counters().get(MetricName.STORAGE_ORACLE_EVENTS_DROPPED));

        assertEquals(100.0, snapshot.latencies().get(MetricName.STORAGE_POSTGRES_LATENCY_MS).max(), 1e-9);
        assertEquals(200.0, snapshot.latencies().get(MetricName.STORAGE_ORACLE_LATENCY_MS).max(), 1e-9);
    }
}
