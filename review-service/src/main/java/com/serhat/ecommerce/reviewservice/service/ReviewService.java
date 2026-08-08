package com.serhat.ecommerce.reviewservice.service;

import com.serhat.ecommerce.reviewservice.dto.ReviewDtos.*;
import com.serhat.ecommerce.reviewservice.event.RatingEventPublisher;
import com.serhat.ecommerce.reviewservice.model.Review;
import com.serhat.ecommerce.reviewservice.model.ReviewStatus;
import com.serhat.ecommerce.reviewservice.repository.RatingAggregate;
import com.serhat.ecommerce.reviewservice.repository.ReviewRepository;
import com.serhat.ecommerce.reviewservice.repository.VerifiedPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final VerifiedPurchaseRepository verifiedPurchaseRepository;
    private final RatingEventPublisher ratingEventPublisher;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listMine(String userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable).map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(Long productId) {
        RatingAggregate aggregate = reviewRepository.aggregateFor(productId);

        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            breakdown.put(star, 0L);
        }
        for (Object[] row : reviewRepository.ratingBreakdown(productId)) {
            breakdown.put((Integer) row[0], (Long) row[1]);
        }

        return new ReviewSummary(productId, round(aggregate.averageOrZero()),
                aggregate.countOrZero(), breakdown);
    }

    /**
     * Only a customer whose completed order contained this product may review it, which is
     * what keeps the rating meaningful - without the check, ratings can be manufactured by
     * throwaway accounts.
     */
    @Transactional
    public ReviewResponse create(String userId, String authorName, ReviewRequest request) {
        if (!verifiedPurchaseRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only review a product from a completed order");
        }
        reviewRepository.findByProductIdAndUserId(request.productId(), userId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reviewed this product");
        });

        Review review = Review.builder()
                .productId(request.productId())
                .userId(userId)
                .authorName(authorName)
                .rating(request.rating())
                .title(request.title())
                .body(request.body())
                .verifiedPurchase(true)
                .status(ReviewStatus.APPROVED)
                .build();

        Review saved = reviewRepository.save(review);
        publishRating(saved.getProductId());
        return ReviewResponse.from(saved);
    }

    @Transactional
    public ReviewResponse update(String userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setBody(request.body());

        Review saved = reviewRepository.save(review);
        publishRating(saved.getProductId());
        return ReviewResponse.from(saved);
    }

    @Transactional
    public void delete(String userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));
        Long productId = review.getProductId();
        reviewRepository.delete(review);
        publishRating(productId);
    }

    @Transactional
    public ReviewResponse moderate(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found: " + reviewId));
        review.setStatus(status);
        Review saved = reviewRepository.save(review);
        publishRating(saved.getProductId());
        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listPendingModeration(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable).map(ReviewResponse::from);
    }

    /** Records that a customer received a product, making them eligible to review it. */
    @Transactional
    public void recordPurchase(String userId, Long productId, String orderId) {
        if (verifiedPurchaseRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        verifiedPurchaseRepository.save(com.serhat.ecommerce.reviewservice.model.VerifiedPurchase.builder()
                .userId(userId)
                .productId(productId)
                .orderId(orderId)
                .build());
    }

    /**
     * Recomputed from the rows and published so product-service and search-service can keep
     * their denormalised copies in step.
     */
    private void publishRating(Long productId) {
        RatingAggregate aggregate = reviewRepository.aggregateFor(productId);
        ratingEventPublisher.publishRatingUpdated(productId, round(aggregate.averageOrZero()),
                aggregate.countOrZero());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
