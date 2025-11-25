package com.example.marketdata.adapter.hazelcast.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that provisions the Hazelcast instance used by adapters and buffers
 * to store market data snapshots.
 */
@Configuration
public class HazelcastConfiguration {

    private HazelcastInstance hazelcastInstance;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("market-data-hazelcast-instance");
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        return hazelcastInstance;
    }

    @PreDestroy
    public void shutdownHazelcastInstance() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }
}
