-- =====================================================================
-- Suyo · PostgreSQL 스키마
-- [A] 공공데이터 참조 테이블 (읽기 전용, CSV로 1회 적재)
-- [B] 서비스 운영 테이블 (JPA 엔티티로 관리, 아래는 참고용)
-- =====================================================================


-- =====================================================================
-- [A] 공공데이터 참조 테이블
-- =====================================================================

-- ---------------------------------------------------------------------
-- A-1. 업종 마스터 (247행) · 전국
--      LLM이 "반려동물 카페" 같은 자연어를 업종코드로 바꿀 때 참조
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS industry_code_mapping CASCADE;
DROP TABLE IF EXISTS store_counts_by_sgg CASCADE;
DROP TABLE IF EXISTS industry_codes CASCADE;

CREATE TABLE industry_codes (
    small_code      VARCHAR(10) PRIMARY KEY,   -- 소분류코드 (예: I21201)
    small_name      VARCHAR(100) NOT NULL,     -- 소분류명   (예: 카페)
    mid_code        VARCHAR(10)  NOT NULL,     -- 중분류코드 (예: I212)
    mid_name        VARCHAR(100) NOT NULL,
    large_code      VARCHAR(10)  NOT NULL,     -- 대분류코드 (예: I2)
    large_name      VARCHAR(100) NOT NULL,
    national_count  INTEGER      NOT NULL      -- 전국 사업체 수
);
CREATE INDEX idx_industry_codes_small_name ON industry_codes (small_name);
CREATE INDEX idx_industry_codes_large_code ON industry_codes (large_code);


-- ---------------------------------------------------------------------
-- A-2. 업종 × 시군구 사업체 수 (6,004행) · 서울특별시 25개 자치구
--      서비스 타겟을 서울로 확정하면서 전국(55,807행)에서 서울만 필터링함
--      L3 경쟁 레이어의 핵심 데이터
-- ---------------------------------------------------------------------
CREATE TABLE store_counts_by_sgg (
    id          BIGSERIAL PRIMARY KEY,
    small_code  VARCHAR(10)  NOT NULL REFERENCES industry_codes(small_code),
    small_name  VARCHAR(100) NOT NULL,
    sido_code   VARCHAR(10)  NOT NULL,
    sido_name   VARCHAR(50)  NOT NULL,
    sgg_code    VARCHAR(10)  NOT NULL,
    sgg_name    VARCHAR(50)  NOT NULL,
    store_count INTEGER      NOT NULL
);
CREATE UNIQUE INDEX uk_store_counts_code_sgg ON store_counts_by_sgg (small_code, sgg_code);
CREATE INDEX idx_store_counts_small_code    ON store_counts_by_sgg (small_code);
CREATE INDEX idx_store_counts_sgg_code      ON store_counts_by_sgg (sgg_code);
CREATE INDEX idx_store_counts_sido          ON store_counts_by_sgg (sido_code);


-- ---------------------------------------------------------------------
-- A-3. 서울 업종별 분기 매출 (1,323행) · 서울만
--      63개 업종 × 21분기 (2021 1Q ~ 2026 1Q)
--      L1 시장 규모·성장률 + L2 고객(타겟)에 사용
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS seoul_sales_quarterly CASCADE;
CREATE TABLE seoul_sales_quarterly (
    id              BIGSERIAL PRIMARY KEY,
    industry_code   VARCHAR(20)  NOT NULL,     -- 서울시 서비스업종코드
    industry_name   VARCHAR(100) NOT NULL,     -- 예: 커피-음료
    quarter         VARCHAR(6)   NOT NULL,     -- 기준 년분기 (예: 20261)
    -- L1 시장
    sales_amount    BIGINT       NOT NULL,     -- 매출 금액
    sales_count     BIGINT       NOT NULL,     -- 매출 건수 (실제 결제 건수)
    -- L2 고객 · 성별
    male_amount     BIGINT       NOT NULL,
    female_amount   BIGINT       NOT NULL,
    -- L2 고객 · 연령대
    age10_amount    BIGINT       NOT NULL,
    age20_amount    BIGINT       NOT NULL,
    age30_amount    BIGINT       NOT NULL,
    age40_amount    BIGINT       NOT NULL,
    age50_amount    BIGINT       NOT NULL,
    age60_amount    BIGINT       NOT NULL,
    -- L2 고객 · 구매 패턴
    weekday_amount  BIGINT       NOT NULL,
    weekend_amount  BIGINT       NOT NULL
);
CREATE UNIQUE INDEX uk_seoul_sales_code_quarter ON seoul_sales_quarterly (industry_code, quarter);
CREATE INDEX idx_seoul_sales_industry_name     ON seoul_sales_quarterly (industry_name);


