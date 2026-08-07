package com.serhat.ecommerce.productservice.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claims a batch of pending events for this relay instance.
     *
     * <p>{@code PESSIMISTIC_WRITE} with {@code SKIP LOCKED} lets several service replicas
     * drain the same table concurrently: each grabs a disjoint batch instead of all of them
     * fighting over - and re-publishing - the same rows.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT e FROM OutboxEvent e WHERE e.published = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> lockPendingBatch(Pageable pageable);

    long countByPublishedFalse();

    void deleteByPublishedTrueAndPublishedAtBefore(Instant cutoff);
}
