<!-- AI작성: 승인된 7단계 화면의 저장 모델 결과 출처와 실제 연동 경계를 기록한다. -->
# 계모임 거래 이상 감지 화면 — 저장 모델 결과 재생

## 2026-10-22 새 합성 거래 분석 시연 추가
- stage10 serve_replay.py를 기존 stage9 서버 대신 같은8767포트로 실행한다. 두 서버 동시 실행 불가.
- 서버 시작 때 별도 runs DB를 새로 만들며 stage8/운영 DB는 보존한다. 계좌10013의 9월18일 이전 이력으로 시작, 화면9월8건/남은6건.
- 로컬 조회에서 서버가 replay.enabled를 반환한 경우만 다음 합성 거래 수신·분석 버튼을 표시한다.
- 버튼은 서버에 정해진 다음 거래를 저장→과거 특성 계산→기존 stage5 scorer로 새 추론→결과 저장 후 목록 갱신한다. 임의 거래 입력·실제 자금 이동·재학습은 없다.
- POST /__moa_test__/groups/12/replay-next의 expectedNextToken은 중복 방지용. 같은 토큰 재전송은 같은 거래를 재사용한다. 새 토큰을 임의로 만들지 않는다.
- 종료 후14건/경고4건이 기존 저장 결과와 일치해야 한다. 첫 출금은 경고 없음, 이후 반복 출금에서 경고가 나타나는 실험이다. 매 거래가 이상이라는 뜻은 아니다.
- 다시 조회는 여전히 읽기 전용. 분석 실패와 수신 성공을 구분하고 원래 거래를 보존한다.
- 실제 은행/인증/운영 DB 미연결, group12/account10013 매핑은 시연 전용이다.


## 2026-10-22 브라우저 확인 완료
로컬 서버 실제 조회14건/경고4건, 모델 경고 없음10건, 상세·페이지·날짜 오류·빈 결과·모바일390px·키보드 닫기를 확인했다.
조회 서버 실제 중단 시 오류/이전 목록 제거와 재시작 복구를 확인했다. 다른 모임은 빈 목록이었다.
모바일 상세의 초기 자동 스크롤 문제는 제목 focus/scrollTop=0으로 수정했다. 최종 빌드·실제 프론트 조회 함수 통합 검증 통과.
현재 합성 결과 조회 시연에 대한 확인이며 운영 인증·실제 금융 연결·실시간 새 추론을 검증한 것은 아니다.

## 2026-10-22 로컬 합성 서버 조회 추가
- 기존 저장 결과 파일은 기본값으로 유지. 개발 모드의 데이터 연결 방식에서 로컬 합성 서버 조회를 명시적으로 선택.
- 조회 서버: ChatGPT workspace의 deliverables/moa-anomaly-stage9/serve_results.py. Python 표준 라이브러리만 사용하고 127.0.0.1:8767에 바인딩.
- Vite /__moa_test__ 프록시로만 연결하고 기존 /api 프록시는 유지.
- GET /__moa_test__/groups/12/transactions는 stage8의 최신 성공 실행 SQLite를 읽기 전용으로 열어 계좌10013의 9월 결과를 반환. 실제 모임12 계좌 관계/권한을 의미하지 않음.
- 요청 시 새로운 추론/재학습을 실행하지 않음. 새 stage8 실행이 완료되면 다시 조회로 최신 실행본을 확인 가능.
- HTTP 실패는 오류로 표시. 저장 파일로 자동 대체하지 않음. 실시간 조회와 새 추론은 구분.
- 분석 시각/revision은 로컬 DB에 저장된 실제 값 사용. 모델 점수/품질 문맥/완료 상태는 저장값 사용.
- production 빌드에서는 로컬 조회 선택 숨김·함수 실행 거부. 실제 인증/은행/운영 API는 여전히 미연결.
- 서버가 없는 경우 오류→서버 시작 후 다시 조회, 다른 모임 빈 목록, 서버 실행본 runId, 경고4건을 확인.
- 실행·인계 문서는 deliverables/moa-anomaly-stage9/README.md 참조.


