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

class LoggerStatsSinkServiceTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggerStatsSinkService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void publishLogsEmptySnapshotMessage() {
        LoggerStatsSinkService sink = new LoggerStatsSinkService();
        appender.start();
        logger.addAppender(appender);

        sink.publish(new StatsSnapshot("snap", Map.of(), Map.of(), Map.of()));

        assertFalse(appender.list.isEmpty());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("(empty)"));
    }

    @Test
    void publishLogsCountersGaugesAndLatencies() {
        LoggerStatsSinkService sink = new LoggerStatsSinkService();
        appender.start();
        logger.addAppender(appender);

        sink.publish(new StatsSnapshot(
                "snap",
                Map.of("c1", 2L),
                Map.of("g1", 3L),
                Map.of("l1", new StatsSnapshot.LatencySnapshot(1.5, 4.0))
        ));

        String combined = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
        assertTrue(combined.contains("counter.c1 = 2"));
        assertTrue(combined.contains("gauge.g1 = 3"));
        assertTrue(combined.contains("latency.l1"));
    }
}
