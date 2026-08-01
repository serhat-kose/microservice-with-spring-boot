package com.serhat.ecommerce.notificationservice.repository;

import com.serhat.ecommerce.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
