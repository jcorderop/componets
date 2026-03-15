# Statistics Collection System

Simple, thread-safe statistics collection for market-data processing.

## What you get

- Flat metric API via `ServiceStatsCollector` (`counter`, `gauge`, `latency`)
- Spring Boot auto-wiring with scheduled reporting (`StatsReporter`)
- Snapshot-based reporting (`snapshotAndReset`) for fixed reporting windows
- Pluggable sinks (`LoggerStatsSinkService`, `PrometheusStatsSinkService`, custom)
- Canonical metric-name constants in `MetricName`
- Gauge semantics optimized for peak values via `setMax(...)`

---

## Quick start (Spring Boot)

```java
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final WrapperConsumedKafkaStats consumedKafka;
    private final WrapperPipelineStats pipeline;
    private final WrapperDispatchedZmqStats dispatchedZmq;

    public void process(Event event) {
        consumedKafka.addConsumed(1);

        pipeline.addReceived(1);
        pipeline.recordLatency(120);
        pipeline.addForwarded(1);

        dispatchedZmq.addDispatched(1);
        dispatchedZmq.recordLatency(40);
        dispatchedZmq.setQueueSizeMax(18);
    }
}
```

All `Wrapper*Stats` classes are Spring beans (`@Component`) and can be injected directly into your services.

`StatsReporter` publishes automatically every minute by default.

Configure interval if needed:

```properties
stats.reporter.fixed-rate-millis=60000
```

Set a custom snapshot name (used in logs and sink labels) if needed:

```properties
marketdata.stats.snapshot.name=marketdata-processor
```

### Required configuration properties

From the class-level configuration/docs (`ServiceStatsCollector`, `StatsReporter`):

- There are **no strictly required** properties to start the stats system because both Spring-injected properties have defaults.
- For production clarity, you should explicitly set the following properties:

| Property | Used by | Default | Why you should set it |
|---|---|---|---|
| `marketdata.stats.snapshot.name` | `ServiceStatsCollector` | `default_service` | Distinguishes this service instance in emitted snapshots/logs/labels. |
| `stats.reporter.fixed-rate-millis` | `StatsReporter` | `60000` | Controls reporting window and publish cadence. |

Example:

```properties
marketdata.stats.snapshot.name=marketdata-processor
stats.reporter.fixed-rate-millis=60000

# sink toggles (only enabled sinks are injected into StatsReporter)
stats.sink.logger.enabled=true
stats.sink.prometheus.enabled=false
stats.sink.telemetry.enabled=false

# optional sink-specific prefixes
stats.sink.prometheus.metric-prefix=marketdata_stats
stats.sink.telemetry.metric-prefix=marketdata_stats
```

### Sink enablement properties

`StatsReporter` receives `List<IStatsSink>` from Spring. Because each built-in sink is conditionally registered, the reporter publishes only to the sinks that are enabled.

| Property | Sink service | Default | Notes |
|---|---|---|---|
| `stats.sink.logger.enabled` | `LoggerStatsSinkService` | `true` | Enabled by default (`matchIfMissing=true`). |
| `stats.sink.prometheus.enabled` | `PrometheusStatsSinkService` | `false` | Requires `MeterRegistry` bean. |
| `stats.sink.prometheus.metric-prefix` | `PrometheusStatsSinkService` | `marketdata_stats` | Prefix used for exported metric names. |
| `stats.sink.telemetry.enabled` | `TelemetryStatsSinkService` | `false` | Requires `OpenTelemetry` bean. |
| `stats.sink.telemetry.metric-prefix` | `TelemetryStatsSinkService` | `marketdata_stats` | Prefix used for exported metric names. |

---

## Quick start (standalone)

```java
ServiceStatsCollector stats = new ServiceStatsCollector("marketdata");
IStatsSink sink = new LoggerStatsSinkService();

stats.counter(MetricName.CONSUMED_EVENTS).add(10);
stats.latency(MetricName.PIPELINE_LATENCY).record(150);
stats.gauge(MetricName.DISPATCHED_ZMQ_QUEUE_SIZE).setMax(27);

StatsSnapshot snapshot = stats.snapshotAndReset();
sink.publish(snapshot);
```

---


## Quick start (typed wrappers)

If you prefer strongly-typed stage APIs over direct `MetricName` usage, use wrapper classes in either mode:

### Spring Boot mode (recommended)

Inject wrappers directly into your service:

