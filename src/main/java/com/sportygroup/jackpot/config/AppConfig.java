package com.sportygroup.jackpot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
@EnableConfigurationProperties(JackpotProperties.class)
public class AppConfig {

    /**
     * RNG for reward evaluation (A16). Injected and seedable so tests are deterministic; the seed is
     * a test aid, not a production setting. Set {@code jackpot.reward.rng-seed} to fix it.
     */
    @Bean
    public Random rewardRandom(@Value("${jackpot.reward.rng-seed:#{null}}") Long seed) {
        return seed != null ? new Random(seed) : new Random();
    }
}
