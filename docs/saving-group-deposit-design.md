# 계모임 공용 계좌 테스트 입금 설계·작업 기록

작성일: 2026-11-06

AI작성: 사용자 확정 정책에 따라 구현한 결과입니다. 구현 코드의 개발자 검토는 아직 수행되지 않았습니다. DB 구조는 다운로드 폴더로 전달한 DB 변경 요청서가 기준이며 담당자의 실제 적용은 별도입니다.

## 동작과 범위

- 실제 금융 API·계좌 발급·입금 확인·웹훅·이체를 호출하지 않고 내부 원장에 DEPOSIT 거래를 생성합니다.
- 사용자에게는 `테스트 입금`으로 안내합니다. 원장 적요에도 개인정보 없이 같은 문구를 사용합니다.
- 기존 공통 groupId 경로를 사용합니다. saving_group_id와 혼동하지 않습니다.
- 진행중 상태 02인 계모임의 현재 APPROVED OWNER·MANAGER·MEMBER만 허용합니다. 삭제 회원은 거절합니다.
- 마스킹된 공용 계좌 하나를 선택하며 계좌번호 원문·대상 계좌 ID·처리자 ID를 본문으로 받지 않습니다.
- 금액은 필수, 1원 이상, 원 단위 정수, DECIMAL(15,2)에 저장 가능한 13자리 정수 범위입니다. 별도의 업무상 한도·고정 회비 일치 규칙은 추가하지 않았습니다.
- 회비·순번계 납부 상태, 알림, 실제 은행 잔액은 변경하지 않습니다. 임의의 납부 종류·증빙·은행 입금 상태를 만들지 않습니다.
- 성공 시 기존 원장 합산으로 잔액을 조회합니다. balance_after에는 기존 쓰기 정책이 없어 값을 생성하지 않습니다.

## 반복 입금과 확인

1. 회원·대상 계좌·금액이 같은 최초 일반 완료 요청의 completed_at부터 5분 미만이면 확인 안내를 반환합니다.
2. 일반 완료 요청은 window_anchor_request_id가 NULL입니다. 추가 확인 입금은 이 요청을 참조하므로 기준 시간을 연장하지 않습니다.
3. 기준 시각으로부터 정확히 5분 이상이면 새로운 일반 입금으로 처리합니다. SQL은 completed_at > 현재시각-5분입니다.
4. 확인 필요 응답에서는 원장·잔액을 바꾸지 않고 보조 기록만 저장합니다.
5. 확인값은 32바이트 SecureRandom URL-safe 문자열이며 DB에는 SHA-256 해시만 저장합니다. 발급 후 5분 미만에만 사용할 수 있습니다. 정확한 만료 시각부터 거절합니다.
6. 추가 확인 성공 시 같은 대기 행을 COMPLETED로 바꾸고 확인 해시를 제거하며 거래 1건을 연결합니다.
7. 같은 요청 키로 재전송하면 기존 완료 결과만 반환합니다. 본인·현재 권한·대상 계좌 검증은 재전송에서도 수행합니다.
8. 동일 요청 키를 다른 금액이나 계좌에 사용하면 409입니다. 의도한 별도 입금은 새 요청 키로 제출합니다.
9. 대기 상태의 최초 요청 응답이 유실되면 같은 키로 재요청하여 확인값을 재발급합니다. 기존 확인값은 무효화하고 새 기한은 재발급 후 5분입니다. 최초 입금의 중복 판단 기준은 변경하지 않습니다.
10. 안내 문구는 자정 경계에서도 사실에 맞도록 `최근 5분 이내 동일한 계좌로 동일한 금액을 보낸 적이 있습니다`를 사용합니다.

취소는 화면의 추가 확인을 닫는 동작이며 이미 저장된 대기 기록은 남습니다. 서버 취소 API·별도 CANCELLED 상태는 추가하지 않았습니다. 확인값은 기한까지 존재할 수 있으나 그 자체로 거래가 생성되지는 않습니다. 확인값 즉시 폐기·보존 기간·자동 정리는 추후 정책이 필요합니다.

## DB 의존성: 담당자 적용 필요

이번 코드 작업에서는 schema.sql이나 실제 DB를 변경하지 않았습니다. 다음 테이블이 없으면 입금 기능은 동작하지 않습니다. 기존 계좌 조회·거래내역 조회는 기존 구조를 사용합니다.

| 신규 테이블 컬럼 | 타입·핵심 제약 |
|---|---|
| deposit_request_id | BIGINT PK AUTO_INCREMENT |
| account_id | BIGINT NOT NULL, 기존 group_account 참조 |
| member_id | BIGINT NOT NULL, 기존 members 참조 |
| amount | DECIMAL(15,2) NOT NULL, 양수 원 단위 |
| request_key | VARCHAR(64) ascii/ascii_bin NOT NULL, UNIQUE(member_id, request_key) |
| status | VARCHAR(30) ascii/ascii_bin NOT NULL, PENDING_CONFIRMATION / COMPLETED |
| window_anchor_request_id | BIGINT NULL, 같은 테이블 최초 일반 완료 요청 참조 |
| confirmation_token_hash | CHAR(64) ascii/ascii_bin NULL UNIQUE |
| confirmation_expires_at | DATETIME(6) NULL |
| transaction_id | BIGINT NULL UNIQUE, 기존 group_transaction 참조 |
| requested_at | DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) |
| completed_at | DATETIME(6) NULL |

