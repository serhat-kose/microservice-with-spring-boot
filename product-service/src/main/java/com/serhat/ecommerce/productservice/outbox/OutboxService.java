package com.serhat.ecommerce.productservice.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;

    /**
     * Joins the caller's transaction ({@code MANDATORY}) on purpose: recording an event
     * outside the transaction that produced the change would reintroduce exactly the
     * dual-write problem the outbox exists to remove.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String messageKey, String payload) {
        repository.save(OutboxEvent.builder()
                .topic(topic)
                .messageKey(messageKey)
                .payload(payload)
                .build());
    }
}
