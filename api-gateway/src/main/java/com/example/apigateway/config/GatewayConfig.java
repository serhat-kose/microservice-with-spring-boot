package com.example.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // auth-service (bypass token check)
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.rewritePath("/api/auth/(?<segment>.*)", "/api/auth/${segment}"))
                        .uri("http://auth-service:8081"))
                // order-service
                .route("order-service", r -> r.path("/api/orders/**")
                        .uri("http://order-service:8083"))
                // stock-service
                .route("stock-service", r -> r.path("/api/stocks/**")
                        .uri("http://stock-service:8084"))
                // notification-service
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("http://notification-service:8085"))
                .build();
    }
}

