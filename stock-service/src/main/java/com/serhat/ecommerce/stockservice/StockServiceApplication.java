package com.serhat.ecommerce.stockservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling is enabled for the sweeper that releases abandoned stock reservations. */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class StockServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockServiceApplication.class, args);
    }
}
