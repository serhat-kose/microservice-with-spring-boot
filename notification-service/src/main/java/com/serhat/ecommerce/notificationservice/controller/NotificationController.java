package com.serhat.ecommerce.notificationservice.controller;

import com.serhat.ecommerce.notificationservice.dto.NotificationPayload;
import com.serhat.ecommerce.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/order-event")
    public ResponseEntity<Void> handleOrderEvent(@RequestBody NotificationPayload payload) {
        notificationService.sendOrderNotification(payload);
        return ResponseEntity.accepted().build();
    }
}
