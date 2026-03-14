package com.example.marketdata.stats.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring scheduling support required by {@code StatsReporter}.
 * <p>
 * The reporter interval is controlled by {@code stats.reporter.fixed-rate-millis}
 * on the reporter itself.
 * </p>
 */
@Configuration
@EnableScheduling
public class StatsConfiguration {
    // Configuration is handled via @Component annotations on individual classes
    // This class enables Spring scheduling for @Scheduled methods
}
