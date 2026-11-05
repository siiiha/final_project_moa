# 계모임 계금 요청 구현 및 검증 기록

<!-- AI작성: 2026-11-05 사용자 확정 정책과 단계별 구현을 정리한 개발자 검토용 문서다. -->

## 작업 환경

- 기준: 로컬 feature/savinggroup, 8bf9b8256a38ef66c3409c2ad9bcbd45d942e401.
- 작업: feature/request-savinggroup-thepool.
- 워크트리: C:\Users\USER\.codex\worktrees\request-savinggroup-thepool\final_project_moa.
- DB 담당자가 스키마를 수정한다. 이 작업에서 schema.sql·DB·마이그레이션은 수정하지 않았다.
- 실제 금융 API·이체·승인·거절·지급 완료 처리는 구현하지 않았다.

## 확정 정책

1. JWT의 memberId만 사용하고 APPROVED 회원만 허용한다. 모임은 삭제되지 않은 진행중(02)이어야 한다.
2. KST 기준 시작 월을 1회차로 계산하며 매월 1일 회차가 증가한다. 시작일 전에는 차단한다.
3. 현재 회차만 허용하고 sequence_number = round_number인 회원만 순번계 요청을 할 수 있다. 요청 가능일 추가 제한은 없다.
4. 회차가 없거나 확정 산정값이 없으면 차단한다. 자동 회차 생성·과거 금액 보정은 하지 않는다.
5. 회차 생성 당시 현재 승인 인원과 그중 이전 회차 지급 완료 회원 수로 약정 금액을 확정해 보존한다.
6. 이전 지급 완료 회원은 이후 회차마다 기본 회비의 1%를 추가 납부한다. 복리·누적 가산이 아니다. 1%의 원 미만은 버린다.
7. 추가 회비 = FLOOR(기본 회비 × 0.01). 약정 계금 = 기본 회비 × 승인 인원 + 추가 회비 × 이전 지급 완료 회원 수.
8. 순번계는 입력 금액·제목·영수증을 받지 않고 회차의 payout_amount를 요청에 복사한다. 요청 시 현재 인원·회비로 재산정하지 않는다.
9. 친목계는 OWNER·MANAGER만 요청한다. 1원 이상, 최대 13자리 정수 금액과 100자 이하의 비어 있지 않은 제목, 영수증을 필수로 받는다.
10. 친목계 영수증은 JPG·PNG, 5MB 이하, 가로·세로 각각 4096픽셀 이하로 제한한다. 기존 프로필 이미지 검사 방식과 ImageIO를 참고했다.
11. 미납·잔액·예산으로 접수를 차단하지 않으며 잔액 차감·자금 예약도 하지 않는다.
12. 순번계 동일 회차의 PENDING·APPROVED·COMPLETED 요청은 차단한다. REJECTED만 있으면 신규 행으로 재요청할 수 있다.
13. 친목계 동일 모임·원본 파일 SHA-256의 PENDING·APPROVED 지출은 차단한다. REJECTED만 있으면 신규 행으로 재요청한다.
14. 영수증 원본은 OWNER만 열람한다. MANAGER와 요청자에게 별도 열람 권한을 부여하지 않는다.

## DB 담당자 적용 전제

meeting_round에 base_dues_amount, approved_member_count, previous_paid_member_count, additional_dues_amount, payout_amount가 필요하다. expenses에는 receipt_sha256이 필요하다. 명칭이나 NULL 정책이 달라지면 Mapper와 검증도 조정해야 한다.

기존 회차에 확정값이 없으면 요청 불가다. 현재 기준 브랜치에는 회차 생성·납부 의무 생성 코드가 확인되지 않아 별도 작업이 필요하다. 이번 구현이 다섯 산정값을 생성하거나 회원별 추가 회비를 부과하지는 않는다. DB 담당자의 컬럼 추가만으로 순번계 요청 데이터가 준비되는 것은 아니다.

## 트랜잭션과 파일

- 동일 모임 행 → 회원 관계 → 회차 순서로 잠근 뒤 중복 확인과 INSERT를 수행한다. 관련 쓰기 경로가 같은 잠금 규칙을 따라야 중복을 방지할 수 있다.
- 요청 생성은 payout_request 또는 expenses에 PENDING만 저장한다. reviewer_id·transaction_id·완료 시각·공동자금 원장은 변경하지 않는다.
- 비공개 저장 위치는 moa.upload.receipt-dir이며 기본값은 ./uploads/private-receipts다. DB receipt_img_url에는 공개 URL이 아닌 서버 생성 파일 키를 저장한다.
- 모임과 OWNER 소속을 검증한 다운로드 API만 파일을 반환하며 no-store·attachment·nosniff 헤더를 적용한다. 배포 서버에서 이 디렉터리를 정적 파일 경로로 노출하면 안 된다.
- 파일 저장 후 DB 트랜잭션이 롤백되면 신규 파일을 정리한다. 프로세스 강제 종료나 파일 삭제 실패 시 남는 파일의 운영 정리 정책은 후속 사항이다.
- 해시는 원본 바이트 기준이므로 재인코딩·편집·재촬영된 동일 영수증은 감지하지 못한다. 이미지에 포함된 개인정보는 첨부 전 가림이 필요하며 자동 마스킹은 구현하지 않았다. 보관·삭제 정책도 운영 전 확정해야 한다.

## 단계별 파일과 커밋

경로의 공통 Java 접두사는 back/src/main/java/com/nightstudy/moa/savingGroup/이며 테스트는 back/src/test/java/com/nightstudy/moa/savingGroup/ 아래다. 각 단계에 docs/ai-usage-log.md 기록을 포함했다.

