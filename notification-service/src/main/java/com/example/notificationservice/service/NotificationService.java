package com.example.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendNotification(String message) {
        // In real app: send email/push/SMS. For demo, just log.
        log.info("[Notification] {}", message);
    }
}
