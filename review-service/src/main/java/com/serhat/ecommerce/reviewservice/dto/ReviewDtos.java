package com.serhat.ecommerce.reviewservice.dto;

import com.serhat.ecommerce.reviewservice.model.Review;
import com.serhat.ecommerce.reviewservice.model.ReviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ReviewDtos {

    public record ReviewRequest(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 255) String title,
            @Size(max = 4000) String body
    ) {}

    public record ReviewUpdateRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 255) String title,
            @Size(max = 4000) String body
    ) {}

    public record ModerationRequest(@NotNull ReviewStatus status) {}

    public record ReviewResponse(
            Long id,
            Long productId,
            String authorName,
            Integer rating,
            String title,
            String body,
            boolean verifiedPurchase,
            ReviewStatus status,
            Instant createdAt
    ) {
        public static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getProductId(), r.getAuthorName(), r.getRating(),
                    r.getTitle(), r.getBody(), r.isVerifiedPurchase(), r.getStatus(), r.getCreatedAt());
        }
    }

    /** Header widget on a product page: average, total, and the star distribution. */
    public record ReviewSummary(
            Long productId,
            double averageRating,
            long reviewCount,
            Map<Integer, Long> ratingBreakdown
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
