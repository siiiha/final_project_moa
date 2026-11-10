# 순번계 순번 지정·다음 달 예약 적용 설계 및 작업 기록

AI작성: 2026-11-06 사용자 확정 정책과 단계별 구현·검증을 마지막 단계에서 일괄 정리한 문서입니다.

## 확정 정책

- 현재 승인된 OWNER만 조회·지정할 수 있습니다. MANAGER·MEMBER·다른 모임 OWNER·탈퇴·추방 회원은 지정할 수 없습니다.
- 미삭제·승인된 현재 회원 중 지급 완료 회원은 재배정에서 제외합니다. 진행 중에는 현재 지급 회차 회원도 제외합니다. OWNER는 같은 기준을 적용합니다.
- 진행 중에는 남은 회원이 0명 또는 1명이면 지정할 수 없습니다. 4인 모임의 3회차에서 1·2회차가 지급 완료되었다면 1~3회차는 고정이고 4회차는 마지막 회원으로 확정됩니다.
- 5인 모임의 2회차에서 1회차가 지급 완료되었다면 3~5회차를 재배정할 수 있습니다. 세 회원의 순번이 모두 같으면 저장을 거절하고, 두 회원만 순번을 맞바꾸면 저장할 수 있습니다.
- 모집중(01)은 즉시 적용·재지정합니다. 진행중(02)은 다음 달부터 적용할 목록을 저장·전체 교체합니다.
- 배정값은 실제 회차 번호입니다. 기존 배정과 비교할 때는 대상 월에 저장한 예약을 우선하고, 대상 월 예약이 없으면 현재 적용 순번을 사용합니다.
- 회원 구성 변경 시 자동 재배정하지 않습니다. 예약안과 현재 대상이 다르면 재지정이 필요합니다.
- 회차 생성, 확정 금액 계산·변경, 지급 상태 변경, 알림·별도 변경 이력, 실제 금융 호출은 추가하지 않았습니다.

## DB 계약과 담당 분리

DB 담당자가 다운로드 폴더의 moa-turn-order-db-change-request.md에 따라 테이블을 적용합니다. 이번 구현은 schema.sql이나 마이그레이션을 수정하지 않습니다. 테이블 적용 전 신규 API와 순번계 지급 연계 기능은 정상 동작할 수 없으므로 DB 적용과 애플리케이션 배포를 함께 조율해야 합니다.

| 모델 | 컬럼·제약 | 용도 |
|---|---|---|
| 기존 group_member | sequence_number | 현재 기본 순번 |
| saving_group_turn_order_plan | turn_order_plan_id PK, saving_group_id FK, effective_month DATE, start_round_number, assigned_by FK, assigned_at | 적용월별 예약 목록 |
| 예약 목록 제약 | UNIQUE(saving_group_id, effective_month), 적용월 1일, 시작 회차 1 이상 | 동일 적용월 예약 전체 교체 |
| saving_group_turn_order_plan_member | turn_order_plan_id FK, member_id FK, round_number | 실제 회차 배정 |
| 상세 제약 | PK(turn_order_plan_id, member_id), UNIQUE(turn_order_plan_id, round_number), 회차 1 이상 | 회원·회차 중복 방지 |

saving_group_id와 공통 group_id는 구분합니다. 지급 완료는 payout_request를 meeting_round에 연결한 뒤 같은 group_id 범위의 COMPLETED로 판단합니다. 같은 saving_group_id를 다른 운영 기간에 재사용하지 않는 기존 모임 단위를 기준으로 합니다. 재사용이 필요하면 별도 운영 기간 식별 정책을 먼저 정해야 합니다.

## 적용과 트랜잭션

