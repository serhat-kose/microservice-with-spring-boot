package com.serhat.ecommerce.sagaorchestrator.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes the saga's next command.
 *
 * <p>A publish failure is raised rather than logged, so the surrounding transaction rolls
 * back: the state change and the event that announces it must not diverge. If the write had
 * committed while the publish was lost, the saga would sit forever in a step nothing was
 * told to perform. Rolling back leaves the event unprocessed so Kafka redelivers it, and the
 * dedupe table makes that redelivery safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCommandPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, String key, String eventType, Object payload) {
        try {
            EventEnvelope<Object> envelope = EventEnvelope.of(eventType, key, payload);
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish " + eventType + " to " + topic, e);
        }
    }

    /** Forwards a message unchanged, used where the payload is simply relayed. */
    public void publishRaw(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message);
    }
}
