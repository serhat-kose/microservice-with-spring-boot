package com.serhat.ecommerce.shipmentservice.service;

import com.serhat.ecommerce.shipmentservice.model.Carrier;
import com.serhat.ecommerce.shipmentservice.model.Shipment;
import com.serhat.ecommerce.shipmentservice.model.ShipmentStatus;
import com.serhat.ecommerce.shipmentservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Value("${ecommerce.shipment.estimated-days:3}")
    private long estimatedDays;

    /**
     * Books a shipment for an order, at most once.
     *
     * <p>An existing shipment is returned unchanged so a redelivered
     * {@code shipment-request} does not dispatch the parcel twice.
     *
     * <p>A missing delivery address fails the shipment rather than proceeding: the saga
     * previously sent the literal string "default" as the destination and the service
     * ignored it entirely, so an order could be "shipped" nowhere.
     */
    @Transactional
    public Shipment schedule(String orderId, String userId, String deliveryAddress) {
        Optional<Shipment> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (!StringUtils.hasText(deliveryAddress) || "default".equals(deliveryAddress)) {
            Shipment failed = shipmentRepository.save(Shipment.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .trackingNumber(generateTrackingNumber())
                    .carrier(Carrier.STANDARD_POST)
                    .status(ShipmentStatus.FAILED)
                    .failureReason("missing_delivery_address")
                    .build());
            log.warn("Shipment for order {} failed: no usable delivery address", orderId);
            return failed;
        }

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .userId(userId)
                .trackingNumber(generateTrackingNumber())
                .carrier(Carrier.STANDARD_POST)
                .status(ShipmentStatus.SCHEDULED)
                .deliveryAddress(deliveryAddress)
                .estimatedDelivery(Instant.now().plus(Duration.ofDays(estimatedDays)))
                .build();

        return shipmentRepository.save(shipment);
    }

    /** Advances a parcel, refusing moves that would run its history backwards. */
    @Transactional
    public Shipment updateStatus(String trackingNumber, ShipmentStatus target) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found: " + trackingNumber));

        if (!shipment.getStatus().canTransitionTo(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot move shipment from " + shipment.getStatus() + " to " + target);
        }

        shipment.setStatus(target);
        if (target == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(Instant.now());
        }
        return shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public Shipment trackByNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found: " + trackingNumber));
    }

    @Transactional(readOnly = true)
    public Shipment findByOrder(String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No shipment for order: " + orderId));
    }

    private String generateTrackingNumber() {
        return "TRK" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
