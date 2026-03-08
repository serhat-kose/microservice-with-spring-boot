package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationPayload;
import com.example.notificationservice.dto.OrderItem;
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
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${auth.service.url:http://auth-service:8080}")
    private String authServiceUrl;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.restTemplate = new RestTemplate();
    }

    // eski basit loglama metodu korunuyor
    public void sendNotification(String message) {
        log.info("[Notification] {}", message);
    }

    // Yeni metot: sipariş olaylarında e-posta gönderimi
    public void sendOrderNotification(NotificationPayload payload) {
        if (payload == null) {
            log.warn("Boş payload, mail atılmayacak.");
            return;
        }

        // Eğer payload içinde doğrudan e-mail yoksa auth-service'ten almayı dene (userId varsa)
        if ((payload.getEmail() == null || payload.getEmail().isEmpty()) && payload.getUserId() != null) {
            try {
                String url = authServiceUrl + "/api/users/" + payload.getUserId();
                // beklenen dönen JSON: {"id":"...","firstName":"...","lastName":"...","email":"..."}
                var resp = restTemplate.getForObject(url, String.class);
                if (resp != null) {
                    var map = objectMapper.readValue(resp, new TypeReference<java.util.Map<String,Object>>(){});
                    if (payload.getFirstName() == null) payload.setFirstName((String) map.getOrDefault("firstName", null));
                    if (payload.getLastName() == null) payload.setLastName((String) map.getOrDefault("lastName", null));
                    if (payload.getEmail() == null) payload.setEmail((String) map.getOrDefault("email", null));
                }
            } catch (RestClientException ex) {
                log.warn("Auth service çağrısı başarısız: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Auth service dönüşünün parse edilmesinde hata: {}", ex.getMessage());
            }
        }

        if (payload.getEmail() == null || payload.getEmail().isEmpty()) {
            log.warn("E-posta adresi yok, mail gönderimi atlandı. payload={}", payload);
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
        } catch (Exception e) {
            log.error("Mail gönderimi sırasında hata: {}", e.getMessage(), e);
        }
    }

    private String buildSubject(String eventType, String orderId) {
        if ("PAYMENT_COMPLETED".equalsIgnoreCase(eventType)) {
            return "Sipariş " + orderId + " - Ödeme Alındı";
        } else if ("RESERVATION_CREATED".equalsIgnoreCase(eventType)) {
            return "Sipariş " + orderId + " - Rezervasyon Oluşturuldu";
        } else {
            return "Sipariş " + orderId + " - Bilgilendirme";
        }
    }

    private String buildBody(String firstName, String lastName, String orderId, List<OrderItem> items) {
        String header = String.format("Merhaba %s %s,%n%nSipariş Numaranız: %s%n%n",
                firstName == null ? "" : firstName,
                lastName == null ? "" : lastName,
                orderId == null ? "-" : orderId);

        String itemsText = "";
        if (items != null && !items.isEmpty()) {
            itemsText = "Ürün Detayları:\n" +
                    items.stream()
                            .map(i -> String.format("- %s (x%d) @ %s = %s",
                                    i.getName() == null ? i.getProductId() : i.getName(),
                                    i.getQuantity(),
                                    i.getPrice() == null ? "N/A" : i.getPrice().toString(),
                                    (i.getPrice() == null ? "N/A" : i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())).toString())))
                            .collect(Collectors.joining("\n"))
                    + "\n\n";
        }

        String footer = "Teşekkürler,\nE-Ticaret Takımı";

        return header + itemsText + footer;
    }
}
