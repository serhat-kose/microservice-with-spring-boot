package com.serhat.ecommerce.notificationservice.controller;

import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.notificationservice.dto.NotificationDtos.NotificationResponse;
import com.serhat.ecommerce.notificationservice.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only notification history for the authenticated caller.
 *
 * <p>The previous {@code POST /order-event} endpoint was removed: it was unauthenticated and
 * sent an email to any address in the request body with attacker-controlled content, i.e. an
 * open relay. Notifications are now only ever raised by consuming domain events from Kafka,
 * which is the only trustworthy trigger.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> myNotifications() {
        return ResponseEntity.ok(
                notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(CurrentUser.requireUserId()).stream()
                        .map(NotificationResponse::from)
                        .toList());
    }
}
