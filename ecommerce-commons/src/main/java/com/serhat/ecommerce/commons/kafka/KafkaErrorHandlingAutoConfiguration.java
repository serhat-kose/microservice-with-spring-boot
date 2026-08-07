package com.serhat.ecommerce.commons.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Retries a failing message with exponential backoff and then routes it to a
 * {@code <topic>.DLT} dead-letter topic rather than dropping it.
 *
 * <p>Previously each of the seven consuming services carried an identical copy of this
 * configuration; centralising it here means a change to the retry policy is made once.
 * Services that need different behaviour can still declare their own
 * {@link DefaultErrorHandler} bean, which takes precedence.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DefaultErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(10000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
