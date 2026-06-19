package com.sportygroup.jackpot.config;

import com.sportygroup.jackpot.messaging.BetConsumer;
import com.sportygroup.jackpot.messaging.BetEventPublisher;
import com.sportygroup.jackpot.messaging.KafkaBetEventPublisher;
import com.sportygroup.jackpot.repository.JackpotRepository;
import com.sportygroup.jackpot.service.BetProcessor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Real Kafka wiring shared by {@code embedded} and {@code external} modes (A17). Resolves the
 * bootstrap servers from the {@code kafkaBootstrapServers} bean (embedded broker address or the
 * configured property). Pins the delivery semantics A5 relies on: producer {@code acks=all} +
 * idempotence, consumer manual ack.
 */
@Configuration
@EnableKafka
@Conditional(RealKafkaCondition.class)
public class RealKafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(String kafkaBootstrapServers) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers));
    }

    @Bean
    public NewTopic betsTopic() {
        return TopicBuilder.name(KafkaTopics.BETS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic betsCancelledTopic() {
        return TopicBuilder.name(KafkaTopics.BETS_CANCELLED).partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(String kafkaBootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(String kafkaBootstrapServers,
                                                           JackpotProperties properties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.kafka().groupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public BetEventPublisher betEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaBetEventPublisher(kafkaTemplate);
    }

    @Bean
    public BetConsumer betConsumer(ObjectMapper objectMapper, JackpotRepository jackpotRepository,
                                   BetProcessor betProcessor, BetEventPublisher publisher,
                                   JackpotProperties properties) {
        JackpotProperties.Contention contention = properties.contention();
        return new BetConsumer(objectMapper, jackpotRepository, betProcessor, publisher,
                contention.maxRetries(), contention.backoffMs());
    }
}
