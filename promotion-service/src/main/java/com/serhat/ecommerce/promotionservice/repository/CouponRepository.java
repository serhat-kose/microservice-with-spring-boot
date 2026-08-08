package com.serhat.ecommerce.promotionservice.repository;

import com.serhat.ecommerce.promotionservice.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    Page<Coupon> findByActiveTrue(Pageable pageable);

    /**
     * Consumes one use of a coupon, but only while uses remain.
     *
     * <p>A single conditional UPDATE rather than read-check-write: under concurrent
     * checkouts the read-then-write pattern lets a limited coupon be redeemed more times
     * than its limit allows, because every request reads the same pre-increment count.
     * Returns the number of rows changed - zero means the limit was already reached.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Coupon c SET c.usedCount = c.usedCount + 1
            WHERE c.id = :id
              AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)
            """)
    int consumeOneUse(@Param("id") Long id);

    /** Compensating decrement used when an order that redeemed a coupon later fails. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount - 1 WHERE c.id = :id AND c.usedCount > 0")
    int releaseOneUse(@Param("id") Long id);
}
