package com.serhat.ecommerce.shipmentservice.repository;

import com.serhat.ecommerce.shipmentservice.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(String orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
