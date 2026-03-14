package com.example.marketdata.stats.collector;

import java.util.Set;

public final class AllowedMetricNames {

    private AllowedMetricNames() {}

    public static final Set<String> ALL = Set.of(
            MetricName.CONSUMED_EVENTS,
            MetricName.CONSUMED_KAFKA_EVENTS,
            MetricName.CONSUMED_FIX_EVENTS,
            MetricName.CONSUMED_RFA_EVENTS,
            MetricName.CONSUMED_BPIPE_EVENTS,

            MetricName.PIPELINE_RECEIVED_EVENTS,
            MetricName.PIPELINE_LATENCY,
            MetricName.PIPELINE_FORWARDED_EVENTS,

            MetricName.DISPATCHED_EVENTS,
            MetricName.DISPATCHED_ZMQ_LATENCY_MS,
            MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED,
            MetricName.DISPATCHED_ZMQ_QUEUE_SIZE,

            MetricName.DISPATCHED_HAZELCAST_EVENTS,
            MetricName.DISPATCHED_HAZELCAST_LATENCY_MS,
            MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED,
            MetricName.DISPATCHED_HAZELCAST_QUEUE_SIZE,

            MetricName.DISPATCHED_KAFKA_EVENTS,
            MetricName.DISPATCHED_KAFKA_LATENCY_MS,
            MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED,
            MetricName.DISPATCHED_KAFKA_QUEUE_SIZE,

            MetricName.STORAGE_POSTGRES_EVENTS,
            MetricName.STORAGE_POSTGRES_LATENCY_MS,
            MetricName.STORAGE_POSTGRES_EVENTS_DROPPED,

            MetricName.STORAGE_ORACLE_EVENTS,
            MetricName.STORAGE_ORACLE_LATENCY_MS,
            MetricName.STORAGE_ORACLE_EVENTS_DROPPED
    );
}
