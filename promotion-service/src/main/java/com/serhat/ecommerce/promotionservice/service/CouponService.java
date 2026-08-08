package com.serhat.ecommerce.promotionservice.service;

import com.serhat.ecommerce.promotionservice.dto.CouponDtos.*;
import com.serhat.ecommerce.promotionservice.model.Coupon;
import com.serhat.ecommerce.promotionservice.model.CouponRedemption;
import com.serhat.ecommerce.promotionservice.repository.CouponRedemptionRepository;
import com.serhat.ecommerce.promotionservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    @Transactional(readOnly = true)
    public Page<CouponResponse> list(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponResponse::from);
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.code().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
        }
        if (request.validUntil().isBefore(request.validFrom())) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        Coupon coupon = Coupon.builder()
                .code(request.code().toUpperCase())
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountAmount(request.maxDiscountAmount())
                .minimumOrderAmount(request.minimumOrderAmount())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .usageLimit(request.usageLimit())
                .perUserLimit(request.perUserLimit())
                .active(request.active() == null || request.active())
                .build();
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coupon not found: " + id));

        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMinimumOrderAmount(request.minimumOrderAmount());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setPerUserLimit(request.perUserLimit());
        if (request.active() != null) {
            coupon.setActive(request.active());
        }
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void deactivate(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coupon not found: " + id));
        // Deactivated rather than deleted so past redemptions still resolve their coupon.
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    /** Read-only quote: tells the shopper what a code would be worth without consuming it. */
    @Transactional(readOnly = true)
    public ValidateResponse validate(String userId, String code, BigDecimal orderAmount) {
        Optional<Coupon> found = couponRepository.findByCode(code.toUpperCase());
        if (found.isEmpty()) {
            return ValidateResponse.invalid(code, "Coupon not found");
        }
        Coupon coupon = found.get();

        String rejection = rejectionReason(coupon, userId, orderAmount);
        if (rejection != null) {
            return ValidateResponse.invalid(coupon.getCode(), rejection);
        }

        BigDecimal discount = coupon.discountFor(orderAmount);
        return new ValidateResponse(true, null, coupon.getCode(), discount, orderAmount.subtract(discount));
    }

    /**
     * Consumes the coupon for an order.
     *
     * <p>Idempotent on orderId: a checkout retried after a timeout returns the discount
     * already recorded instead of consuming a second use. The usage limit itself is enforced
     * by a conditional UPDATE, so concurrent checkouts cannot together exceed it.
     */
    @Transactional
    public ValidateResponse redeem(String userId, String code, BigDecimal orderAmount, String orderId) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new NoSuchElementException("Coupon not found: " + code));

        Optional<CouponRedemption> existing =
                redemptionRepository.findByCouponIdAndOrderId(coupon.getId(), orderId);
        if (existing.isPresent()) {
            BigDecimal discount = existing.get().getDiscountAmount();
            return new ValidateResponse(true, "Already redeemed for this order", coupon.getCode(),
                    discount, orderAmount.subtract(discount));
        }

        String rejection = rejectionReason(coupon, userId, orderAmount);
        if (rejection != null) {
            return ValidateResponse.invalid(coupon.getCode(), rejection);
        }

        if (couponRepository.consumeOneUse(coupon.getId()) == 0) {
            return ValidateResponse.invalid(coupon.getCode(), "Coupon usage limit reached");
        }

        BigDecimal discount = coupon.discountFor(orderAmount);
        redemptionRepository.save(CouponRedemption.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .orderId(orderId)
                .discountAmount(discount)
                .build());

        return new ValidateResponse(true, null, coupon.getCode(), discount, orderAmount.subtract(discount));
    }

    /**
     * Gives a redemption back when the order it belonged to fails, so a customer is not
     * charged a coupon use for an order that never completed.
     */
    @Transactional
    public void releaseForOrder(String orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            couponRepository.releaseOneUse(redemption.getCouponId());
            redemptionRepository.delete(redemption);
            log.info("Released coupon {} held by failed order {}", redemption.getCouponId(), orderId);
        });
    }

    /** @return why the coupon cannot be used, or null when it can. */
    private String rejectionReason(Coupon coupon, String userId, BigDecimal orderAmount) {
        if (!coupon.isActive()) {
            return "Coupon is not active";
        }
        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom())) {
            return "Coupon is not valid yet";
        }
        if (now.isAfter(coupon.getValidUntil())) {
            return "Coupon has expired";
        }
        if (coupon.getMinimumOrderAmount() != null
                && orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return "Order total is below the minimum of " + coupon.getMinimumOrderAmount();
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return "Coupon usage limit reached";
        }
        if (coupon.getPerUserLimit() != null
                && redemptionRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserLimit()) {
            return "You have already used this coupon";
        }
        return null;
    }
}
