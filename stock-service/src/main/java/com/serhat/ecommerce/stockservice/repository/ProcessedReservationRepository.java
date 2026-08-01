package com.serhat.ecommerce.stockservice.repository;

import com.serhat.ecommerce.stockservice.model.ProcessedReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedReservationRepository extends JpaRepository<ProcessedReservation, String> {
}
