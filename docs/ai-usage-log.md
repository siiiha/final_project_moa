<!-- AI작성: 사용자 승인 경로에 작업·검증 기록을 작성한다. -->
# AI 사용 기록

## 2026-10-22 — 가입 승인 2단계: 기존 신청 행 상태 변경

- 목적: 1단계 조회·검증과 조건부 승인 UPDATE를 하나의 Service 트랜잭션에서 실행한다.
- 사용자 지시·승인: DB 변경 확인 후 앞서 안내한 8개 파일 범위로 2단계 진행. Controller와 외부 승인 API는 추가하지 않는다.
- DB 확인: 관리자가 갱신한 schema.sql에 approved_by BIGINT NULL, approved_at DATETIME NULL, 승인자 FK, UNIQUE(member_id, group_id), EXPELLED, ix_group_member_group_status가 존재한다. 공용 DB에는 직접 접속하지 않았다. 회원·상태 복합 인덱스는 확인되지 않아 성능 검토 TODO다.
- AI 작성: approveSavingGroupJoin 계약·구현, approveSavingGroupJoinRequest Mapper·조건부 SQL, LEFT 재신청 승인 정보 초기화, 단위·실제 MySQL 선택 실행 테스트, 설계 명세 갱신.
- 처리: Service 프록시의 @Transactional 진입점에서 초기 식별 → 신청 회원 잠금 → 계모임 잠금 → 신청 재조회·전체 검증 → UPDATE 순서로 처리한다. 내부 검증 메서드의 MANDATORY 자체에 의존하지 않는다.
- 저장: 기존 crew_member_id + group_id + PENDING 행만 APPROVED/MEMBER로 변경한다. joined_at·approved_at은 같은 SQL의 CURRENT_TIMESTAMP, approved_by는 계주 회원 ID다. 신규 INSERT와 requested_at 변경은 없다.
- 실패: 영향 행 수가 1이 아니면 JOIN_REQUEST_APPROVAL_FAILED(409), 기존 검증 실패는 기존 ApiException을 사용한다. 예외 발생 시 트랜잭션을 롤백하며 DB 예외는 공통 처리로 전파한다.
- 재신청: LEFT 행을 PENDING으로 바꿀 때 approved_by·approved_at도 NULL로 초기화한다.
- 검증: Java 17 컴파일·테스트 컴파일 통과, 승인 단위 테스트 12개 통과. 실제 MyBatis 전체 XML 파싱·바인딩과 Spring 트랜잭션 인터셉터의 커밋·롤백 요청을 확인했다. git diff --check 통과.
- 미검증: 별도 MySQL 테스트 접속 환경변수와 생성 허용 설정이 없어 통합 테스트 7개를 건너뛰었다. 실제 저장값·행 수 유지·DB 롤백·재신청 초기화·같은 신청/모임/회원 동시 승인은 미검증이다. 실제 DB 검증 완료로 간주하면 안 된다.
- 통합 테스트는 명시적 MOA_TEST_MYSQL_ALLOW_SCHEMA_CREATE=true 및 별도 테스트 서버 환경변수가 있어야 실행한다. 고유 테스트 스키마를 생성·정리하며 공용 DB 테이블은 사용하지 않는다. 구체적인 실행 요건은 가입 신청 설계 문서 6.5절에 기록했다.
- 작업 시작 시 1단계 SavingGroup 테스트 파일들은 현재 저장소에 없었다. 삭제된 테스트를 임의 복구하지 않았으며 이번 12개 테스트를 과거 43개 회귀 테스트 통과와 동일시하지 않는다.
- 후속 TODO: 별도 테스트 MySQL에서 7개 통합 테스트 실행 및 실제 잠금·교착 영향 확인. 자동 재시도 정책은 추가하지 않았다. 개설 SQL의 pending/approved와 확정 코드 00~04 불일치는 기존 담당 확인 사항으로 유지한다.
- 공식 근거: https://dev.mysql.com/doc/refman/8.4/en/date-and-time-functions.html (한 쿼리의 현재 시각 함수), https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html (프록시와 내부 호출).
- 수정하지 않은 범위: schema.sql, 공용 DB, Controller, API 명세, 의존성·설정. 실제 git commit은 수행하지 않았다. 개발자 최종 검토·수정 대기이며 3단계는 진행하지 않는다.

## 2026-10-22 — 가입 승인 1단계: 대상 조회 및 검증