-- ---------------------------------------------------------------------
-- A-4. 서울매출업종 ↔ 상가업종 매핑 (96행)
--      코드 체계가 달라 이름 기준 연결. 1:N 관계
--      (예: 한식음식점 → 백반/한정식 외 13개 소분류)
-- ---------------------------------------------------------------------
CREATE TABLE industry_code_mapping (
    id                  BIGSERIAL PRIMARY KEY,
    sales_industry_name VARCHAR(100) NOT NULL,
    sales_industry_code VARCHAR(20)  NOT NULL,
    small_code          VARCHAR(10)  NOT NULL REFERENCES industry_codes(small_code),
    small_name          VARCHAR(100) NOT NULL,
    mid_name            VARCHAR(100) NOT NULL,
    national_count      INTEGER      NOT NULL
);
CREATE UNIQUE INDEX uk_mapping_small_code ON industry_code_mapping (small_code);
CREATE INDEX idx_mapping_sales_code       ON industry_code_mapping (sales_industry_code);


-- ---------------------------------------------------------------------
-- A-5. 업종 대분류별 생존율 (11행) · 전국
--      출처: 국가데이터처 기업생멸행정통계 2024년 발표분 (2023p 기준)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS industry_survival_rates CASCADE;
CREATE TABLE industry_survival_rates (
    id            BIGSERIAL PRIMARY KEY,
    large_code    VARCHAR(10),                 -- 전산업 평균 행은 NULL
    large_name    VARCHAR(100) NOT NULL,
    stat_industry VARCHAR(100) NOT NULL,
    survival_1y   NUMERIC(4,1) NOT NULL,       -- 1년 생존율 (%)
    survival_5y   NUMERIC(4,1) NOT NULL,       -- 5년 생존율 (%)
    closure_rate  NUMERIC(4,1) NOT NULL,       -- 소멸률 (%)
    base_year     VARCHAR(10)  NOT NULL,
    source        VARCHAR(200) NOT NULL
);
CREATE UNIQUE INDEX uk_survival_large_code ON industry_survival_rates (large_code)
    WHERE large_code IS NOT NULL;


-- ---------------------------------------------------------------------
-- A-6. 자치구별 인구 (525행) · 서울 25개구 × 21분기
--      출처: 서울시 상권분석서비스 (길단위인구·상주인구·직장인구 행정동)
--      → 행정동 425개를 자치구 25개로 집계함
--      검증: 상주인구 합계 9,360,421명 (서울시 실제 인구와 일치)
--      · 유동인구는 분기 누적치. 일평균은 90으로 나눌 것
--      · 상주인구는 연 1회만 갱신 (4분기 값이 다음 해 1~3분기에 그대로 반복)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS population_by_sgg CASCADE;
CREATE TABLE population_by_sgg (
    id              BIGSERIAL PRIMARY KEY,
    sgg_code        VARCHAR(10)  NOT NULL,
    sgg_name        VARCHAR(50)  NOT NULL,
    quarter         VARCHAR(6)   NOT NULL,
    -- 유동인구 (분기 누적)
    flow_total      BIGINT,
    flow_male       BIGINT,
    flow_female     BIGINT,
    flow_age10      BIGINT,
    flow_age20      BIGINT,
    flow_age30      BIGINT,
    flow_age40      BIGINT,
    flow_age50      BIGINT,
    flow_age60      BIGINT,
    -- 상주인구
    resident_total  BIGINT,
    household_total BIGINT,
    household_apt   BIGINT,
    household_nonapt BIGINT,
    -- 직장인구
    worker_total    BIGINT
);
CREATE UNIQUE INDEX uk_population_sgg_quarter ON population_by_sgg (sgg_code, quarter);
CREATE INDEX idx_population_quarter          ON population_by_sgg (quarter);


-- =====================================================================
-- [B] 서비스 운영 테이블 (JPA 엔티티로 관리 · 참고용 DDL)
-- =====================================================================

