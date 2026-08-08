package com.serhat.ecommerce.sagaorchestrator.repository;

import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import com.serhat.ecommerce.sagaorchestrator.model.SagaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface SagaStateRepository extends JpaRepository<SagaState, String> {

    /**
     * Sagas that have sat in a non-terminal step past the deadline.
     *
     * <p>Locked so replicas running the watchdog concurrently each claim a disjoint batch,
     * rather than all deciding to compensate the same stuck orders.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM SagaState s
            WHERE s.status IN :statuses AND s.updatedAt < :cutoff
            ORDER BY s.updatedAt ASC
            """)
    List<SagaState> lockStalled(@Param("statuses") Collection<SagaStatus> statuses,
                                @Param("cutoff") Instant cutoff,
                                Pageable pageable);

    long countByStatusIn(Collection<SagaStatus> statuses);
}
