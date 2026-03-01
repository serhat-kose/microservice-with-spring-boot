package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDtos;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrderFromEvent(Map<String, Object> payload) {
        // beklenen payload: userId, items (list), total/amount
        String userId = (String) payload.get("userId");
        Object items = payload.get("items");
        BigDecimal amount = payload.get("total") instanceof Number
                ? BigDecimal.valueOf(((Number) payload.get("total")).doubleValue())
                : payload.get("amount") instanceof BigDecimal ? (BigDecimal) payload.get("amount") : BigDecimal.ZERO;

        try {
            String itemsJson = objectMapper.writeValueAsString(items);
            Order order = Order.builder()
                    .userId(userId)
                    .itemsJson(itemsJson)
                    .amount(amount)
                    .status("CREATED")
                    .createdAt(Instant.now())
                    .build();
            Order saved = orderRepository.save(order);

            // publish order-created event
            OrderDtos.OrderCreatedEvent event = new OrderDtos.OrderCreatedEvent(
                    saved.getId(),
                    saved.getUserId(),
                    objectMapper.readValue(saved.getItemsJson(), List.class),
                    saved.getAmount(),
                    saved.getStatus()
            );
            kafkaTemplate.send("order-created", event);
            return saved;
        } catch (Exception ex) {
            throw new RuntimeException("Order creation failed", ex);
        }
    }

    public Order getById(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    public List<Order> getByUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public void updateStatus(Long orderId, String status) {
        Order o = orderRepository.findById(orderId).orElseThrow();
        o.setStatus(status);
        orderRepository.save(o);
    }


    // --- Methods invoked by listeners ---

    /**
     * Mark order as completed. Accepts orderId as String (from event payload).
     */
    public void markCompleted(String orderId) {
        try {
            Long id = Long.valueOf(orderId);
            log.info("Marking order {} as COMPLETED", id);
            updateStatus(id, "COMPLETED");
            // optionally: publish an event or perform post-complete actions here
        } catch (NumberFormatException ex) {
            log.error("Invalid orderId passed to markCompleted: {}", orderId, ex);
            throw ex;
        }
    }

    /**
     * Mark order as failed with an optional reason.
     */
    public void markFailed(String orderId, String reason) {
        try {
            Long id = Long.valueOf(orderId);
            log.info("Marking order {} as FAILED (reason={})", id, reason);
            updateStatus(id, "FAILED");
            // optionally: publish failure reason to kafka or persist failure info
        } catch (NumberFormatException ex) {
            log.error("Invalid orderId passed to markFailed: {}", orderId, ex);
            throw ex;
        }
    }

    /**
     * Mark order as paid.
     */
    public void markPaid(String orderId) {
        try {
            Long id = Long.valueOf(orderId);
            log.info("Marking order {} as PAID", id);
            updateStatus(id, "PAID");
        } catch (NumberFormatException ex) {
            log.error("Invalid orderId passed to markPaid: {}", orderId, ex);
            throw ex;
        }
    }

    /**
     * Mark order as reserved (stock reserved).
     */
    public void markReserved(String orderId) {
        try {
            Long id = Long.valueOf(orderId);
            log.info("Marking order {} as RESERVED", id);
            updateStatus(id, "RESERVED");
        } catch (NumberFormatException ex) {
            log.error("Invalid orderId passed to markReserved: {}", orderId, ex);
            throw ex;
        }
    }
}