- 모임 공통 행 → OWNER 관계 → 승인 회원 순서로 잠금을 얻고 전체 요청을 검증한 후 저장합니다.
- 모집중 저장은 sequence_number를 갱신합니다. 같은 값의 재저장은 변경 행 수 설정에 영향받지 않도록 생략합니다.
- 진행중 저장은 잠금 안에서 조회 당시 회차를 확인한 후 적용월별 헤더를 upsert하고 기존 상세를 교체합니다. 확인된 고정 순번을 새 예약에 보존하고 미래 회차 배정을 저장합니다. 어느 한 건이라도 실패하면 전체 롤백합니다.
- 같은 모임의 지정·회원 관리·지급 처리는 기존 모임 잠금을 공유합니다.
- Asia/Seoul 월과 시작일을 기준으로 다음 달의 회차를 계산합니다.
- 스케줄러가 현재 컬럼을 덮어쓰지 않습니다. 해당 회차의 월 이하 최신 예약을 조회해 유효 순번으로 사용합니다.
- 지급 요청과 승인에 같은 순번 판정 함수를 사용합니다. 지난 회차 심사는 현재 월이 아니라 해당 요청 회차의 적용월을 사용합니다.
- 지급 완료 회원은 기존 순번 또는 예약 순번과 관계없이 다시 요청·승인할 수 없습니다.
- 예약 이후 대상 회원이 추가·탈퇴·추방되면 유효성 충돌로 차단합니다. 지급 완료 회원이 앞으로 배정된 예약에 남아 있으면 자동으로 회차를 당기지 않고 재지정을 요청합니다.
- 배정 범위에 기존 PENDING·APPROVED·COMPLETED 지급 요청이 있으면 자동 취소하지 않고 충돌로 처리합니다.
- 진행중 배정이 종료월을 초과하면 자동 연장하지 않고 충돌로 처리합니다.

## API

기존 JWT memberId 속성, ApiResponse, ApiException 및 GlobalExceptionHandler를 재사용합니다. GET 응답에는 Cache-Control: no-store를 설정합니다.

- GET /api/saving-groups/{savingGroupId}/turn-orders
- PUT /api/saving-groups/{savingGroupId}/turn-orders
- /saving-groups 경로도 같은 별칭으로 지원합니다.

현재 2회차에 재배정 가능한 승인 회원이 두 명인 PUT 예시입니다. 모집 중에는 currentRoundNumber를 생략합니다.

```json
{"currentRoundNumber":2,"turnOrders":[{"memberId":"30","sequenceNumber":3},{"memberId":"40","sequenceNumber":4}]}
```

GET·PUT은 같은 응답 모델을 반환합니다. 아래는 data 예시입니다.

```json
{
  "effectiveMonth":"2026-12-01",
  "currentRoundNumber":2,
  "startRoundNumber":3,
  "scheduled":true,
  "needsReassignment":false,
  "members":[
    {"memberId":"30","nickname":"테스트계주","role":"OWNER","paid":false,"currentSequenceNumber":4,"plannedSequenceNumber":3},
    {"memberId":"40","nickname":"테스트계원","role":"MEMBER","paid":false,"currentSequenceNumber":3,"plannedSequenceNumber":4},
    {"memberId":"50","nickname":"현재회차계원","role":"MEMBER","paid":false,"currentSequenceNumber":2,"plannedSequenceNumber":2,"fixedReason":"CURRENT_ROUND"},
    {"memberId":"60","nickname":"지급완료계원","role":"MEMBER","paid":true,"currentSequenceNumber":1,"plannedSequenceNumber":1,"fixedReason":"PAID"}
  ]
}
```

nullable 순번 필드는 공통 JSON 설정에 따라 생략될 수 있습니다. 프론트는 null·누락 모두 미지정으로 처리합니다. needsReassignment는 다음 달 예약이 없거나 다음 달 전체 대상이 일치하지 않음을 뜻합니다. 다음 달 새 예약이 없다는 사실만으로 이번 달 지급을 막지는 않습니다.

