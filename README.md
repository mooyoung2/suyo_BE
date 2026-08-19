# Suyo · 백엔드

예비창업자가 입력한 아이템을 서울 공공데이터 기반 백분위 점수로 3레이어(시장·고객·경쟁) 진단하고,
데이터로 확인 안 되는 부분은 LLM이 검증 질문지를 만들어주는 서비스의 백엔드.

## 기술 스택

- Java 17, Spring Boot 4.1.0 (Gradle)
- PostgreSQL (운영/로컬), H2 (테스트)
- OpenAI API (질문지 생성, `gpt-4o-mini`)

## 로컬 실행

### 1. PostgreSQL 준비

```bash
createdb suyo
psql -d suyo -f db-init/schema.sql
cd db-init/data && psql -d suyo -f ../load_data.sql
```

`load_data.sql` 마지막 검증 쿼리로 행수·합계가 맞는지 확인한다 (서울 점포수 합계 554,092 등).

### 2. 환경변수

```bash
cp .env.example .env
```

`.env`를 열어 실제 값으로 채운다. 최소한 `DB_PASSWORD`(로컬 Postgres 계정 비밀번호)와
`OPENAI_API_KEY`(질문지 생성 기능에 필요)는 반드시 채워야 한다.

`.env`는 `spring-dotenv` 대신 자체 구현한 `DotenvEnvironmentPostProcessor`
(`src/main/java/com/suyo/suyo/config`)가 읽어서 환경변수로 등록한다.

### 3. 실행

```bash
./gradlew bootRun
```

기본적으로 `local` 프로필로 뜨고 (`.env`의 `SPRING_PROFILES_ACTIVE`), `http://localhost:8080`에서 확인 가능하다.

```bash
curl http://localhost:8080/actuator/health
```

## 테스트

```bash
./gradlew test
```

H2 인메모리 DB(`test` 프로필)로 돈다. `src/test/resources/import.sql`에 스코어링 로직 검증용 합성
참조 데이터가 있어서, 진단 API까지 실제로 계산해서 검증한다. OpenAI 호출(질문지 생성)은
`QuestionnaireGenerationService`를 목(mock)으로 대체해서 네트워크·비용 없이 돈다.

## 배포

- 백엔드: `https://api.suyo-deploy.shop`
- 프론트엔드: `https://www.suyo-deploy.shop`

배포 환경 확인:

```bash
curl https://api.suyo-deploy.shop/actuator/health
```

AWS EC2(Docker) + RDS(PostgreSQL) + ALB(HTTPS) 구성으로 운영 중이며,
CI/CD 없이 수동 빌드·배포한다 (해커톤 일정상 자동화는 범위 밖).

## API 명세

Notion API 명세서(요청/응답 전체 스펙, 에러 코드, 화면 흐름) — 팀 노션 참고.
로컬 기준 Base URL: `http://localhost:8080`

주요 엔드포인트:

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/industries?q=` | 지원 업종 검색 (매핑된 96개) |
| POST | `/api/analyses` | 분석 요청 생성 (업종 매칭 + 3레이어 진단 동기 처리) |
| GET | `/api/analyses` | 분석 목록 (세션별) |
| GET | `/api/analyses/{id}/status` | 처리 상태 |
| GET | `/api/analyses/{id}/diagnosis` | 진단 결과 (accessLevel에 따라 근거 게이팅) |
| GET | `/api/analyses/{id}/evidence` | 확인된 근거 + 미검증 가설 |
| POST | `/api/analyses/{id}/questionnaires` | 검증 질문지 생성 (결제 필요) |
| GET | `/api/analyses/{id}/questionnaires/{qid}` | 질문지 조회 |
| POST | `/api/analyses/{id}/payments` | 결제 (목업, PG 연동 없음) |
| GET | `/api/credits` | 세션 잔여 크레딧 |

모든 요청은 `X-Session-Id` 헤더로 익명 세션을 식별한다 (없으면 서버가 발급해서 응답 헤더로 내려줌).

## 프로젝트 구조

```
db-init/                 스키마·공공데이터 CSV·적재 스크립트
src/main/java/com/suyo/suyo/
  common/                 공통 응답 포맷, 예외, 서울 자치구 상수
  config/                 .env 로더 등 인프라 설정
  controller/             REST 컨트롤러
  domain/                 JPA 엔티티
  dto/                    요청/응답 DTO
  llm/                    OpenAI 연동 (질문지 생성)
  repository/             Spring Data JPA 리포지토리
  scoring/                백분위 기반 3레이어 진단 스코어링 엔진
  service/                비즈니스 로직
  session/                익명 세션(X-Session-Id) 처리
```

## 진단 로직 개요

서울 25개 자치구 × 96개 업종 조합의 실측 데이터(매출·인구·경쟁업소·생존율)를
백분위로 환산해 3개 레이어(시장 30점·고객 40점·경쟁 30점, 총 100점)로 채점한다.
등급 경계값은 서울 2,395개 조합 실측 분포의 상/하위 33% 지점(P33/P67)으로 고정했다.
자세한 산출식은 `레이어_점수산출_설계서.md` 참고.


