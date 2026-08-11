package com.suyo.suyo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.QuestionnaireItem;

public interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, Long> {
}