테이블명은 saving_group_mock_deposit_request입니다. 상세 FK·CHECK·인덱스·실행 SQL은 DB 담당자에게 전달한 `모아_계모임_테스트입금_DB변경요청서_2026-11-06.md`를 기준으로 검토합니다. 구조가 달라지면 배포 전에 DTO·Mapper·이 문서를 동기화해야 합니다. DB 적용 완료 시 schema.sql 및 팀 DB 설계서·모델 명세도 담당자가 확정 구조로 반영해야 합니다.

## 트랜잭션·분리

- 입금 전용 Service 인터페이스·구현체를 분리하고 전용 Controller는 요청·응답만 담당합니다.
- 기존 SavingGroupController 생성자와 소비 테스트를 바꾸지 않도록 동일 도메인 폴더에 SavingGroupDepositController를 추가했습니다.
- 모임→회원 관계→계좌→요청 순서로 잠급니다. 원장 저장과 보조 기록 저장·확인 소비는 하나의 @Transactional 경계입니다.
- 현재 읽기 FOR UPDATE와 회원별 요청 키 UNIQUE를 사용해 동시 요청을 직렬화합니다.
- Mock 지연은 기존 방식의 300~800ms이며 첫 DB 잠금 전에 수행합니다. 테스트에서는 지연만 대체합니다.
- 외부 발급 Service·금융 SDK·가상계좌 조회 모듈을 의존성으로 주입하지 않습니다.
- 시간은 DB CURRENT_TIMESTAMP(6)를 기준으로 비교합니다. DATETIME은 시간대를 저장하지 않으므로 운영 DB 연결의 기존 시각 기준을 확인해야 합니다.

## 화면과 재시도

- 진행중·승인 역할·조회 성공한 발급 계좌에서만 입금 폼을 표시합니다. 최종 판단은 서버에서 재검증합니다.
- 입력 금액은 천 단위 콤마와 원 단위로 안내합니다.
- 버튼 연속 클릭은 ref 잠금과 disabled 상태로 막습니다. 확인 대기 중 금액·계좌 변경을 막습니다.
- 통신 결과가 불확실하면 현재 화면 메모리에 요청 키·금액·확인값을 보존하고 같은 요청만 다시 확인합니다. 확인 전송 결과 불명확 상태에서는 취소로 다른 입금을 등록하도록 유도하지 않습니다.
- 새로고침·화면 이탈 후까지 요청 키를 보존하는 영구 저장은 구현하지 않았습니다. 화면을 벗어난 뒤에는 거래내역을 먼저 확인해야 합니다.
- 확인 만료·교체된 확인값 오류 시 같은 요청 키로 안내를 재발급할 수 있습니다.
- 성공 후 계좌 잔액을 재조회하고 거래내역 패널의 재조회 키를 갱신합니다. 목록의 페이지·필터 상태는 패널 재생성 시 초기화됩니다.
- 입금 성공 안내는 잔액 조회 상태와 분리합니다. 재조회 실패는 기존 카드의 재조회 버튼으로 복구합니다.
- 모임·로그인 세션 변경 시 부모 key와 응답 유효성 검사로 이전 입금 상태를 폐기합니다.

## 단계별 작업 파일·내용·커밋

Java 기준 경로: back/src/main/java/com/nightstudy/moa/savingGroup/

테스트 기준 경로: back/src/test/java/com/nightstudy/moa/savingGroup/

