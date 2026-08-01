package com.serhat.ecommerce.stockservice.listener;

import com.serhat.ecommerce.stockservice.exception.InsufficientStockException;
import com.serhat.ecommerce.stockservice.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Malformed messages / infra errors (DB down, etc.) are intentionally left to
 * propagate to the container-level KafkaErrorHandlingConfig, which retries with
 * backoff and then routes to a dead-letter topic. Only InsufficientStockException -
 * an expected business outcome, not a failure - is handled locally here.
 */
@Slf4j
@Component
public class StockListener {
    private final StockService stockService;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public StockListener(StockService stockService, KafkaTemplate<String, String> kafka) {
        this.stockService = stockService;
        this.kafka = kafka;
    }

    @KafkaListener(topics = "stock-reserve-request", groupId = "stock-group")
    public void onReserveRequest(String message) throws Exception {
        Map<?, ?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        List<Map<String, Object>> items = mapper.convertValue(m.get("items"), List.class);

        try {
            stockService.reserveForOrder(orderId, items);
            String evt = mapper.writeValueAsString(Map.of(
                    "orderId", orderId, "userId", m.get("userId"), "items", items, "amount", m.get("amount")));
            kafka.send("stock-reserved", orderId, evt);
        } catch (InsufficientStockException e) {
            log.warn("Stock reservation failed for order {}: {}", orderId, e.getMessage());
            String evt = mapper.writeValueAsString(Map.of(
                    "orderId", orderId, "reason", "insufficient_stock", "userId", m.get("userId")));
            kafka.send("stock-reservation-failed", orderId, evt);
        }
    }

    @KafkaListener(topics = "stock-release", groupId = "stock-group")
    public void onRelease(String message) throws Exception {
        Map<?, ?> m = mapper.readValue(message, Map.class);
        List<Map<String, Object>> items = mapper.convertValue(m.get("items"), List.class);
        stockService.releaseItems(items);
    }
}
