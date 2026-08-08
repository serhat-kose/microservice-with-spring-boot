package com.serhat.ecommerce.sagaorchestrator.orchestrator;

import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import com.serhat.ecommerce.sagaorchestrator.model.SagaStatus;
import com.serhat.ecommerce.sagaorchestrator.repository.SagaStateRepository;
import com.serhat.ecommerce.sagaorchestrator.service.SagaStateService;
import com.serhat.ecommerce.sagaorchestrator.service.SagaStateService.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Finds sagas that have stopped moving and closes them out.
 *
 * <p>Every step of the saga waits for a reply that may never arrive: a command can exhaust
 * its retries into a dead-letter topic, a service can be down long enough to miss it, the
 * orchestrator can crash between recording a step and publishing it. Any of those used to
 * leave the order stuck forever - the customer's cart was already cleared, stock stayed
 * reserved, no email was sent, and nothing would ever move it again. This puts an upper
 * bound on that: past the timeout the saga is compensated and the order is failed, so the
 * customer is told and the stock comes back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaWatchdog {

    private static final int BATCH_SIZE = 50;

    /** Steps that are waiting on a reply, and so can stall. */
    private static final Set<SagaStatus> IN_FLIGHT = EnumSet.of(
            SagaStatus.STARTED,
            SagaStatus.STOCK_RESERVATION_REQUESTED,
            SagaStatus.PAYMENT_REQUESTED,
            SagaStatus.SHIPMENT_REQUESTED,
            SagaStatus.FAILED_PAYMENT_COMPENSATING,
            SagaStatus.FAILED_SHIPMENT_COMPENSATING);

    private final SagaStateRepository sagaStateRepository;
    private final SagaStateService sagaStateService;
    private final Orchestrator orchestrator;

    @Value("${ecommerce.saga.timeout-minutes:15}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${ecommerce.saga.watchdog-interval-ms:60000}")
    @Transactional
    public void sweepStalledSagas() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));
        List<SagaState> stalled = sagaStateRepository.lockStalled(
                IN_FLIGHT, cutoff, PageRequest.of(0, BATCH_SIZE));

        if (stalled.isEmpty()) {
            return;
        }

        for (SagaState saga : stalled) {
            try {
                recover(saga);
            } catch (Exception e) {
                // One unrecoverable saga must not block the rest of the batch.
                log.error("Failed to recover stalled saga for order {}", saga.getOrderId(), e);
            }
        }
        log.warn("Recovered {} saga(s) stalled for more than {} minutes", stalled.size(), timeoutMinutes);
    }

    /**
     * Undoes whatever the saga had already done, based on how far it got. The step it is
     * stuck in tells us what needs compensating - stock is only held once reservation was
     * requested, and money is only taken once payment succeeded.
     */
    private void recover(SagaState saga) {
        String reason = "saga_timed_out";
        log.warn("Saga for order {} stalled in {} - compensating", saga.getOrderId(), saga.getStatus());

        switch (saga.getStatus()) {
            case STARTED ->
                // Nothing was reserved or charged; just fail the order.
                    sagaStateService.transition(saga.getOrderId(), SagaStatus.TIMED_OUT,
                            SagaContext.error(reason));
            case STOCK_RESERVATION_REQUESTED, PAYMENT_REQUESTED, FAILED_PAYMENT_COMPENSATING -> {
                // Stock may be held. Releasing is keyed by order and idempotent in
                // stock-service, so it is safe even if nothing was actually reserved.
                orchestrator.compensateStock(saga, reason);
                sagaStateService.transition(saga.getOrderId(), SagaStatus.TIMED_OUT,
                        SagaContext.error(reason));
            }
            case SHIPMENT_REQUESTED, FAILED_SHIPMENT_COMPENSATING -> {
                // Payment succeeded, so the money has to come back as well as the stock.
                orchestrator.compensateShipment(saga, reason);
                sagaStateService.transition(saga.getOrderId(), SagaStatus.TIMED_OUT,
                        SagaContext.error(reason));
            }
            default -> log.debug("Saga for order {} in {} needs no recovery",
                    saga.getOrderId(), saga.getStatus());
        }
    }
}
