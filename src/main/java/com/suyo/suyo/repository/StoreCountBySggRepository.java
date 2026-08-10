package com.suyo.suyo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.StoreCountBySgg;

public interface StoreCountBySggRepository extends JpaRepository<StoreCountBySgg, Long> {

    Optional<StoreCountBySgg> findBySmallCodeAndSggCode(String smallCode, String sggCode);

    List<StoreCountBySgg> findBySggCode(String sggCode);

    List<StoreCountBySgg> findBySmallCode(String smallCode);
}
