package com.serhat.ecommerce.stockservice.service;

import com.serhat.ecommerce.stockservice.model.ProcessedReservation;
import com.serhat.ecommerce.stockservice.model.ReservationState;
import com.serhat.ecommerce.stockservice.repository.ProcessedReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Returns stock held by orders that never finished.
 *
 * <p>A saga can stall for reasons the compensation path never sees - a payment request
 * dead-lettered, a service down long enough to exhaust retries, an orchestrator crash. Any
 * of those previously left the reservation held forever, so the stock was subtracted from
 * what could be sold with nothing to ever give it back. This sweeper puts an upper bound on
 * how long a hold can survive without the order completing.
 *
 * <p>Only reservations still in {@code HELD} are eligible: a confirmed sale is never swept,
 * and an already-released one is skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSweeper {

    private static final int BATCH_SIZE = 100;

    private final ProcessedReservationRepository reservationRepository;
    private final StockService stockService;

    /** How long a hold may stand before it is assumed abandoned. */
    @Value("${ecommerce.stock.reservation-ttl-minutes:30}")
    private long reservationTtlMinutes;

    @Scheduled(fixedDelayString = "${ecommerce.stock.sweep-interval-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(reservationTtlMinutes));

        List<ProcessedReservation> expired = reservationRepository.lockExpired(
                ReservationState.HELD, cutoff, PageRequest.of(0, BATCH_SIZE));

        if (expired.isEmpty()) {
            return;
        }

        for (ProcessedReservation reservation : expired) {
            try {
                stockService.releaseForOrder(reservation.getOrderId());
            } catch (Exception e) {
                // One bad reservation must not stop the rest of the batch from being freed.
                log.error("Failed to release expired reservation for order {}",
                        reservation.getOrderId(), e);
            }
        }
        log.warn("Released {} stock reservation(s) abandoned for more than {} minutes",
                expired.size(), reservationTtlMinutes);
    }
}
