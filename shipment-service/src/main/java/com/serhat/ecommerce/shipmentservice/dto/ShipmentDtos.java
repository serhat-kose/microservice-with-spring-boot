package com.serhat.ecommerce.shipmentservice.dto;

import com.serhat.ecommerce.shipmentservice.model.Carrier;
import com.serhat.ecommerce.shipmentservice.model.Shipment;
import com.serhat.ecommerce.shipmentservice.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class ShipmentDtos {

    public record StatusUpdateRequest(@NotNull ShipmentStatus status) {}

    public record ShipmentResponse(
            String orderId,
            String trackingNumber,
            Carrier carrier,
            ShipmentStatus status,
            String deliveryAddress,
            String failureReason,
            Instant estimatedDelivery,
            Instant deliveredAt,
            Instant createdAt
    ) {
        public static ShipmentResponse from(Shipment s) {
            return new ShipmentResponse(s.getOrderId(), s.getTrackingNumber(), s.getCarrier(),
                    s.getStatus(), s.getDeliveryAddress(), s.getFailureReason(),
                    s.getEstimatedDelivery(), s.getDeliveredAt(), s.getCreatedAt());
        }
    }
}
