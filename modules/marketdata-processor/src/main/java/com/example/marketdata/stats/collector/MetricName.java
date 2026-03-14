package com.example.marketdata.stats.collector;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * Constants for metric names to avoid typos and enable refactoring.
 * Use these constants instead of hardcoded strings.
 */
@NoArgsConstructor(access = PRIVATE)
public final class MetricName {

    // Stage prefixes (protected - building blocks)
    private static final String PIPELINE_PREFIX = "pipeline.";
    private static final String DISPATCHED_PREFIX = "dispatched.";
    private static final String STORAGE_PREFIX = "storage.";

    // Metric suffixes (protected - building blocks)
    private static final String METRIC_EVENTS = ".events";
    private static final String METRIC_EVENTS_DROPPED = METRIC_EVENTS + "." + "dropped";
    private static final String METRIC_QUEUE_SIZE = ".queue.size";

    private static final String METRIC_LATENCY = ".latency.";
    private static final String MILLISECONDS = ".ms";
    private static final String METRIC_LATENCY_MS = METRIC_LATENCY + MILLISECONDS;

    // Dispatcher service names (protected - building blocks)
    private static final String DISPATCHER_ZMQ = "zmq";
    private static final String DISPATCHER_HAZELCAST = "hazelcast";
    private static final String DISPATCHER_KAFKA = "kafka";

    // Storage service names (protected - building blocks)
    private static final String STORAGE_POSTGRES = "postgres";
    private static final String STORAGE_ORACLE = "oracle";

    // Stage prefixes (protected - building blocks)
    private static final String CONSUMED_PREFIX = "consumed.";

    // Consumer service names (protected - building blocks)
    private static final String CONSUMER_KAFKA = "kafka";
    private static final String CONSUMER_FIX = "fix";
    private static final String CONSUMER_RFA = "rfa";
    private static final String CONSUMER_BPIPE = "bpipe";

    // Consume stage metrics - common
    public static final String CONSUMED_EVENTS = CONSUMED_PREFIX + METRIC_EVENTS;

    // Consume stage metrics - per consumer service
    public static final String CONSUMED_KAFKA_EVENTS = CONSUMED_PREFIX + CONSUMER_KAFKA + METRIC_EVENTS;
    public static final String CONSUMED_FIX_EVENTS = CONSUMED_PREFIX + CONSUMER_FIX + METRIC_EVENTS;
    public static final String CONSUMED_RFA_EVENTS = CONSUMED_PREFIX + CONSUMER_RFA + METRIC_EVENTS;
    public static final String CONSUMED_BPIPE_EVENTS = CONSUMED_PREFIX + CONSUMER_BPIPE + METRIC_EVENTS;

    // Pipeline stage metrics
    public static final String PIPELINE_RECEIVED_EVENTS = PIPELINE_PREFIX + "receivedEvents";
    public static final String PIPELINE_LATENCY = PIPELINE_PREFIX + METRIC_LATENCY_MS;
    public static final String PIPELINE_FORWARDED_EVENTS = PIPELINE_PREFIX + "forwardedEvents";

    // Dispatched stage - ZMQ
    public static final String DISPATCHED_EVENTS = DISPATCHED_PREFIX + DISPATCHER_ZMQ + METRIC_EVENTS;
    public static final String DISPATCHED_ZMQ_LATENCY_MS = DISPATCHED_PREFIX + DISPATCHER_ZMQ + METRIC_LATENCY_MS;
    public static final String DISPATCHED_ZMQ_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_ZMQ + METRIC_EVENTS_DROPPED;
    public static final String DISPATCHED_ZMQ_QUEUE_SIZE = DISPATCHED_PREFIX + DISPATCHER_ZMQ + METRIC_QUEUE_SIZE;

    // Dispatched stage - Hazelcast
    public static final String DISPATCHED_HAZELCAST_EVENTS = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + METRIC_EVENTS;
    public static final String DISPATCHED_HAZELCAST_LATENCY_MS = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + METRIC_LATENCY_MS;
    public static final String DISPATCHED_HAZELCAST_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + METRIC_EVENTS_DROPPED;
    public static final String DISPATCHED_HAZELCAST_QUEUE_SIZE = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + METRIC_QUEUE_SIZE;

    // Dispatched stage - Kafka
    public static final String DISPATCHED_KAFKA_EVENTS = DISPATCHED_PREFIX + DISPATCHER_KAFKA + METRIC_EVENTS;
    public static final String DISPATCHED_KAFKA_LATENCY_MS = DISPATCHED_PREFIX + DISPATCHER_KAFKA + METRIC_LATENCY_MS;
    public static final String DISPATCHED_KAFKA_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_KAFKA + METRIC_EVENTS_DROPPED;
    public static final String DISPATCHED_KAFKA_QUEUE_SIZE = DISPATCHED_PREFIX + DISPATCHER_KAFKA + METRIC_QUEUE_SIZE;

    // Storage stage - Postgres
    public static final String STORAGE_POSTGRES_EVENTS = STORAGE_PREFIX + STORAGE_POSTGRES + METRIC_EVENTS;
    public static final String STORAGE_POSTGRES_LATENCY_MS = STORAGE_PREFIX + STORAGE_POSTGRES + METRIC_LATENCY_MS;
    public static final String STORAGE_POSTGRES_EVENTS_DROPPED = STORAGE_PREFIX + STORAGE_POSTGRES + METRIC_EVENTS_DROPPED;

    // Storage stage - Oracle
    public static final String STORAGE_ORACLE_EVENTS = STORAGE_PREFIX + STORAGE_ORACLE + METRIC_EVENTS;
    public static final String STORAGE_ORACLE_LATENCY_MS = STORAGE_PREFIX + STORAGE_ORACLE + METRIC_LATENCY_MS;
    public static final String STORAGE_ORACLE_EVENTS_DROPPED = STORAGE_PREFIX + STORAGE_ORACLE + METRIC_EVENTS_DROPPED;
}
