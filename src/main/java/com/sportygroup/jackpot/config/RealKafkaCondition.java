package com.sportygroup.jackpot.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when real Kafka is in use — {@code jackpot.kafka.mode} is {@code embedded} (the default)
 * or {@code external}, but not {@code log-only} (A17). Spring's {@code @ConditionalOnProperty} has
 * no "one of these values" form, so this condition expresses the OR.
 */
public class RealKafkaCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String mode = context.getEnvironment().getProperty("jackpot.kafka.mode", "embedded");
        return "embedded".equals(mode) || "external".equals(mode);
    }
}
