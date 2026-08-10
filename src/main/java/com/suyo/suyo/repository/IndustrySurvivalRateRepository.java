package com.suyo.suyo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.IndustrySurvivalRate;

public interface IndustrySurvivalRateRepository extends JpaRepository<IndustrySurvivalRate, Long> {

    Optional<IndustrySurvivalRate> findByLargeCode(String largeCode);
}