| 상태 | 코드 | 조건 |
|---|---|---|
| 401 | UNAUTHORIZED | 미인증 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 해당 모임 승인 OWNER가 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 없는 모임·삭제 모임 |
| 400 | BAD_REQUEST 또는 공통 Validation 코드 | 잘못된 경로·필수값·양수 검증 |
| 400 | SAVING_GROUP_TURN_ORDER_INVALID | 대상 불일치·회원/회차 중복·누락·회차 범위 오류 |
| 409 | SAVING_GROUP_TYPE_INVALID | 순번계가 아님 |
| 409 | SAVING_GROUP_TURN_ORDER_ROUND_CHANGED | 진행 중 기준 회차가 최신 회차와 다르거나 누락됨 |
| 409 | SAVING_GROUP_TURN_ORDER_CONFLICT | 동일 순번·변경 가능 회원 2명 미만·상태·기간·기존 지급 요청 충돌·회원 변경·저장 실패 등 |

## 화면 흐름

계모임 상세 → 계원 관리 → 순번 지정 → 현재/예약 회차 확인 → 재배정 가능 회원의 회차 선택 → 확인창 → 저장 결과 표시.

- 승인 OWNER·순번계·모집중/진행중에서만 진입 버튼을 표시합니다.
- 지급 완료 회원과 현재 지급 회차 회원은 읽기 전용으로 표시합니다. 한 명만 남으면 마지막 순번도 읽기 전용으로 표시합니다.
- 진행중에는 적용월과 이번 달 순번 유지 안내를 표시합니다.
- 미선택·중복 회차·저장중에는 저장 버튼을 막습니다. 진행 중 순번이 모두 같으면 `이전과 동일한 순번입니다.`를 안내하고 저장을 막습니다.
- 저장중 역할 변경·추방 처리를 같은 요청 잠금으로 막습니다.
- 회차 변경 충돌 시 오류와 함께 최신 순번을 다시 불러옵니다. 다른 저장 오류는 입력을 유지합니다.
- 모임·인증 변경 시 컴포넌트를 새로 생성하고 이전 요청·입력을 폐기합니다.
- 기존 순번 추천 기능은 유지합니다. 추천 결과를 입력에 자동 반영하는 기능은 포함하지 않았습니다.

## 단계별 작업 파일·내용·커밋

2026-11-10 고정 순번 변경은 `44a7b827` 서버 규칙, `9bd8a694` 저장 API와 지급 적용, `6ed396df` 화면, `cf7c531f` 트랜잭션 검증 순서로 진행했습니다. Java 17에서 순번계 테스트 68건이 통과했고 전용 MySQL 1건은 연결 설정이 없어 건너뛰었습니다. 프론트 API·화면 렌더링 테스트 5건과 빌드가 통과했습니다. 전체 백엔드 테스트는 기존 테스트의 오래된 클래스 참조로 컴파일에 실패했습니다.

브라우저에서 4인·3회차 순번 지정 화면을 열어 마지막 순번이 읽기 전용인지 확인합니다. 5인·2회차에서는 동일 배정의 저장 제한과 두 계원의 순번 교환 저장을 확인합니다. Postman에서는 조회 당시 회차를 담아 PUT을 보내고, 과거 회차 값의 409 응답과 계원·순번 중복·누락의 400 응답을 확인합니다.

Java 경로: back/src/main/java/com/nightstudy/moa/savingGroup/. 테스트 경로: back/src/test/java/com/nightstudy/moa/savingGroup/.

