package com.serhat.ecommerce.notificationservice.service;

import com.serhat.ecommerce.notificationservice.dto.NotificationPayload;
import com.serhat.ecommerce.notificationservice.dto.OrderItem;
import com.serhat.ecommerce.notificationservice.model.Notification;
import com.serhat.ecommerce.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${auth.service.url:http://auth-service:8081}")
    private String authServiceUrl;

    public NotificationService(JavaMailSender mailSender, RestTemplate restTemplate,
                                NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.restTemplate = restTemplate;
        this.notificationRepository = notificationRepository;
    }

    public void sendOrderNotification(NotificationPayload payload) {
        if (payload == null) {
            log.warn("Empty payload, nothing to notify.");
            return;
        }

        if ((payload.getEmail() == null || payload.getEmail().isEmpty()) && payload.getUserId() != null) {
            enrichFromAuthService(payload);
        }

        if (payload.getEmail() == null || payload.getEmail().isEmpty()) {
            log.warn("No email address available, skipping notification for order {}", payload.getOrderId());
            persist(payload, "SKIPPED_NO_EMAIL");
            return;
        }

        String subject = buildSubject(payload.getEventType(), payload.getOrderId());
        String body = buildBody(payload.getFirstName(), payload.getLastName(), payload.getOrderId(), payload.getItems());

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(payload.getEmail());
            msg.setSubject(subject);
            msg.setText(body);
            msg.setFrom("no-reply@ecommerce.local");
            mailSender.send(msg);
            log.info("Notification email sent to {} for order {}", payload.getEmail(), payload.getOrderId());
            persist(payload, "SENT");
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage(), e);
            persist(payload, "FAILED");
        }
    }

    private void enrichFromAuthService(NotificationPayload payload) {
        try {
            String url = authServiceUrl + "/api/users/" + payload.getUserId();
            String resp = restTemplate.getForObject(url, String.class);
            if (resp != null) {
                Map<String, Object> map = objectMapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                if (payload.getFirstName() == null) payload.setFirstName((String) map.get("firstName"));
                if (payload.getLastName() == null) payload.setLastName((String) map.get("lastName"));
                if (payload.getEmail() == null) payload.setEmail((String) map.get("email"));
            }
        } catch (RestClientException ex) {
            log.warn("auth-service lookup failed for user {}: {}", payload.getUserId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to parse auth-service response for user {}: {}", payload.getUserId(), ex.getMessage());
        }
    }

    private void persist(NotificationPayload payload, String status) {
        Notification n = Notification.builder()
                .eventType(payload.getEventType())
                .orderId(payload.getOrderId())
                .userId(payload.getUserId())
                .email(payload.getEmail())
                .status(status)
                .build();
        notificationRepository.save(n);
    }

    private String buildSubject(String eventType, String orderId) {
        if ("PAYMENT_COMPLETED".equalsIgnoreCase(eventType)) {
            return "Order " + orderId + " - Payment received";
        } else if ("RESERVATION_CREATED".equalsIgnoreCase(eventType)) {
            return "Order " + orderId + " - Reservation created";
        } else if ("ORDER_FAILED".equalsIgnoreCase(eventType)) {
            return "Order " + orderId + " - Failed";
        } else {
            return "Order " + orderId + " - Update";
        }
    }

    private String buildBody(String firstName, String lastName, String orderId, List<OrderItem> items) {
        String header = String.format("Hello %s %s,%n%nYour order number: %s%n%n",
                firstName == null ? "" : firstName,
                lastName == null ? "" : lastName,
                orderId == null ? "-" : orderId);

        String itemsText = "";
        if (items != null && !items.isEmpty()) {
            itemsText = "Items:\n" +
                    items.stream()
                            .map(i -> String.format("- %s (x%d) @ %s = %s",
                                    i.getName() == null ? i.getProductId() : i.getName(),
                                    i.getQuantity(),
                                    i.getPrice() == null ? "N/A" : i.getPrice().toString(),
                                    (i.getPrice() == null ? "N/A" : i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())).toString())))
                            .collect(Collectors.joining("\n"))
                    + "\n\n";
        }

        String footer = "Thanks,\nThe Ecommerce Team";

        return header + itemsText + footer;
    }
}
