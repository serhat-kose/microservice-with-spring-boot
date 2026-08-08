package com.serhat.ecommerce.promotionservice.repository;

import com.serhat.ecommerce.promotionservice.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    long countByCouponIdAndUserId(Long couponId, String userId);

    Optional<CouponRedemption> findByCouponIdAndOrderId(Long couponId, String orderId);

    Optional<CouponRedemption> findByOrderId(String orderId);
}
