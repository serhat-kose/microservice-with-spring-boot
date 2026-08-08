package com.serhat.ecommerce.reviewservice.repository;

import com.serhat.ecommerce.reviewservice.model.Review;
import com.serhat.ecommerce.reviewservice.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserId(String userId, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(Long productId, String userId);

    Optional<Review> findByIdAndUserId(Long id, String userId);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    /**
     * Recomputes a product's rating from scratch in the database.
     *
     * <p>Aggregating on demand rather than keeping a running counter means a moderation
     * change or a deleted review can never leave the average drifting from the underlying
     * rows - the alternative, incrementing a stored total, silently accumulates error.
     * Only verified, approved reviews count.
     */
    @Query("""
            SELECT new com.serhat.ecommerce.reviewservice.repository.RatingAggregate(
                COALESCE(AVG(CAST(r.rating AS double)), 0.0), COUNT(r))
            FROM Review r
            WHERE r.productId = :productId
              AND r.status = com.serhat.ecommerce.reviewservice.model.ReviewStatus.APPROVED
              AND r.verifiedPurchase = true
            """)
    RatingAggregate aggregateFor(@Param("productId") Long productId);

    /** Rating distribution (how many 5-star, 4-star, ...) for the review summary widget. */
    @Query("""
            SELECT r.rating, COUNT(r)
            FROM Review r
            WHERE r.productId = :productId
              AND r.status = com.serhat.ecommerce.reviewservice.model.ReviewStatus.APPROVED
            GROUP BY r.rating
            """)
    List<Object[]> ratingBreakdown(@Param("productId") Long productId);
}