```java
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final WrapperConsumedKafkaStats consumedKafka;
    private final WrapperPipelineStats pipeline;
    private final WrapperDispatchedZmqStats dispatchedZmq;
    private final WrapperStoragePostgresStats storagePostgres;

    public void process(Event event) {
        consumedKafka.addConsumed(1);
        pipeline.addReceived(1);
        pipeline.recordLatency(18);
        pipeline.addForwarded(1);

        dispatchedZmq.addDispatched(1);
        dispatchedZmq.recordLatency(12);
        dispatchedZmq.setQueueSizeMax(4);

        storagePostgres.addStored(1);
        storagePostgres.recordLatency(25);
    }
}
```

### Standalone mode

Instantiate wrappers manually with one `ServiceStatsCollector`:

```java
ServiceStatsCollector stats = new ServiceStatsCollector("marketdata");

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
```

Available wrappers:

- Consumed: `WrapperConsumedKafkaStats`, `WrapperConsumedFixStats`, `WrapperConsumedRfaStats`, `WrapperConsumedBpipeStats`
- Pipeline: `WrapperPipelineStats`
- Dispatched: `WrapperDispatchedZmqStats`, `WrapperDispatchedKafkaStats`, `WrapperDispatchedHazelcastStats`
- Storage: `WrapperStoragePostgresStats`, `WrapperStorageOracleStats`

All wrappers share a common marker type: `IWrapperStats` (implemented by `AbstractConsumedStats`, `AbstractPipelineStats`, `AbstractDispatchedStats`, and `AbstractStorageStats`).

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

### LoggerStatsSinkService

Logs metrics in a readable format:

- `counter.<name> = <value>`
- `gauge.<name> = <value>`
- `latency.<name> = {avg=<x> ms, max=<y> ms}`

When snapshot is empty, logs one `(empty)` line.

### PrometheusStatsSinkService

Exports metrics to Micrometer `MeterRegistry` so they can be scraped via Prometheus actuator endpoints, including:

- Sanitized metric names
- `snapshot="..."` label (as a Micrometer tag)
- Counter suffix `_total`
- Latency metrics as `<metric>_avg_ms` and `<metric>_max_ms` gauges

### TelemetryStatsSinkService

Exports metrics with the OpenTelemetry SDK (`OpenTelemetry`/`Meter`) for telemetry/APM pipelines (for example, APM -> Elastic), using the same naming and `snapshot` tagging model as `PrometheusStatsSinkService`.

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


## How wrappers work and how to extend

### How they work internally

- Each wrapper receives one `ServiceStatsCollector` instance in its constructor.
- In Spring Boot, wrappers are registered as beans (`@Component`) and are auto-wired into services.
- Abstract wrapper families implement the empty `IWrapperStats` marker interface so all wrappers can be treated as one common type when needed.
- The wrapper resolves and stores metric handles (`counter`, `latency`, `gauge`) exactly once.
- Wrapper methods (`addConsumed`, `addForwarded`, `setQueueSizeMax`, etc.) delegate directly to those metric handles.
- Reporting is unchanged: `StatsReporter` still calls `snapshotAndReset()` and sinks publish the resulting `StatsSnapshot`.

### Extending with a new metric family

When adding a new destination/source/channel, follow this sequence:

1. Add a constant to `MetricName` and include it in `AllowedMetricNames`.
2. Decide whether it fits an existing abstraction (`AbstractConsumedStats`, `AbstractDispatchedStats`, etc.) or requires a new abstract base.
3. Add a concrete wrapper that wires the correct metric constants in the constructor.
4. Add/update tests to assert the wrapper writes to the expected snapshot fields.
5. Document usage and property impact in this README.

This keeps call sites free from raw metric names while preserving the single-source metric catalog in `MetricName`.

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

## Usage examples in tests

Runtime `main(...)` example classes were moved to unit tests to keep usage samples executable and verifiable.

See `src/test/java/com/example/marketdata/stats/example/WrapperUsageExamplesTest.java` for:
- standalone wrapper usage
- common `IWrapperStats` typing for Spring-style exchange/injection

## Testing status

The stats module includes dedicated unit tests for:

- Metric primitives (`AtomicCounterMetric`, `AtomicGaugeMetric`, `AtomicLatencyMetric`)
- Collector snapshot/reset behavior (`ServiceStatsCollector`)
- Snapshot immutability (`StatsSnapshot`)
- Sink formatting and empty behavior (`LoggerStatsSinkService`, `PrometheusStatsSinkService`)
- Reporter orchestration/error isolation (`StatsReporter`)
- Metric constants integrity (`MetricName`)
- Typed wrapper wiring/delegation (`WrapperStatsTest`)
