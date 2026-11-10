-- AI작성: schema.sql에 정의된 기존 테이블과 컬럼만 사용하는 계모임 거래 더미데이터입니다.
-- MySQL 8.x 기준이며 실제 금융 API나 실제 자금 이동을 호출하지 않습니다.
-- 실행 전 테스트할 계모임의 내부 group_id로 변경하세요.
SET @target_group_id := 111;

-- AI작성: 거래내역 테스트 전용 가상 계좌이며 실제 계좌번호나 금융 API를 사용하지 않습니다.
-- account_number는 운영 암호문이 아니므로 계좌번호 복호화·마스킹 화면의 테스트에는 사용하지 않습니다.
INSERT INTO group_account (group_id, bank_code, account_number, account_holder_name)
SELECT g.group_id, '000', CONCAT('MOCK-ONLY-', g.group_id), '모아 테스트 계좌'
FROM `groups` g
JOIN saving_group sg ON sg.group_id = g.group_id
WHERE g.group_id = @target_group_id
  AND g.deleted_at IS NULL
ON DUPLICATE KEY UPDATE account_id = group_account.account_id;

SET @target_account_id := (
    SELECT account_id FROM group_account WHERE group_id = @target_group_id LIMIT 1
);

-- AI작성: 대상 계좌가 없으면 SELECT 결과가 0건이므로 거래도 생성되지 않습니다.
SET @base_balance := (
    SELECT COALESCE(SUM(CASE
        WHEN transaction_type = 'DEPOSIT' THEN amount
        WHEN transaction_type = 'WITHDRAWAL' THEN -amount
        ELSE 0 END), 0)
    FROM group_transaction
    WHERE account_id = @target_account_id
      AND (source_provider IS NULL OR source_provider != 'MOA_DUMMY')
);

-- AI작성: 월별 납부현황과 입출금 화면 확인용 기본 거래 13건입니다.
INSERT INTO group_transaction
    (account_id, transaction_type, amount, balance_after, source_provider,
     source_transaction_id, transacted_at, description)
WITH base_transactions AS (
    SELECT 1 AS transaction_order, 'DEPOSIT' AS transaction_type, 50000.00 AS amount,
           DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 5 MONTH), INTERVAL 10 HOUR) AS transacted_at,
           '더미 회비 입금' AS description UNION ALL
    SELECT 2, 'DEPOSIT', 50000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 4 MONTH), INTERVAL 11 HOUR), '더미 회비 입금' UNION ALL
    SELECT 3, 'WITHDRAWAL', 20000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 4 MONTH), INTERVAL 15 HOUR), '더미 모임 장소 대여비' UNION ALL
    SELECT 4, 'DEPOSIT', 50000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), INTERVAL 9 HOUR), '더미 회비 입금' UNION ALL
    SELECT 5, 'WITHDRAWAL', 15000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 3 MONTH), INTERVAL 18 HOUR), '더미 다과 구입비' UNION ALL
    SELECT 6, 'DEPOSIT', 50000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 2 MONTH), INTERVAL 10 HOUR), '더미 회비 입금' UNION ALL
    SELECT 7, 'DEPOSIT', 30000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 2 MONTH), INTERVAL 12 HOUR), '더미 추가 입금' UNION ALL
    SELECT 8, 'WITHDRAWAL', 30000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), INTERVAL 14 HOUR), '더미 행사 준비비' UNION ALL
    SELECT 9, 'DEPOSIT', 50000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH), INTERVAL 16 HOUR), '더미 회비 입금' UNION ALL
    SELECT 10, 'DEPOSIT', 50000.00, DATE_ADD(CURRENT_DATE, INTERVAL 9 HOUR), '더미 회비 입금' UNION ALL
    SELECT 11, 'WITHDRAWAL', 10000.00, DATE_ADD(CURRENT_DATE, INTERVAL 13 HOUR), '더미 문자 발송비' UNION ALL
    SELECT 12, 'DEPOSIT', 50000.00, DATE_ADD(CURRENT_DATE, INTERVAL 17 HOUR), '더미 회비 입금' UNION ALL
    SELECT 13, 'DEPOSIT', 700000.00, DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH), INTERVAL 9 HOUR), '더미 모임 운영자금 입금'
)
SELECT @target_account_id, bt.transaction_type, bt.amount,
       @base_balance + SUM(CASE WHEN bt.transaction_type = 'DEPOSIT'
           THEN bt.amount ELSE -bt.amount END)
           OVER (ORDER BY bt.transacted_at, bt.transaction_order),
       'MOA_DUMMY',
       CONCAT('SG-', @target_group_id, '-DUMMY-', LPAD(bt.transaction_order, 2, '0')),
       bt.transacted_at, bt.description
