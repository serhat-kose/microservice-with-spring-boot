package com.serhat.ecommerce.sagaorchestrator.repository;

import com.serhat.ecommerce.sagaorchestrator.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    /** Keeps the dedupe table bounded; entries older than any plausible redelivery window. */
    void deleteByProcessedAtBefore(Instant cutoff);
}
