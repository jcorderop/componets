# Statistics Collection System

A simple, flexible statistics collection system for market data processing services.

## Key Features

- **Spring Boot Integration**: Auto-configured with `@Component` annotations
- **Automatic Scheduled Reporting**: Reports every 1 minute using `@Scheduled`
- **Simple flat API**: Path-based metric names (e.g., `"consumed.kafka.events"`, `"dispatched.zmq.max"`)
- **Dynamic metrics**: Only metrics you use are created and exported
- **No unused fields**: Snapshots only contain non-zero metrics
- **Thread-safe**: All metrics use atomic operations
- **Pluggable sinks**: Logger, Prometheus, or custom implementations
- **Zero configuration**: Just inject `ServiceStatsCollector` and start collecting

## Project Structure

```
stats/
├── config/
│   └── StatsConfiguration.java         # Spring configuration (@EnableScheduling)
├── collector/
│   ├── IStatsCollector.java            # Collector interface
│   ├── ServiceStatsCollector.java      # Main stats collector (@Component)
│   └── MetricName.java                 # Constants for metric names
├── metric/
│   ├── ICounterMetric.java             # Counter interface
│   ├── IGaugeMetric.java               # Gauge interface
│   ├── ILatencyMetric.java             # Latency interface
│   ├── AtomicCounterMetric.java        # Counter implementation
│   ├── AtomicGaugeMetric.java          # Gauge implementation
│   └── AtomicLatencyMetric.java        # Latency implementation
├── reporter/
│   └── StatsReporter.java              # Scheduled reporter (@Component, @Scheduled)
├── sink/
│   ├── IStatsSink.java                 # Sink interface
│   ├── LoggerStatsSink.java            # Logger output (@Component)
│   └── PrometheusStatsSink.java        # Prometheus output
├── snapshot/
│   └── StatsSnapshot.java              # Immutable snapshot
└── example/
    ├── SpringBootUsageExample.java     # Spring Boot example
    ├── SimpleUsageExample.java         # Standalone example
    └── CompleteUsageExample.java       # Full standalone example
```

## Spring Boot Usage (Recommended)

### Step 1: Enable Component Scanning

Ensure your Spring Boot application scans the stats package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.marketdata"})
public class MarketDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketDataApplication.class, args);
    }
}
```

### Step 2: Inject and Use ServiceStatsCollector

Simply inject `ServiceStatsCollector` anywhere you need to collect metrics:

```java
import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final ServiceStatsCollector stats;

    public void processEvent(Event event) {
        // Track consumed event
        stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);
        stats.counter(MetricName.CONSUMED_EVENTS).add(1);

        // Process the event
        long startTime = System.currentTimeMillis();
        doProcessing(event);
        long latencyMillis = System.currentTimeMillis() - startTime;

        // Track pipeline metrics
        stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(1);
        stats.latency(MetricName.PIPELINE_LATENCY_MAX_MILLIS).record(latencyMillis);
        stats.latency(MetricName.PIPELINE_LATENCY_AVG_MILLIS).record(latencyMillis);
        stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(1);
    }
}
```

### Step 3: That's It!

- **Automatic Reporting**: `StatsReporter` runs every 1 minute automatically via `@Scheduled`
- **Zero Configuration**: All components are auto-discovered via `@Component` annotations
- **Default Sink**: `LoggerStatsSink` is automatically enabled and logs metrics every minute

### Optional: Configure Reporting Interval

Add to your `application.properties` or `application.yml`:

```properties
# Configure stats reporting interval (default: 60000 milliseconds = 1 minute)
stats.reporter.fixed-rate-millis=60000
```

Or in YAML:

```yaml
stats:
  reporter:
    fixed-rate-millis: 60000  # 1 minute
```

### Optional: Custom Sinks

To add additional sinks (e.g., Prometheus), create Spring beans:

```java
import com.example.marketdata.stats.sink.PrometheusStatsSink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomStatsConfiguration {

    @Bean
    public PrometheusStatsSink prometheusStatsSink() {
        return new PrometheusStatsSink("marketdata");
    }
}
```

## Standalone Usage (Non-Spring Boot)

### Step 1: Create the Stats Collector

```java
import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;

// Create a single instance (typically application-scoped)
ServiceStatsCollector stats = new ServiceStatsCollector();
```

### Step 2: Collect Metrics Using Constants

The API provides three types of metrics: **counter**, **gauge**, and **latency**.

#### Consume Stage Metrics

```java
// Track consumed events - use specific consumer service
stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(1);    // Kafka consumer
stats.counter(MetricName.CONSUMED_FIX_EVENTS).add(1);      // FIX consumer
stats.counter(MetricName.CONSUMED_RFA_EVENTS).add(1);      // RFA consumer
stats.counter(MetricName.CONSUMED_BPIPE_EVENTS).add(1);    // Bloomberg BPIPE consumer

