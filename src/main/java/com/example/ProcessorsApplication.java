package com.example;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.service.ProcessorsHandlerService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;

/**
 * Spring Boot entry point that wires the demo application and triggers event publication
 * on startup so downstream processors receive a steady stream of sample market data.
 */
@SpringBootApplication
@EnableScheduling
public class ProcessorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessorsApplication.class, args);
    }

    @Bean
    public ApplicationRunner marketDataStartupPublisher(ProcessorsHandlerService processorsHandlerService) {
        return args -> {
            double price = 1.0;
            for (int i = 0; i < 100; i++) {
                price = price + (i/10.0);
                MarketDataEvent event = MarketDataMessage.builder()
                        .source("MAX")
                        .symbol("IBM")
                        .price(price)
                        .size(1_000_000)
                        .timestamp(Instant.now())
                        .build();
                processorsHandlerService.onEvent(event);
                Thread.sleep(1000);
            }

        };
    }

}
