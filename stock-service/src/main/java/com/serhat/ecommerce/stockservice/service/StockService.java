package com.serhat.ecommerce.stockservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.stockservice.exception.InsufficientStockException;
import com.serhat.ecommerce.stockservice.model.ProcessedReservation;
import com.serhat.ecommerce.stockservice.model.ReservationState;
import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.repository.ProcessedReservationRepository;
import com.serhat.ecommerce.stockservice.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class StockService {
    private final StockRepository repo;
    private final ProcessedReservationRepository processedReservationRepository;
    private final ObjectMapper objectMapper;

    public StockService(StockRepository repo,
                        ProcessedReservationRepository processedReservationRepository,
                        ObjectMapper objectMapper) {
        this.repo = repo;
        this.processedReservationRepository = processedReservationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Idempotent entry point for the saga's stock-reserve-request: if this orderId was
     * already reserved (a redelivered Kafka message under at-least-once semantics),
     * skips straight to true without decrementing stock again. Otherwise reserves and
     * records the orderId as processed in the same transaction as the decrements, so a
     * crash between "decrement" and "mark processed" can't happen.
     *
     * <p>What was reserved is stored on the record so it can be given back exactly - by the
     * saga's compensation or, if the saga is abandoned, by the TTL sweeper.
     *
     * @return true if reserved (either just now or previously); throws
     *         InsufficientStockException if this is a new reservation attempt that fails.
     */
    public boolean reserveForOrder(String orderId, List<Map<String, Object>> items) {
        if (processedReservationRepository.existsById(orderId)) {
            return true;
        }
        reserveItems(items);
        processedReservationRepository.save(ProcessedReservation.builder()
                .orderId(orderId)
                .reservedAt(Instant.now())
                .state(ReservationState.HELD)
                .reservedItems(serializeItems(items))
                .build());
        return true;
    }

    /**
     * Gives back a hold, keyed by order so it is safe to call more than once - the saga's
     * compensation and the TTL sweeper can both target the same reservation.
     */
    public void releaseForOrder(String orderId) {
        Optional<ProcessedReservation> found = processedReservationRepository.findById(orderId);
        if (found.isEmpty()) {
            return;
        }
        ProcessedReservation reservation = found.get();
        if (reservation.getState() != ReservationState.HELD) {
            return;
        }

        releaseItems(deserializeItems(reservation.getReservedItems()));
        reservation.setState(ReservationState.RELEASED);
        reservation.setReleasedAt(Instant.now());
        processedReservationRepository.save(reservation);
        log.info("Released stock held by order {}", orderId);
    }

    /**
     * Marks a hold as genuinely sold, so the TTL sweeper stops considering it in-flight.
     * Without this a completed order's reservation would eventually "expire" and hand its
     * stock back even though the goods were shipped.
     */
    public void confirmForOrder(String orderId) {
        processedReservationRepository.findById(orderId).ifPresent(reservation -> {
            if (reservation.getState() == ReservationState.HELD) {
                reservation.setState(ReservationState.CONFIRMED);
                processedReservationRepository.save(reservation);
            }
        });
    }

    private String serializeItems(List<Map<String, Object>> items) {
        List<Map<String, Object>> compact = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productId", productId(item));
            entry.put("quantity", quantity(item));
            compact.add(entry);
        }
        try {
            return objectMapper.writeValueAsString(compact);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to record reserved items for release", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deserializeItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.error("Could not read reserved items, stock cannot be returned automatically: {}", json, e);
            return List.of();
        }
    }

    /**
     * Reserves every item atomically per-row (see {@link StockRepository#decrementIfAvailable}).
     * If any item is unavailable, the whole method throws and the surrounding
     * @Transactional rolls back the decrements already applied to earlier items
     * in this same reservation.
     */
    public void reserveItems(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String productId = productId(item);
            int qty = quantity(item);
            int updated = repo.decrementIfAvailable(productId, qty);
            if (updated == 0) {
                throw new InsufficientStockException(productId);
            }
        }
    }

    public void releaseItems(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String productId = productId(item);
            int qty = quantity(item);
            int updated = repo.increment(productId, qty);
            if (updated == 0) {
                // product row no longer exists (deleted) - recreate it rather than losing the released stock
                repo.save(Stock.builder().productId(productId).quantity(qty).build());
            }
        }
    }

    /**
     * Upstream services model the product identifier as a numeric id, so after JSON
     * deserialisation into an untyped map the value arrives as an {@link Integer}/{@link Long},
     * not a {@link String}. Casting it directly to String threw ClassCastException on every
     * reservation, which dead-lettered the message and left the saga stuck with no
     * stock-reserved and no stock-reservation-failed ever published.
     */
    private String productId(Map<String, Object> item) {
        Object raw = item.get("productId");
        if (raw == null) {
            throw new IllegalArgumentException("Reservation item is missing productId");
        }
        return String.valueOf(raw);
    }

    private int quantity(Map<String, Object> item) {
        Object raw = item.get("quantity");
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("Reservation item has a non-numeric quantity: " + raw);
        }
        return number.intValue();
    }

    public Stock createStock(String productId, int quantity) {
        if (repo.existsByProductId(productId)) {
            throw new IllegalArgumentException("Product already exists: " + productId);
        }
        Stock stock = Stock.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
        return repo.save(stock);
    }

    public Optional<Stock> getByProductId(String productId) {
        return repo.findByProductId(productId);
    }

    public List<Stock> listAll() {
        return repo.findAll();
    }

    public boolean isAvailable(String productId, int requiredQuantity) {
        return repo.findByProductId(productId)
                .map(s -> s.getQuantity() != null && s.getQuantity() >= requiredQuantity)
                .orElse(false);
    }

    public Stock updateQuantity(String productId, int newQuantity) {
        Stock s = repo.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        s.setQuantity(newQuantity);
        return repo.save(s);
    }

    public void deleteByProductId(String productId) {
        if (!repo.existsByProductId(productId)) {
            throw new NoSuchElementException("Product not found: " + productId);
        }
        repo.deleteByProductId(productId);
    }

    /**
     * Removes a stock row without complaining if it was never there. Used when reacting to a
     * product being withdrawn: not every product has a stock record, and a missing one is
     * the desired end state anyway, so failing would only dead-letter a harmless event.
     */
    public void deleteQuietly(String productId) {
        if (repo.existsByProductId(productId)) {
            repo.deleteByProductId(productId);
            log.info("Removed stock for withdrawn product {}", productId);
        }
    }
}