| 단계 | 작업 파일 | 작업 내용 | 커밋 |
|---|---|---|---|
| 1 | dto/SavingGroupTurnOrderPlanRow.java, dto/SavingGroupTurnOrderAssignmentRow.java, mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, service/SavingGroupTurnOrderAssignmentMapperTest.java(테스트) | 예약 저장·적용월·지급 완료 조회 매핑. 스키마는 미변경 | 910d2ed — feat: 순번 지정 저장 구조와 Mapper 추가 |
| 2 | dto/SavingGroupTurnOrderUpdateRequest.java, dto/SavingGroupTurnOrderResponse.java, service/SavingGroupTurnOrderService.java, service/SavingGroupTurnOrderServiceImpl.java, service/SavingGroupTurnOrderAssignmentServiceTest.java(테스트) | 권한·전체 대상·실제 회차 검증, 즉시/예약 저장 | b8c8df1 — feat: 계주 순번 지정과 재지정 서비스 추가 |
| 3 | service/SavingGroupTurnOrderServiceImpl.java, service/SavingGroupPayoutServiceImpl.java, service/SavingGroupServiceImpl.java, service/SavingGroupTurnOrderEffectiveSequenceTest.java(테스트) | 적용월별 지급 요청·승인 연계, 중복 수령 차단 | 54e14c0 — feat: 다음 달 예약 순번 적용과 지급 검증 연계 |
| 4 | controller/SavingGroupTurnOrderController.java, controller/SavingGroupTurnOrderAssignmentControllerTest.java(테스트) | GET·PUT·경로 별칭·Validation·공통 응답 | 57eb6a8 — feat: 순번계 순번 조회와 지정 API 추가 |
| 5 | front/src/api/savingGroups.js, front/src/api/savingGroupTurnOrder.test.js | 조회·저장 API, 응답 검증·오류·취소 | e2e7bfd — feat: 순번 조회와 지정 프론트 API 연결 |
| 6 | front/src/components/savingGroup/SavingGroupTurnOrderAssignmentPanel.jsx, 같은 경로의 .test.js | 전체 회원 입력·현재/예정 회차·적용월·상태 안내 | ea9a914 — feat: 계주 순번 지정 패널 구현 |
| 7 | front/src/components/savingGroup/SavingGroupMembersPanel.jsx | 계원 관리 진입·회원 관리와 저장 잠금 연결 | 4dd7d10 — feat: 계원 관리에 순번 지정 화면 연동 |
| 8 | 신규 controller/SavingGroupTurnOrderAssignmentSecurityTest.java, service/SavingGroupTurnOrderAssignmentTransactionTest.java, service/SavingGroupTurnOrderAssignmentMySqlIntegrationTest.java(이상 테스트), 기존 지정 Service·테스트와 프론트 패널 2개 보완 | JWT·롤백·선택 실행 MySQL 동시성 검증, 멱등 저장·인증 전환 상태 보완 | 87e4abd — test: 순번 지정과 예약 적용 통합 검증 |
| 9 | docs/saving-group-turn-order-design.md, docs/saving-groups-api.md, docs/ai-usage-log.md | 최종 정책·API·DB 계약·검증·작업 내역 일괄 문서화 | docs: 순번 지정 API와 화면 및 검증 결과 정리 |

계획의 기존 SavingGroupController 대신 별도 SavingGroupTurnOrderController를 사용해 기존 Controller 생성자와 테스트 연결을 보존했습니다. SavingGroupDetail은 이미 필요한 props를 전달하므로 수정하지 않았습니다.

## 검증 결과

- 최종 순번·지급 관련 백엔드 224건 중 221건 통과, 전용 MySQL 3건 건너뜀, 실패 0건.
- 프론트 API·지정 패널·추천 회귀 SSR 6건 통과.
- 변경 프론트 파일 oxlint 경고·오류 없음.
- 최종 코드의 프로덕션 빌드 통과. 기존 큰 번들 경고 유지.
- 계모임 전체 검사 시점에는 857건 중 2건 실패·14건 건너뜀. 실패는 변경하지 않은 SavingGroupDeletionApplicationServiceTest의 ownerCanRequestDeletion, duplicatePendingRequestIsRejected이며 기존 OWNER 관계 검증에 필요한 테스트 대역이 없는 상태입니다. 전체 검사가 통과했다고 보고하지 않습니다.
- 기본 전체 테스트 컴파일은 무관한 기존 클래스 import 오류로 실패합니다. 계모임 범위에도 기존 생성자 불일치 테스트 3개가 있어 임시 init script에서 제외했습니다. 저장소 빌드 설정은 변경하지 않았습니다.
- 실제 MySQL·브라우저 클릭·모바일 상호작용은 미실시입니다. DB 적용 후 후속 검증이 필요합니다.

