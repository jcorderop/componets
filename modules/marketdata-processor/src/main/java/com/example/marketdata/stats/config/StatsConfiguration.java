package com.example.marketdata.stats.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration for the statistics collection system.
 * Enables scheduled tasks for periodic stats reporting (every 1 minute).
 */
@Configuration
@EnableScheduling
public class StatsConfiguration {
    // Configuration is handled via @Component annotations on individual classes
    // This class enables Spring scheduling for @Scheduled methods
}
