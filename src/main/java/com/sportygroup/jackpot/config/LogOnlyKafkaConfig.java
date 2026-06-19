package com.sportygroup.jackpot.config;

import com.sportygroup.jackpot.messaging.BetEventPublisher;
import com.sportygroup.jackpot.messaging.MockBetEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code log-only} mode (A17): the literal log-only mock; no Kafka, no consumer. */
@Configuration
@ConditionalOnProperty(name = "jackpot.kafka.mode", havingValue = "log-only")
public class LogOnlyKafkaConfig {

    @Bean
    public BetEventPublisher betEventPublisher() {
        return new MockBetEventPublisher();
    }
}