CREATE TABLE IF NOT EXISTS analysis_requests (
    id               BIGSERIAL PRIMARY KEY,
    session_id       VARCHAR(36)  NOT NULL,    -- 익명 세션(X-Session-Id). "내 아이템" 목록 구분용
    item_name        VARCHAR(200) NOT NULL,
    problem          TEXT,
    target_customer  TEXT,
    delivery_method  TEXT,
    region_sgg_code  VARCHAR(10)  NOT NULL     -- 서울 25개 자치구 코드만 허용 (11110~11740)
                     CHECK (region_sgg_code ~ '^11[0-9]{3}$'),
    matched_code     VARCHAR(10),              -- LLM이 매핑한 업종 소분류코드
    match_accuracy   VARCHAR(20),              -- EXACT / APPROXIMATE
    status           VARCHAR(20)  NOT NULL,    -- PENDING / IN_PROGRESS / COMPLETED / FAILED
    payment_status   VARCHAR(20)  NOT NULL DEFAULT 'FREE',  -- FREE / PAID
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_analysis_created ON analysis_requests (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analysis_session ON analysis_requests (session_id, created_at DESC);

-- 건별 결제(목업, PG 연동 없음). PACK3 결제 시 세션에 크레딧 2건 적립 (2026-08-13 신규)
CREATE TABLE IF NOT EXISTS payment_credits (
    session_id        VARCHAR(36) PRIMARY KEY,
    remaining_credits INTEGER   NOT NULL DEFAULT 0,
    expires_at        TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS diagnosis_results (
    id                 BIGSERIAL PRIMARY KEY,
    analysis_id        BIGINT NOT NULL REFERENCES analysis_requests(id) ON DELETE CASCADE,
    total_score        NUMERIC(4,1) NOT NULL,  -- 백분위 기반 산출이라 소수점 발생 (예: 56.8)
    market_score       NUMERIC(4,1),           -- L1 (30점) · 업종 매핑 안 되면 NULL (96/247 소분류만 매핑, 커버리지 72%)
    customer_score     NUMERIC(4,1),           -- L2 (40점) · 위와 동일 사유로 NULL 가능
    competition_score  NUMERIC(4,1) NOT NULL,  -- L3 (30점) · 상가정보 247개 소분류 전체 커버라 항상 존재
    verdict            VARCHAR(100),
    data_coverage      VARCHAR(50),            -- FULL / COMPETITION_ONLY · 지역이 아니라 "업종 매핑 여부"에 따라 갈림 (서비스 전체가 서울 타겟이므로 지역 사유의 NULL은 없음)
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_diagnosis_analysis ON diagnosis_results (analysis_id);

CREATE TABLE IF NOT EXISTS layer_evidences (
    id                BIGSERIAL PRIMARY KEY,
    diagnosis_id      BIGINT NOT NULL REFERENCES diagnosis_results(id) ON DELETE CASCADE,
    layer             VARCHAR(20)  NOT NULL,   -- MARKET / CUSTOMER / COMPETITION
    factor            VARCHAR(200) NOT NULL,
    factor_value      VARCHAR(200),             -- "value"는 H2 등 일부 DB에서 예약어라 회피
    percentile        VARCHAR(50),              -- 예: "서울 상위 21%" (원본값과 함께 노출용)
    sample_size       INTEGER,                  -- 비율 지표의 분모(예: 점포수). LOW_SAMPLE 판정 근거
    source            VARCHAR(200),
    reference_date    VARCHAR(50),
    confidence_status VARCHAR(30)              -- CONFIRMED / INSUFFICIENT_DATA / APPROXIMATE / LOW_SAMPLE
);
CREATE INDEX IF NOT EXISTS idx_evidence_diagnosis ON layer_evidences (diagnosis_id);

CREATE TABLE IF NOT EXISTS unverified_hypotheses (
    id                 BIGSERIAL PRIMARY KEY,
    diagnosis_id       BIGINT NOT NULL REFERENCES diagnosis_results(id) ON DELETE CASCADE,
    layer              VARCHAR(20)  NOT NULL,
    description        TEXT         NOT NULL,
    needs_verification BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_hypothesis_diagnosis ON unverified_hypotheses (diagnosis_id);

CREATE TABLE IF NOT EXISTS questionnaires (
    id                                BIGSERIAL PRIMARY KEY,
    analysis_id                       BIGINT NOT NULL REFERENCES analysis_requests(id) ON DELETE CASCADE,
    type                              VARCHAR(20) NOT NULL,        -- INTERVIEW / SURVEY
    leading_question_check_passed    BOOLEAN,                     -- LLM 자가검증 결과 (질문 생성 직후)
    leading_question_check_summary   TEXT,
    created_at                        TIMESTAMP NOT NULL DEFAULT now()
);

-- 질문지 1개가 가설 여러 개를 근거로 생성될 수 있어 다대다로 분리 (API 명세서 hypothesisIds: [] 배열)
CREATE TABLE IF NOT EXISTS questionnaire_hypotheses (
    questionnaire_id BIGINT NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
    hypothesis_id    BIGINT NOT NULL REFERENCES unverified_hypotheses(id) ON DELETE CASCADE,
    PRIMARY KEY (questionnaire_id, hypothesis_id)
);

CREATE TABLE IF NOT EXISTS questionnaire_items (
    id               BIGSERIAL PRIMARY KEY,
    questionnaire_id BIGINT NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
    question_text    TEXT NOT NULL,
    purpose          TEXT,
    sort_order       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS verification_results (
    id               BIGSERIAL PRIMARY KEY,
    questionnaire_id BIGINT NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
    item_id          BIGINT REFERENCES questionnaire_items(id) ON DELETE CASCADE,
    response_summary TEXT,
    response_count   INTEGER,
    key_observation  TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS customer_score_history (
    id             BIGSERIAL PRIMARY KEY,
    analysis_id    BIGINT NOT NULL REFERENCES analysis_requests(id) ON DELETE CASCADE,
    previous_score NUMERIC(4,1),
    updated_score  NUMERIC(4,1),
    reason         TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_score_history_analysis ON customer_score_history (analysis_id, created_at DESC);