- 목적: 가입 신청 승인 대상의 존재·소속·권한·상태·정원·가입 수를 저장 없이 검증한다.
- 사용자 승인: 앞서 공유한 10개 파일 범위로 1단계 진행 요청. 계주는 groups.member_id 일치 및 OWNER + APPROVED 관계를 모두 확인한다.
- AI 작성: 신청 조회 DTO와 식별·잠금 재조회, 기존 계모임 조회의 ownerMemberId 추가, Service 검증 계약·구현, 승인 검증 테스트와 실제 MyBatis 파싱·바인딩 테스트, 기존 테스트의 Mapper 계약 대응.
- 트랜잭션: 검증 메서드는 Propagation.MANDATORY로 호출자의 트랜잭션에 참여한다. 단독 프록시 호출은 실패한다. 초기 신청 조회는 회원 ID 식별용이며 신청 회원 → 계모임 잠금 후 신청 행을 다시 읽는다. 반환값을 별도 트랜잭션 승인에 사용하지 않는다.
- 기존 조회 재사용: 회원·계모임·계주 관계 잠금, 정원 및 최대 5개 집계를 재사용했다. selectApprovedSavingGroupCount는 초기 비잠금 조회의 스냅샷 영향을 피하도록 FOR UPDATE를 추가했으며 기존 신청 기능도 최신 잠금 집계를 사용한다.
- 조건: 신청 PENDING, 모임 01, 계주 포함 APPROVED 인원 < 양수 정원, 신청 회원의 삭제되지 않은 00~03 APPROVED 계모임 수 < 5. 신청 회원 계정 ACTIVE·본인 인증의 승인 시 재검증은 별도 정책이 확정되지 않아 추가하지 않았다.
- 범위 준수: 승인 UPDATE·Controller API·외부 호출·새 의존성·schema.sql·공용 DB 변경 없음. 승인 처리자·시각 컬럼은 아직 참조하지 않는다.
- 검증: Java 17 경로 지정 compileJava 통과. back에서 아래 명령 실행, 관련 테스트 43개 통과(승인 검증 35, Mapper 5, 기존 신청 3). git diff --check 통과.
  `gradlew.bat -Dorg.gradle.java.installations.paths=C:\dev\jdk-17.0.12_windows-x64_bin\jdk-17.0.12 test --tests "*SavingGroup*"`
- 테스트 범위: 정상·누락·다른 모임·계주 조건·대기 이외 상태·모집중 이외 상태·정원·최대 5개·잘못된 ID·무인증·신청 변경·잠금 순서·무저장·트랜잭션 없는 호출 차단, XML 조건·전체 매퍼 파싱·바인딩. Mockito 기반 단위 테스트는 DB 연결 없이 실행한다.
- 미검증: 실제 MySQL SQL 실행·인덱스·격리 수준·잠금 경합·교착 및 동시 승인. 전체 앱 기동·Postman·브라우저 통합은 실행하지 않았다. 1단계는 외부 API가 없어 Postman으로 호출할 수 없다.
- 후속 TODO: 2단계에서 승인 전체 트랜잭션·조건부 UPDATE·영향 행 수·롤백·동시성 검증. Spring 프록시를 우회하는 내부 호출에서는 MANDATORY 자체가 적용되지 않으므로 호출부의 트랜잭션을 반드시 보장한다. 공용 DB의 UNIQUE·EXPELLED·승인 정보 구조 확인 및 재신청 승인 정보 초기화도 필요하다.
- 연계 확인: 병합된 insertSavingGroup/approveForCreationSavingGroup SQL은 pending/approved 상태 문자열을 사용한다. 확정 상태 코드 00~04와 불일치하므로 별도 담당 확인이 필요하며 이 작업에서 수정하지 않았다.
- 공식 근거: https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html (잠금 조회와 최신 값 조회).
- 개발자 코드 검토·수정: 검토 대기. git commit과 다음 단계 구현은 수행하지 않았다.

## 2026-10-22 — 계모임 가입 승인 DB 선행 변경안 정리

