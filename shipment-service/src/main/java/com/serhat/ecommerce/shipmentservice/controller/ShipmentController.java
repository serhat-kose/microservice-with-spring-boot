package com.serhat.ecommerce.shipmentservice.controller;

import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.shipmentservice.dto.ShipmentDtos.ShipmentResponse;
import com.serhat.ecommerce.shipmentservice.dto.ShipmentDtos.StatusUpdateRequest;
import com.serhat.ecommerce.shipmentservice.model.Shipment;
import com.serhat.ecommerce.shipmentservice.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Parcel tracking. There was no way to look a shipment up at all before - the service
 * generated an id and discarded it.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> track(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.trackByNumber(trackingNumber);
        requireOwnerOrStaff(shipment);
        return ResponseEntity.ok(ShipmentResponse.from(shipment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> byOrder(@PathVariable String orderId) {
        Shipment shipment = shipmentService.findByOrder(orderId);
        requireOwnerOrStaff(shipment);
        return ResponseEntity.ok(ShipmentResponse.from(shipment));
    }

    /** Carrier-facing: moves the parcel along its lifecycle. */
    @PutMapping("/track/{trackingNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponse> updateStatus(@PathVariable String trackingNumber,
                                                          @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                ShipmentResponse.from(shipmentService.updateStatus(trackingNumber, request.status())));
    }

    private void requireOwnerOrStaff(Shipment shipment) {
        var user = CurrentUser.get().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));
        if (user.hasRole("ADMIN") || user.userId().equals(shipment.getUserId())) {
            return;
        }
        // 404 rather than 403 so a tracking number cannot be probed for existence.
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found");
    }
}
