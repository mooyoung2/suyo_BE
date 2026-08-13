package com.suyo.suyo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.suyo.suyo.domain.type.QuestionnaireType;
import com.suyo.suyo.llm.GeneratedItem;
import com.suyo.suyo.llm.GeneratedQuestionnaire;
import com.suyo.suyo.llm.LeadingQuestionCheckResult;
import com.suyo.suyo.llm.QuestionnaireGenerationService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 분석 생성 -> 진단 -> 근거 -> 결제 -> 질문지로 이어지는 주요 플로우 통합테스트.
 * H2(test 프로필)에 합성 참조 데이터(data.sql)를 적재해서 실제 스코어링 로직까지 검증한다.
 * LLM 호출(질문지 생성)은 비용·네트워크 의존성 때문에 QuestionnaireGenerationService를 목으로 대체한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisFlowIntegrationTest {

    private static final String MAPPED_INDUSTRY_CODE = "T00001";
    private static final String UNMAPPED_INDUSTRY_CODE = "T00002";
    private static final String SEOUL_DISTRICT = "11260";

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionnaireGenerationService questionnaireGenerationService;

    private MockMvcTester tester() {
        return mockMvcTester;
    }

    private String createAnalysisRequestBody(String industryCode, String regionSggCode) {
        return """
                {
                  "itemName": "테스트 카페",
                  "industryCode": "%s",
                  "problem": "테스트 문제",
                  "targetCustomer": "테스트 고객",
                  "deliveryMethod": "오프라인",
                  "regionSggCode": "%s"
                }
                """.formatted(industryCode, regionSggCode);
    }

    private long createAnalysis(String sessionId) throws Exception {
        var result = tester().post().uri("/api/analyses")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAnalysisRequestBody(MAPPED_INDUSTRY_CODE, SEOUL_DISTRICT))
                .exchange();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("analysisId").asLong();
    }

    @Test
    void 분석_생성하면_매핑된_업종으로_바로_완료된다() {
        String sessionId = "session-create-" + System.nanoTime();

        tester().post().uri("/api/analyses")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAnalysisRequestBody(MAPPED_INDUSTRY_CODE, SEOUL_DISTRICT))
                .exchange()
                .assertThat()
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("COMPLETED");
    }

    @Test
    void 지원하지_않는_지역이면_422를_반환한다() {
        String sessionId = "session-region-" + System.nanoTime();

        tester().post().uri("/api/analyses")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAnalysisRequestBody(MAPPED_INDUSTRY_CODE, "41000"))
                .exchange()
                .assertThat()
                .hasStatus(422)
                .bodyJson()
                .extractingPath("$.error.code").isEqualTo("REGION_NOT_SUPPORTED");
    }

    @Test
    void 매핑되지_않은_업종이면_422를_반환한다() {
        String sessionId = "session-industry-" + System.nanoTime();

        tester().post().uri("/api/analyses")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAnalysisRequestBody(UNMAPPED_INDUSTRY_CODE, SEOUL_DISTRICT))
                .exchange()
                .assertThat()
                .hasStatus(422)
                .bodyJson()
                .extractingPath("$.error.code").isEqualTo("INDUSTRY_NOT_SUPPORTED");
    }

    @Test
    void 아이템명이_없으면_400과_필드메시지를_반환한다() {
        String sessionId = "session-validation-" + System.nanoTime();
        String body = """
                {
                  "industryCode": "%s",
                  "problem": "문제",
                  "targetCustomer": "고객",
                  "deliveryMethod": "오프라인",
                  "regionSggCode": "%s"
                }
                """.formatted(MAPPED_INDUSTRY_CODE, SEOUL_DISTRICT);

        tester().post().uri("/api/analyses")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange()
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.error.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void 무료_상태_진단조회는_점수는_보이고_근거는_가려진다() throws Exception {
        String sessionId = "session-free-" + System.nanoTime();
        long analysisId = createAnalysis(sessionId);

        tester().get().uri("/api/analyses/{id}/diagnosis", analysisId)
                .header("X-Session-Id", sessionId)
                .exchange()
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.data.accessLevel").isEqualTo("FREE");
    }

    @Test
    void 존재하지_않는_분석_진단조회는_404를_반환한다() {
        tester().get().uri("/api/analyses/{id}/diagnosis", 999_999)
                .header("X-Session-Id", "session-notfound")
                .exchange()
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void 결제_전에는_질문지_생성이_402를_반환한다() throws Exception {
        String sessionId = "session-nopay-" + System.nanoTime();
        long analysisId = createAnalysis(sessionId);

        tester().post().uri("/api/analyses/{id}/questionnaires", analysisId)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hypothesisIds\":[1],\"type\":\"INTERVIEW\"}")
                .exchange()
                .assertThat()
                .hasStatus(402)
                .bodyJson()
                .extractingPath("$.error.code").isEqualTo("PAYMENT_REQUIRED");
    }

    @Test
    void SINGLE_결제하면_잠금이_풀리고_질문지를_생성할_수_있다() throws Exception {
        String sessionId = "session-single-" + System.nanoTime();
        long analysisId = createAnalysis(sessionId);

        tester().post().uri("/api/analyses/{id}/payments", analysisId)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"SINGLE\",\"paymentMethod\":\"KAKAOPAY\"}")
                .exchange()
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.data.unlocked").asBoolean().isTrue();

        tester().get().uri("/api/analyses/{id}/diagnosis", analysisId)
                .header("X-Session-Id", sessionId)
                .exchange()
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.data.accessLevel").isEqualTo("PAID");

        JsonNode evidence = objectMapper.readTree(
                tester().get().uri("/api/analyses/{id}/evidence", analysisId)
                        .header("X-Session-Id", sessionId)
                        .exchange().getResponse().getContentAsString());
        long hypothesisId = evidence.path("data").path("unverifiedHypotheses").get(0).path("hypothesisId").asLong();

        when(questionnaireGenerationService.generate(any(), any(), any(), any(), any()))
                .thenReturn(new GeneratedQuestionnaire(
                        List.of(new GeneratedItem("테스트 질문입니다", "테스트 목적")),
                        new LeadingQuestionCheckResult(true, "테스트 통과")));

        tester().post().uri("/api/analyses/{id}/questionnaires", analysisId)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hypothesisIds\":[" + hypothesisId + "],\"type\":\"" + QuestionnaireType.INTERVIEW + "\"}")
                .exchange()
                .assertThat()
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.data.items[0].questionText").isEqualTo("테스트 질문입니다");
    }

    @Test
    void PACK3_결제하면_크레딧이_적립되고_다음_분석은_자동_결제된다() throws Exception {
        String sessionId = "session-pack3-" + System.nanoTime();
        long firstAnalysisId = createAnalysis(sessionId);

        tester().post().uri("/api/analyses/{id}/payments", firstAnalysisId)
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"PACK3\",\"paymentMethod\":\"TOSSPAY\"}")
                .exchange()
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.data.remainingCredits").convertTo(Integer.class).isEqualTo(2);

        JsonNode credits = objectMapper.readTree(
                tester().get().uri("/api/credits")
                        .header("X-Session-Id", sessionId)
                        .exchange().getResponse().getContentAsString());
        assertThat(credits.path("data").path("remainingCredits").asInt()).isEqualTo(2);

        long secondAnalysisId = createAnalysis(sessionId);

        tester().get().uri("/api/analyses/{id}/diagnosis", secondAnalysisId)
                .header("X-Session-Id", sessionId)
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.data.accessLevel").isEqualTo("PAID");

        JsonNode creditsAfter = objectMapper.readTree(
                tester().get().uri("/api/credits")
                        .header("X-Session-Id", sessionId)
                        .exchange().getResponse().getContentAsString());
        assertThat(creditsAfter.path("data").path("remainingCredits").asInt()).isEqualTo(1);
    }

    @Test
    void 분석목록은_세션별로_격리된다() throws Exception {
        String sessionA = "session-a-" + System.nanoTime();
        String sessionB = "session-b-" + System.nanoTime();
        createAnalysis(sessionA);

        tester().get().uri("/api/analyses?page=0&size=20")
                .header("X-Session-Id", sessionA)
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.data.totalElements").convertTo(Integer.class)
                .matches(count -> count >= 1, "최소 1건 이상");

        tester().get().uri("/api/analyses?page=0&size=20")
                .header("X-Session-Id", sessionB)
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.data.totalElements").convertTo(Integer.class).isEqualTo(0);
    }

    @Test
    void 세션ID_헤더가_없으면_서버가_새로_발급한다() {
        var result = tester().get().uri("/api/analyses").exchange();
        assertThat(result.getResponse().getHeader("X-Session-Id")).isNotBlank();
    }
}