| 단계 | 작업 파일 | 작업 내용 | 커밋 |
|---|---|---|---|
| DTO·Mapper | dto/SavingGroupPayoutTargetRow.java, dto/SavingGroupPayoutRoundRow.java, dto/SavingGroupPayoutRequest.java, dto/SavingGroupPayoutInsertRow.java, mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, 테스트 service/SavingGroupPayoutMapperTest.java | 조회·중복·요청 저장 계약 | a2c30ef — feat: 계모임 계금 요청 DTO와 Mapper 추가 |
| Service | dto/SavingGroupPayoutResponse.java, service/SavingGroupPayoutService.java, service/SavingGroupPayoutServiceImpl.java, service/SavingGroupReceiptStorage.java, 테스트 service/SavingGroupPayoutServiceTest.java, service/SavingGroupReceiptStorageTest.java | 권한·순번·확정값·증빙·롤백 검증 | c8486e6 — feat: 계모임 계금 요청 서비스와 권한 및 증빙 검증 추가 |
| Controller | controller/SavingGroupPayoutController.java, 테스트 controller/SavingGroupPayoutControllerTest.java | 생성·요청 가능 정보·OWNER 증빙 API | 4dee5ab — feat: 계모임 계금 요청 API Controller 추가 |
| 프론트 API | front/src/api/savingGroups.js, front/src/api/savingGroupPayout.test.js | 인증 클라이언트·multipart·오류 전달 | 7f6a287 — feat: 계모임 계금 요청 프론트 API 추가 |
| 화면 | front/src/components/savingGroup/SavingGroupPayoutRequestPanel.jsx, front/src/components/savingGroup/SavingGroupDetail.jsx | 서버 금액·필수 증빙·성공·실패·중복 클릭 방지 | fc40d0d — feat: 계모임 계금 요청 화면과 API 연동 구현 |
| 최종 검증·문서 | 테스트 service/SavingGroupPayoutServiceTest.java, docs/saving-groups-api.md, 이 문서, docs/ai-usage-log.md | 회차 경계·증빙 필수 등 추가 테스트와 API·검증 방법 기록 | test: 계모임 계금 요청 검증 보강 및 API 문서 정리 |

## 검증 결과와 범위

- 정상 Gradle 테스트는 Java 17 설치가 없어 시작 전 실패했다.
- 저장소 설정은 바꾸지 않고 build/ 아래 임시 Gradle init 스크립트에서 설치된 Java 21과 options.release=17을 사용했다. 대상 테스트만 compileTestJava에 포함했다. 실제 Java 17 런타임 검증·전체 백엔드 회귀 테스트와 구분한다.
- Mapper XML 바인딩, Service 모의 Mapper, 임시 디렉터리 파일 저장, 단독 MockMvc 테스트를 수행했다. 실제 JWT 필터·MySQL 트랜잭션·동시 요청 검증은 아니다.
- 최종 대상 테스트 55건 통과: Mapper 1, Service 37, 저장소 2, Controller 15. 실패·오류 0.
- 프론트: node --test src/api/savingGroups.test.js src/api/savingGroupPayout.test.js — 51건 통과.
- npm run build 성공. 기존 대용량 번들 경고가 있다.
- 대상 파일 oxlint 오류 없음. 기존 SavingGroupDetail.jsx 미사용 변수 경고 2건은 변경하지 않았다.
- 브라우저 상호작용과 실제 DB 연결 검증은 미수행. 다음 절차를 DB 준비 후 수행해야 한다.

## DB 적용 후 Postman·브라우저 검증

1. 내부 테스트 모임을 진행중으로 준비하고 APPROVED 회원·순번, 현재 회차와 다섯 산정값을 저장한다. 실제 개인정보·계좌는 사용하지 않는다.
2. JWT로 options를 조회해 본인 순번만 조회 가능한지, 다른 순번·미승인·없는 모임·정지 모임이 실패하는지 확인한다.
3. 순번계 빈 multipart POST 후 payout_request.amount가 확정값이고 상태는 PENDING인지 확인한다. 금액·인원 원장 및 회차 산정값이 변하지 않아야 한다.
4. 같은 회차 요청을 동시에 보내 하나만 성공하는지 확인한다. 내부 테스트에서 기존 요청을 REJECTED로 준비하면 재요청이 가능해야 한다. COMPLETED면 차단되어야 한다.
5. 친목계 OWNER·MANAGER가 금액·제목·영수증으로 요청할 수 있고 MEMBER는 거절되는지 확인한다. 금액 0·음수·소수, 빈 제목·누락 증빙·위장 이미지도 거절되어야 한다.
6. 같은 파일을 다른 파일명으로 첨부해 중복 차단을 확인한다. 거절 후 재요청은 허용되어야 한다. 동시 요청도 확인한다.
7. OWNER만 동일 모임의 증빙을 내려받을 수 있고 MANAGER·다른 모임 OWNER·비회원은 접근할 수 없어야 한다. 정적 URL로 직접 다운로드되지 않아야 한다.
8. 브라우저에서 로딩·차단 이유·성공 안내·더블클릭·통신 실패 후 다시 확인·모임 전환·로그아웃 전환을 확인한다.
9. 요청 전후 group_transaction 및 잔액이 같고 외부 금융 호출이 없음을 확인한다.

개발자 검토·수정: 검토 대기. 기준 브랜치 병합·원격 push는 수행하지 않는다.
