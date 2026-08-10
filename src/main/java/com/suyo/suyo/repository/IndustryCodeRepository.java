package com.suyo.suyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.IndustryCode;

public interface IndustryCodeRepository extends JpaRepository<IndustryCode, String> {

    List<IndustryCode> findBySmallNameContaining(String smallName);

    List<IndustryCode> findByLargeCode(String largeCode);
}
