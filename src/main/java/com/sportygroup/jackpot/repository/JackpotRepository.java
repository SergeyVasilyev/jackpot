package com.sportygroup.jackpot.repository;

import com.sportygroup.jackpot.domain.Jackpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JackpotRepository extends JpaRepository<Jackpot, String> {
}
