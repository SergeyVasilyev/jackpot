package com.sportygroup.jackpot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed binding for the {@code jackpot.*} settings, replacing scattered {@code @Value} lookups whose
 * defaults were duplicated between {@code application.yml} and the injection sites. Defaults now live
 * here only (the values in {@code application.yml} are explicit overrides, not the source of truth),
 * and {@code @Validated} rejects out-of-range values at startup instead of at first use.
 *
 * <p>Note: {@code jackpot.kafka.mode} is intentionally not bound here — it selects bean
 * configurations via {@code @ConditionalOnProperty} / {@link RealKafkaCondition}, which read the raw
 * environment before any bean (this one included) exists. Unknown keys under {@code jackpot.kafka}
 * (such as {@code mode}) are ignored by the binder.
 */
@Validated
@ConfigurationProperties(prefix = "jackpot")
public record JackpotProperties(
        @DefaultValue @Valid Contention contention,
        @DefaultValue @Valid Initializer initializer,
        @DefaultValue @Valid Kafka kafka) {

    /** Optimistic-lock retry budget for the consumer (A8, A9). */
    public record Contention(
            @DefaultValue("3") @Min(0) int maxRetries,
            @DefaultValue("50") @Min(0) long backoffMs) {
    }

    /** Startup jackpot-seed file location (A1). */
    public record Initializer(
            @DefaultValue("classpath:jackpots.json") @NotBlank String file) {
    }

    /** Kafka consumer settings shared by the real-broker modes (A17). */
    public record Kafka(
            @DefaultValue("jackpot-service") @NotBlank String groupId) {
    }
}
