package com.serhat.ecommerce.reviewservice.repository;

/** Projection carrying a product's recomputed average rating and how many reviews it is based on. */
public record RatingAggregate(Double average, Long count) {

    public double averageOrZero() {
        return average == null ? 0.0 : average;
    }

    public long countOrZero() {
        return count == null ? 0L : count;
    }
}
