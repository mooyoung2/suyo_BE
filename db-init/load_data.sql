-- =====================================================================
-- Suyo · 공공데이터 CSV 적재
--
-- 사용법 (터미널에서):
--   1) cd db-init/data   (CSV 5개가 이 폴더에 있음)
--   2) psql -d suyo -f ../load_data.sql
--   ※ schema.sql을 먼저 실행해서 테이블을 만든 뒤에 이 스크립트를 실행할 것
--
-- \copy 는 psql 클라이언트 명령이라 서버 파일 권한이 필요 없다.
-- (COPY 는 서버 로컬 파일만 읽으므로 여기서는 \copy 를 쓴다)
-- =====================================================================

\echo '=== 공공데이터 적재 시작 ==='

-- 순서 중요: industry_codes 를 먼저 넣어야 FK가 걸린다
TRUNCATE industry_code_mapping, store_counts_by_sgg, industry_codes RESTART IDENTITY CASCADE;
TRUNCATE seoul_sales_quarterly RESTART IDENTITY;
TRUNCATE industry_survival_rates RESTART IDENTITY;

\echo '1/5 industry_codes (247행)'
\copy industry_codes (small_code, small_name, mid_code, mid_name, large_code, large_name, national_count) FROM 'industry_codes.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

\echo '2/5 store_counts_by_sgg (6,004행 · 서울 25개 자치구)'
\copy store_counts_by_sgg (small_code, small_name, sido_code, sido_name, sgg_code, sgg_name, store_count) FROM 'store_counts_by_sgg.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

\echo '3/5 seoul_sales_quarterly (1,323행)'
\copy seoul_sales_quarterly (industry_code, industry_name, quarter, sales_amount, sales_count, male_amount, female_amount, age10_amount, age20_amount, age30_amount, age40_amount, age50_amount, age60_amount, weekday_amount, weekend_amount) FROM 'seoul_sales_quarterly.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

\echo '4/5 industry_code_mapping (96행)'
\copy industry_code_mapping (sales_industry_name, sales_industry_code, small_code, small_name, mid_name, national_count) FROM 'industry_code_mapping.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

\echo '5/5 industry_survival_rates (11행)'
\copy industry_survival_rates (large_code, large_name, stat_industry, survival_1y, survival_5y, closure_rate, base_year, source) FROM 'industry_survival_rates.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')

ANALYZE;

-- =====================================================================
-- 적재 검증
-- =====================================================================
\echo ''
\echo '=== 적재 결과 ==='
SELECT 'industry_codes'          AS table_name, count(*) AS rows, 247    AS expected FROM industry_codes
UNION ALL SELECT 'store_counts_by_sgg',      count(*), 6004  FROM store_counts_by_sgg
UNION ALL SELECT 'seoul_sales_quarterly',    count(*), 1323  FROM seoul_sales_quarterly
UNION ALL SELECT 'industry_code_mapping',    count(*), 96    FROM industry_code_mapping
UNION ALL SELECT 'industry_survival_rates',  count(*), 11    FROM industry_survival_rates;

\echo ''
\echo '=== 무결성 검증 (서울 합계 554,092 이어야 함) ==='
SELECT sum(store_count) AS total_stores FROM store_counts_by_sgg;

\echo ''
\echo '=== 동작 확인: 서울 중랑구 카페 ==='
SELECT s.sgg_name, s.small_name, s.store_count,
       v.survival_1y, v.survival_5y
FROM store_counts_by_sgg s
JOIN industry_codes c ON c.small_code = s.small_code
LEFT JOIN industry_survival_rates v ON v.large_code = c.large_code
WHERE s.small_name = '카페' AND s.sgg_name = '중랑구';