## 실행과 데이터
- front에서 npm run dev → /saving-groups/12 → 거래 내역.
- 합성 계좌 10013, 2026-09-01~09-30 거래 14건. 실제 stage4 Isolation Forest 결과에서 경고 4건/경고 없음 10건.
- 입력 출처: deliverables/moa-anomaly-stage4/predictions/test.jsonl
- 원본 전체 파일 SHA-256: 290ccfc169a651e12a8529d7ad3babd1bb4b49d954ef12fa8590a94c02966d92
- 모델: sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d
- 경고: score > 0.6180405340153708. 저장된 점수·기준·경고 여부를 변경하지 않음.
- 데이터는 기존 mocks/savingGroupAnomalies.js에 스냅샷으로 보관. 브라우저에서 Python을 실행하지 않음. getMockSavingGroupAnomalies는 450ms로 읽는 테스트 전송 계층이며 실제 서버 API가 아님.
- group12는 UI 시연 위치. 합성 account10013과 운영 group12의 실제 관계를 주장하지 않음. 다른 모임에는 복제하지 않음.
- 실제 거래 설명·시나리오명은 표시하지 않고 합성 계좌 입금/출금으로 대체. 금액·잔액·점수·경고·cold-start·quality는 원본을 유지.
- source ID는 행 매칭용이며 모델 입력이 아님. 평가 정답 라벨 파일은 읽지 않음.

## 상태와 표시
- 모두 완료된 저장 결과이므로 임의 PENDING/FAILED 행을 혼합하지 않음. 컴포넌트의 미분석·실패 방어 처리는 후속 서버 연결을 위해 유지.
- 실제 분석 시각과 DB revision은 원본에 없으므로 null로 두고 미기록으로 표시.
- requiresReview는 stage5와 동일하게 cold-start 또는 quality flag 존재 여부로 유도. isAnomaly와 별개. 실제 사고 정답이 아님.
- 금액 합계는 적용한 날짜/입출금 조건 기준이고 분석 상태 필터와 무관. 잔액은 전체 재생 목록 마지막 거래 기준.
- 빈 목록·조회 오류·권한 없음은 별도 조회 상태 시연. 저장 거래를 가짜 실패로 바꾸는 기능이 아님.
- 모델 점수는 확률이 아니며 실제 사고·안전 능력을 이 화면만으로 검증하지 않음.

## 수동 확인
1. group12 거래 메뉴: 로딩 후 14건, 2페이지.
2. 모델 경고 필터: 4건. 경고 없음: 10건.
3. 상세 원 점수/기준/모델 해시 확인. 표시 소수 6자리 반올림이며 판정은 원 정밀도 기준.
4. 이력·품질 확인은 모델 경고 없음과 동시에 나타날 수 있음.
5. 날짜 역전 오류, 빈 기간, Mock 조회 오류/403/빈 상태 확인.
6. 모바일 목록/상세 dialog Escape·닫기, 다른 모임에 데이터 노출 없음 확인.

## 실제 연결 전 남은 작업
현재 화면은 은행·인증·실제 MOA 거래 조회 서버와 연결되지 않음. 실제 목록 함수는 Mock과 별도로 작성하고 실패 시 자동 Mock 대체하지 않음.
stage6 계약에 맞춰 savingGroupId/groupId/accountId 매핑, 모임장·승인 운영진 서버 인가, 최신 revision 결과, 서버 필터·페이지·합계를 확정해야 함.
서버 snake_case는 API 경계에서 camelCase로 변환(status, revision_no, anomaly_score, threshold, is_anomaly, requires_review, cold_start, quality_flags, model_version, feature_schema_version, analyzed_at).
금융정보 조회·출금·재분석 요청·확인 완료 저장·다운로드·모델 재학습은 이번 변경 범위가 아님.


## 내부 거래 이력 분석 연결 (2026-10-22)

AI작성: 공개 조회/인가/DB 저장 API가 아닌 Spring 내부 호출 계약이다. 기존 화면 연결은 변경하지 않았다.

