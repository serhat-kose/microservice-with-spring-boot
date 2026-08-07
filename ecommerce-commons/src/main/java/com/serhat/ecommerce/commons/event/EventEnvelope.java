package com.serhat.ecommerce.commons.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Wrapper carried by every domain event on Kafka.
 *
 * <p>{@code eventId} is the idempotency key: consumers record the ids they have already
 * applied and skip repeats, which matters because Kafka only guarantees at-least-once
 * delivery (a rebalance or a retry after a transient failure replays the message).
 * {@code correlationId} ties every event produced while handling one user action together
 * so a checkout can be followed end to end across services.
 *
 * @param <T> the domain payload type
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        T payload
) {

    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("payload") T payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public static <T> EventEnvelope<T> of(String eventType, String correlationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID().toString(), eventType, Instant.now(), correlationId, payload);
    }
}
