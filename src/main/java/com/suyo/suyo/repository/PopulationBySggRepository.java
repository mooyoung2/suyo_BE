package com.suyo.suyo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.PopulationBySgg;

public interface PopulationBySggRepository extends JpaRepository<PopulationBySgg, Long> {

    Optional<PopulationBySgg> findBySggCodeAndQuarter(String sggCode, String quarter);

    List<PopulationBySgg> findByQuarter(String quarter);
}