실행 명령(back 폴더):

```powershell
.\gradlew.bat '-Dorg.gradle.java.installations.paths=C:/dev/jdk-17.0.12_windows-x64_bin/jdk-17.0.12' -I build/turn-order-tests.init.gradle test --tests '*SavingGroupTurnOrder*' --tests '*SavingGroupPayout*' --offline --console=plain
```

임시 init script(빌드 산출물, 커밋 제외):

```groovy
allprojects {
  plugins.withId('java') {
    sourceSets.test.java.setIncludes(['com/nightstudy/moa/savingGroup/**'])
    sourceSets.test.java.exclude('**/SavingGroupPayoutDecisionSecurityTest.java', '**/SavingGroupMemberMySqlIntegrationTest.java', '**/SavingGroupPayoutDecisionTransactionTest.java')
  }
}
```

선택 실행 MySQL 테스트는 MOA_RUN_MYSQL_JOIN_TESTS=true와 MOA_TEST_DB_URL/USER/PASSWORD가 필요합니다. 테스트는 이름이 moa_join_test_로 시작하는 빈 전용 DB만 허용하며 테스트 테이블 생성·삭제를 수행합니다. 운영 DB에 실행하지 않습니다. 실제 비밀번호·접속 정보는 이 문서에 기록하지 않습니다.

## Postman·브라우저 후속 검증

1. DB 담당자의 테이블 적용 확인 후 테스트용 승인 OWNER·미지급 회원·지급 완료 회원을 준비합니다.
2. OWNER JWT로 GET하고 전체 회원과 지급 완료 제외 상태를 확인합니다.
3. 모집중 PUT 후 GET으로 즉시 순번을 확인하고 같은 요청을 재전송합니다.
4. 진행중 PUT 후 이번 달 기본 순번이 유지되고 다음 달 예약 전체가 저장되는지 확인합니다.
5. 같은 적용월 재지정 후 예전 예약 일부가 남지 않는지 확인합니다.
6. MANAGER·다른 모임 OWNER·미인증, 중복/누락/비승인 회원 요청 실패와 DB 불변성을 확인합니다.
7. 전용 테스트 환경의 시간·시작일로 월 경계를 검증하고 현재 회차 지급 요청·승인이 같은 적용 순번을 쓰는지 확인합니다.
8. 예약 이후 가입 승인·탈퇴·추방·지급 완료 변경에서 자동 재배정 없이 충돌·재지정 안내를 확인합니다.
9. 브라우저에서 지정 패널 열기·저장 취소·저장·재조회·실패 후 재시도·계정 전환·키보드·모바일을 확인합니다.

## 남은 TODO와 제한

- DB 담당자 적용 전이며 실제 MySQL 동시 저장·롤백은 아직 실행하지 않았습니다.
- 이번 달 지급 대상자의 탈퇴·추방, 과거 미지급 회차의 이월, 승인된 미완료 지급 요청의 취소·이월은 별도 지급 정책으로 남깁니다. 자동 처리하지 않습니다.
- 예약이 적용된 월에 회원·지급 상태가 달라져 유효하지 않게 된 경우 즉시 자동 복구하지 않습니다. 현재 정책상 진행중 재지정은 다음 달부터이므로 해당 월의 예외 복구 정책은 추가 확인이 필요합니다.
- 동일 모임 ID의 운영 기간 재사용, 운영 기간 연장, 순번 확정·별도 변경 이력은 추가하지 않았습니다.
- 실제 지급 실행 기능은 이번 범위가 아니며, 향후 지급 실행에도 같은 운영 기간 중복 수령 검증을 적용해야 합니다.
- 개발자 코드 검토·수정 내역은 아직 없습니다. 사용자 정책 승인이 코드 검토 완료를 의미하지 않습니다.