// Track total consumed events (all sources)
stats.counter(MetricName.CONSUMED_EVENTS).add(1);
```

#### Pipeline Stage Metrics

```java
// Track events entering pipeline
stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(1);

// Track pipeline processing latency
stats.latency(MetricName.PIPELINE_LATENCY_MAX).record(250);  // microseconds
stats.latency(MetricName.PIPELINE_LATENCY_AVG).record(180);

// Track events exiting pipeline
stats.counter(MetricName.PIPELINE_FORWARDED_EVENTS).add(1);
```

#### Dispatched Stage Metrics

```java
// Track total dispatched events (all dispatchers)
stats.counter(MetricName.DISPATCHED_EVENTS).add(1);

// ZMQ dispatcher
stats.latency(MetricName.DISPATCHED_ZMQ_MAX).record(80);
stats.latency(MetricName.DISPATCHED_ZMQ_AVG).record(65);
stats.counter(MetricName.DISPATCHED_ZMQ_EVENTS_DROPPED).add(1);

// Hazelcast dispatcher
stats.latency(MetricName.DISPATCHED_HAZELCAST_MAX).record(120);
stats.latency(MetricName.DISPATCHED_HAZELCAST_AVG).record(95);
stats.counter(MetricName.DISPATCHED_HAZELCAST_EVENTS_DROPPED).add(1);

// Kafka dispatcher
stats.latency(MetricName.DISPATCHED_KAFKA_MAX).record(200);
stats.latency(MetricName.DISPATCHED_KAFKA_AVG).record(150);
stats.counter(MetricName.DISPATCHED_KAFKA_EVENTS_DROPPED).add(1);
```

#### Storage Stage Metrics

```java
// Track total storage events (all databases)
stats.counter(MetricName.STORAGE_EVENTS).add(1);

// Postgres storage
stats.latency(MetricName.STORAGE_POSTGRES_MAX).record(350);
stats.latency(MetricName.STORAGE_POSTGRES_AVG).record(280);
stats.counter(MetricName.STORAGE_POSTGRES_EVENTS_DROPPED).add(1);

// Oracle storage
stats.latency(MetricName.STORAGE_ORACLE_MAX).record(400);
stats.latency(MetricName.STORAGE_ORACLE_AVG).record(320);
stats.counter(MetricName.STORAGE_ORACLE_EVENTS_DROPPED).add(1);
```

### Step 3: Batch Processing

```java
// Add multiple events at once
int batchSize = 100;
stats.counter(MetricName.CONSUMED_KAFKA_EVENTS).add(batchSize);
stats.counter(MetricName.PIPELINE_RECEIVED_EVENTS).add(batchSize);
```

### Step 4: Setup Manual Reporting (Standalone Only)

For standalone applications, you need to manually trigger reporting:

```java
import com.example.marketdata.stats.reporter.StatsSnapshot;
import com.example.marketdata.stats.sink.LoggerStatsSink;
import com.example.marketdata.stats.sink.IStatsSink;

// Create sinks
IStatsSink loggerSink = new LoggerStatsSink();

        // Periodically (e.g., every minute in a scheduled task)
        StatsSnapshot snapshot = stats.snapshotAndReset();
loggerSink.

        publish(snapshot);
```

**How it works:**
1. Call `stats.snapshotAndReset()` to capture all metrics
2. Publish snapshot to your sinks
3. All metrics are automatically reset to zero for the next window

### Step 5: Accessing Snapshot Data

You can access snapshot data programmatically:

```java
import com.example.marketdata.stats.reporter.StatsSnapshot;

// Take snapshot and reset all metrics
StatsSnapshot snapshot = stats.snapshotAndReset();

        // Access counter values
        Long consumedEvents = snapshot.counters().get("consumed.events");
        Long droppedEvents = snapshot.counters().get("dispatched.zmq.eventsDropped");

        // Access gauge values (if using gauges)
        Long queueSize = snapshot.gauges().get("some.gauge.metric");

        // Access latency metrics
        StatsSnapshot.LatencySnapshot pipelineLatency = snapshot.latencies().get("pipeline.latencyMax");