| 단계 | 작업 파일 | 내용 | 실제 커밋 |
|---|---|---|---|
| 2 | Java dto/SavingGroupDepositRequest.java, SavingGroupDepositConfirmRequest.java, SavingGroupDepositResponse.java, SavingGroupDepositRow.java; mapper/SavingGroupMapper.java; back/src/main/resources/mappers/SavingGroupMapper.xml; 테스트 service/SavingGroupDepositMapperTest.java | 요청 검증·보조 기록·내부 원장 SQL | 28dc979 — feat: 계모임 테스트 입금 DTO와 Mapper 추가 |
| 3 | Java service/SavingGroupDepositService.java, SavingGroupDepositServiceImpl.java; 테스트 service/SavingGroupDepositServiceTest.java | 권한·진행 상태·금액·5분 기준·확인값·재전송 처리 | 3701b8e — feat: 계모임 테스트 입금 서비스와 중복 확인 구현 |
| 4 | Java controller/SavingGroupDepositController.java; 테스트 controller/SavingGroupDepositControllerTest.java, SavingGroupDepositSecurityIntegrationTest.java | 입금·추가 확인 API 및 실제 JWT 필터 검증 | 17084b1 — feat: 계모임 테스트 입금 요청과 확인 API 연결 |
| 5 | 테스트 service/SavingGroupDepositTransactionTest.java, SavingGroupDepositMySqlIntegrationTest.java | 트랜잭션 롤백·전용 DB 동시 요청 검증 코드 | a7f8d88 — test: 계모임 테스트 입금 저장과 중복 확인 통합 검증 |
| 6 | front/src/api/savingGroups.js, savingGroupDeposit.test.js | JWT client 입금·확인 함수와 오류·응답 필드 검증 | 6e7fc0b — feat: 계모임 테스트 입금 프론트 API 추가 |
| 7 | front/src/components/savingGroup/SavingGroupDepositForm.jsx, SavingGroupDepositForm.test.js | 마스킹 계좌·금액·중복 확인·재시도 화면 | c512926 — feat: 계모임 테스트 입금 입력폼과 중복 확인창 추가 |
| 8 | front/src/components/savingGroup/SavingGroupAccountCard.jsx, SavingGroupAccountCard.test.js, SavingGroupDetail.jsx; front/src/pages/SavingGroupDetailPage.jsx | 진행중 모임 화면 연결·잔액 및 거래내역 갱신 | 2736967 — feat: 계모임 테스트 입금 화면 연결과 잔액 갱신 |
| 9 | front/src/api/savingGroupDeposit.test.js; front/src/components/savingGroup/SavingGroupDepositForm.test.js | 응답 유실 재전송·확인 만료·입력 및 버튼 잠금 검증 | 355d1e2 — test: 계모임 테스트 입금 화면 회귀 검증 |
| 최종 | docs/saving-group-deposit-design.md, docs/saving-groups-api.md, docs/ai-usage-log.md | 설계·API·단계별 파일·검증 결과 일괄 문서화 | docs: 계모임 테스트 입금 설계와 API 및 검증 결과 정리 |

마지막 문서 커밋 해시는 해당 커밋 생성 후 git log에서 확인합니다. DB 작업은 담당자에게 분리했고, 거래내역 API·화면은 최근 기존 커밋의 구현을 재사용하여 별도 신설하지 않았습니다.

## 실행한 검증

- Java 17: C:/dev/jdk-17.0.12_windows-x64_bin/jdk-17.0.12 경로 지정으로 실행했습니다. 저장소 설정은 변경하지 않았습니다.
- 전체 테스트 컴파일은 기존 AuthSessionService·MemberFeatureRestrictionService·KakaoApiClient 패키지 참조와 기존 생성자 불일치 때문에 실패합니다. 무관한 코드는 수정하지 않았습니다.
- 임시 Gradle init script로 입금·기존 계좌 Service·거래내역·계좌 암호 보조 클래스만 포함해 실행했습니다. 총 69건 중 67건 통과, MySQL 전용 2건 건너뜀, 실패 0입니다.
- 입금 관련 신규 테스트만 보면 25건 중 24건 통과·MySQL 전용 1건 건너뜀입니다.
- 프론트 src/api 및 components/savingGroup의 기존 Node/Vite SSR 회귀 145건 모두 통과했습니다.
- npm run build 통과. 기존 대형 번들 경고가 있습니다.
- 변경 프론트 파일의 oxlint는 오류 없음. 기존 SavingGroupDetail의 미사용 선언 2건 경고는 유지했습니다.
- git diff --check 통과.
- 실제 금융 서비스 호출 없음. 실제 DB 연결·MySQL CREATE 실행·브라우저 클릭 검증은 수행하지 않았습니다.
- MySQL 테스트는 최소 테스트 테이블을 사용하는 실제 Mapper·트랜잭션 검증이며 DB 담당자 요청서의 전체 FK·CHECK 검증을 대체하지 않습니다.

## DB 적용 후 수동 검증

1. 담당자가 확정 구조를 적용하고 실제 DB 시간 기준·제약을 확인합니다.
2. 테스트 데이터의 진행중 모임·승인 OWNER/MANAGER/MEMBER로 첫 입금 후 원장 1건과 잔액 증가를 확인합니다.
3. 새 요청 키로 5분 이내 같은 금액을 보내 확인 대기만 생성되는지 확인합니다.
4. 취소 시 거래 변화 없음, 추가 확인 시 거래 1건 증가, 같은 확인 재전송 시 증가 없음을 확인합니다.
5. 다른 회원·계좌·금액, 정확히 5분 경계, 확인 만료·위조·재발급, 동시 요청을 검증합니다.
6. 비회원·LEFT·PENDING·EXPELLED·REJECTED·삭제 회원·다른 모임 회원과 비진행 모임의 실패를 확인합니다.
7. 브라우저에서 마스킹·금액·로딩·오류·잔액 재조회·모임/계정 전환·모바일·키보드를 확인합니다.

전용 MySQL 통합 테스트는 기존 MOA_TEST_DB_ENABLED=true 및 MOA_TEST_DB_URL/USER/PASSWORD 설정이 필요합니다. URL은 자격증명을 포함하면 안 되며 DB 이름은 moa_join_test_로 시작하는 전용 이름이고 테이블·뷰가 없는 빈 DB여야 합니다. 테스트가 만든 고정 이름의 테이블만 finally에서 정리합니다. 실서비스 DB에는 실행하지 않습니다.
