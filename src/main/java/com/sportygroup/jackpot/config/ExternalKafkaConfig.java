package com.sportygroup.jackpot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@code external} mode (A17): a real broker addressed by {@code spring.kafka.bootstrap-servers}. */
@Configuration
@ConditionalOnProperty(name = "jackpot.kafka.mode", havingValue = "external")
public class ExternalKafkaConfig {

    @Bean
    public String kafkaBootstrapServers(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return bootstrapServers;
    }
}