if(pipelineLatency !=null){
        long count = pipelineLatency.count();
        double avgMicros = pipelineLatency.avgMicros();
        long maxMicros = pipelineLatency.maxMicros();
}
```

---

## Metric Types

### Counter (ICounterMetric / AtomicCounterMetric)
Monotonically increasing count (e.g., events processed)

```java
stats.counter("consume.events").add(1);
stats.counter("consume.events").add(batchSize);
long total = stats.counter("consume.events").value();
```

### Gauge (IGaugeMetric / AtomicGaugeMetric)
Point-in-time value (e.g., queue size)

```java
stats.gauge("consume.queueSize").set(120);
long size = stats.gauge("consume.queueSize").value();
```

### Latency (ILatencyMetric / AtomicLatencyMetric)
Tracks average and max latency

```java
stats.latency("pipeline.latency").record(250);  // microseconds
double avg = stats.latency("pipeline.latency").avg();
long max = stats.latency("pipeline.latency").max();
```

## Custom Sinks

```java
import com.example.marketdata.stats.sink.IStatsSink;
import com.example.marketdata.stats.reporter.StatsSnapshot;

public class ElasticsearchSink implements IStatsSink {
  @Override
  public void publish(StatsSnapshot snapshot) {
    // Convert snapshot to Elasticsearch document
    // Send to Elasticsearch
  }
}
```

## Benefits vs Monolithic Approach

### Old Way (Monolithic)
```java
class Stats {
    long consumeEvents;
    long consumeQueueSize;      // Always present, even if unused
    long pipelineEvents;
    long pipelineLatencyAvg;
    long pipelineLatencyMax;
    long forwardZmqEvents;      // All dispatchers predefined
    long forwardZmqQueueSize;
    // ... many more fields, most unused
}
```

### New Way (Dynamic)
```java
// Only creates metrics you actually use
stats.counter(MetricName.CONSUME_EVENTS).add(1);
stats.gauge(MetricName.FORWARD_ZMQ_QUEUE_SIZE).set(15);

// Snapshot contains ONLY these metrics
// No unused fields in output
```

## Thread Safety

**100% Thread-Safe for Concurrent Multi-Process/Multi-Thread Usage**

The entire statistics system is designed for high-concurrency environments where many threads/processes simultaneously update metrics:

### Concurrent Updates
- ✅ **Multiple threads can safely call** `counter.add()`, `gauge.set()`, `latency.record()` simultaneously
- ✅ **No data loss** - all updates are atomic and lock-free
- ✅ **High performance** - uses `LongAdder` for counters (optimized for high contention)

### Atomic Metric Implementations
- **AtomicCounterMetric**: Uses `LongAdder.sumThenReset()` - atomically snapshots and resets
- **AtomicGaugeMetric**: Uses `AtomicLong.getAndSet()` - atomically snapshots and resets
- **AtomicLatencyMetric**: Atomically snapshots all 3 values (count, total, max) and resets

### Thread-Safe Collection
- **ServiceStatsCollector**: Uses `ConcurrentHashMap` for metric storage
- **snapshotAndReset()**: Atomically captures values and resets in single operation - no race conditions

### Example: Safe Concurrent Usage

```java
import com.example.marketdata.stats.collector.MetricName;
import com.example.marketdata.stats.collector.ServiceStatsCollector;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import com.example.marketdata.stats.reporter.StatsSnapshot;

// Multiple threads can safely update the same metrics simultaneously
ServiceStatsCollector stats = new ServiceStatsCollector();

// Thread 1
new

        Thread(() ->{
        for(
        int i = 0;
        i< 1000;i++){
        stats.

        counter(MetricName.CONSUME_EVENTS).

        add(1);
    }
            }).

        start();

// Thread 2 (updating same metric)
new

        Thread(() ->{
        for(
        int i = 0;
        i< 1000;i++){
        stats.

        counter(MetricName.CONSUME_EVENTS).

        add(1);
    }
            }).

        start();

// Reporter thread (periodic snapshots)
new

        Thread(() ->{
        while(running){
        StatsSnapshot snapshot = stats.snapshotAndReset();
        // No data loss - snapshot is atomic
    }
            }).

        start();

