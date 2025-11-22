package com.example;

import com.example.demo.MarketDataMessage;
import com.example.marketdata.model.MarketDataEvent;
import com.example.marketdata.service.MarketDataHandlerService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public ApplicationRunner marketDataStartupPublisher(MarketDataHandlerService marketDataHandlerService) {
        return args -> {
            for (int i = 0; i < 100; i++) {
                MarketDataEvent event = MarketDataMessage.builder()
                        .source("MAX")
                        .symbol("IBM")
                        .price(1.25)
                        .size(1_000_000)
                        .timestamp(Instant.now())
                        .build();
                marketDataHandlerService.onEvent(event);
                Thread.sleep(1000);
            }

        };
    }

}
