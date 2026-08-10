package com.suyo.suyo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.IndustryCodeMapping;

public interface IndustryCodeMappingRepository extends JpaRepository<IndustryCodeMapping, Long> {

    Optional<IndustryCodeMapping> findBySmallCode(String smallCode);
}
