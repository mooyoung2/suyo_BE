package com.suyo.suyo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.SeoulSalesQuarterly;

public interface SeoulSalesQuarterlyRepository extends JpaRepository<SeoulSalesQuarterly, Long> {

    Optional<SeoulSalesQuarterly> findByIndustryCodeAndQuarter(String industryCode, String quarter);

    List<SeoulSalesQuarterly> findByIndustryCodeOrderByQuarterAsc(String industryCode);
}
