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
    private static final String METRIC_EVENTS = "events";
    private static final String METRIC_EVENTS_DROPPED = METRIC_EVENTS + "." + "dropped";

    private static final String METRIC_LATENCY = "latency.";
    private static final String MILLISECONDS = "ms";
    private static final String METRIC_LATENCY_MAX_MS = METRIC_LATENCY + "max" + "." + MILLISECONDS;
    private static final String METRIC_LATENCY_AVG_MS = METRIC_LATENCY + "avg" + "." + MILLISECONDS;

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
    public static final String CONSUMED_KAFKA_EVENTS = CONSUMED_PREFIX + CONSUMER_KAFKA + "." + METRIC_EVENTS;
    public static final String CONSUMED_FIX_EVENTS = CONSUMED_PREFIX + CONSUMER_FIX + "." + METRIC_EVENTS;
    public static final String CONSUMED_RFA_EVENTS = CONSUMED_PREFIX + CONSUMER_RFA + "." + METRIC_EVENTS;
    public static final String CONSUMED_BPIPE_EVENTS = CONSUMED_PREFIX + CONSUMER_BPIPE + "." + METRIC_EVENTS;

    // Pipeline stage metrics
    public static final String PIPELINE_RECEIVED_EVENTS = PIPELINE_PREFIX + "receivedEvents";
    public static final String PIPELINE_LATENCY_MAX_MS = PIPELINE_PREFIX + METRIC_LATENCY_MAX_MS;
    public static final String PIPELINE_LATENCY_AVG_MS = PIPELINE_PREFIX + METRIC_LATENCY_AVG_MS;
    public static final String PIPELINE_FORWARDED_EVENTS = PIPELINE_PREFIX + "forwardedEvents";

    // Dispatched stage - common
    public static final String DISPATCHED_EVENTS = DISPATCHED_PREFIX + METRIC_EVENTS;

    // Dispatched stage - ZMQ
    public static final String DISPATCHED_ZMQ_MAX_MS = DISPATCHED_PREFIX + DISPATCHER_ZMQ + "max" + "." + MILLISECONDS;
    public static final String DISPATCHED_ZMQ_AVG_MS = DISPATCHED_PREFIX + DISPATCHER_ZMQ + "avg" + "." + MILLISECONDS;
    public static final String DISPATCHED_ZMQ_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_ZMQ + "." + METRIC_EVENTS_DROPPED;

    // Dispatched stage - Hazelcast
    public static final String DISPATCHED_HAZELCAST_MAX_MS = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + "max" + "." + MILLISECONDS;
    public static final String DISPATCHED_HAZELCAST_AVG_MS = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + "avg" + "." + MILLISECONDS;
    public static final String DISPATCHED_HAZELCAST_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_HAZELCAST + "." + METRIC_EVENTS_DROPPED;

    // Dispatched stage - Kafka
    public static final String DISPATCHED_KAFKA_MAX_MS = DISPATCHED_PREFIX + DISPATCHER_KAFKA + "max" + "." + MILLISECONDS;
    public static final String DISPATCHED_KAFKA_AVG_MS = DISPATCHED_PREFIX + DISPATCHER_KAFKA + "avg" + "." + MILLISECONDS;
    public static final String DISPATCHED_KAFKA_EVENTS_DROPPED = DISPATCHED_PREFIX + DISPATCHER_KAFKA + "." + METRIC_EVENTS_DROPPED;

    // Storage stage - common
    public static final String STORAGE_EVENTS = STORAGE_PREFIX + METRIC_EVENTS;

    // Storage stage - Postgres
    public static final String STORAGE_POSTGRES_MAX_MS = STORAGE_PREFIX + STORAGE_POSTGRES + "max" + "." + MILLISECONDS;
    public static final String STORAGE_POSTGRES_AVG_MS = STORAGE_PREFIX + STORAGE_POSTGRES + "avg" + "." + MILLISECONDS;
    public static final String STORAGE_POSTGRES_EVENTS_DROPPED = STORAGE_PREFIX + STORAGE_POSTGRES + "." + METRIC_EVENTS_DROPPED;

    // Storage stage - Oracle
    public static final String STORAGE_ORACLE_MAX_MS = STORAGE_PREFIX + STORAGE_ORACLE + "max" + "." + MILLISECONDS;
    public static final String STORAGE_ORACLE_AVG_MS = STORAGE_PREFIX + STORAGE_ORACLE + "avg" + "." + MILLISECONDS;
    public static final String STORAGE_ORACLE_EVENTS_DROPPED = STORAGE_PREFIX + STORAGE_ORACLE + "." + METRIC_EVENTS_DROPPED;
}
