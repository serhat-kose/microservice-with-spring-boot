package com.serhat.ecommerce.sagaorchestrator.service;

import com.serhat.ecommerce.sagaorchestrator.model.ProcessedEvent;
import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import com.serhat.ecommerce.sagaorchestrator.model.SagaStatus;
import com.serhat.ecommerce.sagaorchestrator.repository.ProcessedEventRepository;
import com.serhat.ecommerce.sagaorchestrator.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaStateService {

    private final SagaStateRepository sagaStateRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Whether this event has already been applied.
     *
     * <p>Checked before doing any work, and the marker is written in the same transaction as
     * the step - so a crash cannot leave the step applied but the event unrecorded, which
     * would let a redelivery repeat it.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public boolean alreadyProcessed(String eventId) {
        return eventId != null && processedEventRepository.existsById(eventId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markProcessed(String eventId, String eventType, String orderId) {
        if (eventId == null) {
            return;
        }
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .orderId(orderId)
                .processedAt(Instant.now())
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Optional<SagaState> find(String orderId) {
        return sagaStateRepository.findById(orderId);
    }

    /**
     * Moves the saga on, refusing transitions that would take it backwards or out of a
     * terminal state.
     *
     * @return the saga if the move was applied, or empty if it was refused - callers use
     *         this to decide whether to publish the next command, so a refused transition
     *         also suppresses the message that would have followed it.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<SagaState> transition(String orderId, SagaStatus target, SagaContext context) {
        SagaState state = sagaStateRepository.findById(orderId).orElseGet(() -> SagaState.builder()
                .orderId(orderId)
                .status(SagaStatus.STARTED)
                .build());

        if (state.getStatus() != null && !state.getStatus().canTransitionTo(target)) {
            log.warn("Refusing saga transition for order {}: {} -> {}", orderId, state.getStatus(), target);
            return Optional.empty();
        }

        // Context is only ever filled in, never blanked, so a later event that omits the
        // amount or items cannot erase what an earlier one recorded - the compensation
        // paths depend on those values still being there.
        if (context != null) {
            if (context.userId() != null) state.setUserId(context.userId());
            if (context.amount() != null) state.setAmount(context.amount());
            if (context.items() != null) state.setItems(context.items());
            if (context.error() != null) state.setLastError(context.error());
        }

        state.setStatus(target);
        return Optional.of(sagaStateRepository.save(state));
    }

    /** Fields carried on the saga so compensation does not depend on the failing event. */
    public record SagaContext(String userId, String amount, String items, String error) {

        public static SagaContext of(String userId, String amount, String items) {
            return new SagaContext(userId, amount, items, null);
        }

        public static SagaContext error(String message) {
            return new SagaContext(null, null, null, message);
        }

        public static SagaContext empty() {
            return new SagaContext(null, null, null, null);
        }
    }
}