- 목적: DB 관리자가 검토·반영할 가입 승인 DB 변경 요청과 컬럼 명세를 저장소에 남긴다.
- AI 제안·작성: approved_by·approved_at, UNIQUE, EXPELLED, 복합 인덱스, 기존 데이터 사전 확인, 재신청 초기화 및 적용 후 확인 절차 문서화.
- 사용자 지시: schema.sql과 공용 DB는 직접 수정하지 않고 DB 관리자에게 변경 내용을 전달한다.
- 확정 업무 정책: 01만 승인 가능, 계주도 정원 포함, 승인 가입 중 삭제되지 않은 00~03 계모임을 최대 5개에 포함, 계주는 groups.member_id와 OWNER + APPROVED 관계를 함께 확인한다.
- 미확정: 제안 컬럼·제약·인덱스의 최종 DB 정의 및 공용 DB 반영 여부. 과거 승인 정보를 임의 보정하지 않는다.
- 개발자 검토·수정: 최종 검토 대기. 관리자에게 직접 전송하지 않았다.
- 변경 범위: DB 요청 문서 신규 작성, 가입 신청 설계 문서 연결, 본 기록 갱신. schema.sql·Mapper·Java·의존성 변경 없음.
- 검증: 문서 링크·변경 범위 및 git diff --check 확인. 문서만 변경하므로 컴파일·애플리케이션 테스트는 실행하지 않는다. 실제 DB 제약·잠금·동시성은 미검증이다.
- 후속: 관리자 반영 내역과 갱신 schema.sql 수신 후 구조 확인, 재신청 승인 정보 초기화와 테스트 반영. 승인 1~3단계 구현은 아직 진행하지 않았다.

## 2026-10-20 — 계모임 목록 조회 구현

- 목적: 확정 설계에 따라 공개 목록 API와 기존 React 화면을 연결한다.
- AI 제안·작성: Controller/Service/ServiceImpl/MyBatis, 입력 검증, 고정 6개 페이징,
  5개 번호 묶음, 승인 회원 일괄 집계, LIKE 이스케이프, 카드 변환, 로딩·오류·취소, 테스트·명세.
- 사용자 검토·확정: 01~10 지역 코드와 상위 지역명만 사용, 배지 01 모집중/02 진행중,
  NULL 대체 문구, docs 경로 승인. 소수 회비 입력 제한은 생성·수정 작업에서 적용하도록 확정.
- 개발자 코드 검토·수정: 아직 수행 여부 확인되지 않음. 적용 전 변경 내용을 검토해야 한다.
- DB·금융·인증 외부 서비스 호출 및 데이터 변경: 수행하지 않음.
- 의존성 설치·변경: 기존 Gradle 빌드 의존성 해석 외 신규 라이브러리 추가 없음.
- 검증: JDK 17 지정 Gradle 대상 테스트, Node 내장 테스트, lint, 프로덕션 빌드.
- 결과: 백엔드 단위·Controller·매퍼 XML 테스트 22개 통과. 실제 MySQL 테스트는
  전용 환경변수가 없어 3개 테스트 항목을 건너뜀. 프론트 단위 테스트 4개 통과. lint 통과.
- 프로덕션 빌드: 통과. 기존 번들 크기 경고는 별도 최적화 대상으로 유지.
- 직접 확인 필요: 실제 MySQL·브라우저 통합, 지역 데이터, 모임장·중복 이력,
  실제 collations/격리 수준, 이상 납부일·소수 회비 별도 정정,
  로컬 YAML의 중복 spring 블록 정리. 상세 Mock 화면 연결은 이번 범위에서 제외.
- 파일별 이유와 재현 방법: [계모임 API 명세](saving-groups-api.md).

## 2026-10-20 — 기존 PageMeta 재사용

- 사용자 요청: 게시글의 PageMeta 재사용, 페이지·크기·개수·offset을 int로 통일.
- 변경: Service에서 PageMeta.of/getOffset 사용, 결과 DTO는 items/pageMeta만 보유,
  Controller는 ApiResponse.ok(items, pageMeta) 반환. 전체 개수 Mapper도 int로 변경.
- 프론트: data 배열과 meta를 기존 화면 상태 필드로 변환하여 화면 구성 유지.
- 검증: 공통 응답 구조·0/1/6/7건·범위 초과·큰 페이지·입력 오류 테스트 및 빌드.
- DB 구조·데이터·공통 PageMeta·의존성은 변경하지 않음. 개발자 최종 검토 대기.
- 최종 검증 결과: 백엔드 28개 통과, MySQL 전용 3개 건너뜀, 프론트 4개 통과, lint·프로덕션 빌드 통과. 기존 번들 크기 경고 유지.

## 2026-10-22 — 계모임 가입 신청 Mapper 계약 및 SQL

- 목적: 계모임 가입 신청의 회원·대상 모임·기존 관계 조회, 정원·가입 수 집계,
  신규 신청 및 자진 탈퇴 후 재신청 저장을 위한 MyBatis 계약과 SQL을 준비한다.
