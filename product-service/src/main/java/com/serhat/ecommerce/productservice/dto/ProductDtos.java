package com.serhat.ecommerce.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductDtos {

    public record ProductRequest(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            BigDecimal price
    ) {}
}