// Result: All 2000 events are counted, no data loss during snapshot
```

### Guarantees
- ✅ No locks needed
- ✅ No data loss during concurrent updates
- ✅ No race conditions during snapshot
- ✅ Updates happening during snapshot are captured in next window
- ✅ Optimized for high-throughput, low-latency scenarios

## Example Output

### Logger Format
```
=== Statistics Report: service ===
counter.consume.events = 1500
gauge.consume.queueSize = 100
counter.pipeline.events = 1000
latency.pipeline.latency = {count=1000, avg=125.00 µs, max=150 µs}
counter.forward.zmq.events = 800
latency.forward.zmq.latency = {count=800, avg=65.00 µs, max=80 µs}
gauge.forward.zmq.queueSize = 50
```

### Prometheus Format
```
marketdata_service_consume_events_total 1500
marketdata_service_consume_queuesize 100
marketdata_service_pipeline_events_total 1000
marketdata_service_pipeline_latency_count 1000
marketdata_service_pipeline_latency_sum_micros 125000
marketdata_service_pipeline_latency_max_micros 150
```

## Naming Conventions

### Metric Constants (MetricName class)
All metric names are defined as constants in `MetricName.java` to avoid typos.

#### Building Blocks (protected - for internal use only)

**Stage Prefixes:**
- `CONSUMED_PREFIX = "consumed."`
- `PIPELINE_PREFIX = "pipeline."`
- `DISPATCHED_PREFIX = "dispatched."`
- `STORAGE_PREFIX = "storage."`

**Metric Suffixes:**
- `METRIC_EVENTS = "events"`
- `METRIC_EVENTS_DROPPED = "eventsDropped"`
- `METRIC_LATENCY_MAX = "latencyMax"`
- `METRIC_LATENCY_AVG = "latencyAvg"`

**Consumer Service Names:**
- `CONSUMER_KAFKA = "kafka"`
- `CONSUMER_FIX = "fix"`
- `CONSUMER_RFA = "rfa"`
- `CONSUMER_BPIPE = "bpipe"`

**Dispatcher Service Names:**
- `DISPATCHER_ZMQ = "zmq"`
- `DISPATCHER_HAZELCAST = "hazelcast"`
- `DISPATCHER_KAFKA = "kafka"`

**Storage Service Names:**
- `STORAGE_POSTGRES = "postgres"`
- `STORAGE_ORACLE = "oracle"`

#### Public Constants (for actual use in code)

**Consume Stage:**
```java
CONSUMED_EVENTS                 // "consumed.events"
CONSUMED_KAFKA_EVENTS          // "consumed.kafka.events"
CONSUMED_FIX_EVENTS            // "consumed.fix.events"
CONSUMED_RFA_EVENTS            // "consumed.rfa.events"
CONSUMED_BPIPE_EVENTS          // "consumed.bpipe.events"
```

**Pipeline Stage:**
```java
PIPELINE_RECEIVED_EVENTS       // "pipeline.receivedEvents"
PIPELINE_LATENCY_MAX          // "pipeline.latencyMax"
PIPELINE_LATENCY_AVG          // "pipeline.latencyAvg"
PIPELINE_FORWARDED_EVENTS     // "pipeline.forwardedEvents"
```

**Dispatched Stage:**
```java
DISPATCHED_EVENTS                    // "dispatched.events"

// ZMQ
DISPATCHED_ZMQ_MAX                  // "dispatched.zmq.max"
DISPATCHED_ZMQ_AVG                  // "dispatched.zmq.avg"
DISPATCHED_ZMQ_EVENTS_DROPPED       // "dispatched.zmq.eventsDropped"

// Hazelcast
DISPATCHED_HAZELCAST_MAX            // "dispatched.hazelcast.max"
DISPATCHED_HAZELCAST_AVG            // "dispatched.hazelcast.avg"
DISPATCHED_HAZELCAST_EVENTS_DROPPED // "dispatched.hazelcast.eventsDropped"

// Kafka
DISPATCHED_KAFKA_MAX                // "dispatched.kafka.max"
DISPATCHED_KAFKA_AVG                // "dispatched.kafka.avg"
DISPATCHED_KAFKA_EVENTS_DROPPED     // "dispatched.kafka.eventsDropped"
```

**Storage Stage:**
```java
STORAGE_EVENTS                       // "storage.events"

// Postgres
STORAGE_POSTGRES_MAX                // "storage.postgres.max"
STORAGE_POSTGRES_AVG                // "storage.postgres.avg"
STORAGE_POSTGRES_EVENTS_DROPPED     // "storage.postgres.eventsDropped"

// Oracle
STORAGE_ORACLE_MAX                  // "storage.oracle.max"
STORAGE_ORACLE_AVG                  // "storage.oracle.avg"
STORAGE_ORACLE_EVENTS_DROPPED       // "storage.oracle.eventsDropped"
```

### Interfaces
Prefixed with `I` (e.g., `IStatsCollector`, `ICounterMetric`)

### Implementations
- Descriptive names indicating behavior:
  - `AtomicCounterMetric` - uses atomic operations
  - `LoggerStatsSink` - outputs to logger

## See Also

- `example/SimpleUsageExample.java` - Basic collection example
- `example/CompleteUsageExample.java` - Full example with reporting
