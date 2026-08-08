package com.serhat.ecommerce.reviewservice.repository;

import com.serhat.ecommerce.reviewservice.model.VerifiedPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerifiedPurchaseRepository extends JpaRepository<VerifiedPurchase, Long> {

    boolean existsByUserIdAndProductId(String userId, Long productId);
}