- Python: `POST /v1/anomalies/score-history`. 입력은 `account_id`, `target_transaction_id`, `transactions`. 각 거래는 `transaction_id`, `account_id`, `transaction_type`, `amount`, offset 포함 `transacted_at`, 선택 `balance_after`, `source_sequence`.
- 단일 계좌 이력과 대상 거래를 포함한다. 1~2000건/1MiB 제한이며 한도 초과 시 조용히 자르지 않는다. 필요한 과거 이력의 완전성은 호출 측이 보장해야 한다. 대상 이후 시각은 제외하고 동일 시각은 기존 피처 정책을 따른다.
- 결과: 거래/계좌 ID, 점수, 기준, 경고, cold_start, quality_flags, requires_review, 모델/피처 버전. 오류 422 입력, 413 크기 초과, 503 모델 미준비/분석 실패.
- Spring: `AnomalyService.analyze(AnomalyHistoryRequest)`. 기본 비활성. `moa.anomaly.enabled=true`, `moa.anomaly.base-url=http://127.0.0.1:8001` 설정 시 호출 가능. 연결 2초/읽기 10초 기본, 각각 `connect-timeout-ms`, `read-timeout-ms` 설정 가능. 공개 Controller 없음.
- DB 트랜잭션을 오래 유지한 채 호출하지 않도록 향후 저장 서비스에서 분리해야 한다. 자동 재시도·DB 저장·수집·공개 인가는 이번 범위가 아니다.
- Python 실행: ml 디렉터리에서 의존성 준비 후 `python -m uvicorn app.main:app --host 127.0.0.1 --port 8001`. 인증 미구현이므로 외부 공개 금지. 별도 호스트 운영 시 TLS/서버 인증 설계 필요.
- 검증: `python tests/verify_history.py`, `python tests/verify_service.py`. Spring은 `gradlew.bat test --tests com.nightstudy.moa.anomaly.AnomalyServiceTest`; 실행 중 Python 주소를 `MOA_TEST_PYTHON_URL`로 지정하면 실제 HTTP 테스트도 실행한다.


## 제안 스키마 기준 저장 코드 — DB 미적용 (2026-10-22)

AI작성: `AnomalyIngestionService.receive(StoredTransaction)`와 `AnomalyStoreService`, `AnomalyStorageMapper.xml`을 추가했다. 공개 Controller·프론트 변경 없음.

- 저장 Bean은 `moa.anomaly.persistence.enabled=true`일 때만 생성한다. 기본 비활성. DB 담당자 확정 및 테스트 DB 적용·검증 전 활성화하지 않는다. 모델 호출 활성화는 별도 `moa.anomaly.enabled` 설정이다.
- 전제: 제안한 group_transaction 추가 4컬럼과 transaction_anomaly_analysis 복합 PK가 모두 존재한다. DDL·기존 schema.sql은 변경/실행하지 않았다. source_sequence를 생략하는 DB안이면 SQL/DTO도 조정해야 한다.
- 내부 수집 어댑터가 검증한 단일 원천 거래만 전달한다. 원천 제공사·영구 거래 ID가 필수인 경로다. 원천 ID 미제공 거래 수집은 아직 지원하지 않는다. 실제 은행 호출·스케줄러는 없음.
- 임시 시간 계약은 KST LocalDateTime/초 정밀도다. 소수 초 입력은 반올림하지 않고 거부한다. 실제 DB·원천 시간대/정밀도가 확정되면 조정한다.
- register는 계좌 행 FOR UPDATE 후 중복/충돌 확인, 거래 INSERT와 revision 1 PENDING 등록을 REQUIRES_NEW에서 수행한다. 중복은 DUPLICATE 반환·재분석 없음. 내용 충돌은 덮어쓰지 않는다.
- receive는 NEVER로 외부 DB 트랜잭션 안 호출을 거부한다. 등록 커밋 후 모델 호출, 별도 트랜잭션에서 COMPLETED/FAILED 갱신. 서비스 프록시를 통해 호출해야 한다.
- fingerprint에는 정규화 입력·이력·예상 모델/피처 버전을 포함한다. 완료 직전 계좌 잠금 후 이력과 버전을 재검증하고 변경 시 FAILED 처리한다. 모든 관련 거래 작성자가 같은 계좌 잠금 규약을 따라야 한다.
- 이력이 2000건을 넘으면 2001건을 감지용으로 읽고 거래/PENDING 등록 후 HISTORY_LIMIT 실패 처리한다. 오래된 이력을 자른 점수를 반환하지 않는다. 대용량 계좌 최적화는 별도 작업이다.
- expected-model-version은 현재 합성 모델 해시가 기본값이다. 모델 변경 시 설정과 평가를 함께 갱신해야 한다.
- DB 상태 갱신 자체가 실패하거나 프로세스가 종료되면 PENDING이 남을 수 있다. 자동 복구·재시도·지연 거래 영향분 전체 재분석·revision 2 이상 생성은 미구현이다.
- 최신 결과 조회·권한 API도 아직 없다. 현재 DB Mapper SQL 파싱/파라미터 및 mock 기반 서비스 분기 테스트만 통과했으며 실제 SQL 실행·FK·UNIQUE·롤백·동시 잠금은 미검증이다.


