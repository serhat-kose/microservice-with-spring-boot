package com.serhat.ecommerce.reviewservice.controller;

import com.serhat.ecommerce.commons.security.AuthenticatedUser;
import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.reviewservice.dto.ReviewDtos.*;
import com.serhat.ecommerce.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewService reviewService;

    /** Public: anyone browsing a product page sees its approved reviews. */
    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> listForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(PageResponse.of(
                reviewService.listForProduct(productId, pageable(page, size))));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewSummary> summary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.summary(productId));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<ReviewResponse>> myReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(PageResponse.of(
                reviewService.listMine(CurrentUser.requireUserId(), pageable(page, size))));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        AuthenticatedUser user = CurrentUser.get().orElseThrow();
        return new ResponseEntity<>(
                reviewService.create(user.userId(), user.username(), request), HttpStatus.CREATED);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long reviewId,
                                                  @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.update(CurrentUser.requireUserId(), reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable Long reviewId) {
        reviewService.delete(CurrentUser.requireUserId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/moderation/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(reviewService.listPendingModeration(pageable(page, size))));
    }

    @PutMapping("/{reviewId}/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> moderate(@PathVariable Long reviewId,
                                                    @Valid @RequestBody ModerationRequest request) {
        return ResponseEntity.ok(reviewService.moderate(reviewId, request.status()));
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
