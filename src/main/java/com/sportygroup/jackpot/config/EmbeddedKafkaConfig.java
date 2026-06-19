package com.sportygroup.jackpot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

/**
 * {@code embedded} mode (default, A17): an in-JVM {@link EmbeddedKafkaKraftBroker} that lets the
 * whole publish → consume → contribute → evaluate flow run with one command and no external infra.
 * This is the assignment's "mock" in spirit — but a real broker, so it actually drives the consumer.
 */
@Configuration
@ConditionalOnProperty(name = "jackpot.kafka.mode", havingValue = "embedded", matchIfMissing = true)
public class EmbeddedKafkaConfig {

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        // Implements InitializingBean/DisposableBean, so Spring starts it (afterPropertiesSet) before
        // it is injected into kafkaBootstrapServers and stops it on shutdown.
        return new EmbeddedKafkaKraftBroker(1, 1, KafkaTopics.BETS, KafkaTopics.BETS_CANCELLED);
    }

    @Bean
    public String kafkaBootstrapServers(EmbeddedKafkaBroker broker) {
        return broker.getBrokersAsString();
    }
}
