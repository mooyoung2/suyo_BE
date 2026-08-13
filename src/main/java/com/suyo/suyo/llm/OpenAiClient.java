package com.suyo.suyo.llm;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.suyo.suyo.common.exception.BusinessException;
import com.suyo.suyo.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * OpenAI Chat Completions API의 최소 래퍼. JSON 모드로 구조화된 응답만 다룬다.
 */
@Slf4j
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey, @Value("${openai.model}") String model,
                         ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /** system/user 프롬프트로 JSON 객체 문자열을 요청하고, 파싱된 JsonNode를 반환한다. */
    public JsonNode chatJson(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").path(0).path("message").path("content").asString();
            return objectMapper.readTree(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new BusinessException(ErrorCode.LLM_ERROR, "LLM 호출에 실패했습니다.");
        }
    }
}
