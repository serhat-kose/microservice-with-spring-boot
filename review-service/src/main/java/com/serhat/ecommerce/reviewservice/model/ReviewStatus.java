package com.serhat.ecommerce.reviewservice.model;

public enum ReviewStatus {
    /** Awaiting moderation; not shown publicly and not counted in the average. */
    PENDING,
    /** Visible and counted. */
    APPROVED,
    /** Rejected by moderation; retained so the author is not able to simply resubmit. */
    REJECTED
}
