package com.suyo.suyo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suyo.suyo.domain.Questionnaire;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
}
