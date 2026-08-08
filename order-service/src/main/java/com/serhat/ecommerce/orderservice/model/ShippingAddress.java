package com.serhat.ecommerce.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The delivery address as it stood when the order was placed.
 *
 * <p>Embedded as a snapshot rather than referencing an address-book row in auth-service:
 * a customer editing or deleting a saved address must not retroactively change where a past
 * order was sent, and the shipment step must not depend on auth-service being reachable.
 *
 * <p>Before this, the saga sent the literal string {@code "default"} as the address on every
 * shipment request.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress {

    @Column(name = "ship_recipient_name")
    private String recipientName;

    @Column(name = "ship_phone")
    private String phone;

    @Column(name = "ship_line1")
    private String line1;

    @Column(name = "ship_line2")
    private String line2;

    @Column(name = "ship_city")
    private String city;

    @Column(name = "ship_district")
    private String district;

    @Column(name = "ship_postal_code")
    private String postalCode;

    @Column(name = "ship_country")
    private String country;

    public String singleLine() {
        StringBuilder sb = new StringBuilder();
        if (line1 != null) sb.append(line1);
        if (line2 != null && !line2.isBlank()) sb.append(", ").append(line2);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (city != null) sb.append(", ").append(city);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }
}
