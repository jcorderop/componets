package com.example.marketdata.stats.sink;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.marketdata.stats.reporter.StatsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusStatsSinkTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(PrometheusStatsSink.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void publishLogsEmptyMessageForEmptySnapshot() {
        PrometheusStatsSink sink = new PrometheusStatsSink("marketdata");
        appender.start();
        logger.addAppender(appender);

        sink.publish(new StatsSnapshot("snap", Map.of(), Map.of(), Map.of()));

        assertFalse(appender.list.isEmpty());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("(empty)"));
    }

    @Test
    void publishLogsPrometheusLinesWithSanitizedNameAndEscapedLabel() {
        PrometheusStatsSink sink = new PrometheusStatsSink("market-data");
        appender.start();
        logger.addAppender(appender);

        sink.publish(new StatsSnapshot(
                "snap\"name",
                Map.of("counter.one", 2L),
                Map.of("gauge-two", 3L),
                Map.of("lat.one", new StatsSnapshot.LatencySnapshot(1.25, 4.5))
        ));

        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
        assertTrue(logs.contains("# TYPE market_data_counter_one_total counter"));
        assertTrue(logs.contains("market_data_counter_one_total{snapshot=\"snap\\\"name\"} 2"));
        assertTrue(logs.contains("# TYPE market_data_gauge_two gauge"));
        assertTrue(logs.contains("market_data_lat_one_avg_ms{snapshot=\"snap\\\"name\"} 1.2500"));
        assertTrue(logs.contains("market_data_lat_one_max_ms{snapshot=\"snap\\\"name\"} 4.5000"));
    }
}
