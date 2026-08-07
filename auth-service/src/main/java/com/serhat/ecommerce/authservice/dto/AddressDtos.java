package com.serhat.ecommerce.authservice.dto;

import com.serhat.ecommerce.authservice.model.UserAddress;
import jakarta.validation.constraints.NotBlank;

public class AddressDtos {

    public record AddressRequest(
            @NotBlank String title,
            @NotBlank String recipientName,
            @NotBlank String phone,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            String district,
            @NotBlank String postalCode,
            @NotBlank String country,
            boolean defaultAddress
    ) {}

    public record AddressResponse(
            Long id,
            String title,
            String recipientName,
            String phone,
            String line1,
            String line2,
            String city,
            String district,
            String postalCode,
            String country,
            boolean defaultAddress
    ) {
        public static AddressResponse from(UserAddress a) {
            return new AddressResponse(a.getId(), a.getTitle(), a.getRecipientName(), a.getPhone(),
                    a.getLine1(), a.getLine2(), a.getCity(), a.getDistrict(), a.getPostalCode(),
                    a.getCountry(), a.isDefaultAddress());
        }
    }
}
