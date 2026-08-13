package com.suyo.suyo.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.suyo.suyo.common.exception.BusinessException;
import com.suyo.suyo.common.exception.ErrorCode;
import com.suyo.suyo.domain.type.QuestionnaireType;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * 가설을 근거로 검증 질문지를 생성한다.
 * "과거 경험·현재 해결 방식·발생 빈도·실제 지출"만 묻고 유도질문은 배제하도록 프롬프트로 강제하며,
 * 생성 직후 같은 호출에서 LLM 스스로 유도질문 여부를 점검하게 한다 (레이어_점수산출_설계서.md 10번).
 */
@Component
@RequiredArgsConstructor
public class QuestionnaireGenerationService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 예비창업자의 창업 아이디어를 검증하기 위한 %s 질문지를 설계하는 전문가입니다.
            반드시 아래 규칙을 지켜 JSON으로만 응답하세요.

            규칙:
            1. 질문은 반드시 과거 경험, 현재 해결 방식, 발생 빈도, 실제 지출/행동 중 하나를 묻는 형태여야 합니다.
            2. "이런 서비스가 있으면 이용하시겠습니까?" 같은 미래 의향을 묻는 유도질문은 절대 만들지 마세요.
            3. 질문은 3개를 만들고, 각 질문마다 이 질문으로 무엇을 확인하려는지 purpose를 한 문장으로 답니다.
            4. 질문을 다 만든 뒤, 방금 만든 질문들이 왜 유도질문이 아닌지 스스로 점검해서 1~2문장으로 설명하세요.

            다음 JSON 스키마로만 응답하세요:
            {"items": [{"questionText": "...", "purpose": "..."}], "leadingQuestionCheck": {"passed": true, "summary": "..."}}
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            아이템: %s
            해결하려는 문제: %s
            예상 고객: %s

            검증이 필요한 가설:
            %s
            """;

    private final OpenAiClient openAiClient;

    public GeneratedQuestionnaire generate(String itemName, String problem, String targetCustomer,
                                            QuestionnaireType type, List<String> hypothesisDescriptions) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(
                type == QuestionnaireType.INTERVIEW ? "인터뷰" : "설문");
        String hypothesesText = hypothesisDescriptions.stream()
                .map(d -> "- " + d)
                .collect(Collectors.joining("\n"));
        String userPrompt = USER_PROMPT_TEMPLATE.formatted(itemName, problem, targetCustomer, hypothesesText);

        JsonNode root = openAiClient.chatJson(systemPrompt, userPrompt);

        List<GeneratedItem> items = new ArrayList<>();
        for (JsonNode itemNode : root.path("items")) {
            String questionText = itemNode.path("questionText").asString();
            String purpose = itemNode.path("purpose").asString();
            if (questionText != null && !questionText.isBlank()) {
                items.add(new GeneratedItem(questionText, purpose));
            }
        }
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.LLM_ERROR, "질문지를 생성하지 못했습니다.");
        }

        LeadingQuestionCheckResult check = parseLeadingQuestionCheck(root.path("leadingQuestionCheck"));

        return new GeneratedQuestionnaire(items, check);
    }

    private LeadingQuestionCheckResult parseLeadingQuestionCheck(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new LeadingQuestionCheckResult(node.path("passed").asBoolean(), node.path("summary").asString());
        } catch (Exception e) {
            return null;
        }
    }
}