## 지급 기록 업무 문맥 — 내부 조회 준비 (2026-10-22)

AI작성: 기존 payout_request.transaction_id FK만 재사용하는 AnomalyContextService/Mapper를 추가했다. DB·모델·경고·프론트는 변경하지 않았다.

- 기본 비활성. `moa.anomaly.context.enabled=true`에서만 서비스 Bean 생성. 공개 Controller 없음. 모임장/회원 인가는 호출 측이 먼저 검증해야 하며 groupId는 계좌 소속 범위 확인이지 회원 인증이 아니다.
- `findContext(groupId, transactionId)` 결과 상태: NO_LINK(연결 기록 없음), LINKED_COMPLETED_PAYOUT(완료·검토 기록 있는 지급 요청이 직접 연결됨), LINK_REQUIRES_REVIEW(중복·미완료·불일치·검토 정보 부족). 모델 경고 자동 해제/점수 보정 없음.
- 지급 요청의 회차 모임과 계좌 모임, 출금 여부, 금액, COMPLETED 상태, 검토자 및 요청/검토/완료 시각 순서를 확인한다. 현재 DB 기록의 정합성일 뿐 지급 승인 진위·정상성·사전 승인까지 입증하는 것은 아니다.
- expenses/fee_payment는 직접 거래 ID 연결이 없어 사용하지 않는다. 금액·날짜 유사 매칭은 하지 않는다. 향후 연결 컬럼 필요 여부는 DB 담당자와 별도 결정한다.
- 여러 지급 요청이 같은 거래를 참조하면 임의 한 건을 선택하지 않는다. 미일치/다른 모임 기록의 ID·금액·검토자는 외부 결과에 포함하지 않는다.
- 추가 분석 테이블·잔액/source 컬럼 없이 기존 스키마를 사용한다. 실제 DB 실행·업무 승인 기록 정확성·권한 API·화면 연동은 미검증이다. 연결 기록 없음은 정상 또는 이상 판정이 아니다.


## 내부 추론 응답 검증 보강 (2026-10-22)

AI작성: 응답의 ID뿐 아니라 거래 종류·시각(UTC offset 차이는 동일 순간 비교), 품질 코드 allowlist를 검사한다. 잘못된 응답은 저장 전에 거부한다. 지연/깨진 JSON/모순 응답/불일치/UTC 동등 시각은 AnomalyFaultTest로 검증한다. 외부 API 계약·DB·프론트는 변경하지 않았다.


## 2026-10-23 사용자 지정 schema.sql 기준 최신 분석 조회

AI작성: 기준은 back/src/main/resources/schema.sql의 groups/group_id이다. 실제 Aiven의 meeting 구조로 변경하지 않는다.

- AnomalyQueryService.findLatest(authenticatedMemberId, groupId, transactionId)는 내부 서비스다. 공개 Controller 없음. memberId는 향후 인증 계층에서 얻어야 하며 클라이언트 전달값을 신뢰하면 안 된다.
- 기본 스키마의 group_account.group_id와 group_member.group_id를 연결해 승인된 OWNER·탈퇴 아님·활성 회원·삭제되지 않은 모임의 거래인지 확인한다. 미승인/비소유/타 모임/없는 거래는 같은 NOT_FOUND 응답으로 처리한다.
- moa.anomaly.results.enabled=false가 기본이다. 분석 테이블을 조회하지 않고 STORAGE_UNAVAILABLE을 반환한다. 기본 schema.sql에 transaction_anomaly_analysis와 추가 거래 컬럼은 없다. 미반영 확장은 기본 구조로 가장하지 않는다.
- 분석 확장 반영 후 명시적으로 enabled=true일 때 최신 revision_no를 조회한다. 결과 없음은 NOT_ANALYZED, 최신 PENDING/FAILED는 점수·경고를 null로 반환한다. 과거 완료 점수로 대체하지 않는다.
- COMPLETED 점수·기준·경고·모델/피처 버전·품질 JSON의 정합성을 검증한다. 손상 결과는 내부 오류로 처리한다. 계좌번호·원천 거래 ID·설명·개인정보는 조회하지 않는다.
- 기본 파일과 불일치하는 현재 Aiven DB에서 이 조회를 활성화하지 않는다. DB 연결·DDL·프론트·모델은 이번 작업에서 변경하지 않았다. 테스트는 mock 분기·XML 파싱·기본 파일 계약 검증이며 실제 SQL 실행/회원 인증 연동을 의미하지 않는다.


