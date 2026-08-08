package com.serhat.ecommerce.sagaorchestrator.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Surfaces dead-lettered saga messages.
 *
 * <p>Every service routes exhausted retries to a {@code <topic>.DLT}, but nothing consumed
 * those topics, so a poisoned message vanished silently and the order it belonged to simply
 * stopped moving. Logging them at ERROR gives the failure somewhere to be noticed - and the
 * watchdog still compensates the stalled saga independently, so a dead-lettered command no
 * longer means a permanently lost order.
 *
 * <p>Messages are only reported here, never reprocessed: replaying a message that failed
 * repeatedly would most likely fail again, and the saga has already been unwound by the time
 * anyone looks.
 */
@Slf4j
@Component
public class DeadLetterMonitor {

    @KafkaListener(
            topics = {
                    "order-create.DLT",
                    "order-created.DLT",
                    "stock-reserve-request.DLT",
                    "stock-reserved.DLT",
                    "stock-reservation-failed.DLT",
                    "stock-release.DLT",
                    "payment-request.DLT",
                    "payment-result.DLT",
                    "payment-refund.DLT",
                    "payment-refund-result.DLT",
                    "shipment-request.DLT",
                    "shipment-result.DLT",
                    "order-completed.DLT",
                    "order-failed.DLT"
            },
            groupId = "saga-dlt-monitor")
    public void onDeadLetter(String message) {
        log.error("Dead-lettered saga message needs investigation: {}", message);
    }
}
