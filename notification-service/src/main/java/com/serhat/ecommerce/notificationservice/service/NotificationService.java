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
        String body = buildBody(payload);

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
        return switch (eventType == null ? "" : eventType.toUpperCase()) {
            case "ORDER_COMPLETED" -> "Order " + orderId + " - Confirmed";
            case "ORDER_FAILED" -> "Order " + orderId + " - Could not be completed";
            case "SHIPMENT_SCHEDULED" -> "Order " + orderId + " - On its way";
            default -> "Order " + orderId + " - Update";
        };
    }

    /**
     * Body for the notification. Each event type says something specific rather than all of
     * them sharing one generic "here is your order" text, so an ORDER_FAILED email actually
     * explains what went wrong and a shipment email carries the tracking number.
     */
    private String buildBody(NotificationPayload payload) {
        StringBuilder body = new StringBuilder();
        body.append(String.format("Hello %s %s,%n%n",
                payload.getFirstName() == null ? "" : payload.getFirstName(),
                payload.getLastName() == null ? "" : payload.getLastName()).trim().replace(" ,", ","));
        body.append(System.lineSeparator()).append(System.lineSeparator());

        String orderId = payload.getOrderId() == null ? "-" : payload.getOrderId();
        String eventType = payload.getEventType() == null ? "" : payload.getEventType().toUpperCase();

        switch (eventType) {
            case "ORDER_COMPLETED" -> body.append("Your order ").append(orderId)
                    .append(" is confirmed and being prepared.").append(System.lineSeparator());
            case "ORDER_FAILED" -> {
                body.append("We were unable to complete your order ").append(orderId).append(".")
                        .append(System.lineSeparator());
                if (payload.getReason() != null) {
                    body.append("Reason: ").append(humanReason(payload.getReason()))
                            .append(System.lineSeparator());
                }
                body.append("Any amount reserved has been released back to you.")
                        .append(System.lineSeparator());
            }
            case "SHIPMENT_SCHEDULED" -> {
                body.append("Your order ").append(orderId).append(" has been handed to the carrier.")
                        .append(System.lineSeparator());
                if (payload.getTrackingNumber() != null) {
                    body.append("Tracking number: ").append(payload.getTrackingNumber())
                            .append(System.lineSeparator());
                }
                if (payload.getCarrier() != null) {
                    body.append("Carrier: ").append(payload.getCarrier()).append(System.lineSeparator());
                }
            }
            default -> body.append("There is an update on your order ").append(orderId).append(".")
                    .append(System.lineSeparator());
        }

        List<OrderItem> items = payload.getItems();
        if (items != null && !items.isEmpty()) {
            body.append(System.lineSeparator()).append("Items:").append(System.lineSeparator());
            body.append(items.stream()
                    .map(i -> String.format("- %s (x%d) @ %s = %s",
                            i.getName() == null ? i.getProductId() : i.getName(),
                            i.getQuantity(),
                            i.getPrice() == null ? "N/A" : i.getPrice().toString(),
                            i.getPrice() == null ? "N/A"
                                    : i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())).toString()))
                    .collect(Collectors.joining(System.lineSeparator())));
            body.append(System.lineSeparator());
        }

        body.append(System.lineSeparator()).append("Thanks,").append(System.lineSeparator())
                .append("The Ecommerce Team");
        return body.toString();
    }

    /** Turns an internal failure code into something a customer can read. */
    private String humanReason(String reason) {
        return switch (reason) {
            case "insufficient_stock" -> "one or more items sold out before payment completed";
            case "insufficient_funds" -> "the payment was declined";
            case "amount_exceeds_limit" -> "the payment exceeded the allowed limit";
            case "missing_delivery_address" -> "the delivery address was incomplete";
            case "saga_timed_out" -> "the order could not be processed in time";
            default -> reason.replace('_', ' ');
        };
    }
}