FROM base_transactions bt
WHERE @target_account_id IS NOT NULL
ON DUPLICATE KEY UPDATE transaction_id = group_transaction.transaction_id;

SET @expense_base_balance := (
    SELECT COALESCE(SUM(CASE
        WHEN transaction_type = 'DEPOSIT' THEN amount
        WHEN transaction_type = 'WITHDRAWAL' THEN -amount
        ELSE 0 END), 0)
    FROM group_transaction WHERE account_id = @target_account_id
);

-- AI작성: 취미·모임 관련 매장 50곳을 순환하여 최근 6개월 지출 200건을 생성합니다.
INSERT INTO group_transaction
    (account_id, transaction_type, amount, balance_after, source_provider,
     source_transaction_id, transacted_at, description)
WITH RECURSIVE expense_sequence AS (
    SELECT 1 AS sequence_number
    UNION ALL
    SELECT sequence_number + 1 FROM expense_sequence WHERE sequence_number < 200
),
merchant_names AS (
    SELECT 1 AS merchant_order, '모아문화센터' AS merchant_name UNION ALL
    SELECT 2, '한마음볼링장' UNION ALL
    SELECT 3, '스매싱탁구클럽' UNION ALL
    SELECT 4, '늘봄배드민턴센터' UNION ALL
    SELECT 5, '푸른산등산용품점' UNION ALL
    SELECT 6, '별빛캠핑마켓' UNION ALL
    SELECT 7, '소담도예공방' UNION ALL
    SELECT 8, '나무향목공방' UNION ALL
    SELECT 9, '오늘의그림미술공방' UNION ALL
    SELECT 10, '한땀가죽공방' UNION ALL
    SELECT 11, '꽃길원예공방' UNION ALL
    SELECT 12, '느티나무사진관' UNION ALL
    SELECT 13, '우리동네노래연습장' UNION ALL
    SELECT 14, '햇살작은영화관' UNION ALL
    SELECT 15, '마을소극장' UNION ALL
    SELECT 16, '책마루서점' UNION ALL
    SELECT 17, '한빛문구인쇄센터' UNION ALL
    SELECT 18, '어울림모임공간' UNION ALL
    SELECT 19, '라온스터디룸' UNION ALL
    SELECT 20, '모아카페' UNION ALL
    SELECT 21, '정다운한식당' UNION ALL
    SELECT 22, '함께하는바비큐마당' UNION ALL
    SELECT 23, '행복버스대절' UNION ALL
    SELECT 24, '추억여행사' UNION ALL
    SELECT 25, '한마음레크리에이션' UNION ALL
    SELECT 26, '백세건강체조교실' UNION ALL
    SELECT 27, '라온댄스스튜디오' UNION ALL
    SELECT 28, '우리마을요가원' UNION ALL
    SELECT 29, '굿샷스크린골프' UNION ALL
    SELECT 30, '한결당구클럽' UNION ALL
    SELECT 31, '푸른숲자전거마을' UNION ALL
    SELECT 32, '튼튼생활체육센터' UNION ALL
    SELECT 33, '정다운바둑교실' UNION ALL
    SELECT 34, '한수장기교실' UNION ALL
    SELECT 35, '모아보드게임카페' UNION ALL
    SELECT 36, '다정한요리공방' UNION ALL
    SELECT 37, '향기로운제과교실' UNION ALL
    SELECT 38, '차향다도공방' UNION ALL
    SELECT 39, '고운소리악기점' UNION ALL
    SELECT 40, '어울림노래교실' UNION ALL
    SELECT 41, '마을풍물연습실' UNION ALL
    SELECT 42, '행복한복지관강당' UNION ALL
    SELECT 43, '푸른마을체육관' UNION ALL
    SELECT 44, '한마음세미나실' UNION ALL
    SELECT 45, '느티나무야외캠핑장' UNION ALL
    SELECT 46, '햇살농촌체험마을' UNION ALL
    SELECT 47, '바다향낚시용품점' UNION ALL
    SELECT 48, '산들바람둘레길쉼터' UNION ALL
    SELECT 49, '정겨운전통시장' UNION ALL
    SELECT 50, '모아단체도시락'
),
expense_rows AS (
    SELECT es.sequence_number,
           CASE MOD(es.sequence_number - 1, 10)
               WHEN 0 THEN 4800.00 WHEN 1 THEN 7500.00
               WHEN 2 THEN 12000.00 WHEN 3 THEN 15800.00
               WHEN 4 THEN 22000.00 WHEN 5 THEN 28500.00
               WHEN 6 THEN 35000.00 WHEN 7 THEN 42000.00
               WHEN 8 THEN 55000.00 ELSE 68000.00 END AS amount,
           DATE_ADD(DATE_SUB(CURRENT_DATE,
               INTERVAL (200 - es.sequence_number) * 21 HOUR),
               INTERVAL 8 + MOD(es.sequence_number * 3, 12) HOUR) AS transacted_at,
           mn.merchant_name
    FROM expense_sequence es
    JOIN merchant_names mn
      ON mn.merchant_order = MOD(es.sequence_number - 1, 50) + 1
)
SELECT @target_account_id, 'WITHDRAWAL', er.amount,
       @expense_base_balance - SUM(er.amount)
           OVER (ORDER BY er.transacted_at, er.sequence_number),
       'MOA_DUMMY',
       CONCAT('SG-', @target_group_id, '-DUMMY-EXP-', LPAD(er.sequence_number, 3, '0')),
       er.transacted_at, CONCAT(er.merchant_name, ' 더미 지출')
