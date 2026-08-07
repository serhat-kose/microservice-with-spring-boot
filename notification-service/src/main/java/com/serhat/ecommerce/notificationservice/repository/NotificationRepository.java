package com.serhat.ecommerce.notificationservice.repository;

import com.serhat.ecommerce.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Bounded by design so a long-lived account cannot pull an unbounded history in one call. */
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
}
