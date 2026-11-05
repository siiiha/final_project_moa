<!-- AI작성: 2026-10-20 확정 설계에 따른 구현·검증 명세. -->
# 계모임 목록 조회 API
# 계모임 조회 API

<!-- AI작성: 공지 페이징·고정의 현재 계약을 과거 단계별 기록과 구분한다. -->
> 공지 목록·고정 기능은 문서 하단의 [2026-11-04 페이징·고정 최신 계약](#notice-pagination-and-pin)을 우선한다. 아래 기존 기록의 최신 4건·meta 없음·고정 제외는 당시 구현 범위다.

## 2026-10-20 상세 조회 확장 — 최신 계약

<!-- AI작성: 구현 중 사용자 정정 사항을 반영한다. 아래 기존 목록 구현 기록보다 이 절을 우선한다. -->

### 사용자 확정·정정

- 가입 신청·거절·탈퇴 이력은 별도 테이블이 아닌 group_member.membership_status에서 관리한다.
- 기존 DB 코드 OWNER(계주), MANAGER(회계), MEMBER(계원)와 APPROVED(승인)를 유지한다.
  PENDING·REJECTED·LEFT는 인원수와 역할 조회에서 제외한다. schema.sql은 변경하지 않는다.
- 이번 작업은 공개 조회까지 구현하며 JWT 연결은 후속 작업으로 남긴다.

### 상세 요청과 응답

`GET /saving-groups/{id}` / 프록시 별칭 `GET /api/saving-groups/{id}`

- id는 groups.group_id이며 Controller에서 Long + @Positive로 검증한다.
- 타입 오류·Long 범위 초과·0·음수는 400 BAD_REQUEST다.
- groups.deleted_at IS NULL, saving_group.status IN ('01','02')만 허용한다.
- 없는 모임·삭제·그 외 상태는 모두 404 NOT_FOUND, 메시지는 '조회할 수 있는 계모임이 없습니다.'다.
- 성공은 ApiResponse.ok(data), meta/error는 null이다. 예기치 않은 실패는 기존 500 INTERNAL_ERROR다.

```json
{
  "success": true,
  "data": {
    "groupId": 42,
    "groupName": "테스트 모임",
    "description": null,
    "regionName": "서울",
    "maxMembers": 8,
    "memberCount": 1,
    "duesAmount": 100000.00,
    "duesDay": 10,
    "status": "01",
    "notices": [{"noticeId": 7, "title": "모임 안내"}],
    "myRole": null
  },
  "meta": null,
  "error": null
}
```

groupId·noticeId는 Java Long/기존 숫자 직렬화를 사용한다. URL 문자열은 프론트에서
Number로 변환하지 않는다. 응답 BIGINT가 JavaScript 안전 정수 범위를 넘는 경우를 위한
문자열 직렬화 계약은 후속 확인이 필요하다. 회비는 BigDecimal이며 nullable 값을 유지한다.

### 조회와 권한

- 기본 정보 → 승인 인원수 → 최근 공지의 3 SELECT를 읽기 전용 트랜잭션으로 수행한다.
- 계원 수는 APPROVED 회원의 COUNT(DISTINCT member_id). 계주 +1 없음, 공지와 집계 조인 없음.
- 공지는 created_at DESC, notice_id DESC LIMIT 4. ID·제목만 조회하며 0건은 []다.
- group_notice에는 삭제 컬럼이 없으므로 임의의 소프트 삭제 조건은 추가하지 않는다.
- Controller는 memberId나 myRole 입력을 받지 않으며 항상 검증된 사용자 없음(null)으로 호출한다.
  JWT 검증이 없어 현재 모든 HTTP 응답의 myRole은 null이다. Authorization 값도 역할 판정에 사용하지 않는다.
- Service의 역할 조회와 프론트 탭 매핑은 후속 인증 연결을 위해 준비했다. 서버에서 검증된
  memberId를 연결하기 전에는 HTTP로 회원 전용 역할을 얻을 수 없다.
- 추후 승인 역할 OWNER는 9개, MANAGER/MEMBER는 공통 6개, 그 외는 공지 1개다.
  충돌하는 승인 역할·정의되지 않은 역할은 임의 선택하지 않고 서버 오류로 처리한다.
- 실제 업무 탭 API의 권한 검증은 별도다. 탭 숨김으로 서버 권한 검증을 대신하지 않는다.
- 실제 InnoDB 격리 수준에 따른 조회 일관성은 운영 환경 확인이 필요하다.

### 프론트 연결

- SavingGroupDetailPage는 URL ID로 조회하므로 직접 접근과 새로고침을 지원한다.
- 목록 응답에 groupId를 추가하고 기존 savingGroupId는 유지한다. 카드 URL은 groupId를 사용한다.
- 로딩·400·401·403·404·서버/네트워크 오류, 재시도, 빈 공지를 구분한다.
- 요청 취소와 응답 ID·재시도 회차 비교로 이전 응답이 새 화면을 덮어쓰지 않게 한다.
- 소개·공지·회비·납부일·정원·인원수·상태는 실제 상세 응답만 사용한다.
  Mock 모임장·다음 모임·최근 활동 등 응답에 없는 정보는 표시하지 않는다.
- 회비는 프론트에서 천 단위 구분과 원 단위를 붙인다. 소수 두 자리까지 유지하며 null은 회비 미정이다.
- 납부일 29~31은 '납부일 확인 필요'로 표시하고 DB 값을 수정하지 않는다.
- 가입 신청 버튼은 역할과 무관하게 유지한다. 클릭 시 준비 중 안내만 하며 완료를 가장하지 않는다.
- 이번 범위 밖의 업무 탭은 준비 중 안내이며 기존 Mock 업무 패널을 실제 상세와 섞지 않는다.

### 상세 확장 변경 파일과 이유

Java 운영 코드 루트: `back/src/main/java/com/nightstudy/moa/`.
테스트 루트: `back/src/test/java/com/nightstudy/moa/savingGroup/`.

| 파일 | 구분 | 변경과 이유 |
|---|---|---|
| savingGroup/controller/SavingGroupController.java | 수정 | 공개 상세 경로, Long 양수 검증, 인증 입력 불허 |
| savingGroup/service/SavingGroupService.java | 수정 | 상세 조회 계약 추가 |
| savingGroup/service/SavingGroupServiceImpl.java | 수정 | 404, 읽기 전용 조회 조합, 승인 역할 확인, 목록 groupId 전달 |
| savingGroup/mapper/SavingGroupMapper.java | 수정 | 상세·인원수·공지·승인 역할 조회 계약 |
| savingGroup/dto/SavingGroupDetailResponse.java | 추가 | 필요한 공개 필드와 역할만 반환 |
| savingGroup/dto/SavingGroupNoticeSummaryResponse.java | 추가 | 본문 없이 공지 ID·제목 반환 |
| savingGroup/dto/SavingGroupSummaryResponse.java | 수정 | 목록에서 상세로 이동할 groupId 추가, 기존 ID 유지 |
| common/exception/GlobalExceptionHandler.java | 수정 | 경로 범위 검증 실패를 400 공통 응답으로 처리 |
| back/src/main/resources/mappers/SavingGroupMapper.xml | 수정 | 분리 SELECT, 상태·삭제 조건, 집계·정렬, 공지 생성자 매핑 |
| front/src/api/savingGroups.js | 수정 | 상세 통신·취소 신호, groupId 카드 URL, 금액 포맷·탭 매핑 |
| front/src/pages/SavingGroupDetailPage.jsx | 수정 | Mock 대신 URL 기반 조회와 상태 처리 |
| front/src/components/savingGroup/SavingGroupDetail.jsx | 수정 | 실제 공개 필드·공지 제목, 미정 처리, Mock 업무 데이터 분리 |
| front/src/components/savingGroup/SavingGroupMenuTabs.jsx | 수정 | 역할별 탭 표시 |
| SavingGroupControllerTest.java | 수정 | 공개 상세·위조 입력 무시·400·404·500 |
| SavingGroupServiceTest.java | 수정 | ID 구분·계주 포함 집계·역할·빈 공지·404·충돌 역할 |
| SavingGroupMapperTest.java | 수정 | 실제 XML 파싱과 상세 SQL 필터·바인딩·정렬 |
| SavingGroupMySqlTest.java | 수정 | 별도 테스트 DB의 임시 테이블에서 상태·공지·이력·역할 검증 |
| front/src/api/savingGroups.test.js | 수정 | 카드 URL·역할·금액·상세 성공과 실패 |
| front/src/api/savingGroupDetailView.test.js | 추가 | 실제 JSX 렌더링으로 탭·공지 본문 미노출·가입 버튼 검증 |
| docs/saving-groups-api.md | 수정 | 상세 API와 변경 파일별 이유·검증 방법 |
| docs/ai-usage-log.md | 수정 | 사용자 결정·수행 내용·검증 결과 |

기존 Mapper XML의 사용자 공백·들여쓰기 변경은 보존했다. 새 의존성·스키마·인덱스·실제 DB 변경 없음.

### 검증과 직접 확인

- 백엔드: 기존 안내의 JDK 17 옵션과 `gradlew.bat test --tests "*SavingGroup*" --offline`.
  Controller 23, Service 10, Mapper 4: 합계 37개 통과. MySQL 5개 항목은 환경변수 부재로 건너뜀.
- 프론트: `node --test src/api/savingGroups.test.js src/api/savingGroupDetailView.test.js` → 8개 통과.
- `npm run lint`, `npm run build` 통과. 기존 대형 번들 경고는 남음.
- MySQL 테스트는 MOA_TEST_MYSQL_URL/USER/PASSWORD가 설정된 별도 테스트 DB에서만 실행한다.
  임시 테이블만 사용하며 실제 데이터는 수정하지 않는다.
- 브라우저/Postman 실서버 통합은 미실행이다. `/api/saving-groups/{실제 group_id}`를 토큰 없이 조회하고,
  삭제·상태 00/03/04의 404, `/0`·`/abc`의 400, 공지 0/1/4/5개 이상을 확인해야 한다.
- 브라우저에서 `/saving-groups/{실제 group_id}` 직접 접근·새로고침·목록 이동·재시도·모바일 레이아웃을 확인한다.
- JWT 연동·로그인 변경 시 재조회·토큰 오류 정책·업무 API 권한 검증은 후속 작업이다.
- 실제 DB의 계주 APPROVED 등록, 두 ID가 다른 데이터, 중복 가입·saving_group의 1:1 관계,
  인덱스·실행 계획·격리 수준, 기존 29~31 납부일·소수 회비를 확인한다.
- 기존 로컬 YAML 중복 spring 블록 문제는 본 작업에서 읽거나 수정하지 않았다.

---

## 기존 목록 조회 구현 기록
# 계모임 API — 목록 조회 및 가입 신청 승인

## 요청

`GET /saving-groups` (기존 프론트 프록시용 별칭: `GET /api/saving-groups`)

공개 조회이며 토큰을 요구하지 않는다. 현재 저장소에는 Spring Security/JWT 구현·의존성이 없다.
인증 기능 도입 시 두 GET 경로의 공개 접근 및 만료 토큰 정책을 따로 확인해야 한다.

| 파라미터 | 타입 | 기본값 | 검증 |
|---|---|---|---|
| keyword | String | 조건 없음 | 앞뒤 공백 제거 후 최대 20 Java 문자열 길이, 공백만 있으면 생략 |
| regionCode | String | 조건 없음 | 기존 호환용 01~10. 새 지역 조건과 동시 사용 불가 |
| provinceName | String | 조건 없음 | 시·도, 공백 제거 후 최대 30자 |
| districtName | String | 조건 없음 | 시·군·구, 공백 제거 후 최대 50자, provinceName 필요. 세종은 빈 문자열 허용 |
| townName | String | 조건 없음 | 읍·면·동, 공백 제거 후 최대 50자, provinceName·districtName 필요 |
| page | Integer | 1 | 1 이상, 빈 값·문자·소수·범위 초과 거부 |

지역: 01 서울 / 02 경기 / 03 강원 / 04 충북 / 05 충남 / 06 전북 / 07 전남 /
08 경북 / 09 경남 / 10 제주도.
DB ID와 코드를 동일시하지 않으며 `region.region_name`을 해당 명칭과 비교한다.
<!-- AI작성: 기존 코드 검색을 유지하면서 회원 지역 데이터 기반의 단계별 검색을 추가한다. -->
새 화면은 `/api/auth/regions`의 정규 지역명을 사용하며 선택하지 않은 단계는 생략한다.
시만 선택하면 `r.province_name`만 비교해 그 시의 모든 구·동 모임을 포함한다.
시·구 선택 시 두 컬럼을, 동까지 선택 시 세 컬럼을 AND 비교한다. 목록과 전체 건수는 동일 조건이다.
`regionCode`와 새 지역 조건을 함께 보내거나 상위 없이 하위 조건을 보내면 HTTP 400이다.
존재하지 않거나 서로 일치하지 않는 지역명 조합은 결과 0건이며 실제 지역 존재 여부를 별도 조회하지 않는다.
세종은 구 미선택 시 districtName을 생략하고, 구 행을 선택한 경우 빈 문자열을 전달해 동 선택과 구분한다.
지역 데이터는 API가 생성하지 않으며 기존 모임의 region 행에 상위 지역 컬럼이 채워져 있어야 새 조건으로 검색된다.

예: `/api/saving-groups?keyword=산책&regionCode=01&page=1`

## 응답

2026-10-22부터 기존 게시글과 동일하게 `ApiResponse.ok(items, pageMeta)`를 사용한다.
`data`는 카드 배열, `meta`는 `PageMeta`이며 페이지·크기·전체 개수·전체 페이지·offset은 모두 int다.

```json
{
  "success": true,
  "data": [{
    "savingGroupId": 101,
    "groupId": 42,
    "groupName": "산책 모임",
    "description": "이웃과 함께하는 모임",
    "regionName": "서울",
    "maxMembers": 8,
    "memberCount": 5,
    "duesAmount": 50000.00,
    "duesDay": 25,
    "status": "01"
  }],
  "meta": {
    "page": 1,
    "size": 6,
    "total": 1,
    "totalPages": 1,
    "offset": 0
  },
  "error": null
}
```

- PageMeta의 기존 getOffset()도 JSON의 offset 필드로 직렬화된다.
- 카드 필드는 위 예시와 같다. description/maxMembers/duesAmount/duesDay는 null 가능하다.
- 결과 0건: data=[], meta.total=0, meta.totalPages=0, meta.page=요청 페이지, meta.size=6.
- 전체 7건에 page=3: data=[], meta.total=7, meta.totalPages=2, meta.page=3, meta.size=6.
- 범위 초과·빈 결과는 HTTP 200이며 요청 페이지를 강제로 보정하지 않는다.
- 잘못된 요청: HTTP 400, success=false, data/meta=null, error.code=`BAD_REQUEST`.
- DB 등 예기치 않은 실패: 기존 HTTP 500, error.code=`INTERNAL_ERROR`, 공통 메시지 사용.
- 빈 결과에 ApiException을 만들거나 Service에서 DTO 입력 검증을 반복하지 않는다.

## 조회 규칙

- saving_group → groups → region 조인, 조회 단위는 saving_group_id.
- groups.deleted_at IS NULL 및 saving_group.status IN ('01','02').
- 이름 또는 소개글 부분 검색과 지역 조건을 AND 결합한다.
- LIKE ESCAPE '!'를 사용하며 !, %, _를 순서대로 이스케이프하고 바인딩한다.
- 스키마의 utf8mb4_unicode_ci 비교를 활용한다. 실제 DB에도 동일한 collation이 필요하다.
- groups.created_at DESC, saving_group.saving_group_id DESC 정렬.
- 페이지 크기 6, offset=(page-1L)*6. 목록·개수 조건은 XML 조각을 재사용한다.
- 개수 → 페이지 목록 → 해당 group_id들의 APPROVED 회원 COUNT(DISTINCT member_id), 최대 3 SELECT.
- 승인 모임장도 회원 행에 있으므로 +1 없음. 집계 없는 모임은 0명으로 유지한다.
- 결과 없음·범위 초과는 1 SELECT, 페이지 행이 없으면 집계를 생략한다.
- ServiceImpl의 readOnly 트랜잭션 사용. 동일 스냅샷은 실제 InnoDB 격리 수준 확인이 필요하다.
- 데이터·인덱스·CHECK·공통 예외·공통 응답·프로젝트 의존성은 변경하지 않았다.

## 화면 규칙

- 기존 Mock 대신 API 호출. 상세·개설 화면의 Mock은 바꾸지 않는다.
- 검색 제출·지역 변경 시 1페이지, 페이지 이동 시 조건 유지.
- 요청 취소와 활성 요청 표시로 오래된 응답·오류 덮어쓰기 방지.
- 로딩, 오류·다시 시도, 빈 목록, 범위 초과·첫 페이지 복귀를 구분한다.
- 페이지 번호 최대 5개 묶음, 이전·다음 한 페이지 이동. 결과 0건은 번호 숨김.
- 01→모집중, 02→진행중. 정원이 찼다고 상태를 임의로 변경하지 않는다.
- <!-- AI작성: 시·구·동 선택값은 선택 변경 즉시 반영하며 하위 선택을 초기화한다. --> 지역 선택 변경 시 현재 검색어와 결합해 첫 페이지를 조회한다. 검색 버튼은 입력 중인 검색어를 적용한다. 상세·카드 지역 표시 형식은 유지한다.
- 모임명 한 줄 말줄임, 소개글 기존 줄바꿈 유지, 대표 이미지는 기존 배경색 영역 유지.
- 5 / 8명 · 월 50,000원 / 납부일 매월 25일.
- 빈 값: 소개글이 없습니다. / 정원 미정 / 회비 미정 / 납부일 미정.
- 소수 등 비정수 금액은 회비 확인 필요로 표시하며 반올림·수정하지 않는다.
  사용자의 추가 확정에 따라 신규 입력 제한은 생성·수정 API 작업에 남긴다.

## 변경 파일과 이유

아래 경로의 백엔드 Java 기준 루트는 `back/src/main/java/com/nightstudy/moa/`이다.

| 파일 | 구분 | 내용·이유 |
|---|---|---|
| savingGroup/controller/SavingGroupController.java | 추가 | 두 GET 경로, Validation, 공통 응답 |
| savingGroup/service/SavingGroupService.java | 추가 | 기존 Service 인터페이스 구조 준수 |
| savingGroup/service/SavingGroupServiceImpl.java | 추가 | 읽기 전용 조회·패턴 가공·계원 조합·페이징 |
| savingGroup/mapper/SavingGroupMapper.java | 추가 | 최대 세 번의 조회 계약 |
| savingGroup/dto/SavingGroupSearchCondition.java | 추가 | 정규화 및 요청 Validation |
| savingGroup/dto/SavingGroupListCondition.java | 추가 | 서버 가공 조건과 long offset 전달 |
| savingGroup/dto/SavingGroupRow.java | 추가 | 내부 groupId를 포함한 MyBatis 결과 |
| savingGroup/dto/SavingGroupMemberCount.java | 추가 | 일괄 집계 결과 매핑 |
| savingGroup/dto/SavingGroupSummaryResponse.java | 추가 | 내부 groupId 없는 공개 카드 응답 |
| savingGroup/dto/SavingGroupListResponse.java | 추가 | 목록·전체 개수·페이지 정보 |
| back/src/main/resources/mappers/SavingGroupMapper.xml | 추가 | 실제 스키마 기반 SELECT와 공통 조건 |
| front/src/api/savingGroups.js | 추가 | 지역 코드·실제 API·카드 변환·번호 묶음 |
| front/src/pages/SavingGroupsPage.jsx | 수정 | 서버 조회와 로딩·오류·재시도·조건 상태 |
| front/src/components/savingGroup/SavingGroupList.jsx | 수정 | 5개 묶음·이전다음·범위 초과 안내 |
| back/src/test/java/com/nightstudy/moa/savingGroup/SavingGroupControllerTest.java | 추가 | 공개 요청·검증·공통 오류 테스트 |
| back/src/test/java/com/nightstudy/moa/savingGroup/SavingGroupServiceTest.java | 추가 | 빈 결과·집계·offset·쿼리 횟수·실패 테스트 |
| back/src/test/java/com/nightstudy/moa/savingGroup/SavingGroupMapperTest.java | 추가 | 실제 XML 파싱·동적 조건·바인딩 테스트 |
| back/src/test/java/com/nightstudy/moa/savingGroup/SavingGroupMySqlTest.java | 추가 | 선택 실행 실제 MySQL 임시 테이블 테스트 |
| front/src/api/savingGroups.test.js | 추가 | 표시·빈 값·소수·지역·번호·통신 실패 테스트 |
| docs/saving-groups-api.md | 추가 | API·화면·파일별 변경·검증 명세 |
| docs/ai-usage-log.md | 추가 | 작업 및 검토·검증 기록 |

## 검증

백엔드: back 폴더에서 `gradlew.bat test --tests "*SavingGroup*"`.
JDK 17을 자동 탐지하지 못하면 실행 옵션
`-Dorg.gradle.java.installations.paths=C:\dev\jdk-17.0.12_windows-x64_bin\jdk-17.0.12`
를 사용할 수 있다. 프로젝트 설정은 변경하지 않았다.

프론트: front 폴더에서 `node --test src/api/savingGroups.test.js`, `npm run lint`, `npm run build`.

실제 MySQL 테스트는 별도 테스트 DB의 MOA_TEST_MYSQL_URL, MOA_TEST_MYSQL_USER,
MOA_TEST_MYSQL_PASSWORD 환경변수가 있을 때만 실행된다. 값은 파일·커밋·로그에 넣지 않는다.
TEMPORARY TABLE 권한이 필요하며 실제 매퍼를 연결별 임시 테이블로 실행한다.
기존 데이터를 수정하지 않고 종료 시 롤백·연결 종료한다.
필터·대소문자·리터럴 검색·상태·삭제·정렬·0/1/6/7건·승인 회원 중복 집계를 검증한다.

실행 결과와 미검증 항목은 ai-usage-log.md에 기록한다.

## 직접 확인할 사항

1. 실제 DB 스키마·collation·격리 수준 및 region_name의 10개 명칭 일치 여부.
2. 실제 가입 데이터의 모임장 APPROVED 등록·중복 상태 이력·계모임과 groups의 일대일 여부.
3. 기존 소수 회비와 납부일 29~31일 등 이상 데이터의 별도 정정.
   현재 스키마 CHECK는 1~31일이고 DECIMAL은 소수를 허용한다.
4. 백엔드의 무시된 로컬 application.yaml에는 최상위 spring 블록이 중복되어 있어
   실제 앱 실행 전에 하나로 병합해야 한다. 이 작업에서는 로컬 접속 설정을 수정하지 않았다.
5. 실제 서버·DB를 띄운 Postman 및 브라우저 통합 확인.
6. 상세 페이지는 기존 Mock 상태이므로 목록 카드의 실제 ID와 상세 Mock 데이터의 연결은 별도 범위다.
7. 생성·수정 API에서 소수 회비 차단 및 1~28일 납부일 검증을 구현해야 한다.
8. 현재 Java 17, Boot 4.0.9-SNAPSHOT, MyBatis starter 4.0.1 선언을 유지했다.

## 계모임 개설 신청 및 관리자 심사 API

### 신청

<!-- AI작성: 2026-11-03 개설 지역 선택 3단계의 화면 동작과 검증 범위를 기록한다. -->
개설 화면은 기존 `/api/auth/regions`를 조회해 시·도 → 시·군·구 → 읍·면·동 순서로 선택한다.
시·구는 필수, 동은 선택이다. 시 변경 시 구·동, 구 변경 시 동을 초기화한다.
세종은 빈 districtName에 해당하는 행을 `세종특별자치시 전체`로 표시하고 구 단계에서 선택한다.
동을 선택하면 해당 동의 regionName, 선택하지 않으면 구의 regionName을 기존 요청 필드에 담는다.
지역 로딩·실패 중에는 제출을 막고 실패 시 재시도를 제공한다. 빈 지역 목록도 실패로 안내한다.
동 데이터가 없으면 동 선택을 비활성화하고 구 단위 신청을 허용한다. 실제 DB 데이터는 이번 작업에서 추가하지 않는다.
이번 3단계는 개설 화면만 변경했다. 서버의 구·동 단위 검증 강화와 승인 후 상세 표시 확인은 후속 단계다.

<!-- AI작성: 4단계에서는 기존 요청 계약을 유지하고 개설 multipart 전송을 단위 검증했다. -->
4단계: 기존 `createSavingGroupApplication`이 최종 `regionName`을 multipart 본문에 포함하므로 운영 코드 변경 없이 요청 테스트를 보강했다.
구 단위·동 단위·부산·세종, 이미지 포함/미포함, 같은 FormData의 지역 변경 후 재요청, 서버 지역 오류·통신 실패 전달을 검증했다.
이 검증은 client.post를 대체한 단위 테스트이며 브라우저 선택 초기화·실제 HTTP·DB 저장 성공을 검증한 것은 아니다.
브라우저에서는 지역을 변경한 뒤 Network의 POST `/api/saving-group` Form Data에서 regionName이 최종 선택값 하나인지 확인해야 한다.

<!-- AI작성: 5단계 서버 지역 검증은 기존 regionName 요청·region_id 저장 계약을 유지한다. -->
5단계: 서버는 regionName의 앞뒤 공백을 제거하고 region에서 상위 지역 정보를 함께 조회한다.
시·도와 시·군·구 정보가 있는 구 또는 동 행만 허용한다. 세종특별자치시는 districtName이 빈 문자열인 행도 허용한다.
지역 누락·존재하지 않는 이름·중복 이름·상위 정보 누락·세종 이외의 빈 구 이름은 HTTP 400 `REGION_NOT_FOUND`로 거절한다.
구 미지정 여부는 저장된 지역 행으로 판단하며, 별도 시·구·동 입력 조합은 받지 않는다. region 테이블의 계층 데이터 정확성은 전제다.
클라이언트가 보낸 regionId는 저장에 사용하지 않고 조회된 ID로 덮어쓴다. 동을 선택하지 않아도 구 행의 ID를 groups.region_id에 저장한다.
기존 생성 트랜잭션·관리자 심사·상세 조회 구조와 DB 스키마는 유지한다.

<!-- AI작성: 6단계 상세 표시 점검과 실제 환경에서 남은 최종 검증을 구분한다. -->
6단계 점검: 생성 시 groups.region_id 저장 → 상세 Mapper의 region 조인 → Service의 regionName 전달 → 화면 상단·모임 정보 두 위치 표시를 코드에서 확인했다.
기존 상세 표시 코드는 변경하지 않았다. 공개 상세는 상태 01·02만 조회하므로 개설 신청 후 관리자 승인이 필요하다.
구·동·세종·기존 광역 지역명이 상세 API 함수의 재조회마다 유지되는지 단위 테스트를 추가했다. 이 테스트는 실제 DB 저장·화면 새로고침 검증을 대체하지 않는다.

최종 수동 검증(미실행):

1. Java 17 환경에서 SavingGroupCreationServiceImplTest를 실행한다.
2. 인증된 사용자로 구만 선택한 신청과 동까지 선택한 신청을 각각 제출한다. Network에서 최종 regionName 하나를 확인한다.
3. groups.region_id가 선택한 region 행을 참조하는지 DB에서 확인한다. 잘못된 지역 요청은 400이고 모임이 생성되지 않아야 한다.
4. 관리자가 두 신청을 승인한 뒤 목록에서 상세로 이동한다. 상단과 모임 정보의 지역명이 각각 신청한 구 또는 동 이름과 같은지 확인한다.
5. 상세 주소에서 새로고침 후에도 동일한 지역이 표시되는지 확인한다. 세종과 기존 광역 지역 모임도 조회한다.
6. 개설 화면에서 지역 미선택 제출, 시 변경 시 구·동 초기화, 구 변경 시 동 초기화, 지역 조회 실패·재시도를 확인한다.

현재 검증 한계: Java 17 부재로 서버 테스트를 실행하지 못했으며 실행 중인 로컬 서버·DB 포트를 확인하지 못했다. 실제 신청·승인·DB·브라우저 검증은 남아 있다.

`POST /api/saving-group`은 `multipart/form-data`로 아래 필드를 받는다.

| 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| groupName | String | 예 | 1~100자 |
| groupType | String | 예 | 친목계 또는 순번계 |
| regionName | String | 예 | `region` 테이블의 지역명 |
| duesAmount | BigDecimal | 예 | 1원 이상의 원 단위 정수 |
| duesDay | Integer | 예 | 1~28 |
| maxMembers | Integer | 예 | 1~999 |
| description | String | 예 | 1~50자(현재 스키마 기준) |
| groupImage | File | 아니오 | JPG/PNG/WEBP, 5MB 이하 |
| memberId | Long | 예 | 인증 연동 전의 임시 신청자 ID |

처리 성공 시 `groups`, `saving_group(status='00')`, `meeting_approval(type='CREATE', status='PENDING')`를 하나의 트랜잭션에서 생성한다. 지역명은 서버에서 `region_id`로 조회하므로 프론트가 DB ID를 추정하지 않는다.

`memberId`는 로그인 인증이 병합되기 전 시연을 위한 임시 계약이다. 현재 프론트는 `1`을 전송하며, 실제 테스트 DB에는 삭제되지 않은 회원 ID 1이 필요하다. JWT 도입 시 이 필드는 제거하고 서버 인증 회원 ID로 대체해야 한다.

### 관리자 개설·삭제 심사

| API | 설명 |
|---|---|
| `GET /api/admin/saving-group-applications?type=&status=&keyword=&page=` | CREATE/DELETE 유형 신청 목록. type은 CREATE/DELETE, status는 PENDING/APPROVED/REJECTED, 페이지 크기는 8 |
| `GET /api/admin/saving-group-applications/{approvalId}` | 신청 상세 |
| `PATCH /api/admin/saving-group-applications/{approvalId}/approve` | 대기 신청을 승인하고 `saving_group.status`를 공개 모집 상태 `01`로 변경 |
| `PATCH /api/admin/saving-group-applications/{approvalId}/reject` | `{ "rejectReason": "..." }`를 받아 대기 신청을 반려 |

목록의 신청 ID는 JavaScript 정밀도 손실을 피하기 위해 문자열로 반환한다. 승인·반려는 `meeting_approval` 행을 잠근 뒤 PENDING 상태인지 재확인하므로 이미 처리된 신청은 HTTP 409으로 거절한다. 반려된 계모임은 비공개 심사 상태 `00`으로 남아 공개 목록에 노출되지 않는다.

<!-- AI작성: 별도 삭제 심사 테이블 없이 기존 meeting_approval과 groups.deleted_at을 재사용한 계약이다. -->
#### 계모임 삭제 심사

| API | 설명 |
|---|---|
| `POST /api/saving-groups/{savingGroupId}/deletion-applications` | 인증된 계주가 `{ "reason": "..." }`으로 삭제 심사를 신청. 사유는 필수, 최대 500자 |
| `PATCH /api/admin/saving-group-applications/{approvalId}/approve` | DELETE 신청이면 삭제 가능 조건을 재검사하고 `groups.deleted_at`을 기록 |
| `PATCH /api/admin/saving-group-applications/{approvalId}/reject` | DELETE 신청이면 반려 사유를 저장하고 계모임은 유지 |

삭제 신청은 `meeting_approval(type='DELETE')`에 저장한다. 같은 계모임에 PENDING 삭제 신청을 중복 등록할 수 없다. 승인 시 `saving_group.status`가 `02` 또는 `03`이거나, 거래 원장 합계로 계산한 공동자금이 0원이 아니거나, `PENDING`/`APPROVED` 출금 요청이 남아 있으면 HTTP 409로 차단한다. 심사 상태 변경과 소프트 삭제는 같은 트랜잭션에서 처리한다.

<!-- AI작성: 신청자 탈퇴로 미처리 DELETE 심사가 사라지지 않도록 관리자 조회와 개인정보 표시 정책을 기록한다. -->
삭제 신청 후 계주가 탈퇴해도 관리자는 해당 DELETE 심사의 목록·상세를 계속 조회하고 처리할 수 있다. 이때 탈퇴한 신청자의 실제 이름은 응답하지 않고 `탈퇴한 회원`으로 표시한다. CREATE 심사는 기존과 동일하게 탈퇴하지 않은 신청자만 조회한다.

`/api/admin/**` 경로는 Spring Security에서 `ADMIN` 역할을 요구한다. 토큰이 없으면 HTTP 401, 일반 회원 토큰이면 HTTP 403으로 차단한다.
   Java 17로 컴파일·대상 테스트를 검증하며 버전 업그레이드는 하지 않는다.

## 공식 근거

- https://mybatis.org/mybatis-3/sqlmap-xml.html
- https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://dev.mysql.com/doc/refman/8.4/en/string-comparison-functions.html
- https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html

<!-- AI작성: 가입 신청 승인 Controller 계약과 사용자 요청으로 제외한 JWT 연결 TODO를 기록한다. -->
## 가입 신청 승인 API (3단계)

### 경로와 요청

```http
PATCH /api/saving-groups/{savingGroupId}/join-requests/{crewMemberId}/approve
```

기존 Controller의 별칭도 유지한다.

```http
PATCH /saving-groups/{savingGroupId}/join-requests/{crewMemberId}/approve
```

- savingGroupId: 양수 Long, saving_group.saving_group_id.
- crewMemberId: 양수 Long, group_member.crew_member_id.
- 요청 본문 없음. memberId·approvedBy·역할·승인 상태를 클라이언트 입력으로 받지 않는다.
- 승인자는 서버 인증 처리에서 설정한 request attribute `memberId`(Long)만 사용한다. 해당 모임의 계주 ID와 OWNER + APPROVED 관계를 Service에서 확인한다.
- 문자열·Long 범위 초과 경로 변수는 기존 공통 타입 변환 예외 처리로 400을 반환한다. 0·음수는 기존 Service 입력 검증으로 400을 반환한다.

### JWT 연결 TODO와 현재 제한

사용자 요청에 따라 JWT 구현 병합과 인증 설정 수정은 이번 작업에서 제외했다.
다른 인증 브랜치의 LoginInterceptor는 JWT 검증 후 request.setAttribute("memberId", memberId)를 사용한다. 현재 Controller는 이 전달 계약을 사용하며 Principal의 이름을 회원 ID로 추정하지 않는다.

- TODO: JWT 브랜치 병합 후 LoginInterceptor가 위 두 승인 경로를 모두 보호하도록 WebConfig에 등록한다. 공개 목록 GET 접근은 유지한다.
- TODO: 실제 JWT 발급·검증, 만료·변조 토큰 차단, 두 경로 인증 적용을 통합 검증한다. 인증 브랜치의 LOGIN_REQUIRED·INVALID_TOKEN 오류 코드와 최종 API 명세도 맞춘다.
- 현재 서버 인증 속성이 없거나 양수가 아니면 HTTP 401 / UNAUTHORIZED를 반환하고 Service를 호출하지 않는다.
- JWT 연결 전에는 정상 Bearer 헤더가 있어도 서버 속성이 설정되지 않으므로 실제 HTTP 승인 성공 흐름을 사용할 수 없다. 토큰을 검증하지 않고 승인하거나 임시 회원 ID를 넣는 우회는 없다.
- 테스트의 requestAttr 주입은 인증 완료 상황을 모사하는 용도다. 클라이언트의 헤더·쿼리·JSON 본문은 request attribute를 대신하지 않는다.

### 성공 응답

HTTP 200, 기존 `ApiResponse.ok()`를 사용하며 새 응답 DTO는 추가하지 않는다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

Controller는 기존 `approveSavingGroupJoin(savingGroupId, crewMemberId, memberId)`만 호출한다.
조회·잠금·권한 검증·기존 행 조건부 UPDATE·영향 행 수 확인은 기존 Service 트랜잭션에서 수행한다.

### 오류 응답

기존 GlobalExceptionHandler와 ApiResponse.fail을 재사용한다. 오류 시 success=false, data/meta=null, error에 code와 message가 들어간다.

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 경로 변수 타입·범위 오류 |
| 401 | UNAUTHORIZED | 서버의 인증 회원 ID 없음 또는 잘못된 값 (현재 Controller 기준) |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 해당 모임의 승인된 계주가 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 계모임 없음 또는 삭제됨 |
| 404 | JOIN_REQUEST_NOT_FOUND | 신청 없음 또는 다른 계모임의 신청 |
| 404 | JOIN_REQUEST_MEMBER_NOT_FOUND | 신청 회원 없음 또는 삭제됨 |
| 409 | JOIN_REQUEST_NOT_PENDING | 이미 승인됐거나 대기가 아닌 신청 |
| 409 | SAVING_GROUP_JOIN_UNAVAILABLE | 모집중(01)이 아닌 계모임 |
| 409 | SAVING_GROUP_CAPACITY_NOT_CONFIGURED | 정원이 NULL 또는 0 이하 |
| 409 | SAVING_GROUP_CAPACITY_FULL | 계주 포함 승인 인원이 정원에 도달 |
| 409 | SAVING_GROUP_JOIN_LIMIT_EXCEEDED | 신청 회원의 유효한 승인 계모임 수가 5개 이상 |
| 409 | JOIN_REQUEST_CHANGED | 잠금 후 신청 회원 식별 정보가 변경됨 |
| 409 | JOIN_REQUEST_APPROVAL_FAILED | 조건부 UPDATE 영향 행 수가 1이 아님 |
| 500 | INTERNAL_ERROR | 예상하지 못한 DB·서버 오류, 기존 공통 메시지 사용 |

### 테스트 및 직접 검증

back 폴더에서 실행한다.

```powershell
.\gradlew.bat '-Dorg.gradle.java.installations.paths=C:\dev\jdk-17.0.12_windows-x64_bin\jdk-17.0.12' test --tests '*SavingGroupJoinApproval*'
```

- Controller 테스트 31개: 두 경로 정상·인증 누락, 잘못된 인증 ID, 클라이언트 회원 ID 무시, 경로 변수 오류, 권한·대상·상태 등 기존 Service 오류의 공통 응답 및 아래 신청 목록 조회 계약.
- Service 단위 테스트 12개: 기존 승인 로직의 회귀 검증.
- 실제 MySQL 테스트 9개: 별도 테스트 서버 설정이 없으면 건너뛴다. 실행 조건은 saving-group-join-request-design.md의 6.5절을 참고한다.
- Postman: 요청 본문 없이 PATCH하면 현재 JWT 미연결 상태에서는 401 / UNAUTHORIZED가 예상 결과다. 성공·권한 실패·만료 토큰 등의 실제 JWT 통합 확인은 병합과 경로 등록 후 수행한다. 인증 우회용 헤더나 임시 회원 ID는 추가하지 않는다.
- 실제 앱 기동·JWT·공용 DB 통합은 아직 검증하지 않았다.

공식 근거: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestattrib.html

<!-- AI작성: 계주가 승인 계원을 추방하는 API의 식별자·권한·상태 전이 계약을 기록한다. -->
## 승인 계원 추방 API (4단계)

### 경로와 요청

```http
PATCH /api/saving-groups/{groupId}/members/{memberId}/expel
```

기존 Controller 별칭도 유지한다.

```http
PATCH /saving-groups/{groupId}/members/{memberId}/expel
```

- groupId: 양수 Long, `groups.group_id`이면서 삭제되지 않은 계모임이어야 한다.
- memberId: 양수 Long, 추방 대상 `members.member_id`다.
- 요청 본문은 없다. 대상은 URL 경로로만 식별하며, 실행 계주·상태·역할은 클라이언트 입력으로 받지 않는다.
- 실행자는 서버 인증 처리에서 설정한 request attribute `memberId`(Long)만 사용한다. 현재 JWT 연결 전에는 이 속성이 없으므로 HTTP 요청은 401을 반환한다.
- Service는 `groups.member_id` 일치와 `group_member`의 `OWNER` + `APPROVED` 관계를 모두 확인한다.

### 처리와 상태 전이

1. 계모임과 요청 계주 관계를 `FOR UPDATE`로 확인한다.
2. `groupId`와 대상 `memberId`의 `group_member` 행을 잠금 조회하고, `APPROVED` 상태인 경우에만 `crew_member_id`를 사용한다.
3. 계주 본인을 대상자로 요청하면 상태를 변경하지 않고 409을 반환한다.
4. 대상 행의 `crew_member_id`, `group_id`, `membership_status = 'APPROVED'`가 모두 일치할 때만 `membership_status`를 `EXPELLED`로 변경한다.

`EXPELLED`는 기존 `group_member.membership_status` 상태값이므로 DB 마이그레이션·컬럼 변경은 없다. `left_at`, 역할, 승인 이력은 이 작업에서 변경하지 않는다.

```java
// TODO: 알림 기능 구현 후 추방당한 회원에게 추방 알림을 발송한다.
```

위 TODO만 남겼으며 알림 테이블 저장, 외부 알림 발송, 외부 서비스 연동은 구현하지 않는다.

### 성공 응답

HTTP 200, 기존 `ApiResponse.ok()`를 사용한다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 오류 응답

기존 `GlobalExceptionHandler`와 `ApiResponse.fail`을 재사용한다.

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 경로 변수 타입·범위 오류 |
| 401 | UNAUTHORIZED | 서버 인증 회원 ID가 없거나 0 이하 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 해당 모임의 승인된 계주가 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 계모임이 없거나 삭제됨 |
| 404 | SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND | 대상 계원이 없거나 현재 상태가 APPROVED가 아님 |
| 409 | SAVING_GROUP_OWNER_EXPULSION_NOT_ALLOWED | 계주 본인을 추방하려는 요청 |
| 409 | SAVING_GROUP_MEMBER_EXPULSION_FAILED | 조건부 상태 변경 영향 행 수가 1이 아님 |
| 500 | INTERNAL_ERROR | 예상하지 못한 DB·서버 오류 |

### 테스트 및 직접 검증

`SavingGroupJoinApprovalServiceTest`는 계주 성공, 비계주, 대상 없음·비승인 상태, 계주 본인 차단, 조건부 UPDATE 영향 행 수 불일치를 검증한다. `SavingGroupJoinApprovalControllerTest`는 두 URL 별칭, 인증 누락, 잘못된 경로 값, Service 오류의 공통 응답을 검증한다.

현재 실행 환경에는 Java 17 toolchain이 없어 Gradle 테스트는 컴파일 전에 중단된다. Java 17을 준비한 뒤 아래 명령으로 재검증한다.

```powershell
.\gradlew.bat test --tests '*SavingGroupJoinApprovalServiceTest' --tests '*SavingGroupJoinApprovalControllerTest' --tests '*SavingGroupServiceImplTest'
```

<!-- AI작성: 실제 가입 신청 조회 API와 프론트 개별 승인 연결의 계약·제한·파일 목록을 기록한다. -->
## 계주 전용 가입 신청 목록과 프론트 승인 연결

### 목록 요청

`GET /api/saving-groups/{savingGroupId}/join-requests`

기존 Controller 별칭 `/saving-groups/{savingGroupId}/join-requests`도 동일하게 제공한다.

| 파라미터 | 타입 | 규칙 |
| --- | --- | --- |
| savingGroupId | Long 경로 변수 | 양수, saving_group.saving_group_id |
| status | String 쿼리 | 생략하면 전체, PENDING / APPROVED / REJECTED만 허용 |
| page | int 쿼리 | 기본 1, 1 이상, 고정 페이지 크기 10 |

- 서버 인증 속성 memberId가 없거나 유효하지 않으면 401 / UNAUTHORIZED. 클라이언트가 보낸 회원 ID를 사용하지 않는다.
- 모임 없음·삭제는 404 / SAVING_GROUP_NOT_FOUND, groups.member_id 불일치 또는 OWNER + APPROVED 관계가 없으면 403 / SAVING_GROUP_OWNER_REQUIRED.
- 권한 확인은 승인과 같은 공통 검증을 사용한다. 기존 계모임·계주 관계의 잠금 조회를 재사용하므로 조회 Service도 트랜잭션으로 실행한다.
- 목록은 해당 group_id의 현재 PENDING / APPROVED / REJECTED 관계만 대상으로 한다. OWNER 관계, LEFT·EXPELLED, 삭제 회원은 제외한다. 별도 신청 이력 테이블을 만들지 않는다.
- counts와 rows는 동일한 SQL 조건을 사용하며 requested_at DESC, crew_member_id DESC 정렬, 10개씩 조회한다. count가 0이거나 범위 초과 페이지면 목록 SELECT 없이 정상 빈 배열을 반환한다.
- 모집중이 아니어도 계주는 목록을 볼 수 있다. 실제 승인 가능 여부는 승인 Service가 다시 검사한다.

### 목록 응답

기존 ApiResponse의 data 배열·meta 구조를 사용한다.

```json
{
  "success": true,
  "data": [{
    "crewMemberId": "9007199254740993",
    "applicant": "가**",
    "regionName": "서울",
    "identityVerified": true,
    "regionVerified": false,
    "applicationMessage": null,
    "membershipStatus": "PENDING"
  }],
  "meta": {"page": 1, "size": 10, "total": 1, "totalPages": 1, "offset": 0},
  "error": null
}
```

- crewMemberId는 BIGINT 정밀도 손실을 막기 위해 문자열이다. 프론트는 Number로 변환하지 않고 승인 경로에 전달한다.
- 이름은 SQL에서 첫 글자 외 나머지를 *로 마스킹한다. 1글자 이름에도 *를 붙인다. 서버에 이름 원문을 반환하지 않는다.
- 회원 ID·연락처·생년월일·계좌·인증 시각 원문은 조회·반환하지 않는다. 지역명과 인증 완료 여부만 제공한다. 관심사는 이번 목록 응답에 추가하지 않는다.
- 신청 메시지는 기존 application_message를 표시하며 React의 텍스트 렌더링을 사용한다. HTML로 삽입하지 않는다. 메시지의 자유 입력 내용에 대한 별도 자동 개인정보 탐지 정책은 이번 범위에 없다.

### 프론트 동작

1. 신청 목록 탭에서 공통 client의 /api 기준 주소로 실제 목록을 조회한다. 기존 가입 신청 Mock 배열은 import하지 않는다.
2. 전체·대기·승인·거절 필터 변경 시 1페이지로 돌아가고 서버에서 다시 조회한다. 페이지 번호는 기존 최대 5개 묶음 함수를 재사용한다.
3. 조회 중 안내, 오류·다시 시도, 빈 목록, 범위 초과·첫 페이지 이동을 각각 표시한다. 실패를 빈 목록이나 Mock 데이터로 바꾸지 않는다.
4. 화면에 있는 대기 신청의 실제 ID로 본문 없는 PATCH를 보낸다. 같은 화면의 추가 승인·필터·페이지 이동은 승인 중 차단하고 연속 클릭은 ref로도 차단한다.
5. 승인 성공 후 완료 모달을 띄우고 서버 목록을 다시 읽는다. 로컬 상태를 임의로 승인으로 바꾸지 않는다. 재조회 실패 시에는 오류와 재시도를 표시한다.
6. 승인 실패는 서버 오류 문구로 안내하고 목록을 재조회한다. 실패한 신청을 성공으로 표시하지 않는다. 화면 전환으로 끝난 이전 조회는 취소·무시한다.
7. 선택 승인·거절·상세 조회는 API가 확정되지 않아 비활성화한다. 선택 승인·거절 연결 TODO를 남긴다.

### 연결 전 남은 사항

- JWT와 토큰 제공은 사용자 요청대로 TODO다. JWT 브랜치 병합 후 위 GET 두 경로와 승인 PATCH 두 경로에 인증을 등록하고 기존 client의 토큰 제공 방식을 연결해야 한다. 임의 토큰 저장소·회원 ID·인증 우회는 추가하지 않았다.
- 현재 인증 미연결 상태의 실제 목록 요청은 401이며 화면에 로그인 필요 문구가 표시된다. 정상 조회·승인 응답 검증은 테스트에서만 인증 완료 속성을 주입한다.
- 상위 상세 페이지의 기본 모임 정보·모임 존재 여부 판정은 기존 Mock 상태다. 따라서 Mock에 없는 실제 모임 ID는 상위 페이지에서 차단된다. 상세 조회 API 연결은 별도 작업이며 실제 전체 모임 접근 검증은 그 뒤에 가능하다.
- 계금 요청 탭의 기존 Mock은 별도 탭으로 유지했다. 실제 가입 신청 목록에 금융 Mock 데이터를 섞지 않는다.
- schema.sql·공용 DB·새 의존성은 변경하지 않았다. 실제 MySQL SQL·마스킹·동시성은 환경 설정이 없어 미검증이며 브라우저·실제 JWT 통합도 아직 수행하지 않았다.

### 변경 파일 (17개)

Java 구현 경로 기준은 back/src/main/java/com/nightstudy/moa/savingGroup/이다.

| 영역 | 파일 |
| --- | --- |
| Controller | controller/SavingGroupController.java |
| Service | service/SavingGroupService.java, service/SavingGroupServiceImpl.java |
| Mapper | mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml |
| 신규 DTO | dto/SavingGroupJoinRequestRow.java, dto/SavingGroupJoinRequestListResponse.java |
| 신규 목록 테스트 | back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupJoinRequestServiceTest.java |
| 기존 테스트 확장 | back/src/test/java/com/nightstudy/moa/savingGroup/controller/SavingGroupJoinApprovalControllerTest.java, back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupJoinApprovalMySqlTest.java |
| 프론트 API | front/src/api/savingGroups.js, front/src/api/savingGroups.test.js |
| 화면 | front/src/components/savingGroup/SavingGroupDetail.jsx, front/src/components/savingGroup/SavingGroupJoinRequestsPanel.jsx, front/src/components/savingGroup/SavingGroupJoinRequestList.jsx |
| 문서 | docs/saving-groups-api.md, docs/ai-usage-log.md |

### 검증 결과와 재현

백엔드: back에서 `gradlew.bat -Dorg.gradle.java.installations.paths=C:\dev\jdk-17.0.12_windows-x64_bin\jdk-17.0.12 test --tests "*SavingGroupJoin*"`.
컴파일 및 Controller 31개·승인 Service 12개·목록 Service/실제 XML 바인딩 22개, 총 65개 통과. MySQL 통합 9개는 건너뜀.

프론트: front에서 `node --test src/api/savingGroups.test.js` 15개 통과, `npm run lint`와 `npm run build` 통과. 기존 번들 크기 경고는 유지한다.

브라우저 직접 확인: 기존 상세 화면에서 신청 목록 탭을 연다. JWT 미연결 시 로그인 필요 오류와 다시 시도 버튼이 나타나고 Mock 신청은 나오지 않아야 한다. JWT·상세 조회 연결 후에는 계주의 실제 모임으로 정상 목록·필터·빈 목록·개별 승인·실패·중복 클릭을 확인한다.

<!-- AI작성: 계원이 모집중인 계모임에서 본인 관계만 탈퇴 처리하는 API 계약이다. -->
## 계원 자진 탈퇴 API

### 경로와 요청

```http
DELETE /api/saving-groups/{savingGroupId}/members/me
```

기존 Controller 별칭도 유지한다.

```http
DELETE /saving-groups/{savingGroupId}/members/me
```

- `savingGroupId`는 양수 Long이며 `saving_group.saving_group_id`다.
- 요청 본문은 없다. 탈퇴 대상 회원 ID는 URL·쿼리·헤더·본문에서 받지 않고, 서버 인증 처리의 request attribute `memberId`(Long)로만 결정한다.
- 인증 속성이 없거나 0 이하이면 HTTP 401 `UNAUTHORIZED`를 반환한다.
- 현재 JWT 인터셉터가 이 브랜치에 연결되지 않았으므로 실제 HTTP 요청은 인증 속성이 없어 401을 반환한다. JWT 병합 시 두 DELETE 경로를 보호하고 `memberId`를 설정해야 한다.

### 처리와 상태 전이

1. `saving_group`과 삭제되지 않은 `groups` 관계를 `FOR UPDATE`로 조회한다.
2. 계모임 상태가 기존 시작 전 상태 코드 `01(모집중)`인지 확인한다. `00`, `02(진행중)`, `03`, `04`에서는 탈퇴할 수 없다.
3. 로그인 회원의 해당 `group_member` 관계를 잠금 조회하고 `membership_status = 'APPROVED'`인지 확인한다.
4. 조회한 `crew_member_id`, 내부 `group_id`, `membership_status = 'APPROVED'`가 모두 일치할 때만 `membership_status`를 `LEFT`로 변경하고 `left_at`에 현재 시각을 기록한다.

기존 `group_member.membership_status`의 `LEFT` 및 `left_at` 컬럼을 재사용한다. 물리 삭제·새 DTO·DB 마이그레이션은 추가하지 않는다. 조건부 UPDATE 영향 행이 정확히 1건이 아니면 트랜잭션을 롤백한다.

### 성공 응답

HTTP 200, 기존 삭제 성공 형식인 `ApiResponse.ok()`를 사용한다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 오류 응답

기존 `GlobalExceptionHandler`와 `ApiResponse.fail`을 재사용한다.

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 계모임 ID가 없거나 0 이하, 경로 타입이 올바르지 않음 |
| 401 | `UNAUTHORIZED` | 서버 인증 회원 ID가 없거나 0 이하 |
| 404 | `SAVING_GROUP_NOT_FOUND` | 계모임이 없거나 삭제됨 |
| 404 | `SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND` | 로그인 회원의 승인된 계원 관계가 없음 |
| 409 | `SAVING_GROUP_LEAVE_UNAVAILABLE` | 상태가 모집중(`01`)이 아님 |
| 409 | `SAVING_GROUP_MEMBER_LEAVE_FAILED` | 조건부 상태 변경 영향 행 수가 1이 아님 |
| 500 | `INTERNAL_ERROR` | 예기치 않은 DB·서버 오류 |

### 테스트 및 직접 검증

`SavingGroupLeaveServiceTest`는 모집중 승인 계원의 탈퇴, 계원이 아님, `00·02·03·04` 상태 차단, 조건부 UPDATE 실패 오류를 검증한다. `SavingGroupLeaveControllerTest`는 두 경로 별칭의 성공 응답, 비로그인 401, 비계원 404, 진행중 409 공통 응답을 검증한다.

Java 17이 준비된 환경에서 back 폴더에서 실행한다.

```powershell
.\gradlew.bat test --tests "*SavingGroupLeave*"
```

실제 JWT와 MySQL 통합은 아직 검증하지 않았다. JWT 병합 후에는 유효 토큰의 성공 요청, 비회원·진행중 모임의 실패, `LEFT`와 `left_at` 저장값, 동시 탈퇴 시 조건부 UPDATE·롤백을 별도 테스트 DB에서 확인해야 한다.

<!-- AI작성: 5단계 검색 초기화·결과 건수 표시 동작만 기록한다. -->
## 검색 초기화와 결과 건수 표시

- 정상 조회 결과가 0건이면 기존 빈 결과 안내 아래 `검색 조건 초기화` 버튼을 표시한다. 범위 초과 페이지는 기존 첫 페이지 이동 버튼을 유지한다.
- 초기화하면 입력 중인 검색어·제출한 검색어·시·구·동·검색어 오류·페이지를 초기 상태로 되돌리고 전체 목록을 다시 조회한다. 이미 전체 조건이더라도 재조회한다.
- 결과 건수는 기존과 같이 목록 조회 성공 후에만 표시한다. 로딩·실패 시 건수를 표시하지 않는다.
- 브라우저 검증: 결과 없는 검색 후 초기화하여 검색어·지역이 비워지고 1페이지 전체 조회가 실행되는지 확인한다. 전체 결과가 0건인 경우에도 초기화 재조회가 실행되는지 확인한다. 느린 네트워크와 목록 오류 상태에서 건수가 표시되지 않는지 확인한다.
- 실행 검증: 변경 페이지 oxlint 및 프론트 빌드 통과(번들 크기 경고). 실제 브라우저 검증은 미실행이다.

<!-- AI작성: 2026-11-03 승인된 가입 신청 제한 3단계의 상세 응답 계약입니다. -->
## 가입 신청 가능 여부 응답 확장 (2026-11-03)

상세 API의 기존 필드에 아래 4개 필드를 추가한다. 본인의 관계만 반환하며 다른 회원의 신청 정보는 포함하지 않는다. 기존 myRole 권한 판정과 공개 상세 조회 권한은 유지한다.

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| membershipStatus | String 또는 null | 본인 관계의 PENDING / APPROVED / REJECTED / LEFT / EXPELLED. 비로그인·관계 없음은 null |
| canApply | boolean | 조회 시점의 신청 가능 여부. 실제 신청에서는 최신 데이터를 다시 검사 |
| joinRestrictionCode | String 또는 null | 신청 불가 사유 코드. 신청 가능하면 null |
| joinRestrictionMessage | String 또는 null | 신청 불가 안내 문구. 신청 가능하면 null |

- 비로그인도 상세 조회는 성공한다. canApply=false, joinRestrictionCode=UNAUTHORIZED, membershipStatus=null을 반환한다. 이 코드는 정상 상세 응답의 데이터이며 HTTP 401 응답을 의미하지 않는다.
- 로그인한 사용자는 본인 기존 관계 제한을 먼저 안내한다. APPROVED는 가입 완료, PENDING은 심사 중이며 두 상태 모두 신청 불가다.
- REJECTED와 EXPELLED는 모두 JOIN_REAPPLICATION_FORBIDDEN을 유지하되 안내 문구에 거절·추방 사유를 각각 표시한다.
- LEFT는 재신청 대기 기간이 없지만 계정·본인 인증·역할·모임·정원 등 나머지 조건을 충족해야 한다. 재신청 성공은 가입 완료가 아닌 PENDING이다.
- 그다음 계정·본인 인증·역할 등 기존 신청 자격과 모임 조건을 검사한다. 상세 조회에 쓰는 회원·관계·가입 수 조회는 FOR UPDATE 없는 읽기 전용 조회다.
- 미정의 가입 상태 등 데이터 오류는 신청 불가로 숨기지 않고 기존 서버 오류로 처리한다.
- 신청·승인 인원 집계 및 중복 방지 구조는 유지한다. 상세 응답은 조회 시점의 안내이므로 신청 또는 승인 성공을 보장하지 않는다.
- 3단계에서 서버 조건과 응답을 구현했고, 5단계에서 아래와 같이 기존 버튼 크기를 유지하며 화면에 연결했다.

계모임은 기존과 같이 status=01일 때만 신규 신청할 수 있다. 정원 미설정·정원 도달과 최대 5개 가입 제한을 동일하게 반영한다. 상세 URL은 groupId, 가입 신청 URL은 savingGroupId를 사용하므로 두 식별자를 혼용하지 않는다.

수동 확인: GET /api/saving-groups/{groupId}를 비로그인·가입·심사 대기·거절·추방·탈퇴·관계 없음으로 각각 조회한다. POST /api/saving-groups/{savingGroupId}/join-requests에서도 같은 제한이 유지되는지 확인한다.

<!-- AI작성: 버튼 크기 변경 없이 재적용한 5단계 화면 동작입니다. -->
## 2026-11-03 가입 신청 화면 연결 — 기존 버튼 크기 유지

- GroupJoinAction은 상태 문구·클릭·활성 여부만 공유한다. 두 상세 화면이 기존 JoinButton을 ButtonComponent로 전달하므로 기존 너비·높이·글자 크기·여백·모바일 스타일을 그대로 사용한다. 버튼을 감싸는 고정 너비 영역이나 너비 100%·높이 44px 등의 새 스타일은 추가하지 않는다. 기존 화면에 이미 있던 모바일 너비 100% 설정은 유지한다.
- 버튼의 기존 styled CSS뿐 아니라 두 상세 컴포넌트의 기존 styled CSS 블록 전체를 4단계 기준 코드와 비교해 동일함을 확인했다. 상태 문구에 따른 자연스러운 너비 차이는 있을 수 있으나 기본 '가입 신청하기' 문구와 기존 치수 설정은 유지한다.
- 제한 사유는 GroupJoinReason으로 헤더 아래의 별도 문단에 표시한다. 버튼 안이나 버튼 폭을 결정하는 래퍼에 사유를 넣지 않는다. 안내는 13px이며 aria-describedby와 상태 알림으로 연결한다.

| 상태 | 버튼·처리 |
| --- | --- |
| 신청 가능·LEFT | 기존 문구 '가입 신청하기', 신청 가능 |
| 요청 처리 중 | '가입 신청 중...', 추가 클릭 차단 |
| APPROVED / PENDING | '가입 완료' / '심사 중', 비활성 |
| REJECTED / EXPELLED | '재신청 불가' / '재가입 불가', 비활성 및 사유 안내 |
| 모집 마감·정원 도달 | '모집 마감'·'정원 마감', 비활성 |
| 소모임 일정 경과·미설정 | '일정 종료'·'일정 미설정', 비활성 |
| 비로그인·본인 인증 미완료 | 기존 로그인·인증 화면 이동 버튼 |
| 신청 판단 필드 누락 | '상태 확인 필요', 임의 허용하지 않고 비활성 |

- 실제 신청은 서버가 canApply=true로 명시한 경우에만 실행한다. 가입 상태는 권한 역할 myRole과 분리하고, 소모임 표시 변환에도 네 응답 필드를 보존한다.
- 두 페이지는 ref 잠금으로 같은 렌더에서의 연속 클릭을 막는다. 성공 또는 오류 후 상세 GET으로 최신 상태를 다시 확인한다. 재조회 중·실패 시 오래된 신청 가능 화면을 숨기며 POST를 자동 반복하지 않는다.
- 신청 안내와 상세 응답을 모임 ID·authVersion으로 구분하고 화면·로그인 계정 변경 후 이전 응답이 안내를 열지 않게 한다. 완료 안내는 가입 승인과 구분해 운영자 심사를 기다리도록 표시한다.
- 로컬 검증 API를 이용한 실제 브라우저에서 상태 표시 22건, 두 모임의 연속 클릭 후 심사 중 갱신과 정원 충돌 후 갱신을 확인했다. NEW·CONFLICT 모두 각 모임 GET=2·POST=1이었다. 실제 Spring 서버·DB 종단 검증은 포함하지 않는다.
- 브라우저 실측: 데스크톱 두 버튼은 높이 38px·글자 13px·padding 8px 14px. 375px 모바일에서 소모임은 34px·12px·7px 10px, 계모임은 38px·13px·8px 14px로 기존 설정과 일치했다.
- 수동 확인: 기존 버튼 크기와 비교하고 상태별 문구·사유, 연속 클릭, 신청 성공 후 심사 중 전환, 정원 충돌 후 마감 안내, 로그인·인증 이동, 요청 중 모임·계정 변경, 조회 실패 후 재시도를 확인한다.

<!-- AI작성: 사용자 승인에 따라 공지 작성 1~5단계 구현과 검증 범위를 6단계에서 일괄 기록합니다. -->
## 계모임 공지 작성 (2026-11-04)

### 요청·응답

- `POST /api/saving-groups/{savingGroupId}/notices`
- 기존 Controller 별칭인 `POST /saving-groups/{savingGroupId}/notices`도 지원한다.
- `savingGroupId`는 양수 BIGINT인 계모임 식별자다. 상세 화면 URL의 공통 `groupId`와 구분한다.
- `Authorization: Bearer <로그인 토큰>`, `Content-Type: application/json`을 사용한다. JWT 필터가 설정한 요청 속성 `memberId`만 작성자로 사용한다.

| 요청 필드 | 타입 | 필수·검증 |
| --- | --- | --- |
| title | String | 필수, 공백만 입력 불가, 최대 100자 |
| content | String | 필수, 공백만 입력 불가, 최대 5,000자 |

```json
{
  "title": "다음 모임 안내",
  "content": "다음 모임 일정을 안내합니다."
}
```

기존 게시글과 동일하게 요청 문자열 길이를 검증한 뒤 앞뒤 공백을 제거해 저장한다. 작성자·모임 ID·작성일은 본문 입력 항목이 아니다. 성공은 HTTP 200, `ApiResponse<Void>`이며 공지 ID를 반환하지 않는다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 권한·상태·저장 흐름

1. Controller는 경로와 JSON 본문을 검증하고 Service에 인증 회원 ID·제목·내용을 전달한다.
2. Service는 로그인과 양수 계모임 ID를 확인하고 `selectSavingGroupJoinTargetForUpdate`로 삭제되지 않은 계모임과 공통 모임을 조회·잠근다.
3. 모집중(`01`)·진행중(`02`)만 허용한다. 심사중(`00`)·정지(`03`)·종료(`04`) 및 미정의 상태는 차단한다.
4. 조회한 내부 `groupId`와 인증 회원 ID로 `selectSavingGroupMembershipForUpdate`를 호출한다. `APPROVED`이면서 `OWNER` 또는 `MANAGER`여야 한다.
5. `SavingGroupNoticeCreateRow`와 `insertSavingGroupNotice`로 공지를 저장한다. 영향 행 수가 1이 아니면 예외를 발생시킨다. 검증과 저장은 하나의 트랜잭션으로 처리한다.

일반 회원, 대상 모임 비회원, 가입 대기·거절·탈퇴·추방 회원, 다른 모임에만 OWNER·MANAGER 관계가 있는 회원은 작성할 수 없다. 관리자 `CONTENT` 이용 제한은 담당자 별도 구현 범위이며 이번 공지 Service에는 추가하지 않았다. 기존 JWT·세션 인증 검사는 유지한다.

기존 `group_notice(group_id, writer_id, title, content, created_at)`를 사용한다. `notice_id`는 자동 증가, `created_at`은 DB 기본값이다. 공지 테이블의 제목 저장 용량은 200자지만 API 입력 정책은 기존 게시글과 동일한 100자다. DB 스키마·마이그레이션 변경은 없다.

### 실패 응답

실패는 공통 `ApiResponse.fail(code, message)` 형식이다.

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 경로 타입·양수 조건, 제목·내용 필수·길이 검증 실패 |
| 400 | INVALID_REQUEST | JSON 문법 오류, 본문 누락 또는 null |
| 401 | LOGIN_REQUIRED | `/api/**` Security 진입 시 미인증 |
| 401 | UNAUTHORIZED | Service에 유효한 인증 회원 ID가 전달되지 않음 |
| 403 | SAVING_GROUP_NOTICE_WRITE_FORBIDDEN | 대상 모임의 승인된 OWNER·MANAGER가 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 대상 계모임이 없거나 공통 모임이 삭제됨 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중·진행중 외 모임 상태 |
| 500 | SAVING_GROUP_NOTICE_WRITE_FAILED | INSERT 영향 행 수가 1이 아님 |

JWT 만료·세션 무효화 등 인증 필터 오류와 예상하지 못한 DB 오류는 기존 공통 인증·예외 처리 정책을 따른다. 위 표의 저장 실패 코드는 모든 DB 예외를 변환하는 코드가 아니다.

### React 연동

- `createSavingGroupNotice(savingGroupId, { title, content }, { signal })`는 공통 Axios client의 `/api` baseURL·인증 처리를 재사용한다. ID 범위·정밀도를 확인하며 제목·내용만 JSON으로 전송한다.
- 서버 오류 코드·메시지·HTTP 상태와 요청 취소를 보존한다. 성공 시 서버의 `data`(null)를 반환한다.
- 최근 공지 영역의 작성 버튼·폼은 `membershipStatus=APPROVED`, `myRole=OWNER/MANAGER`, `status=01/02`를 모두 만족할 때만 표시한다. 누락·미정의 값은 작성 UI를 표시하지 않는다.
- 제목·내용 글자 수, 필수·공백 검증, 등록·취소, 처리 중 입력·버튼 비활성화와 ref 중복 제출 잠금을 적용한다. 일반 등록 실패는 입력을 유지한다.
- 401·403·404·409 응답 후에는 작성 UI를 숨기고 새로고침으로 현재 권한·상태를 다시 확인한다. 입력은 컴포넌트가 유지되는 동안 보존한다.
- 등록 성공 후 폼을 닫고 기존 상세 GET을 재조회한다. 재조회 실패 시 등록 성공을 안내하고 새로고침만 제공한다. POST를 자동 반복하지 않는다.
- 모임·인증 계정 변경 시 폼을 초기화하고, 이탈한 화면의 완료 응답은 반영하지 않는다. 서버에 이미 전달된 POST 자체를 되돌리지는 않는다.

### 작업 파일·재사용 구조

| 파일 (back/src/main/java/com/nightstudy/moa 기준) | 역할 |
| --- | --- |
| savingGroup/dto/SavingGroupNoticeCreateRow.java | 공지 저장 DTO |
| savingGroup/mapper/SavingGroupMapper.java | 공지 저장 계약 |
| savingGroup/service/SavingGroupService.java | 공지 작성 Service 계약 |
| savingGroup/service/SavingGroupServiceImpl.java | 모임·회원·권한 검증과 저장 |
| savingGroup/dto/SavingGroupNoticeCreateRequest.java | JSON 요청 Validation |
| savingGroup/controller/SavingGroupController.java | 작성 API와 공통 성공 응답 |

추가 변경 파일:

- `back/src/main/resources/mappers/SavingGroupMapper.xml`: 공지 INSERT.
- `back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupNoticeCreationServiceTest.java`: Service 테스트.
- `back/src/test/java/com/nightstudy/moa/savingGroup/controller/SavingGroupNoticeCreationControllerTest.java`: Controller 테스트.
- `front/src/api/savingGroups.js`, `front/src/api/savingGroups.test.js`: 요청 함수·API 테스트.
- `front/src/components/savingGroup/SavingGroupNoticeCreateForm.jsx`: 작성 폼.
- `front/src/components/savingGroup/SavingGroupDetail.jsx`, `front/src/pages/SavingGroupDetailPage.jsx`: 화면 연결·상세 갱신.

기존 계모임·회원 관계 Mapper, 공지 요약 조회, 게시글 Validation·trim 관례, `ApiResponse`, `ApiException`, `GlobalExceptionHandler`, 공통 Axios client·Button·테마를 재사용했다.

### 검증 결과와 남은 확인

- 3단계에서 운영 코드 컴파일 통과, 공지 Controller 21건·Service 29건 총 50건 통과. 로컬 Java 17 경로를 실행 인자로 지정했다.
- 기존 다른 테스트의 오래된 import로 전체 테스트 컴파일이 실패해 임시 Gradle init script로 위 두 테스트만 컴파일·실행했다. 저장소 빌드 설정이나 다른 테스트는 수정하지 않았다. 이 결과는 전체 백엔드 테스트 통과를 의미하지 않는다.
- 4단계 `node --test src/api/savingGroups.test.js` 37건 통과. 공지 요청 테스트 4건과 기존 회귀 테스트를 포함한다. 테스트는 client 대역을 사용하며 실제 HTTP·DB 검증이 아니다.
- 5단계 `npm run build` 통과. 변경 JSX lint 오류 없음, 기존 `ImagePlaceholder`·`Schedule` 미사용 경고 2건과 번들 크기 경고 유지. `git diff --check` 통과.
- 브라우저 자동 검증은 내장 브라우저의 로컬 서버 연결 실패 및 Windows 도구의 URL 확인 실패로 중단했다. 임시 검증 화면·서버를 정리했다. 실제 UI 상호작용·모바일·Postman·MySQL 저장·트랜잭션 동시성 검증은 미완료다.

수동 검증 절차:

1. 테스트용 계모임·계정에서 OWNER·MANAGER 각각 모집중·진행중 정상 등록을 확인한다. DB에서 내부 group_id·인증 writer_id·앞뒤 공백 제거·created_at을 확인한다.
2. Postman으로 일반 회원·비회원·가입 대기·탈퇴·타 모임 운영진의 403, 없는 모임·삭제 모임의 404, 심사중·정지·종료의 409, 미인증의 401을 확인한다.
3. 제목 100/101자, 내용 5,000/5,001자, 누락·공백·잘못된 JSON·경로를 확인한다.
4. 브라우저에서 위 역할·회원·모임 상태별 작성 버튼 숨김, 등록·취소·글자 수·실패 시 입력 유지·빠른 연속 클릭 차단을 확인한다.
5. 등록 성공 뒤 최근 공지 갱신, 성공 후 조회 실패 시 GET만 재시도, 작성 중 권한·모임 상태 변경, 계정·모임 변경 후 이전 응답 무시를 확인한다.
6. 모바일 줄바꿈·스크롤·키보드 초점 이동과 큰 글씨 가독성을 확인한다.

TODO·제외 범위:

- `NOTICE_CREATED` enum은 있으나 실제 사용 연계가 없어 공지 알림·SSE는 연결하지 않았다. 수신 대상·문구·이동 경로는 추후 정책 확정 후 별도 구현한다.
- 고정·첨부파일·공개 범위·예약 발행은 기존 저장 구조가 없어 추가하지 않았다. 수정·삭제·목록·상세·댓글·일정 연계는 이번 범위 밖이다. 기존 최근 공지 요약 조회만 재사용한다.
- 별도 금칙어 규칙과 관리자 이용 제한 연계, 실제 DB·브라우저 검증 및 전체 테스트 import 오류 정리는 담당자 후속 확인이 필요하다.

<!-- AI작성: 계모임 공지 요약에 소모임과 같은 작성자·작성일을 추가한 현재 응답 계약입니다. -->
## 계모임 최근 공지 작성자·작성일 표시 (2026-11-04)

기존 계모임 상세 조회 `GET /api/saving-groups/{id}`의 `data.notices` 항목에 다음 두 필드를 추가했다. 여기서 상세 URL의 id는 공통 groupId이며 공지 작성 URL의 savingGroupId와 다르다. 초기의 공지 ID·제목 전용 응답을 확장한 것으로 기존 필드는 유지한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| noticeId | Long | 기존 공지 식별자 |
| title | String | 기존 제목 |
| writerNickname | String 또는 null | 작성자 회원의 현재 닉네임. 실명·회원 ID는 추가 공개하지 않음 |
| createdAt | LocalDateTime의 ISO 문자열 | 기존 공지 작성일. 예: 2026-11-04T10:30:00 |

- SavingGroupNoticeSummaryResponse 생성자와 XML resultMap에 writerNickname·createdAt을 추가했다. SQL은 group_notice.writer_id로 members를 LEFT JOIN한다. 회원 행이 없으면 닉네임은 null이지만 공지 자체는 목록에 남긴다.
- 최신 작성일·공지 ID 내림차순과 최대 4건 조회는 유지한다. DB 스키마, 작성 API, Controller, Service, React 요청 함수는 변경하지 않았다.
- 화면은 소모임처럼 제목·작성자·작성일 열로 구성한다. 날짜는 한국어 연월일 형식이며 작성자 누락은 '작성자 정보 없음', 날짜 누락·오류는 '날짜 확인 필요'로 표시한다. 제목·닉네임은 줄바꿈을 허용한다.
- 공지 작성 버튼의 카드 우측 상단 배치, 작성 폼 표시 시 목록 숨김, 등록 성공·취소 시 목록 복귀 동작을 유지한다.
- 검증: Java 17 compileJava, 프론트 npm run build, 기존 savingGroups API 테스트 37건, XML 파싱·생성자 매핑 확인, git diff --check 통과. JSX lint는 기존 미사용 변수 경고 2건만 남았다. 이번 변경에 대한 실제 DB 조회·브라우저 검증은 수행하지 않았다.
- 수동 확인: 상세 응답 notices의 네 필드, 실제 작성자 닉네임·작성일과 목록 순서, 빈 목록, 긴 제목·닉네임, 모바일 줄바꿈을 확인한다. 작성·취소·등록 성공 전후 목록 전환도 확인한다. 닉네임은 작성 당시 스냅샷이 아닌 현재 회원 테이블 값이다.

<!-- AI작성: 사용자 확정 정책과 공지 상세 조회 1~5단계 구현·검증 결과를 6단계에서 일괄 기록한다. -->
## 계모임 공지 상세 조회 (2026-11-04)

이 절은 공지 상세 조회의 최신 계약이다. 앞의 공지 작성 작업에서 상세를 제외 범위로 기록한 내용은 당시 작업 범위를 의미하며, 이번 단계에서 아래 상세 기능을 추가했다.

### 요청·식별자·권한

- `GET /api/saving-groups/{groupId}/notices/{noticeId}`
- 기존 Controller 별칭 `GET /saving-groups/{groupId}/notices/{noticeId}`도 지원한다.
- 두 경로 변수는 양수 Long이다. `groupId`는 `groups.group_id`이며 공지 작성 POST의 `savingGroupId` (`saving_group_id`)와 다르다. 요청 본문은 없다.
- 인증은 기존 JWT·세션 검증과 서버 요청 속성 `memberId`를 사용한다. 쿼리의 회원 ID는 사용하지 않는다.
- 해당 계모임의 `APPROVED` 관계를 가진 `OWNER / MANAGER / MEMBER`만 본문을 조회할 수 있다. 작성은 OWNER·MANAGER만 가능하지만 상세 조회는 MEMBER도 가능하다.
- 관계 없음과 `PENDING / REJECTED / LEFT / EXPELLED`는 403이다. 공개 계모임 상세의 최근 공지 요약 공개 정책은 유지하며 본문 조회 권한과 구분한다.
- 기존 계모임 상세와 동일하게 `groups.deleted_at IS NULL`, `saving_group.status IN ('01','02')`인 모임만 조회한다. 조회할 수 없는 모임은 404다. 작성 API의 상태 제한 409 정책과 구분한다.

### 성공 응답

HTTP 200, 기존 `ApiResponse.ok(data)`를 사용한다. 응답 항목은 아래 6개뿐이다.

```json
{
  "success": true,
  "data": {
    "noticeId": 40,
    "title": "다음 모임 안내",
    "content": "다음 모임 일정을 안내합니다.",
    "writerId": 30,
    "writerNickname": "테스트닉네임",
    "createdAt": "2026-11-04T10:00:00"
  },
  "meta": null,
  "error": null
}
```

ID는 기존 Long 숫자 직렬화, 작성일은 LocalDateTime ISO 문자열이다. 작성자 닉네임은 members의 현재 값이며 LEFT JOIN으로 회원 행이 없으면 null이다. 작성자 ID는 공지 행의 writer_id를 반환한다. 실명·전화번호·계좌 등은 응답에 포함하지 않는다. React는 작성자 ID와 공지 ID를 화면 본문에 표시하지 않는다. 큰 ID의 응답 숫자 정밀도에 관한 기존 계약 한계는 유지한다.

### Service·Mapper 처리

1. Service에서 인증 회원 ID와 두 경로 ID를 검증한다.
2. 기존 `selectSavingGroupDetail(groupId)`로 계모임 존재·삭제·상태를 확인한다.
3. 기존 잠금 없는 `selectSavingGroupMembership(groupId, memberId)`로 승인 상태와 역할을 확인한다. 승인 관계의 null·미정의 역할은 기존 상세의 데이터 오류 관례에 따라 500으로 처리한다.
4. `selectSavingGroupNoticeDetail(groupId, noticeId)`가 두 ID를 모두 WHERE 조건에 사용한다. 공지 없음과 다른 모임 소속 공지는 모두 동일한 404다.
5. 전체 조회는 `@Transactional(readOnly = true)`이며 Controller는 요청 검증·전달·응답만 담당한다. 조회 수 증가나 쓰기 잠금은 추가하지 않았다.

기존 `group_notice`, `members`, 계모임·회원 관계 Mapper, 공지 요약의 작성자 LEFT JOIN, `ApiResponse`, `ApiException`, `GlobalExceptionHandler`를 재사용했다. DB 스키마·마이그레이션·의존성·보안 설정 변경은 없다.

### 실패 응답

기존 실패 봉투 `success=false`, `data/meta=null`, `error.code/message`를 사용한다.

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 경로 타입·Long 범위·양수 검증 실패 |
| 401 | LOGIN_REQUIRED | `/api/**` 보안 계층에서 미인증 차단 |
| 401 | UNAUTHORIZED | Service에 유효한 인증 회원 ID가 전달되지 않음 |
| 403 | SAVING_GROUP_NOTICE_READ_FORBIDDEN | 대상 모임 관계 없음 또는 APPROVED가 아님 |
| 404 | NOT_FOUND | 모임 없음·삭제·상태 01/02 외. 메시지: 조회할 수 있는 계모임이 없습니다. |
| 404 | SAVING_GROUP_NOTICE_NOT_FOUND | 공지 없음 또는 다른 모임 소속. 메시지: 공지를 찾을 수 없습니다. |
| 500 | INTERNAL_ERROR | 미정의 승인 역할·관계 데이터 오류·예기치 않은 서버 오류 |

검증 순서는 인증 → 경로 ID → 모임 → 회원 관계 → 공지다. 미승인 사용자는 공지 존재 여부와 관계없이 회원 관계 단계에서 차단된다. JWT 만료·변조·세션 오류는 기존 인증 처리를 따른다.

### React 화면·요청

- `getSavingGroupNoticeDetail(groupId, noticeId, { signal })`는 기존 공통 client의 `/api` baseURL·인증을 재사용한다. 두 ID는 기존 양수 BIGINT 검증을 통과해야 하며 큰 문자열 ID를 Number로 변환하지 않는다.
- 서버 코드·메시지·HTTP 상태를 보존하고 `ERR_CANCELED`는 원본 그대로 전달한다. 통신·실패 봉투의 기본 오류는 `SAVING_GROUP_NOTICE_READ_FAILED`, 본문 객체가 없는 성공 응답은 `INVALID_RESPONSE`다.
- 화면 경로는 `/saving-groups/:id/notices/:noticeId`이며 id는 공통 group_id다. 승인된 OWNER·MANAGER·MEMBER이고 모임 상태가 01/02이면 기존 최근 공지 제목이 링크로 표시된다. 그 외에는 제목 텍스트를 유지한다. 서버가 실제 권한을 다시 검사한다.
- 페이지가 URL·요청·로딩·오류·재시도를 관리하고 별도 표시 컴포넌트가 제목·작성자 닉네임·한국어 작성일·본문을 렌더링한다. 본문은 일반 텍스트로 표시하며 줄바꿈을 유지한다. 닉네임·날짜 누락 안내는 기존 목록 관례를 따른다.
- URL·계정(authVersion)·재시도 회차가 바뀌면 이전 본문을 숨기고 이탈한 요청을 취소한다. 통신·서버 오류와 INVALID_RESPONSE에는 재시도를 제공한다. 400·401·403·404는 원인별 안내를 표시한다.
- '계모임 공지로 돌아가기'는 같은 groupId의 계모임 기본 공지 탭으로 이동한다. 직접 URL 접근·새로고침도 URL 기반 GET으로 처리한다.
- 공지 작성 폼·등록 후 최근 공지 갱신은 기존 코드를 유지한다. 전체 공지 목록 API나 화면은 추가하지 않았다.

### 작업 파일과 커밋

| 단계 | 파일 | 커밋 |
| --- | --- | --- |
| 1 | back/src/main/java/com/nightstudy/moa/savingGroup/dto/SavingGroupNoticeDetailResponse.java, savingGroup/mapper/SavingGroupMapper.java 및 back/src/main/resources/mappers/SavingGroupMapper.xml | 996620e |
| 2 | back/src/main/java/com/nightstudy/moa/savingGroup/service/SavingGroupService.java, SavingGroupServiceImpl.java 및 back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupNoticeDetailServiceTest.java | d49a930 |
| 3 | back/src/main/java/com/nightstudy/moa/savingGroup/controller/SavingGroupController.java 및 back/src/test/java/com/nightstudy/moa/savingGroup/controller/SavingGroupNoticeDetailControllerTest.java | 6e42e16 |
| 4 | front/src/api/savingGroups.js, savingGroups.test.js | adbcc29 |
| 5 | front/src/App.jsx, front/src/pages/SavingGroupNoticeDetailPage.jsx, front/src/components/savingGroup/SavingGroupNoticeDetail.jsx, SavingGroupDetail.jsx | 950bb07 |
| 6 | docs/saving-groups-api.md, docs/ai-usage-log.md | 개발자 커밋 예정 |

### 검증 결과·수동 확인·TODO

- 1단계 Mapper XML 구문 확인 통과. 각 단계 git diff --check 통과.
- 2단계 신규 상세·기존 작성 Service 테스트, 3단계 신규 상세·기존 작성 Controller 테스트 실행을 시도했으나 Java 17 toolchain을 찾지 못해 컴파일·테스트 실행 전에 중단됐다. Java 21은 확인했으며 프로젝트 설정은 변경하지 않았다. 앞의 작성 기능에 기록된 Java 17 성공 결과를 이번 상세 기능의 검증 결과로 간주하지 않는다.
- 4·5단계 `node --test src/api/savingGroups.test.js` 41건 통과. client 대역을 사용하는 요청 계약 테스트이며 실제 HTTP·DB·React 상호작용 검증은 아니다.
- 5단계 `npm run build` 통과. 변경 JSX lint는 기존 ImagePlaceholder·Schedule 미사용 경고 2건, 빌드는 번들 크기 경고가 남았다. 브라우저·Postman·MySQL 연동은 미검증이다.
- Service 테스트의 모임 제외·공지 소속 불일치는 Mapper null 대역으로 검증하므로 실제 SQL 조건·조인 동작을 입증하지 않는다. Controller standalone 테스트에는 실제 JWT 필터가 포함되지 않는다.

후속 수동 검증:

1. Java 17 환경에서 신규 상세 Service·Controller 및 기존 작성 테스트를 실행한다. 기존 다른 테스트의 컴파일 문제 여부도 확인한다.
2. 테스트용 계모임의 group_id와 saving_group_id가 다른 데이터로 GET 경로가 group_id를 사용하는지 확인한다. 승인 OWNER·MANAGER·MEMBER와 모임 상태 01/02에서 200 및 6개 필드를 확인한다.
3. 미인증 401, 관계 없음·PENDING·REJECTED·LEFT·EXPELLED 403, 모임 없음·삭제·상태 00/03/04의 404, 공지 없음·다른 모임 공지의 404, 두 ID 각각 타입·범위·양수 오류 400을 확인한다.
4. 실제 DB에서 공지·작성자 매핑, 닉네임 누락, 같은 공지 반복 조회 시 조회 데이터 변경 없음과 소속 조건을 확인한다.
5. 브라우저에서 승인 역할별 제목 링크, 직접 접근·새로고침·돌아가기, 오류 안내·재시도, URL·계정 변경·로그아웃 뒤 이전 본문 숨김을 확인한다. 작성·취소·등록 성공 후 최근 공지 갱신도 회귀 확인한다.
6. 긴 제목·본문·닉네임, 줄바꿈·HTML 문자 텍스트 표시, 모바일·키보드 초점·가독성을 확인한다.

수정일·고정·첨부파일·조회 수·공지 삭제/비공개 상태는 테이블에 없어 추가하지 않았다. 향후 필요 시 별도 정책·DB 설계를 확정해야 한다. 작성·수정·삭제·전체 목록·댓글·알림·SSE 연계는 이번 상세 조회 범위 밖이다.

<!-- AI작성: 사용자 확정 정책에 따라 페이징·고정 구현을 완료하고 마지막 단계에서 일괄 문서화한다. -->
<a id="notice-pagination-and-pin"></a>

## 계모임 공지 페이징·고정 최신 계약 (2026-11-04)

### 확정 정책과 DB 선행 조건

- 기존 계모임 홈 공지 영역에 일반 공지를 페이지당 4개 표시한다. 고정 공지는 모든 페이지 상단의 별도 영역에 표시하며 일반 목록과 전체 개수에 중복 포함하지 않는다.
- 고정은 모임당 최대 2개다. 해당 모임의 APPROVED OWNER·MANAGER만 지정·해제한다. APPROVED MEMBER는 본문을 읽을 수 있지만 고정을 변경할 수 없다.
- DB 담당자가 `group_notice.is_pinned TINYINT(1) NOT NULL DEFAULT 0`을 확정·반영했다고 사용자가 확인했다. 0은 일반, 1은 고정이다. AI는 DB 접속·DDL 실행·schema.sql 변경을 하지 않았다.
- 저장소의 schema.sql에는 아직 해당 컬럼이 없다. 신규 DB를 schema.sql만으로 생성하는 경우 DB 담당자가 공유하는 최신 스키마 반영이 선행되어야 한다. 소모임 SQL과 공지 작성 INSERT는 변경하지 않았으며 기존 INSERT는 컬럼 기본값 0을 사용한다.

### 기존 상세 GET의 공지 조회 확장

`GET /saving-groups/{id}?noticePage=1&noticeSort=LATEST`

`/api/saving-groups/{id}` 별칭도 동일하다. id는 saving_group_id가 아닌 공통 groups.group_id다. 별도 공지 목록 GET 경로는 추가하지 않았다.

| 항목 | 규칙 |
| --- | --- |
| noticePage | Integer, 기본 1, 1 이상. 타입 오류·범위 초과·빈 값·0·음수는 400 BAD_REQUEST |
| noticeSort | 기본 LATEST. LATEST 또는 OLDEST만 허용하며 빈 값·다른 값은 400 BAD_REQUEST |
| 페이지 크기 | 서버에서 4 고정. size 변경 파라미터는 제공하지 않음 |
| data.notices | 현재 페이지의 일반 공지 요약 |
| data.pinnedNotices | 페이지·일반 정렬과 독립적인 고정 공지 요약, 작성일·공지 ID 내림차순 |
| meta | 기존 PageMeta. page·size·total·totalPages와 기존 getOffset()의 offset |

두 목록의 항목은 기존 SavingGroupNoticeSummaryResponse(noticeId, title, writerNickname, createdAt)를 재사용한다. 본문·작성자 회원 ID는 포함하지 않는다. 일반 공지 정렬은 작성일과 공지 ID를 함께 ASC 또는 DESC로 정렬한다. members LEFT JOIN으로 작성자 행이 없어도 공지를 유지한다.

기존 상세 데이터 필드와 공개 범위는 유지한다. 고정 목록도 기존 공개 요약 범위이며 공지 본문은 기존 승인 회원 상세 API를 사용한다. 없는 모임·삭제된 모임·상태 01/02 이외의 모임 조회는 기존 404 정책을 유지한다.

```json
{
  "success": true,
  "data": {
    "groupId": 20,
    "notices": [],
    "pinnedNotices": [
      { "noticeId": 40, "title": "예시 고정 공지", "writerNickname": "예시 작성자", "createdAt": "2026-11-04T10:00:00" }
    ]
  },
  "meta": { "page": 1, "size": 4, "total": 0, "totalPages": 0, "offset": 0 },
  "error": null
}
```

예시는 공지 관련 항목만 발췌했다. 실제 data에는 기존 모임 정보·본인 역할·가입 상태 등이 유지된다. 고정만 존재해도 일반 공지 total은 0이다. Service는 빈 목록·범위 초과에서 offset 계산·일반 목록 SQL을 생략하고 요청 페이지의 meta를 유지한다. React는 마지막 유효 페이지(최소 1)로 URL을 보정해 재조회한다. 공통 PageMeta는 수정하지 않았다.

### 고정 지정·해제 PATCH

`PATCH /saving-groups/{groupId}/notices/{noticeId}/pin`

`/api/saving-groups/{groupId}/notices/{noticeId}/pin` 별칭도 지원한다. 두 ID는 양수 Long이다. JWT 인증 계층이 전달하는 요청 속성 memberId만 사용하며 쿼리·본문의 처리자 ID는 권한 근거로 사용하지 않는다.

```json
{ "pinned": true }
```

지정은 true, 해제는 false다. pinned는 필수이며 누락·null은 400이다. 성공은 HTTP 200의 기존 ApiResponse.ok()다. 토글이 아니라 목표 상태를 전달하며 이미 같은 상태인 요청은 추가 UPDATE 없이 성공한다.

| HTTP·코드 | 조건 |
| --- | --- |
| 400 BAD_REQUEST | 잘못된 ID·필수 고정 여부 누락·입력 검증 실패 |
| 400 INVALID_REQUEST | 공통 예외 처리기가 처리하는 잘못된 JSON·본문 없음 |
| 401 UNAUTHORIZED | 인증 회원 없음 |
| 403 SAVING_GROUP_NOTICE_PIN_FORBIDDEN | 관계 없음, 미승인 관계, 승인 MEMBER 또는 허용되지 않는 역할 |
| 404 NOT_FOUND | 계모임 없음·삭제 |
| 404 SAVING_GROUP_NOTICE_NOT_FOUND | 공지 없음 또는 다른 모임의 공지 |
| 409 SAVING_GROUP_ACTIVITY_RESTRICTED | 계모임 상태가 01·02가 아님 |
| 409 SAVING_GROUP_NOTICE_PIN_LIMIT_EXCEEDED | 새로운 고정 지정 시 이미 2개 이상 존재 |
| 500 SAVING_GROUP_NOTICE_PIN_FAILED | 상태 변경 UPDATE 영향 행 수가 예상과 다름 |

Service는 쓰기 트랜잭션에서 모임·승인 관계·대상 공지 순서로 잠근다. 새로운 고정 지정일 때 FOR UPDATE로 현재 고정 ID 전체를 조회하고 목록 크기로 제한을 검증한다. 모임 잠금은 고정 공지가 0개일 때도 동시 변경을 직렬화하는 기준이다. 해제에는 남은 고정 슬롯을 요구하지 않는다. 공지 UPDATE는 group_id·notice_id를 모두 제한하며 요청 상태와 다른 행만 변경한다. 실제 MySQL의 잠금·동시성 실행 검증은 별도로 필요하다.

### React 화면과 상태 유지

- getSavingGroupDetail은 기존 상세 경로에 noticePage·noticeSort를 전달하고 기존 data 필드를 유지하면서 meta를 noticePageMeta로 전달한다. pinnedNotices는 data의 별도 배열을 그대로 사용한다.
- 우측 정렬 버튼은 최신순·과거순이며 기본 최신순이다. 선택 상태를 aria-pressed로 표시한다. 정렬 변경은 1페이지로 이동한다.
- 페이지 번호는 최대 3개 묶음이다. 전체 7페이지에서 1/2/3의 이전은 비활성, 다음은 4/5/6의 4페이지다. 여기서 이전은 1/2/3의 3페이지, 다음은 7페이지다. 7의 이전은 6페이지이고 다음은 비활성이다.
- 0건·한 페이지 모두 `비활성 이전 | 1 | 비활성 다음`을 표시한다. 페이지 이동은 스크롤을 강제로 옮기지 않는다.
- URL noticePage·noticeSort를 기준으로 조회한다. 상세 링크에도 두 조건을 전달하며 상세의 돌아가기와 브라우저 뒤로 가기·새로고침에서 복원한다. 직접 상세 진입은 최신순 1페이지로 돌아간다. 직접 입력한 잘못된 프론트 URL 조건은 기본값으로 해석한다.
- 고정 영역은 [고정] 배지·배경·별도 제목으로 구분한다. 승인 OWNER·MANAGER에게만 고정 지정·해제 버튼을 제공한다. 일반 공지와 동일한 상세 링크·작성자·작성일 표시를 재사용한다.
- 로딩은 `공지를 불러오는 중입니다.`, 목록 오류는 `공지를 불러오지 못했습니다.`와 `새로 고침`, 전체 0건은 `등록된 공지가 없습니다.`, 고정만 있으면 `등록된 일반 공지가 없습니다.`를 표시한다.
- 고정 변경 중에는 정렬·페이지·고정·작성 버튼을 비활성화하고 중복 PATCH를 ref로 차단한다. 전송된 PATCH를 화면 이탈로 취소한 것으로 간주하지 않는다.
- 고정 변경 후 현재 페이지·정렬로 다시 조회하며 페이지가 사라지면 마지막 유효 페이지로 보정한다. 변경 성공 후 재조회 실패는 성공한 PATCH를 반복하지 않고 조회만 재시도한다. 변경 실패·응답 유실도 목록 확인 전까지 추가 고정 변경을 막는다.
- 작성 성공 후에는 최신순 1페이지로 갱신한다. GET 취소·요청 식별·모임/계정 범위 검사를 사용해 이전 응답을 현재 화면에 반영하지 않는다.

### 단계별 파일·커밋

| 단계 | 주요 파일 | 커밋 |
| --- | --- | --- |
| 페이징 DTO·Mapper | SavingGroupNoticeSearchCondition, SavingGroupMapper.java/XML | 96c46a7 |
| 페이징 Service | SavingGroupService/Impl, SavingGroupDetailPageResponse, PagingServiceTest | 894c413 |
| 페이징 Controller | SavingGroupController, PagingControllerTest | 8fb8c64 |
| 페이징 프론트 API | front/src/api/savingGroups.js/test.js | b36fdb8 |
| 페이징 화면 | SavingGroupDetailPage, SavingGroupDetail, API/계산 테스트 | 230dbf6 |
| 조회 상태 복원 | 홈·상세 페이지, 상세 컴포넌트, 작성 폼, API/테스트 | a64d083 |
| 고정 DTO·Mapper | SavingGroupNoticePinRequest/Row, SavingGroupMapper.java/XML, PinMapperTest | d7ced79 |
| 고정 Service | SavingGroupService/Impl, SavingGroupDetailResponse, PinServiceTest | f6b00dc |
| 고정 Controller | SavingGroupController, PinControllerTest | a12d449 |
| 고정 프론트 API | front/src/api/savingGroups.js/test.js | 47d4c5a |
| 고정 화면 | SavingGroupDetailPage, SavingGroupDetail, SavingGroupNoticeCreateForm | a07d2e7 |

### 검증 결과와 후속 수동 검증

- 설치된 Java 17 경로를 Gradle 실행 인자로 지정했다. 운영 코드 컴파일과 공지 관련 Controller/Service/Mapper·가입 제한 테스트 208건이 통과했다. 프로젝트 빌드 설정은 변경하지 않았다.
- 전체 테스트는 기존 타 기능의 AuthSessionService 등 클래스 누락으로 컴파일이 막히므로 임시 Gradle init script로 compileTestJava 대상을 관련 파일로 제한했다. 전체 테스트 통과를 의미하지 않는다.
- `node --test src/api/savingGroups.test.js` 47건, 최종 `npm run build`, `git diff --check` 통과. 변경 파일 lint 오류 없음. 기존 ImagePlaceholder·Schedule 미사용 경고 2건과 번들 크기 경고는 유지했다.
- 임시 Vite SSR 렌더링으로 고정 2개+일반 4개, 운영진/일반회원/비회원 표시 차이, 복귀 링크, 처리 중 비활성, 오류 재조회, 고정만 있는 상태를 확인했다. 이는 실제 브라우저 클릭 검증이 아니다. 임시 파일·실행 설정은 정리한다.
- 실제 DB 접속·SQL 실행·JWT 필터 포함 HTTP·브라우저/모바일·동시성 검증은 수행하지 않았다. DB 컬럼 반영은 사용자 확인에 근거한다.

수동 확인 순서:

1. 실제 상세 GET에서 일반 공지 최대 4개, 고정 별도 배열, meta의 일반 공지 개수와 양방향 정렬을 확인한다.
2. 승인 OWNER와 MANAGER로 고정 지정·해제하고 3번째 지정의 409, 동일 상태 재요청의 성공, MEMBER·미승인·타 모임 요청 차단을 확인한다.
3. 두 운영진이 동시에 마지막 슬롯을 지정해도 결과가 최대 2개인지 실제 MySQL 환경에서 검증한다.
4. 7페이지 기준 묶음 이동, 한 페이지·빈 목록, 상세 복귀·뒤로 가기·새로고침을 확인한다.
5. 고정 변경으로 마지막 일반 페이지가 사라질 때 보정, 통신 실패·PATCH 응답 유실 후 조회만 재시도, 계정 변경·빠른 정렬 전환을 확인한다.
6. 작은 화면의 긴 제목·작성자·날짜와 키보드 초점, 비활성 버튼·고정 배지 구분을 확인한다.

공지 수정·삭제·첨부·예약·알림/SSE 연계는 이번 범위 밖이다. 기존 BIGINT JSON 숫자 정밀도 한계는 별도 작업이다. DB 담당자의 저장소 기준 스키마 공유도 후속 확인 항목이다.
# 계모임 공지 수정 — 2026-11-04

<!-- AI작성: 사용자 최종 확정인 작성자 본인 수정 정책을 기록한다. -->
- `PUT /api/saving-groups/{groupId}/notices/{noticeId}` 및 `/saving-groups` 별칭.
- 첫 경로 ID는 기존 상세·고정 API와 동일한 `groups.group_id`이며 `saving_group_id`가 아니다.
- JWT `memberId`와 공지 `writer_id`가 일치하는 작성자만 수정한다. 기존 승인 회원·운영진(OWNER 또는 MANAGER)·모임 상태 조건도 적용하며 타 운영진의 수정은 금지한다.
- 모집중(01)·진행중(02)의 삭제되지 않은 계모임과 미삭제 소속 공지만 수정한다.
- JSON 본문: `{"title":"제목","content":"내용"}`. 두 필드 필수, 제목 최대 100자·내용 최대 5,000자, 앞뒤 공백 제거. 부분 수정은 지원하지 않는다.
- 성공: 기존 `ApiResponse<Long>`의 data에 공지 ID. 동일 내용 재요청도 성공.
- 제목·내용 및 기존 updated_at만 갱신하고 원작성자·작성일·고정 상태는 보존한다. DB 변경 없음.
- 기존 공통 오류 응답: 400 BAD_REQUEST, 401 UNAUTHORIZED, 404 NOT_FOUND(계모임), 404 SAVING_GROUP_NOTICE_NOT_FOUND(없음·타 모임·삭제 공지), 403 SAVING_GROUP_NOTICE_EDIT_FORBIDDEN, 409 SAVING_GROUP_ACTIVITY_RESTRICTED, 500 SAVING_GROUP_NOTICE_EDIT_FAILED.
- 상세 내부 수정 폼은 작성자 및 현재 모임 역할·상태를 확인해 표시한다. JWT subject 비교는 버튼 표시용이며 최종 권한은 서버 검증이다.
- 저장 중 중복 제출 방지, 취소 시 미저장, 실패 시 입력 유지. 권한·상태 제한 응답은 편집을 종료하고 안내한다. 성공 후 정규화된 제목·내용을 상세에 반영하며 목록 복귀 시 기존 페이지·정렬을 유지한다.
- 검증: 공지 관련 백엔드 244건, 프론트 API 48건, 변경 파일 lint·빌드 통과. 수정 폼은 Vite SSR로 초기값·입력 길이·버튼 렌더링 확인. 기존 번들 크기 경고 존재.
- 기존 타 도메인 테스트 import 오류로 전체 테스트 컴파일은 불가하여 임시 Gradle init script로 공지 테스트만 분리했다. 저장소 설정·의존성은 변경하지 않았다.
- 실제 MySQL·Postman·브라우저 클릭·JWT 필터 통합 검증은 미실행. SQL 검증은 실제 XML 파싱·바인딩이며 DB 실행이 아니다.
- 수동 검증: 작성자 상세→수정→저장·취소, 타 운영진 및 일반 회원 PUT 차단, 삭제·타 모임 공지 404, 공백·길이 오류, 작성자·작성일·고정 보존 확인.
- TODO: 수정자 기록·수정 이력·첨부파일·알림은 기존 연계가 없어 추가하지 않았다.
# 계모임 공지 삭제 — 2026-11-04

<!-- AI작성: 수정 완료 efcf44f를 기준으로 구현한 작성자 논리 삭제 계약과 검증 결과다. -->
- `DELETE /api/saving-groups/{groupId}/notices/{noticeId}`, `/saving-groups` 별칭 유지. 요청 본문 없음.
- 첫 경로 ID는 공통 groups.group_id. 인증은 JWT 필터의 memberId만 사용하며 성공은 기존 ApiResponse<Void>다.
- 대상 모임의 APPROVED OWNER·MANAGER인 작성자 본인만 삭제한다. 다른 운영진·일반 회원·비회원·대기·탈퇴·추방 회원은 거절한다. 모임 상태는 모집중(01)·진행중(02)만 허용한다.
- 모임→회원 관계→미삭제 소속 공지 순으로 잠근 뒤 작성자를 비교한다. 저장 SQL에서도 group_id·notice_id·writer_id·deleted_at IS NULL을 확인한다.
- group_notice.deleted_at에 CURRENT_TIMESTAMP를 저장한다. 실제 DB DATETIME·NULL 허용·기본값 NULL 준비는 사용자가 확인했다. AI는 스키마·마이그레이션·실제 DB를 변경하지 않았다.
- 없는 공지·다른 모임 공지·이미 삭제된 공지는 동일한 404 SAVING_GROUP_NOTICE_NOT_FOUND로 처리한다. 삭제 행 수가 1이 아니면 500 SAVING_GROUP_NOTICE_DELETE_FAILED.
- 나머지 공통 오류: 400 BAD_REQUEST, 401 UNAUTHORIZED, 404 NOT_FOUND(모임), 403 SAVING_GROUP_NOTICE_DELETE_FORBIDDEN, 409 SAVING_GROUP_ACTIVITY_RESTRICTED.
- 삭제 공지는 최근·일반 목록·전체 개수·고정 영역·상세·수정·고정 변경·고정 개수 산정에서 모두 제외한다. 고정 공지 삭제 후 새 공지를 고정할 자리가 생긴다.
- 기존 상세 화면에서 작성자 조건으로 삭제 버튼을 노출하고 ConfirmModal 확인 후 요청한다. 취소는 미요청, 진행 중 중복 제출 차단, 실패 시 기존 본문 유지 및 오류 안내. 권한·상태 오류는 변경 버튼을 숨기고 안내한다.
- 성공 후 기존 공지 페이지·정렬로 복귀해 재조회한다. 마지막 페이지가 없어지면 기존 범위 보정 로직을 사용한다. 삭제 완료 안내와 후속 목록 조회 실패를 분리하며 기존 빈 상태를 재사용한다.
- 변경 파일: SavingGroupMapper.java/XML, SavingGroupService.java/Impl, SavingGroupController, DeletionService/Controller/MapperTest, savingGroups.js/test, SavingGroupNoticeDeleteButton, SavingGroupNoticeDetailPage, SavingGroupDetailPage, 본 문서·AI 사용 기록.
- 검증: 전체 공지 관련 백엔드 295건·프론트 API 49건 통과. 수정→삭제→재수정·재삭제 차단은 Service 대역 테스트, 삭제·조회 조건은 실제 MyBatis XML 바인딩 테스트다. 변경 파일 lint·프론트 빌드 및 수정·삭제 컴포넌트 초기 SSR 렌더링 통과.
- 실제 MySQL·Postman·브라우저 클릭·JWT 필터 통합 검증은 미실행. 기존 다른 도메인 테스트 import 오류 때문에 임시 Gradle init script로 공지 테스트만 분리했다. 저장소 설정·의존성 변경 없음. 기존 번들 크기 경고 존재.
- 수동 검증: 본인 공지 수정→삭제→목록 제외, 고정 공지 삭제 후 고정 수 복원, 타 작성자 403, 재삭제·재수정 404, 삭제 후 DB deleted_at 저장, 확인 취소·통신 실패·삭제 완료 후 목록 재조회 실패 안내를 확인한다.
- TODO: 첨부파일·댓글·알림·삭제 이력은 기존 삭제 연계가 없어 추가하지 않았다.

<!-- AI작성: 2026-11-05 사용자 확정 정책에 따른 계원 목록·역할 관리 계약 및 검증 기록. -->
## 계주 전용 계원 목록 및 역할 관리 (2026-11-05)

### 공통 정책

- 두 API의 첫 경로 ID는 `saving_group.saving_group_id`다. 상세 화면 URL의 공통 `group_id`와 다르므로 프론트는 상세 응답의 `savingGroupId`를 전달한다.
- 기존 `/saving-groups` 별칭도 동일하게 지원한다. 처리자는 JWT 필터가 설정한 request attribute `memberId`만 사용한다.
- 해당 모임의 `groups.member_id`가 인증 회원과 일치하고, 그 회원 관계가 `OWNER + APPROVED`여야 한다. MANAGER·MEMBER·비회원·다른 모임 OWNER는 조회·변경할 수 없다.
- 모임이 미삭제이고 모집중(`01`) 또는 진행중(`02`)일 때만 허용한다. 인증·존재·상태·소속·권한·역할 검증은 Service가 수행한다.
- 모임→OWNER 관계→대상 관계 순서로 잠근다. 같은 모임 역할 변경은 모임 잠금으로 직렬화하며 회계 제한은 잠금 현재 읽기로 확인한다.
- DB 스키마·마이그레이션·의존성·보안 설정 변경 없음. OWNER 이전·해제는 지원하지 않는다. 알림을 발송하지 않으며 새 변경 이력·감사 필드는 추가하지 않는다.

### 계원 목록

`GET /api/saving-groups/{savingGroupId}/members?page=1`

| 입력 | 규칙 |
| --- | --- |
| savingGroupId | 양수 Long, 계모임 전용 ID |
| page | int, 기본 1, 1 이상 |

- `APPROVED`이면서 `MEMBER / MANAGER`인 관계만 포함하고 OWNER·삭제 회원·PENDING·REJECTED·LEFT·EXPELLED는 제외한다.
- `joined_at DESC, crew_member_id DESC` 정렬이다. 한 페이지 10명, 전체 조회 대상은 최대 20명으로 제한한다.
- meta.total도 최대 20, meta.totalPages는 최대 2다. 3페이지 이상 및 빈 결과는 정상 빈 배열로 반환하고 offset을 계산하지 않는다.
- 정원 20명에 OWNER가 포함되므로 정상 데이터의 관리 대상은 최대 19명이다. 기존 조회의 방어적 20명 제한은 유지한다. 정원·가입 정책은 이 API에서 변경하지 않는다.
- 이름·원문 이메일 검색은 아래 검색 확장 계약을 따른다. 역할·상태·크기·정렬 쿼리는 입력에 포함하지 않는다.

HTTP 200, 기존 `ApiResponse`의 data 배열·meta를 사용한다.

```json
{
  "success": true,
  "data": [{
    "memberId": "50",
    "name": "테스트계원",
    "email": "t***@example.com",
    "role": "MEMBER",
    "joinedAt": "2026-11-05T10:00:00"
  }],
  "meta": {"page": 1, "size": 10, "total": 1, "totalPages": 1}
}
```

- memberId는 실제 `members.member_id`이며 BIGINT 정밀도 보호를 위해 문자열로 반환한다.
- 이름은 사용자의 명시적 정책에 따라 마스킹하지 않는다. 이메일은 SQL에서 로컬 부분 첫 글자 외 마스킹한다. null은 null, 유효한 로컬 부분이 없는 값은 `***`다. 프론트에서 원문을 복원하지 않는다.
- 가입일은 `group_member.joined_at`이며 서비스 회원가입일로 대체하지 않는다. null이면 화면에 날짜 확인 필요를 표시한다.
- 닉네임·전화번호·회원 상태·계좌 등 다른 개인정보는 반환하지 않는다. 회원 상태는 조회 조건으로만 사용한다.

### 역할 변경

`PATCH /api/saving-groups/{savingGroupId}/members/{memberId}/role`

- memberId는 양수 Long의 대상 회원 ID다. 관계의 crew_member_id와 구분한다.
- 요청: `{"role":"MANAGER"}` 또는 `{"role":"MEMBER"}`. 누락·null·빈 값·OWNER·미정의 값·소문자·앞뒤 공백은 400이다.
- 대상은 이 모임의 현재 APPROVED 관계와 미삭제 회원이어야 한다. 없는 회원과 다른 모임 회원은 같은 404를 반환한다.
- OWNER 및 계주 본인 변경은 차단한다. 마지막 OWNER를 해제하거나 이전하는 경로는 없다.
- MANAGER는 모임당 APPROVED 관계 기준 최대 한 명이며 0명인 상태는 허용한다. 이 제한은 목록에 노출된 최대 20명이 아닌 모임 전체 관계에 적용한다.
- 다른 회계가 있으면 409다. 기존 회계를 MEMBER로 변경한 뒤 새 회계를 지정해야 하며 자동 교체·일괄 저장은 하지 않는다.
- 이미 같은 역할이면 UPDATE 없이 성공한다. 다만 다른 회계가 존재하는 비정상 데이터에서 MANAGER 재지정은 중복 제한으로 실패한다.
- UPDATE에서도 group_id·crew_member_id·APPROVED·변경 전 역할·미삭제 회원·허용 목표 역할을 다시 확인한다. 영향 행이 1건이 아니면 409 예외로 롤백한다.
- 성공은 HTTP 200, 기존 `ApiResponse.ok()` (`success: true`, data는 기존 공통 직렬화 규칙)다.
- 계정 account_status별 별도 역할 정책은 기존 역할 관리 구현이 없어 새로 만들지 않았다. 미삭제 및 모임 관계 APPROVED를 검증하며 추가 상태 정책은 별도 확정 대상이다.

### 오류

| HTTP | 코드 | 조건 |
| --- | --- | --- |
| 400 | BAD_REQUEST | 경로·페이지·요청 본문·역할 Validation 실패 |
| 401 | LOGIN_REQUIRED / UNAUTHORIZED | 기존 `/api` Security 인증 실패 / Service 인증 속성 없음 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 해당 모임의 승인된 OWNER 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 모임 없음·삭제 |
| 404 | SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND | 대상 소속 없음·미승인·탈퇴·추방·삭제 회원 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중·진행중 외 모임 |
| 409 | SAVING_GROUP_OWNER_ROLE_CHANGE_NOT_ALLOWED | 계주 본인 또는 OWNER 대상 변경 |
| 409 | SAVING_GROUP_MANAGER_ALREADY_EXISTS | 다른 승인된 MANAGER가 있음 |
| 409 | SAVING_GROUP_MEMBER_ROLE_INVALID | 기존 관계의 역할이 허용 코드가 아님 |
| 409 | SAVING_GROUP_MEMBER_ROLE_UPDATE_FAILED | 조건부 UPDATE 영향 행 불일치 |

### 프론트 화면

- 기존 계원 관리 메뉴·패널·목록을 재사용한다. APPROVED OWNER와 모임 상태 01/02일 때만 패널을 생성하고 모임 변경 시 key로 편집 상태를 초기화한다.
- Mock 목록을 가져오지 않는다. 현재 모임·페이지의 응답만 표시하고 이전 GET은 취소한다. 오류와 빈 목록을 구분하고 재조회 버튼을 제공한다.
- 이름 원문·마스킹 이메일·계원/회계·모임 가입일을 표시하며 페이지 번호는 최대 1·2다.
- 역할 수정 → 회원별 선택 → 해당 회원 저장 → 서버 재조회 순서다. 편집 종료는 미저장 변경을 폐기한다. 저장 중 선택·추가 저장·페이지 이동을 차단한다.
- 성공·실패·통신 오류 후 모두 서버 상태를 재조회한다. 저장 완료 안내와 후속 조회 오류를 각각 표시하고, 재조회 실패 중에는 이전 목록을 계속 편집하지 않는다.
- 회계 한 명 제한·교체 순서를 화면에 안내한다. 알림 생성은 하지 않으며 화면의 처리 결과 안내만 표시한다.
- 검색창은 아래 검색 확장에 따라 실제 목록 API에 연결한다. Mock 검색은 적용하지 않는다.

### 단계별 파일·커밋

Java 경로는 `back/src/main/java/com/nightstudy/moa/savingGroup/`, 테스트 경로는 `back/src/test/java/com/nightstudy/moa/savingGroup/` 기준이다.

| 단계 | 작업 파일 | 커밋 및 작업 내용 |
| --- | --- | --- |
| 1 | dto/SavingGroupMemberResponse.java, dto/SavingGroupMemberListResponse.java, mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, service/SavingGroupMemberMapperTest.java(테스트) | `4e2de3b` feat: 계모임 계원 목록 DTO와 Mapper 조회 구현 |
| 2 | service/SavingGroupService.java, service/SavingGroupServiceImpl.java, service/SavingGroupMemberListServiceTest.java(테스트) | `f2a507d` feat: 계모임 계원 목록 서비스 및 계주 권한 검증 추가 |
| 3 | controller/SavingGroupController.java, controller/SavingGroupMemberListControllerTest.java(테스트) | `e388473` feat: 계모임 계원 목록 조회 API Controller 추가 |
| 4 | dto/SavingGroupMemberRoleUpdateRequest.java, Mapper Java/XML, service/SavingGroupMemberMapperTest.java(테스트) | `cbbed63` feat: 계모임 계원 역할 변경 DTO와 Mapper 구현 |
| 5 | Service Java/Impl, service/SavingGroupMemberRoleServiceTest.java(테스트) | `204c02b` feat: 계모임 계원 역할 변경 서비스 및 회계 1명 제한 구현 |
| 6 | SavingGroupController.java, controller/SavingGroupMemberRoleControllerTest.java(테스트) | `13231e0` feat: 계모임 계원 역할 변경 API Controller 추가 |
| 7 | front/src/api/savingGroups.js, front/src/api/savingGroupMembers.test.js | `0006031` feat: 계모임 계원 목록 및 역할 변경 요청 API 추가 |
| 8 | front/src/components/savingGroup/의 SavingGroupDetail.jsx, SavingGroupMembersPanel.jsx, SavingGroupMemberList.jsx, SavingGroupMemberList.test.js | `91a8946` feat: 계모임 계원 관리 화면 및 역할 변경 API 연동 |
| 9 | service/SavingGroupMemberMySqlIntegrationTest.java(테스트), docs/saving-groups-api.md, docs/ai-usage-log.md | test: 계모임 계원 관리 통합 검증 및 API 문서화 |

기존 `996620e` DTO·Mapper, `2053c81` Service, `27cd546` Controller, `82155a2` 프론트 API, `bf0acdf` 화면, `ae4587e` 문서화 커밋의 작업 단위를 따랐다. 단계마다 검증 후 별도 커밋했으며 원격 push는 수행하지 않는다.

### 검증 결과와 재실행 방법

- Java 17 운영 코드 컴파일 통과. 관련 백엔드 409건 중 406건 통과·선택형 MySQL 3건 건너뜀. 신규 계원 관리 대역/바인딩 테스트는 91건이다.
- 최초 일반 Gradle test는 기존 다른 도메인 테스트의 누락 import 13건 때문에 compileTestJava에서 실패했다. 해당 파일·설정을 수정하지 않고 임시 init script로 `SavingGroupMember*Test`, `SavingGroupNotice*Test`, `SavingGroupJoinAvailabilityServiceTest`, `SavingGroupJoinRejection*Test` 소스만 컴파일·실행했다. 전체 저장소 테스트 통과를 의미하지 않는다.
- 프론트 기존 계모임 API 49건 + 신규 API 5건 + SSR 렌더링 1건 = 55건 통과. `node --test src/api/savingGroups.test.js src/api/savingGroupMembers.test.js src/components/savingGroup/SavingGroupMemberList.test.js` (front에서 실행).
- 변경 파일 oxlint 오류 없음. 기존 SavingGroupDetail의 미사용 스타일 경고 2건 유지. 프로덕션 빌드 통과, 기존 번들 크기 경고 유지. diff 공백 검사 통과.
- Service는 Mapper 대역, Controller는 JWT 필터 없는 standalone MockMvc, SQL은 MyBatis XML 바인딩, 화면은 SSR 렌더링이다. 실제 브라우저 클릭·Postman·JWT HTTP 통합은 미실행이다.
- `SavingGroupMemberMySqlIntegrationTest`는 실제 Mapper·트랜잭션 프록시로 20명 제한/마스킹/가입일, 동시 회계 지정, 외부 트랜잭션 롤백/OWNER 보호를 검증하도록 작성했다. 전용 DB가 없어 실행하지 않았으며 컴파일만 확인했다.
- 선택형 실행: `MOA_MEMBER_TEST_DB_ENABLED=true`, `MOA_MEMBER_TEST_DB_URL`, `MOA_MEMBER_TEST_DB_USER`, `MOA_MEMBER_TEST_DB_PASSWORD`를 로컬 환경에서 제공한다. URL은 MySQL 8 이상, 스키마는 `moa_members_test_`로 시작하는 빈 전용 DB여야 한다. URL에 인증 정보·자동 DB 생성 옵션을 포함하지 않는다. 테스트는 schema.sql의 네 테이블만 생성하고 자신이 만든 테이블만 정리한다. 실제 값은 코드·문서·커밋·로그에 남기지 않는다.

### Postman·브라우저 수동 확인

1. 테스트용 모임의 saving_group_id와 group_id를 다르게 준비한다. 승인된 OWNER로 목록을 요청하고 이름 원문·마스킹 이메일·모임 가입일을 확인한다.
2. 승인 계원 0명·1명·10명·11명·20명·21명 이상에서 data/meta와 최대 두 페이지를 확인한다. OWNER·삭제·미승인 관계는 목록에서 제외되어야 한다. 3페이지는 빈 배열이다.
3. MEMBER→MANAGER→MEMBER 변경, 기존 회계가 있는 상태의 추가 지정 409, 기존 회계 해제 후 새 회계 지정 성공을 확인한다.
4. 서로 다른 계원을 동시에 MANAGER로 지정해 최종 회계가 한 명인지 확인한다. 다른 모임의 회계는 제한에 영향을 주지 않아야 한다.
5. MANAGER·MEMBER·비회원·다른 모임 OWNER 요청은 403, 비로그인은 기존 인증 오류, OWNER 지정은 400, OWNER 대상 해제는 409인지 확인한다.
6. 없는 모임·회원·다른 모임 회원·PENDING·REJECTED·LEFT·EXPELLED·삭제 회원·잘못된 역할·활동 제한 모임의 실패 응답을 확인한다.
7. 화면에서 역할 저장 중 중복 클릭·페이지 이동 차단, 실패 후 재조회, 재조회 실패 시 편집 차단, 모바일 표시, 모임 전환 시 이전 결과 미표시를 확인한다.
8. 검색창의 이름·이메일 검색과 빈 검색어로 전체 조회 복귀를 확인하고, 역할 저장으로 알림이 생성되지 않는지 확인한다.

### 남은 TODO

- 검색 API·검색창 연결은 아래 검색 확장에서 구현했다. 검색 조건을 먼저 적용한 뒤 결과에 최대 20명 제한을 적용한다.
- OWNER 이전·해제와 역할 변경 이력은 이번 범위에서 지원하지 않는다. 알림은 확정 정책에 따라 구현하지 않는다.
- 정상 정원 20명 기준 관리 대상은 최대 19명이다. 비정상 초과 데이터에서도 전체 승인 계원을 검색해 찾을 수 있고, 회계 제한은 검색 결과와 관계없이 모임 전체에 적용한다.
- 실제 DB 동시성·브라우저 상호작용·인증 통합은 위 절차로 후속 확인해야 한다.

<!-- AI작성: 2026-11-05 사용자 승인에 따른 계원 관리 검색 확장 및 단계별 커밋 기록. -->
## 계원 관리 이름·이메일 검색 확장 (2026-11-05)

### 요청 및 검색 규칙

`GET /api/saving-groups/{savingGroupId}/members?keyword=검색어&page=1`

기존 `/saving-groups` 별칭, OWNER 전용 권한, APPROVED MEMBER/MANAGER 범위, 모집중·진행중 조건, data/meta 및 오류 형식을 유지한다.

| 입력 | 규칙 |
| --- | --- |
| keyword | 선택 문자열. 앞뒤 공백 정리 후 최대 20자. 생략·빈 문자열·공백만 입력하면 전체 조회 |
| page | 기본 1, 정수 1 이상. 빈 문자열·문자·0·음수는 400 |

- 해당 모임의 전체 승인 계원에서 `members.name` 또는 **마스킹 전 `members.email`** 부분 일치를 검색한다. 이후 기존 가입일·관계 ID 내림차순 정렬과 10명씩 최대 2페이지 제한을 적용한다.
- 정상 정원 20명에 계주가 포함되므로 OWNER 제외 시 최대 19명이다. 검색 전 상위 20명으로 자르는 방식은 사용하지 않으며, 방어적 조회 상한은 유지한다.
- 대소문자·문자 비교는 기존 MySQL 컬럼 collation을 따른다. 새 collation·DB 인덱스·마이그레이션을 추가하지 않는다.
- 기존 Service의 `escapeKeyword`를 재사용한다. `%`, `_`, `!`는 LIKE 와일드카드가 아닌 입력 문자로 검색한다. SQL은 `#{keywordPattern}`과 `ESCAPE '!'`를 사용하고 문자열 SQL 삽입은 하지 않는다.
- `(이름 조건 OR 이메일 조건)`을 괄호로 묶고, 목록과 전체 개수에 동일한 공통 SQL 조건을 적용한다. 검색어로 다른 모임·OWNER·미승인·탈퇴·추방·삭제 회원이 노출되지 않는다.
- 검색 결과 수를 meta.total에 반환하며 최대 20이다. 결과 없음은 HTTP 200, data 빈 배열, total/totalPages 0이다. 범위 초과 페이지도 정상 빈 배열이다.
- 응답 필드 변경 없음. 이름 원문·마스킹 이메일을 유지하고 원문 이메일은 응답이나 로그에 추가하지 않는다. 20자보다 긴 전체 이메일은 20자 이하 부분 문자열로 검색할 수 있다.
- 기존 검색 없는 Service 호출도 검색어 null로 같은 구현에 연결한다. 역할 변경 API·회계 한 명 제한·알림 정책은 변경하지 않는다.

### 화면 동작

- 기존 검색창과 버튼을 활성화한다. 입력 중인 keyword와 조회에 적용한 submittedKeyword를 분리한다.
- 검색 버튼 또는 Enter로만 조회한다. 입력 중에는 기존 결과를 유지하며 자동 검색하지 않는다.
- 검색 실행 시 1페이지로 이동하고 미저장 역할 편집값·편집 모드를 초기화한다. 같은 검색어를 다시 제출해도 재조회한다.
- 검색어를 비우고 검색하면 전체 계원 조회로 돌아간다. 화면에 20자 제한과 전체 조회 복귀 방법을 안내한다.
- 페이지 이동·다시 조회·역할 저장 후 재조회는 **적용된 검색어**를 유지한다. 새로 입력만 하고 제출하지 않은 값은 조회에 적용하지 않는다.
- 검색 조건을 포함한 조회 키로 결과를 구분하고 이전 GET은 취소한다. 검색을 바꾸면 이전 목록·편집값을 현재 결과처럼 표시하지 않는다.
- 역할 저장 중에는 검색 입력·검색 버튼·추가 제출을 차단한다. 목록 로딩 중 새 검색은 허용해 이전 요청을 취소할 수 있다.
- 검색 결과는 '검색 결과 N명', 검색 결과 없음은 '검색 조건에 맞는 계원이 없습니다.'로 표시한다. 전체 목록 0명은 기존 '승인된 계원이 없습니다.'로 구분한다. 통신·권한 오류는 빈 결과와 구분하고 다시 조회를 제공한다.

### 단계별 변경 파일·커밋

백엔드 Java 경로는 `back/src/main/java/com/nightstudy/moa/savingGroup/`, 테스트 경로는 `back/src/test/java/com/nightstudy/moa/savingGroup/` 기준이다.

| 단계 | 변경 파일·내용 | 커밋 |
| --- | --- | --- |
| 1 | dto/SavingGroupMemberSearchCondition.java, mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, ServiceImpl 기존 호출 인자 정합성 수정, MemberMapperTest/MemberListServiceTest/MemberSearchConditionTest | `c68f786` feat: 계모임 계원 검색 DTO와 Mapper 조건 추가 |
| 2 | service/SavingGroupService.java, service/SavingGroupServiceImpl.java, service/SavingGroupMemberListServiceTest.java | `ce07205` feat: 계모임 계원 검색 서비스 및 페이징 처리 추가 |
| 3 | controller/SavingGroupController.java, controller/SavingGroupMemberListControllerTest.java | `13897a3` feat: 계모임 계원 목록 API 검색 조건 연결 |
| 4 | front/src/api/savingGroups.js, front/src/api/savingGroupMembers.test.js | `6025b39` feat: 계모임 계원 조회 요청에 검색어 전달 추가 |
| 5 | front/src/components/savingGroup/SavingGroupMembersPanel.jsx, SavingGroupMemberList.jsx, SavingGroupMemberList.test.js | `5a5b41c` feat: 계모임 계원 관리 검색창 및 결과 화면 연동 |
| 6 | service/SavingGroupMemberMySqlIntegrationTest.java, docs/saving-groups-api.md, docs/ai-usage-log.md | test: 계모임 계원 검색 검증 및 API 문서화 |

### 검증 결과

- Java 17 컴파일·관련 백엔드 총 437건 중 432건 통과, 선택형 MySQL 5건 건너뜀. 검색 추가분은 실행 테스트 26건과 선택형 MySQL 2건이다.
- 이전 작업에서 확인한 타 도메인 테스트 누락 import 오류로 전체 test 소스는 컴파일되지 않으므로, 임시 Gradle init script로 `SavingGroupMember*Test`, `SavingGroupNotice*Test`, `SavingGroupJoinAvailabilityServiceTest`, `SavingGroupJoinRejection*Test`만 분리 실행했다. 프로젝트 빌드·의존성·설정 변경은 없다.
- 프론트 기존 API·신규 검색 API·SSR 렌더링 총 57건 통과. 명령: front에서 `node --test src/api/savingGroups.test.js src/api/savingGroupMembers.test.js src/components/savingGroup/SavingGroupMemberList.test.js`.
- 변경 프론트 파일 oxlint 오류·경고 없음. 프로덕션 빌드 및 diff 공백 검사 통과. 기존 번들 크기 경고는 유지한다.
- 실제 DB 검색은 전용 환경이 없어 미실행이다. 선택형 테스트에 원문 이메일 검색/마스킹 응답, 이름 부분 일치, 검색 후 제한·페이지, 공백 해제, 특수문자, 따옴표, 권한 범위의 실제 SQL 검증을 추가했고 컴파일만 확인했다. 기존 전용 DB 활성화·안전 조건을 유지한다.
- Service는 Mapper 대역, Controller는 standalone MockMvc, SQL은 MyBatis 바인딩, 프론트 API는 client 대역, 화면은 SSR 검증이다. 검색 버튼·Enter·페이지 이동·저장 후 재조회·빠른 검색 변경의 실제 브라우저 상호작용과 JWT HTTP 통합은 후속 수동 검증 대상이다.
- 정원보다 많은 기존 합성 DB fixture는 검색 전 제한을 잘못 적용하는 구현을 잡는 경계 테스트이며, 실제 모임 정원을 늘리는 변경이 아니다.

### Postman·브라우저 수동 확인

1. 승인 OWNER로 이름 일부·원문 이메일 일부를 keyword에 전달하고 해당 계원만 반환되는지, 응답 이메일은 마스킹되어 있는지 확인한다.
2. keyword 생략·빈 값·공백에서 전체 목록으로 복귀하고, 앞뒤 공백은 제거되는지 확인한다. 20자 허용·21자 400을 확인한다.
3. `%`, `_`, `!`, 따옴표가 포함된 합성 회원명으로 해당 문자 자체가 검색되는지 확인한다. 실제 개인정보를 테스트 데이터·로그에 넣지 않는다.
4. 검색 결과 0명·10명·11명·19명에서 개수·페이지를 확인하고, 검색 중 2페이지에 있다가 새 검색을 실행하면 1페이지로 돌아오는지 확인한다.
5. 같은 검색어로 OWNER·MANAGER·MEMBER·비회원·타 모임 OWNER 요청을 비교하고 기존 조회 권한이 유지되는지 확인한다. OWNER·미승인·삭제 관계는 검색 결과에서도 제외되어야 한다.
6. 검색 결과에서 역할을 저장한 뒤 검색 조건과 페이지가 유지되는지, 저장 중 검색이 차단되는지 확인한다. 검색 시 미저장 역할 변경은 폐기되어야 한다.
7. 빠르게 다른 검색을 제출해 이전 응답이 현재 결과를 덮어쓰지 않는지 확인한다. 검색 결과 없음·통신 오류·재조회 안내도 구분한다.
