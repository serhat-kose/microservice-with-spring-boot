package com.serhat.ecommerce.promotionservice.dto;

import com.serhat.ecommerce.promotionservice.model.Coupon;
import com.serhat.ecommerce.promotionservice.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CouponDtos {

    public record CouponRequest(
            @NotBlank @Size(max = 50) String code,
            @Size(max = 255) String description,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin("0.0") BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minimumOrderAmount,
            @NotNull Instant validFrom,
            @NotNull Instant validUntil,
            Integer usageLimit,
            Integer perUserLimit,
            Boolean active
    ) {}

    public record CouponResponse(
            Long id,
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minimumOrderAmount,
            Instant validFrom,
            Instant validUntil,
            Integer usageLimit,
            Integer perUserLimit,
            Integer usedCount,
            boolean active
    ) {
        public static CouponResponse from(Coupon c) {
            return new CouponResponse(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(),
                    c.getDiscountValue(), c.getMaxDiscountAmount(), c.getMinimumOrderAmount(),
                    c.getValidFrom(), c.getValidUntil(), c.getUsageLimit(), c.getPerUserLimit(),
                    c.getUsedCount(), c.isActive());
        }
    }

    public record ValidateRequest(
            @NotBlank String code,
            @NotNull @DecimalMin("0.0") BigDecimal orderAmount
    ) {}

    /**
     * Quote for a coupon against a basket. {@code valid == false} carries a human-readable
     * reason rather than an error status, because "this coupon has expired" is a normal
     * outcome of a shopper trying a code, not a failure.
     */
    public record ValidateResponse(
            boolean valid,
            String reason,
            String code,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
        public static ValidateResponse invalid(String code, String reason) {
            return new ValidateResponse(false, reason, code, BigDecimal.ZERO, null);
        }
    }

    public record RedeemRequest(
            @NotBlank String code,
            @NotNull @DecimalMin("0.0") BigDecimal orderAmount,
            @NotBlank String orderId
    ) {}

    public record PageResponse<T>(List<T> content, int page, int size,
                                  long totalElements, int totalPages, boolean last) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
