package com.example.marketdata.monitor.service;

import com.example.marketdata.monitor.processor.ProcessorStatsSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessorStatsSinkPrometheusServiceTest {

    @Test
    void publishIncrementsCountersAndUpdatesGaugesPerProcessor() {
        MeterRegistry registry = new SimpleMeterRegistry();
        ProcessorStatsSinkPrometheusService sink = new ProcessorStatsSinkPrometheusService(registry);

        ProcessorStatsSnapshot firstSnapshot = new ProcessorStatsSnapshot(
                "alpha",
                100L,
                200L,
                3L,
                2L,
                1L,
                5L,
                15L,
                7.5,
                4
        );

        sink.publish(List.of(firstSnapshot));

        assertThat(counterValue(registry, "marketdata.processor.events.enqueued", "alpha"))
                .isEqualTo(3.0);
        assertThat(counterValue(registry, "marketdata.processor.events.processed", "alpha"))
                .isEqualTo(2.0);
        assertThat(counterValue(registry, "marketdata.processor.events.dropped", "alpha"))
                .isEqualTo(1.0);

        assertThat(gaugeValue(registry, "marketdata.processor.queue.size", "alpha"))
                .isEqualTo(4.0);
        assertThat(gaugeValue(registry, "marketdata.processor.latency.avg", "alpha"))
                .isEqualTo(7.5);
        assertThat(gaugeValue(registry, "marketdata.processor.window.end", "alpha"))
                .isEqualTo(200.0);

        ProcessorStatsSnapshot secondSnapshot = new ProcessorStatsSnapshot(
                "alpha",
                200L,
                300L,
                1L,
                1L,
                0L,
                3L,
                8L,
                5.0,
                2
        );

        sink.publish(List.of(secondSnapshot));

        assertThat(counterValue(registry, "marketdata.processor.events.enqueued", "alpha"))
                .isEqualTo(4.0);
        assertThat(counterValue(registry, "marketdata.processor.events.processed", "alpha"))
                .isEqualTo(3.0);
        assertThat(counterValue(registry, "marketdata.processor.events.dropped", "alpha"))
                .isEqualTo(1.0);

        assertThat(gaugeValue(registry, "marketdata.processor.queue.size", "alpha"))
                .isEqualTo(2.0);
        assertThat(gaugeValue(registry, "marketdata.processor.latency.avg", "alpha"))
                .isEqualTo(5.0);
        assertThat(gaugeValue(registry, "marketdata.processor.window.end", "alpha"))
                .isEqualTo(300.0);
    }

    @Test
    void publishSkipsWhenSnapshotsAreEmpty() {
        MeterRegistry registry = new SimpleMeterRegistry();
        ProcessorStatsSinkPrometheusService sink = new ProcessorStatsSinkPrometheusService(registry);

        sink.publish(List.of());

        assertThat(registry.getMeters()).isEmpty();
    }

    private double counterValue(MeterRegistry registry, String name, String processor) {
        return registry.find(name)
                .tags("processor", processor)
                .counter()
                .count();
    }

    private double gaugeValue(MeterRegistry registry, String name, String processor) {
        Gauge gauge = registry.find(name)
                .tags("processor", processor)
                .gauge();
        assertThat(gauge).as("Gauge %s not registered", name).isNotNull();
        return gauge.value();
    }
}
