package com.sportygroup.jackpot.repository;

import com.sportygroup.jackpot.domain.JackpotContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, Long> {

    Optional<JackpotContribution> findByBetId(String betId);
}
