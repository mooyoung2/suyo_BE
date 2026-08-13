package com.suyo.suyo.llm;

import java.util.List;

public record GeneratedQuestionnaire(List<GeneratedItem> items, LeadingQuestionCheckResult leadingQuestionCheck) {
}