## 정정: 사용자 첨부 최신 스키마를 기준으로 적용 (2026-10-23)

AI작성: 이 절이 앞선 구버전 schema.sql 기준의 미반영 판단보다 우선한다. 기준은 사용자가 제공한 붙여넣은 텍스트.txt의 전체 정의이며 테스트 계약은 src/test/resources/anomaly-approved-schema.sql에 원본 해시와 함께 추출했다. 기존 resources/schema.sql과 실제 DB는 이번에 덮어쓰거나 변경하지 않았다.

- groups/group_id 관계와 기존 거래 저장 → PENDING 등록 → Python 분석 → COMPLETED/FAILED 저장 → 최신 결과 조회 구조를 유지한다. 첨부 정의에 balance_after/source_provider/source_transaction_id 및 transaction_anomaly_analysis가 모두 존재한다.
- source_sequence는 첨부 스키마에 없다. 저장 DTO·SQL 의존을 제거하고 Python 요청에는 null을 전달한다. bank balance를 버리지 않으며 동시각 거래는 기존 순서 미상 정책을 적용한다. API의 선택 source_sequence 필드/품질 코드는 하위 호환과 피처 규격을 위해 유지한다.
- 잘못 추가한 분석 비저장 우회 서비스·기본 거래 전용 Mapper·구버전 파일 기반 저장 차단기는 제거했다. 제거한 신규 파일 7개는 ChatGPT 작업 폴더의 deliverables/schema-correction-backup에 백업했다.
- persistence.enabled / results.enabled / context.enabled / anomaly.enabled의 기존 명시적 활성화 정책은 유지한다. 실제 DB 구조·시간대 및 테스트 데이터 범위 확인 없이 공유 DB에 쓰지 않는다.
- 새 계약 테스트는 Mapper 결과 컬럼 존재, source_sequence SQL 미사용, group_id 관계를 검사한다. Python 테스트는 순번 없는 동시각 요청의 source_sequence_missing/same_timestamp_order_unknown 문맥을 확인한다.
- 이번 검증은 컴파일·단위 테스트·로컬 Python API이며 실제 DB SQL 실행/트랜잭션/권한 Principal·프론트 연동 완료를 뜻하지 않는다.


## 2026-10-23 로컬 DB 통합 및 독립 Python 환경 검증

AI작성: 공유 Aiven 설정을 로드하지 않는 AnomalyLocalDatabaseTest를 추가했다. MOA_LOCAL_DB_TEST=true 및 명시적 로컬 JSON 설정/127.0.0.1 Python 주소가 있을 때만 실행된다.

- 첨부 스키마를 적용한 격리 MySQL 8.0.46 / moa_ml_test_20261023 / +09:00에서 Spring 트랜잭션·실제 MyBatis·모델 HTTP 호출을 검증했다. 계정·주소를 코드에 넣지 않고 기존 application.yaml은 변경하지 않았다.
- 합성 거래 저장·모델 분석·최신 조회, 중복·내용 충돌·타 회원/모임 차단, 분석 503 시 원거래 보존, 동시 중복 한 건 삽입, PENDING 등록 오류 시 거래 롤백을 확인했다. revision 2/3은 조회 검증용 직접 fixture이며 자동 재분석 구현이 아니다.
- 모델 경고가 나온다는 연결 검증이지 실제 금융 탐지 성능을 보증하는 테스트는 아니다. 인증 Principal/공개 API/브라우저 연결은 아직 미완료다.
- ml/.venv를 새로 생성해 requirements.txt 설치 후 pip check와 기존 14건 점수·입력 오류·미래 제외·동시각 순번 미상 테스트를 통과했다. PYTHONPATH/사용자 패키지는 사용하지 않았다.
- Python 3.14.6에서 python -m venv .venv → .venv/Scripts/python.exe -m pip install -r requirements-lock.txt → PowerShell의 ./run-server.ps1 순서로 실행한다. 기본 포트 8001, -Port로 변경 가능. Windows PowerShell 정책이 스크립트를 막으면 .venv/Scripts/python.exe -E -s -m uvicorn app.main:app --host 127.0.0.1 --port 8001을 사용한다.
- 검증은 같은 PC의 새 가상환경에서 수행했다. 다른 PC/OS에서의 실제 실행까지 검증했다고 표현하지 않는다. 잠금 파일은 버전 목록이며 공급망 해시 잠금은 아니다.
