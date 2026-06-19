package com.sportygroup.jackpot.repository;

import com.sportygroup.jackpot.domain.JackpotReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotRewardRepository extends JpaRepository<JackpotReward, Long> {

    Optional<JackpotReward> findByBetId(String betId);
}