FROM expense_rows er
WHERE @target_account_id IS NOT NULL
ON DUPLICATE KEY UPDATE transaction_id = group_transaction.transaction_id;

-- AI작성: 승인 회원이 0명이면 결과가 0건이 되어 입금 연결을 안전하게 건너뜁니다.
INSERT INTO saving_group_mock_deposit_request
    (account_id, member_id, amount, request_key, status,
     transaction_id, requested_at, completed_at)
WITH approved_members AS (
    SELECT gm.member_id,
           ROW_NUMBER() OVER (ORDER BY gm.crew_member_id) AS member_order
    FROM group_member gm
    JOIN members m ON m.member_id = gm.member_id AND m.deleted_at IS NULL
    WHERE gm.group_id = @target_group_id
      AND gm.membership_status = 'APPROVED'
      AND gm.member_role IN ('OWNER', 'MANAGER', 'MEMBER')
),
approved_member_count AS (
    SELECT COUNT(*) AS total FROM approved_members
),
deposit_transactions AS (
    SELECT gt.transaction_id, gt.account_id, gt.amount, gt.transacted_at,
           ROW_NUMBER() OVER (ORDER BY gt.transacted_at, gt.transaction_id) AS deposit_order
    FROM group_transaction gt
    WHERE gt.account_id = @target_account_id
      AND gt.source_provider = 'MOA_DUMMY'
      AND gt.source_transaction_id LIKE CONCAT('SG-', @target_group_id, '-DUMMY-__')
      AND gt.transaction_type = 'DEPOSIT'
)
SELECT dt.account_id, am.member_id, dt.amount,
       CONCAT('dummy-', @target_group_id, '-', LPAD(dt.deposit_order, 2, '0')),
       'COMPLETED', dt.transaction_id, dt.transacted_at, dt.transacted_at
FROM deposit_transactions dt
JOIN approved_member_count amc ON amc.total > 0
JOIN approved_members am
  ON am.member_order = MOD(dt.deposit_order - 1, NULLIF(amc.total, 0)) + 1
ON DUPLICATE KEY UPDATE
    deposit_request_id = saving_group_mock_deposit_request.deposit_request_id;

-- 검증: 이 파일이 대상 계모임에 생성한 거래 결과입니다.
SELECT gt.transaction_id, gt.transaction_type, gt.amount, gt.balance_after,
       gt.transacted_at, gt.description, dr.member_id
FROM group_transaction gt
LEFT JOIN saving_group_mock_deposit_request dr ON dr.transaction_id = gt.transaction_id
WHERE gt.account_id = @target_account_id
  AND gt.source_provider = 'MOA_DUMMY'
ORDER BY gt.transacted_at, gt.transaction_id;
