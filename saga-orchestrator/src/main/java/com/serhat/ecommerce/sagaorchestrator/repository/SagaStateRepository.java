package com.serhat.ecommerce.sagaorchestrator.repository;

import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaStateRepository extends JpaRepository<SagaState, String> {
}
