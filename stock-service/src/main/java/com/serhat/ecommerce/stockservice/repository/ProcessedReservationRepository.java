package com.serhat.ecommerce.stockservice.repository;

import com.serhat.ecommerce.stockservice.model.ProcessedReservation;
import com.serhat.ecommerce.stockservice.model.ReservationState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProcessedReservationRepository extends JpaRepository<ProcessedReservation, String> {

    /**
     * Reservations still held past their deadline, claimed for release.
     *
     * <p>Locked so that several stock-service replicas sweeping at the same time each take a
     * disjoint set rather than all releasing the same reservations - which would give the
     * stock back more than once.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM ProcessedReservation r
            WHERE r.state = :state AND r.reservedAt < :cutoff
            ORDER BY r.reservedAt ASC
            """)
    List<ProcessedReservation> lockExpired(@Param("state") ReservationState state,
                                           @Param("cutoff") Instant cutoff,
                                           Pageable pageable);
}