- AI 제안·작성: 가입 신청용 내부 조회 DTO 3개, `SavingGroupMapper` 메서드 7개,
  잠금 조회·승인 계원 집계·최대 5개 집계·PENDING INSERT·LEFT 상태 재신청 UPDATE SQL,
  Mapper XML 정책 단위 테스트를 추가했다.
- 사용자 검토·확정: 가입 신청은 '01' 상태만 허용, '00'~'03' 상태의 승인 가입은
  최대 5개에 포함, '04'와 삭제 모임은 제외, LEFT만 재신청 허용, REJECTED·EXPELLED는 차단.
- 개발자 코드 검토·수정: 아직 수행 여부 확인되지 않음. 적용 전 변경 내용을 검토해야 한다.
- DB·금융·인증 외부 서비스 호출 및 데이터 변경: 수행하지 않음. EXPELLED 상태와
  `(member_id, group_id)` 유니크 제약은 다른 담당 작업의 선행 조건으로 남겨 둔다.
- 의존성 설치·변경: 없음.
- 검증: XML 파싱, Mapper statement ID 존재 여부, `git diff --check` 통과.
  Gradle 테스트는 실행을 시도했으나 현재 환경에 Java 17 toolchain이 없어 실행하지 못했다.
- 직접 확인 필요: Java 17 설치 또는 Gradle toolchain 설정 후 `back\\gradlew.bat test` 실행,
  실제 MySQL에서 `FOR UPDATE`, 유니크 제약, 동시 신청 동작을 통합 테스트해야 한다.

## 2026-10-22 — 계모임 가입 신청 Service 구현

- 목적: 인증 회원 본인의 계모임 가입 신청을 트랜잭션 안에서 검증하고 대기 상태로 저장한다.
- AI 제안·작성: Service 계약, 회원 ACTIVE·본인 인증·계모임 상태·정원·최대 5개·기존 관계 검증,
  신규 PENDING 저장, LEFT 재신청 갱신, 중복 키 예외 변환, 정상·대기 중복·LEFT 재신청 단위 테스트를 추가했다.
- 사용자 검토·확정: 연계 인증·Controller·DB 제약 작업은 다른 담당 범위이며 이번 작업에서 수정하지 않는다.
- 개발자 코드 검토·수정: 아직 수행 여부 확인되지 않음. 적용 전 변경 내용을 검토해야 한다.
- DB·금융·인증 외부 서비스 호출 및 데이터 변경: 수행하지 않음.
- 의존성 설치·변경: 없음.
- 검증: `git diff --check` 통과. Gradle 테스트를 실행했으나 Java 17 toolchain 미설치로
  컴파일 전 단계에서 중단되어 테스트 결과는 미확정이다.
- 직접 확인 필요: Java 17 설치 또는 Gradle toolchain 설정 후 `back\\gradlew.bat test` 실행,
  실제 MySQL 트랜잭션과 DB 유니크 제약 반영 후 동시 신청 통합 테스트를 수행해야 한다.

## 2026-10-22 — 계모임 가입 신청 프론트 연결

- 목적: 계모임 상세 화면의 가입 신청 버튼을 실제 API와 연결하고 성공·본인 인증 필요·일반 실패 흐름을 구분한다.
- AI 제안·작성: 가입 신청 API 함수, 요청 중 버튼 비활성화, 성공 모달, 본인 인증 안내 및 인증관리 이동 모달,
  일반 실패 모달, API 성공·본인 인증 오류 단위 테스트를 추가했다.
- 사용자 검토·확정: 로그인 복귀와 인증 토큰 제공은 다른 담당 작업 범위이며 이번 변경에서 구현하지 않는다.
- 개발자 코드 검토·수정: 아직 수행 여부 확인되지 않음. 적용 전 변경 내용을 검토해야 한다.
- DB·금융·인증 외부 서비스 호출 및 데이터 변경: 수행하지 않음.
- 의존성 설치·변경: 없음. Node 내장 테스트 러너를 사용한다.
- 검증: Node 내장 테스트 2건 통과, `npm run lint` 통과, `npm run build` 통과.
- 직접 확인 필요: 인증 담당 작업이 병합된 뒤 실제 JWT를 포함한 401·403 응답과
  `/member/verify` 이동, 브라우저에서 가입 신청 중 버튼 상태를 통합 확인해야 한다.
