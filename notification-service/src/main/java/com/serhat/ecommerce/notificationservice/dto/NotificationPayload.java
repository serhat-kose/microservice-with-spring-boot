package com.serhat.ecommerce.notificationservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationPayload {

    private String eventType;
    private String orderId;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private List<OrderItem> items;

    /** Why the order failed, so the customer is told something more useful than "failed". */
    private String reason;

    /** Present on shipment notifications. */
    private String trackingNumber;
    private String carrier;
}
