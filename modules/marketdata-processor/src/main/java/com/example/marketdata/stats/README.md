# Statistics Collection System

Simple, thread-safe statistics collection for market-data processing.

## What you get

- Flat metric API via `ServiceStatsCollector` (`counter`, `gauge`, `latency`)
- Spring Boot auto-wiring with scheduled reporting (`StatsReporter`)
- Snapshot-based reporting (`snapshotAndReset`) for fixed reporting windows
- Pluggable sinks (`LoggerStatsSink`, `PrometheusStatsSink`, custom)
- Canonical metric-name constants in `MetricName`
- Gauge semantics optimized for peak values via `setMax(...)`

---

## Quick start (Spring Boot)

```java
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final ServiceStatsCollector stats;

    public void process(Event event) {
        stats.counter(MetricName.CONSUMED_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);

        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(1);
        stats.latency(MetricName.PIPELINE_LATENCY).record(120);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(1);

        stats.counter(MetricName.DISPATCHED_EVENTS).add(1);
        stats.latency(MetricName.DISPATCHED_ZMQ_LATENCY_MS).record(40);
        stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(18);
    }
}
```

`StatsReporter` publishes automatically every minute by default.

Configure interval if needed:

```properties
stats.reporter.fixed-rate-millis=60000
```

---

## Quick start (standalone)

```java
ServiceStatsCollector stats = new ServiceStatsCollector("marketdata");
IStatsSink sink = new LoggerStatsSink();

stats.counter(MetricName.CONSUMED_EVENTS).add(10);
stats.latency(MetricName.PIPELINE_LATENCY).record(150);
stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(27);

StatsSnapshot snapshot = stats.snapshotAndReset();
sink.publish(snapshot);
```

---

## Metric API semantics

### Counter

Use for monotonically increasing values within the reporting window.

```java
stats.counter(MetricName.CONSUMED_EVENTS).add(1);
```

- Implementation: `AtomicCounterMetric`
- Snapshot behavior: `sumThenReset()`

### Gauge

Use for peak/window max values (e.g., queue depth spikes).

```java
stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(21);
stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(18); // ignored (lower)
```

- Implementation: `AtomicGaugeMetric`
- API: `setMax(long)`
- Snapshot behavior: returns max seen in the window, then resets to `0`

### Latency

Use for time values; system stores **average and max** per window.

```java
stats.latency(MetricName.PIPELINE_LATENCY).record(100);
stats.latency(MetricName.PIPELINE_LATENCY).record(300);
```

- Implementation: `AtomicLatencyMetric`
- Snapshot behavior: returns `{avg, max}` and resets window accumulators

---

## Snapshot model

`StatsSnapshot` contains:

- `name` (snapshot name)
- `counters` (`Map<String, Long>`)
- `gauges` (`Map<String, Long>`)
- `latencies` (`Map<String, LatencySnapshot(avg, max)>`)

All maps are immutable from the consumer perspective.

---

## Built-in sinks

### LoggerStatsSink

Logs metrics in a readable format:

- `counter.<name> = <value>`
- `gauge.<name> = <value>`
- `latency.<name> = {avg=<x> ms, max=<y> ms}`

When snapshot is empty, logs one `(empty)` line.

### PrometheusStatsSink

Exports Prometheus exposition-style lines (logged), including:

- `# TYPE` metadata lines
- Sanitized metric names
- `snapshot="..."` label
- Counter suffix `_total`
- Latency metrics as `<metric>_avg_ms` and `<metric>_max_ms`

---

## MetricName catalog

Use `MetricName` constants instead of raw strings.

### Consumed stage
- `CONSUMED_EVENTS`
- `CONSUMED_KAFKA_EVENTS`
- `CONSUMED_FIX_EVENTS`
- `CONSUMED_RFA_EVENTS`
- `CONSUMED_BPIPE_EVENTS`

### Pipeline stage
- `PIPELINE_RECEIVED_EVENTS`
- `PIPELINE_LATENCY`
- `PIPELINE_FORWARDED_EVENTS`

### Dispatched stage
- ZMQ
  - `DISPATCHED_EVENTS`
  - `DISPATCHED_ZMQ_LATENCY_MS`
  - `DISPATCHED_ZMQ_EVENTS_DROPPED`
  - `DISPATCHED_ZMQ_QUEUE_SIZE`
- Hazelcast
  - `DISPATCHED_HAZELCAST_EVENTS`
  - `DISPATCHED_HAZELCAST_LATENCY_MS`
  - `DISPATCHED_HAZELCAST_EVENTS_DROPPED`
  - `DISPATCHED_HAZELCAST_QUEUE_SIZE`
- Kafka
  - `DISPATCHED_KAFKA_EVENTS`
  - `DISPATCHED_KAFKA_LATENCY_MS`
  - `DISPATCHED_KAFKA_EVENTS_DROPPED`
  - `DISPATCHED_KAFKA_QUEUE_SIZE`

### Storage stage
- Postgres
  - `STORAGE_POSTGRES_EVENTS`
  - `STORAGE_POSTGRES_LATENCY_MS`
  - `STORAGE_POSTGRES_EVENTS_DROPPED`
- Oracle
  - `STORAGE_ORACLE_EVENTS`
  - `STORAGE_ORACLE_LATENCY_MS`
  - `STORAGE_ORACLE_EVENTS_DROPPED`

---

## Custom sink

```java
public class ElasticsearchSink implements IStatsSink {
    @Override
    public void publish(StatsSnapshot snapshot) {
        // transform and push snapshot
    }
}
```

---

## Testing status

The stats module includes dedicated unit tests for:

- Metric primitives (`AtomicCounterMetric`, `AtomicGaugeMetric`, `AtomicLatencyMetric`)
- Collector snapshot/reset behavior (`ServiceStatsCollector`)
- Snapshot immutability (`StatsSnapshot`)
- Sink formatting and empty behavior (`LoggerStatsSink`, `PrometheusStatsSink`)
- Reporter orchestration/error isolation (`StatsReporter`)
- Metric constants integrity (`MetricName`)

