package com.suyo.suyo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.PaymentCredit;

public interface PaymentCreditRepository extends JpaRepository<PaymentCredit, String> {

    Optional<PaymentCredit> findBySessionId(String sessionId);
}
