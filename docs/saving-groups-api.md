<!-- AI작성: 2026-10-20 확정 설계에 따른 구현·검증 명세. -->
# 계모임 목록 조회 API
# 계모임 조회 API

<!-- AI작성: 공지 페이징·고정의 현재 계약을 과거 단계별 기록과 구분한다. -->
> 공지 목록·고정 기능은 문서 하단의 [2026-11-04 페이징·고정 최신 계약](#notice-pagination-and-pin)을 우선한다. 아래 기존 기록의 최신 4건·meta 없음·고정 제외는 당시 구현 범위다.

## 2026-11-10 계모임 경로 식별값

<!-- AI작성: 계모임 전용 경로와 공통 모임 신고의 식별값을 구분한다. -->
- 브라우저 상세·설정·공지 URL과 계모임 전용 API의 첫 경로 변수는 `saving_group.saving_group_id`다.
- 서비스는 이 ID로 계모임을 확인한 후 회원 소속·공지·일정·계좌 등 기존 내부 조회에 연결된 `groups.group_id`를 사용한다. 모임 신고는 공통 API의 `targetId`에 `groups.group_id`를 보낸다.
- 설정 조회 `GET /api/saving-groups/{savingGroupId}/settings`와 수정 `PATCH /api/saving-groups/{savingGroupId}`는 모두 계모임 고유 ID로 대상을 조회한다. 수정 시 해당 행을 잠그고, 조회한 공통 모임 ID로 승인 계주 소속과 기본 정보 저장 대상을 확인한다.
- 목록·설정·공지 화면 및 새 알림 링크는 계모임 ID로 상세에 진입한다. 예전에 공통 모임 ID로 저장된 주소는 현재 상세 경로의 식별값 계약과 다르다.
- 브라우저 또는 Postman에서는 계모임 A의 계모임 ID와 B의 공통 모임 ID가 같은 데이터로 확인한다. 각 탭은 A를 조회하고 A에 등록해야 한다. B 소속 공지·일정의 수정·삭제는 차단돼야 하며 모임 신고는 A의 공통 모임 ID를 사용한다.

## 2026-10-20 상세 조회 확장 — 최신 계약

<!-- AI작성: 구현 중 사용자 정정 사항을 반영한다. 아래 기존 목록 구현 기록보다 이 절을 우선한다. -->

### 사용자 확정·정정

- 가입 신청·거절·탈퇴 이력은 별도 테이블이 아닌 group_member.membership_status에서 관리한다.
- 기존 DB 코드 OWNER(계주), MANAGER(회계), MEMBER(계원)와 APPROVED(승인)를 유지한다.
  PENDING·REJECTED·LEFT는 인원수와 역할 조회에서 제외한다. schema.sql은 변경하지 않는다.
- 이번 작업은 공개 조회까지 구현하며 JWT 연결은 후속 작업으로 남긴다.

### 상세 요청과 응답

`GET /saving-groups/{id}` / 프록시 별칭 `GET /api/saving-groups/{id}`

- id는 saving_group.saving_group_id이며 Controller에서 Long + @Positive로 검증한다.
- 타입 오류·Long 범위 초과·0·음수는 400 BAD_REQUEST다.
- groups.deleted_at IS NULL, saving_group.status IN ('01','02')만 허용한다.
- 없는 모임·삭제·그 외 상태는 모두 404 NOT_FOUND, 메시지는 '조회할 수 있는 계모임이 없습니다.'다.
- 성공은 ApiResponse.ok(data), meta/error는 null이다. 예기치 않은 실패는 기존 500 INTERNAL_ERROR다.

```json
{
  "success": true,
  "data": {
    "savingGroupId": 101,
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
- 목록 응답에 groupId를 추가하고 기존 savingGroupId는 유지한다. 카드 URL은 savingGroupId를 사용한다.
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
PATCH /api/saving-groups/{savingGroupId}/members/{memberId}/expel
```

기존 Controller 별칭도 유지한다.

```http
PATCH /saving-groups/{savingGroupId}/members/{memberId}/expel
```

- groupId: 양수 Long, `groups.group_id`이면서 삭제되지 않은 계모임이어야 한다.
- memberId: 양수 Long, 추방 대상 `members.member_id`다.
- 요청 본문은 없다. 대상은 URL 경로로만 식별하며, 실행 계주·상태·역할은 클라이언트 입력으로 받지 않는다.
- 실행자는 서버 인증 처리에서 설정한 request attribute `memberId`(Long)만 사용한다. 기존 JWT 인증 필터가 설정한 속성을 사용하며, `/api` 인증 실패는 LOGIN_REQUIRED, Service 인증 속성 누락은 UNAUTHORIZED 정책을 따른다.
- Service는 `groups.member_id` 일치와 `group_member`의 `OWNER` + `APPROVED` 관계를 모두 확인한다.

### 처리와 상태 전이

1. 계모임을 `FOR UPDATE`로 조회하고 모집중(`01`)인지 확인한 뒤, 생성자 일치 및 요청자의 `APPROVED OWNER` 관계를 잠금 검증한다. 진행중(`02`)·종료 등 나머지 상태에서는 추방할 수 없다.
2. `groupId`와 대상 `memberId`의 `group_member` 행을 잠금 조회하고, `APPROVED` 상태인 경우에만 `crew_member_id`를 사용한다.
3. 대상 회원이 미삭제 상태로 존재하는지 확인한다. 계주 본인 및 다른 OWNER 대상은 409이며, MEMBER·MANAGER는 직접 추방할 수 있다. 미정의 역할은 차단한다.
4. 대상 행의 `crew_member_id`, `group_id`, `membership_status = 'APPROVED'`가 모두 일치할 때만 `membership_status`를 `EXPELLED`로 변경한다.

MANAGER는 MEMBER로 먼저 변경할 필요가 없다(B 방식). 추방된 MANAGER는 APPROVED 회계 조회에서 제외되어 새 회계 지정을 막지 않는다. 기존 가입 정책에 따라 EXPELLED 회원의 재가입 신청은 JOIN_REAPPLICATION_FORBIDDEN으로 차단한다.

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
| 409 | SAVING_GROUP_OWNER_EXPULSION_NOT_ALLOWED | 계주 본인 또는 다른 OWNER를 추방하려는 요청 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중(01) 외 상태 |
| 409 | SAVING_GROUP_MEMBER_ROLE_INVALID | MEMBER·MANAGER·OWNER 외 역할 |
| 409 | SAVING_GROUP_MEMBER_EXPULSION_FAILED | 조건부 상태 변경 영향 행 수가 1이 아님 |
| 500 | INTERNAL_ERROR | 예상하지 못한 DB·서버 오류 |

### 테스트 및 직접 검증

<!-- AI작성: 2026-11-05 진행 전 추방 구현 및 실제 검증 결과로 갱신한다. -->
신규 SavingGroupMemberExpulsionServiceTest 29건, SavingGroupMemberExpulsionControllerTest 14건을 검증한다. 기존 역할 관리 Service 34건·Controller 19건, 가입 가능 여부 10건을 포함한 총 106건이 통과했다.

기본 Gradle 실행은 Java 17 toolchain 부재로 실패했다. 설치된 Java 21과 `options.release = 17`을 사용하는 임시 init script로 운영 코드를 컴파일했다. 전체 테스트 컴파일은 기존 다른 영역의 AuthSessionService·MemberFeatureRestrictionService·KakaoApiClient import 오류 13건으로 실패하여, 임시 sourceSets 필터로 위 5개 테스트 클래스만 컴파일·실행했다. 저장소 build.gradle은 변경하지 않았다. 실제 Java 17 런타임·MySQL·JWT 필터 통합·동시 요청은 미검증이다.

Java 17 및 기존 테스트 컴파일 오류 해결 후 재실행:

```powershell
.\gradlew.bat test --tests '*SavingGroupMemberExpulsion*' --tests '*SavingGroupMemberRole*' --tests '*SavingGroupJoinAvailabilityServiceTest'
```

Postman 수동 검증: 테스트 DB에서 모집중 모임의 OWNER JWT로 본문 없이 PATCH를 호출하여 200 및 EXPELLED 저장을 확인한다. MANAGER 대상도 같은 결과여야 한다. 비OWNER는 403, 다른 모임·비승인 대상은 404, 진행중·종료 및 OWNER 대상은 409인지 확인한다. 재추방은 404, 재가입 신청은 기존 JOIN_REAPPLICATION_FORBIDDEN이어야 한다. 실제 금융 호출은 하지 않는다.

TODO: 추방 사유·추방일·추방자·별도 이력·순번·회비·공동자금·정산 후속 정책은 미확정이며 새 컬럼이나 처리를 추가하지 않았다. 알림은 기존 TODO를 유지한다.

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
- 기존 JWT 인증 필터가 설정하는 `memberId`를 두 경로에서 동일하게 사용한다.

### 처리와 상태 전이

1. `saving_group`과 삭제되지 않은 `groups` 관계를 `FOR UPDATE`로 조회한다.
2. 계모임 상태가 기존 시작 전 상태 코드 `01(모집중)`인지 확인한다. `00`, `02(진행중)`, `03`, `04`에서는 탈퇴할 수 없다.
3. 로그인 회원의 해당 `group_member` 관계를 잠금 조회하고 `membership_status = 'APPROVED'`인지 확인한다.
4. 승인 관계의 역할이 일반 계원(`MEMBER`) 또는 회계(`MANAGER`)인지 확인한다. 계주(`OWNER`)는 탈퇴할 수 없다.
5. 조회한 `crew_member_id`, 내부 `group_id`, `membership_status = 'APPROVED'`가 모두 일치할 때만 `membership_status`를 `LEFT`로 변경하고 `left_at`에 현재 시각을 기록한다.

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
| 403 | `SAVING_GROUP_LEAVE_FORBIDDEN` | 승인된 관계의 역할이 일반 계원 또는 회계가 아님 |
| 404 | `SAVING_GROUP_NOT_FOUND` | 계모임이 없거나 삭제됨 |
| 404 | `SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND` | 로그인 회원의 승인된 계원 관계가 없음 |
| 409 | `SAVING_GROUP_LEAVE_UNAVAILABLE` | 상태가 모집중(`01`)이 아님 |
| 409 | `SAVING_GROUP_MEMBER_LEAVE_FAILED` | 조건부 상태 변경 영향 행 수가 1이 아님 |
| 500 | `INTERNAL_ERROR` | 예기치 않은 DB·서버 오류 |

### 테스트 및 직접 검증

1. Postman에서 모집 중인 모임의 승인 일반 계원·회계 토큰으로 각각 요청해 HTTP 200과 테스트 DB의 `LEFT`, `left_at` 기록을 확인한다. 계주 403, 미인증 401, 미승인 또는 비회원 404, 진행 중인 모임 409도 확인한다.
2. 브라우저에서 모집 중인 모임의 승인 일반 계원·회계에게만 탈퇴 버튼이 표시되는지 확인한다. 취소 시 요청이 없어야 하며, 요청 중 중복 전송을 막고, 성공하면 계모임 목록으로 이동한다. 실패하면 같은 확인창에 오류를 표시한다.

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

계모임은 기존과 같이 status=01일 때만 신규 신청할 수 있다. 정원 미설정·정원 도달과 최대 5개 가입 제한을 동일하게 반영한다. 상세 URL과 가입 신청 URL은 savingGroupId를 사용한다.

수동 확인: GET /api/saving-groups/{savingGroupId}를 비로그인·가입·심사 대기·거절·추방·탈퇴·관계 없음으로 각각 조회한다. POST /api/saving-groups/{savingGroupId}/join-requests에서도 같은 제한이 유지되는지 확인한다.

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
- `savingGroupId`는 양수 BIGINT인 계모임 식별자다. 상세 화면 URL도 같은 식별자를 사용한다.
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

기존 계모임 상세 조회 `GET /api/saving-groups/{id}`의 `data.notices` 항목에 다음 두 필드를 추가했다. 상세 URL과 공지 작성 URL은 savingGroupId를 사용한다. 초기의 공지 ID·제목 전용 응답을 확장한 것으로 기존 필드는 유지한다.

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

- `GET /api/saving-groups/{savingGroupId}/notices/{noticeId}`
- 기존 Controller 별칭 `GET /saving-groups/{savingGroupId}/notices/{noticeId}`도 지원한다.
- 두 경로 변수는 양수 Long이며 공지 API의 첫 ID는 `saving_group.saving_group_id`다. 요청 본문은 없다.
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

- `getSavingGroupNoticeDetail(savingGroupId, noticeId, { signal })`는 기존 공통 client의 `/api` baseURL·인증을 재사용한다. 두 ID는 기존 양수 BIGINT 검증을 통과해야 하며 큰 문자열 ID를 Number로 변환하지 않는다.
- 서버 코드·메시지·HTTP 상태를 보존하고 `ERR_CANCELED`는 원본 그대로 전달한다. 통신·실패 봉투의 기본 오류는 `SAVING_GROUP_NOTICE_READ_FAILED`, 본문 객체가 없는 성공 응답은 `INVALID_RESPONSE`다.
- 화면 경로는 `/saving-groups/:id/notices/:noticeId`이며 id는 saving_group_id다. 승인된 OWNER·MANAGER·MEMBER이고 모임 상태가 01/02이면 기존 최근 공지 제목이 링크로 표시된다. 그 외에는 제목 텍스트를 유지한다. 서버가 실제 권한을 다시 검사한다.
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
2. 테스트용 계모임의 group_id와 saving_group_id가 다른 데이터로 GET 경로가 saving_group_id를 사용하는지 확인한다. 승인 OWNER·MANAGER·MEMBER와 모임 상태 01/02에서 200 및 6개 필드를 확인한다.
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

`/api/saving-groups/{id}` 별칭도 동일하다. id는 saving_group.saving_group_id다. 별도 공지 목록 GET 경로는 추가하지 않았다.

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

`PATCH /saving-groups/{savingGroupId}/notices/{noticeId}/pin`

`/api/saving-groups/{savingGroupId}/notices/{noticeId}/pin` 별칭도 지원한다. 두 ID는 양수 Long이다. JWT 인증 계층이 전달하는 요청 속성 memberId만 사용하며 쿼리·본문의 처리자 ID는 권한 근거로 사용하지 않는다.

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
- `PUT /api/saving-groups/{savingGroupId}/notices/{noticeId}` 및 `/saving-groups` 별칭.
- 첫 경로 ID는 `saving_group.saving_group_id`다.
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
- `DELETE /api/saving-groups/{savingGroupId}/notices/{noticeId}`, `/saving-groups` 별칭 유지. 요청 본문 없음.
- 첫 경로 ID는 saving_group.saving_group_id. 인증은 JWT 필터의 memberId만 사용하며 성공은 기존 ApiResponse<Void>다.
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

<!-- AI작성: 2026-11-04 사용자 승인에 따라 테스트 계좌 연동 2~10단계의 계약·검증을 마지막에 일괄 기록한다. -->
## 관리자 개설 승인과 테스트 가상계좌 발급·저장

### 범위와 선행 조건

- 작업 브랜치: `feature/issue-savinggroup-account`, 기준 커밋: `7fc94685f07543f736a13604f26631b4a52647c6`.
- 기존 관리자 개설 승인 API에 발급 → 암호화 저장 → 기존 승인을 연결한다. 폐기 예정 브랜치는 병합하거나 cherry-pick하지 않았다.
- 사용자에게 테스트 상점·키 관리·보안 정책·팀 승인이 확정됐음을 확인했다. 실제 은행·상점 예금주와 키 값은 아직 주입하지 않았다.
- 일반 테스트 가상계좌를 사용한다. 은행의 영구 공동계좌를 제공하는 기능이 아니다. 테스트 계좌의 번호는 내부 DB에 남아도 실제 입금 가능한 계좌를 의미하지 않는다.
- 테스트 환경 계좌는 `X` 접두사가 붙으며 직접 입금할 수 없다. [토스 공식 테스트 환경 안내](https://docs.tosspayments.com/guides/v2/get-started/environment)
- DB 적용은 담당자가 수행한다. 이 작업에서 실제 DB·`schema.sql`·빌드 설정·패키지 의존성은 변경하지 않았다. 새 이력 테이블은 개설 반려에서도 조회하므로 서비스 배포 전에 DB 변경 반영이 필요하다.
- DB 입금·출금·거래 생성·납부 처리·입금 웹훅·운영 API는 범위 밖이다. 내부 장부 잔액은 기존 거래 합산을 사용하고 거래가 없으면 0원이다.

### 관리자 승인 계약

```http
PATCH /api/admin/saving-group-applications/{approvalId}/approve
```

- 기존 관리자 JWT 인증·`ADMIN` 권한·양수 경로 검증을 유지한다. 요청 본문은 없다.
- `approvalId`는 심사 번호다. `groupId` 및 `savingGroupId`와 혼동하지 않는다.
- 성공은 기존 `ApiResponse<Void>`의 HTTP 200 / `success: true`이며 계좌번호를 포함하지 않는다.
- 신청 존재·PENDING 검사와 잠금을 먼저 수행한다. 이미 처리된 신청은 기존 409, 없는 신청은 기존 404를 유지한다.
- 신청 시 이미 생성된 계모임과 OWNER/PENDING 관계를 재사용한다. 승인 중 계모임을 다시 생성하지 않는다.
- 삭제 승인에는 발급을 연결하지 않는다. Controller에서 외부 통신하거나 기존 승인 API를 HTTP로 재호출하지 않는다.

처리 순서:

1. 관리자 인증·권한 → 신청 잠금·PENDING 검사.
2. 토스 테스트 설정과 암호화 설정을 검사한다. 부족하면 외부 호출과 발급 이력 생성 전에 중단한다.
3. 별도 `REQUIRES_NEW` 트랜잭션에서 기존 계좌·이력을 확인하고 `REQUESTED` 이력을 커밋한다.
4. 토스 테스트 발급 요청 후 응답을 검증한다.
5. 기존 승인 트랜잭션에서 암호화 계좌 저장과 이력 `COMPLETED` 변경을 수행한다.
6. 같은 트랜잭션에서 기존 신청 승인 → 계모임 상태 `01` → 개설자 계주 가입 승인을 수행하고 커밋한다.
7. 5~6단계가 롤백되면 계좌와 승인 변경을 함께 되돌린다. 롤백 완료 통지에서 별도 트랜잭션으로 실패 이력을 보존한다.

외부 발급은 DB 롤백으로 취소되지 않는다. 커밋 실패나 외부 결과 불확실 상태에서도 자동 재발급·자동 취소를 하지 않는다. 실패 이력 갱신까지 실패하면 `REQUESTED`가 남아 추가 발급을 계속 차단한다. 별도 이력 트랜잭션 때문에 승인 요청당 외부 요청 중에는 승인용 연결과 이력용 연결이 필요하다.

| 이력 상태 | 의미 | 승인 재요청 | 개설 반려 |
|---|---|---|---|
| `REQUESTED` | 처리 중 또는 중단 여부 확인 필요 | 차단 | 차단 |
| `COMPLETED` | 계좌 저장·기존 승인까지 커밋 완료 | 차단 | 기존 신청 상태 검사에서 차단 |
| `FAILED` | 외부 인증·권한 거절로 발급되지 않았음이 확실 | 자동 재시도 없음 | 기존 PENDING 정책에 따라 가능 |
| `UNKNOWN` | 타임아웃·응답 이상·외부 발급 후 DB 실패 등 | 차단 | 차단 |

이력 한 행은 한 신청·한 모임을 대표한다. 재시도 이력을 여러 행으로 쌓는 구조가 아니며, 관리자가 DB 상태를 강제로 바꾸거나 행을 지워 재발급하는 절차는 제공하지 않는다. 수동 확인 후 복구·재시도 정책은 후속 설계 대상이다.

### 외부 요청과 응답 검증

고정된 HTTPS 주소의 `POST /v1/virtual-accounts`에 서버 전용 테스트 키를 Basic 인증으로 전달한다. 연결 제한 5초, 읽기 제한 10초이며 클라이언트 자체 재시도는 없다. 운영 키 접두사는 설정 검사에서 거절한다. [토스 공식 발급 API](https://docs.tosspayments.com/reference)

| 요청값 | 적용 기준 |
|---|---|
| `amount` | 1000. 내부 장부 잔액으로 적립하지 않음 |
| `orderId` | `moa_` + 무작위 UUID, 발급 이력에 저장 |
| `orderName` | `모아 계모임 테스트 계좌 발급` |
| `customerName` | 실제 회원 이름 대신 `모아테스트` |
| `bank` | 상점에서 확인한 두 자리 은행 코드를 환경 변수로 지정 |
| `validHours` | 168시간 |
| `accountKey` | 일반형이므로 전송하지 않음 |

구매자명은 은행에 표시되는 예금주와 동일하다고 가정하지 않는다. `account_holder_name`에는 별도로 확인한 상점 예금주 설정을 저장한다. 연락처·회원 실명·이메일은 전송하지 않는다.

응답의 주문번호·통화 KRW·금액 1000·입금대기 상태·일반 계좌 유형·은행·미만료·입금 기한·테스트 번호 형식을 검사한다. 같은 은행의 세 자리 응답은 공식 표에 따라 두 자리로 정규화한다. KB국민은행은 `004 → 06`이므로 단순히 앞의 0을 제거하지 않는다. [토스 공식 기관 코드](https://docs.tosspayments.com/codes/org-codes)

| HTTP | 오류 코드 | 의미 |
|---|---|---|
| 503 | `TOSS_TEST_NOT_CONFIGURED` | 비활성 또는 테스트 키·은행·예금주 미설정 |
| 503 | `ACCOUNT_ENCRYPTION_NOT_CONFIGURED` | 키 식별자 또는 암호화 키 설정 이상 |
| 409 | `ACCOUNT_ISSUE_REVIEW_REQUIRED` | 기존 이력·계좌가 있어 확인 필요 |
| 502 | `ACCOUNT_ISSUE_REJECTED` | 외부 401·403 거절. 이력은 FAILED |
| 502 | `ACCOUNT_ISSUE_UNKNOWN` | 외부 오류·타임아웃·통신 결과 불확실 |
| 502 | `ACCOUNT_RESPONSE_INVALID` | 발급 응답 검증 실패. 이력은 UNKNOWN |
| 500 | `ACCOUNT_STORAGE_FAILED` | 저장 결과 또는 완료 갱신 실패 |
| 500 | `ACCOUNT_DATA_INVALID` | 암호문·복호화·계좌 데이터 이상 |

외부 오류 본문·계좌 원문·인증 헤더를 예외에 넣거나 로그로 출력하지 않는다. 발급 실패 응답과 DB 이력의 안전한 내부 코드는 구분하며 이력에는 외부 원문을 저장하지 않는다.

### 암호화 및 환경 변수

서버의 `@ConfigurationProperties`로 직접 바인딩하므로 별도 YAML 수정은 필요하지 않다. 아래 이름은 `VirtualAccountEnvironmentBindingTest`에서 검증했다. 하이픈이 제거된 `VIRTUALACCOUNT`, `SECRETKEY`, `BANKCODE`, `ACCOUNTHOLDERNAME`, `KEYID` 표기를 사용한다.

| 환경 변수 | 값·기본값 |
|---|---|
| `MOA_TOSS_VIRTUALACCOUNT_ENABLED` | 기본 false. DB·상점 설정 확인 후 true |
| `MOA_TOSS_VIRTUALACCOUNT_SECRETKEY` | 테스트 상점의 서버 시크릿. 허용 접두사 `test_sk_`. 실제 값은 외부 보안 설정에만 저장 |
| `MOA_TOSS_VIRTUALACCOUNT_BANKCODE` | 상점에서 발급 가능한 두 자리 은행 코드. 아직 확정하지 않음 |
| `MOA_TOSS_VIRTUALACCOUNT_ACCOUNTHOLDERNAME` | 상점에서 확인한 예금주 표시 기준. 아직 확정하지 않음 |
| `MOA_ACCOUNT_ENCRYPTION_KEYID` | 영문·숫자·`_`·`-`, 1~24자의 키 식별자 |
| `MOA_ACCOUNT_ENCRYPTION_KEY` | 암호학적 난수 32바이트를 Base64로 인코딩한 AES-256 키 |

키 값은 채팅·문서·테스트·커밋에 넣지 않는다. 서버 재시작 후에도 같은 키를 안전하게 보관해야 한다. 현재 코드는 단일 활성 키만 지원한다. 키 식별자가 포함돼 있어도 과거 키 조회·자동 키 교체 기능이 구현된 것은 아니므로 기존 계좌가 있는 상태에서 키를 임의 교체하지 않는다.

계좌번호는 매번 새 12바이트 IV와 128비트 GCM 인증 태그를 사용하며 `moa-account:{groupId}`를 인증 데이터에 연결한다. 저장 형식은 `v1:<keyId>:<IV Base64>:<암호문과 태그 Base64>`이다. 키 자체는 DB에 저장하지 않는다. 다른 모임으로 암호문을 복사하거나 암호문을 변조하면 복호화가 실패한다.

Mock 발급 구현은 테스트에서 명시적으로 생성할 때만 사용한다. Spring 빈이 아니며 설정 누락·외부 실패를 Mock 성공으로 대체하지 않는다.

### DB 담당자 적용 대상

실제 DDL 실행과 기존 데이터 전환은 DB 담당자 작업이다. 아래는 실행하지 않은 전달용 변경안이며 `schema.sql`에 자동 반영하지 않았다.

- 기존 `group_account.account_number VARCHAR(255) NOT NULL` 유지. 기존 평문이 있다면 별도 전환 계획 필요.
- `group_account.group_id`에 `uq_group_account_group` UNIQUE 추가. 중복 계좌와 다른 모임 도메인의 사용 정책을 먼저 확인한다.
- `saving_group_account_issue` 신규 생성. 이력 테이블에는 계좌번호를 중복 저장하지 않는다.
- 기존 `group_transaction` 구조와 거래 합산 SQL 유지. 잔액 컬럼·가상 입금 거래 추가 없음.

| 신규 테이블 컬럼 | 타입 | NULL | 기본값·제약 |
|---|---|---|---|
| `issue_id` | BIGINT | 불가 | AUTO_INCREMENT, PK |
| `approval_id` | BIGINT | 불가 | 기본값 없음, UNIQUE |
| `group_id` | BIGINT | 불가 | 기본값 없음, UNIQUE |
| `order_id` | VARCHAR(64) | 불가 | 기본값 없음, ASCII / ascii_bin, UNIQUE |
| `status` | ENUM(REQUESTED, COMPLETED, FAILED, UNKNOWN) | 불가 | 기본값 없음 |
| `account_id` | BIGINT | 허용 | NULL, UNIQUE |
| `error_code` | VARCHAR(100) | 허용 | NULL |
| `created_at` | DATETIME | 불가 | CURRENT_TIMESTAMP |
| `updated_at` | DATETIME | 불가 | CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP |

이력 테이블의 외래키는 현재 설계에서 추가하지 않는다. 잠긴 신청을 별도 이력 트랜잭션이 외래키로 참조하면 잠금 대기가 생길 수 있기 때문이다. 소속 일치는 Service에서 검증하며 이력의 UNIQUE 제약으로 중복을 차단한다. CASCADE 삭제도 추가하지 않는다. 실제 잠금과 담당자의 최종 스키마는 별도 확인이 필요하다.

```sql
-- AI작성: DB 담당자 검토용이며 이 작업에서는 실행하지 않았다.
SELECT group_id, COUNT(*) AS account_count
FROM group_account GROUP BY group_id HAVING COUNT(*) > 1;

-- 중복이 없고 동일 제약이 없는 경우에만 담당자가 적용한다.
ALTER TABLE group_account
    ADD CONSTRAINT uq_group_account_group UNIQUE (group_id);

CREATE TABLE saving_group_account_issue (
    issue_id BIGINT NOT NULL AUTO_INCREMENT,
    approval_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    order_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status ENUM('REQUESTED','COMPLETED','FAILED','UNKNOWN') NOT NULL,
    account_id BIGINT NULL DEFAULT NULL,
    error_code VARCHAR(100) NULL DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_id),
    UNIQUE KEY uq_account_issue_approval (approval_id),
    UNIQUE KEY uq_account_issue_group (group_id),
    UNIQUE KEY uq_account_issue_order (order_id),
    UNIQUE KEY uq_account_issue_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 승인 모임원 계좌 조회와 React 화면

```http
GET /api/saving-groups/{savingGroupId}/account
```

- `savingGroupId`는 계모임 고유 번호다. 기존 Controller 규칙에 따라 `/saving-groups/{savingGroupId}/account` 별칭도 제공한다.
- 로그인한 해당 모임의 APPROVED OWNER·MANAGER·MEMBER만 조회한다. JWT의 회원 번호만 사용하고 요청의 memberId는 신뢰하지 않는다.
- 기존 상세 조회가 허용하는 상태 01·02 및 미삭제 모임을 따른다. 미조회 모임은 404, 비로그인은 401, 승인 모임원이 아니면 `403 SAVING_GROUP_ACCOUNT_FORBIDDEN`이다.
- 성공 data: `issued`, `bankCode`, `maskedAccountNumber`, `balance`, `testAccount`.
- 미발급은 `issued: false`, 계좌 필드 null, 잔액 0으로 반환한다. 오류를 미발급 성공으로 바꾸지 않는다.
- 원문 계좌·예금주·발급 주문번호는 응답하지 않는다. 계좌번호는 끝 네 자리 외 마스킹하고 금액은 원 단위로 표시한다.
- React는 상세의 승인 회원 영역에서 독립된 계좌 카드를 조회한다. 로딩·오류·재조회·미발급·정상을 구분하고 모임·계정 변경 시 이전 응답을 숨긴다.
- 테스트 계좌이며 실제 입금할 수 없다는 안내와 서비스 내부 장부 잔액임을 표시한다. 기존 Mock 대시보드의 금액과 합치지 않는다.
- 관리자 승인 요청은 본문 없이 기존 함수를 사용한다. 해당 요청만 대기 시간을 30초로 지정하고 연속 클릭을 ref로 차단한다. 불확실한 응답을 자동 재전송하지 않는다.

### 검증 결과와 한계

2026-11-04 최종 검증:

- Java 17 설치 경로를 Gradle 실행 인자로 지정했다. 프로젝트 빌드 설정은 변경하지 않았다. 앞선 Java 21 보조 검증 이후 Java 17로 재검증했다.
- 계모임 테스트 315개 중 304개 통과, 실제 MySQL이 필요한 11개 건너뜀. 별도로 기존 삭제 신청 실패 메서드 2개를 명시적으로 제외했다. 전체 저장소 테스트 통과를 의미하지 않는다.
- 제외한 `SavingGroupDeletionApplicationServiceTest.ownerCanRequestDeletion`, `duplicatePendingRequestIsRejected`는 기준 커밋 `7fc9468`을 임시 폴더에 내보낸 테스트에서도 동일하게 실패했다. 기존 테스트가 APPROVED OWNER 관계 조회를 준비하지 않아 권한 단계에서 실패하며 해당 테스트·삭제 로직은 수정하지 않았다.
- 기존 다른 도메인의 오래된 import 때문에 전체 테스트 소스 컴파일이 막혀 임시 init script로 test 소스를 savingGroup 범위로 제한했다.
- 실제 보안 필터에서 비로그인 차단·일반 회원의 관리자 승인 차단·관리자 식별자 전달·비모임원 조회 차단을 확인했다. JWT 파싱·세션 조회는 대역이며 실제 로그인 E2E는 아니다.
- 실제 Spring 트랜잭션 프록시와 JDBC 연결 대역으로 이력의 별도 커밋, 계좌 저장 후 승인 실패 시 rollback 호출, rollback 이후 별도 실패 기록, 저장 실패·기록 실패 뒤 재발급 차단을 검증했다. 이는 실제 DB의 SQL·잠금·영속성 검증은 아니다.
- 선택형 MySQL 테스트 4개를 작성했지만 실행하지 않았다. 이 테스트도 토스와 신청 승인 Mapper는 대역이며 계좌·이력 Mapper의 실제 저장·롤백·예약 UNIQUE 경쟁만 검증한다. 신청·계주 변경 SQL과 실제 승인 잠금 경합은 추가 수동 검증 대상이다.
- 환경 변수 6개의 실제 바인딩, 토스 응답의 주문·은행·금액·통화·유형·만료·테스트 번호·기한 이상, 타임아웃과 오류 원문 비노출을 확인했다.
- 프론트 API·카드 SSR 테스트 51개 통과. 실제 컴포넌트의 정상·미발급·오류·로딩 출력을 확인했다. 실제 브라우저·모바일 레이아웃·버튼 클릭·계정 전환 E2E 검증은 아니다.
- `npm run build` 통과. 변경 파일 lint 오류 없음. 기존 번들 크기 경고와 ImagePlaceholder·Schedule 미사용 경고 2개 유지.
- 이전 브라우저 자동화가 Windows URL 확인 문제로 중단되어 이번에는 재시도하지 않았다. 실제 DB 변경·토스 호출·기준 브랜치 병합·원격 push는 하지 않았다.

검증 재현 시 다른 도메인 테스트를 수정하지 않고 다음 임시 설정을 사용한다. 기존 실패 2개가 제외된다는 사실을 결과 보고에 반드시 남긴다.

```powershell
# AI작성: back 폴더에서 실행. 실제 DB 테스트는 비활성화한다.
$accountVerifyInit = Join-Path $env:TEMP 'moa-account-final-scope.gradle'
@'
allprojects {
    afterEvaluate {
        if (plugins.hasPlugin('java')) {
            sourceSets.test.java.setIncludes(['**/savingGroup/**'])
            tasks.named('test') {
                filter {
                    excludeTestsMatching '*.SavingGroupDeletionApplicationServiceTest.ownerCanRequestDeletion'
                    excludeTestsMatching '*.SavingGroupDeletionApplicationServiceTest.duplicatePendingRequestIsRejected'
                }
            }
        }
    }
}
'@ | Set-Content -LiteralPath $accountVerifyInit -Encoding utf8
$env:MOA_TEST_DB_ENABLED = 'false'
.\gradlew.bat '-Dorg.gradle.java.installations.paths=C:/dev/jdk-17.0.12_windows-x64_bin/jdk-17.0.12' --init-script $accountVerifyInit test --tests 'com.nightstudy.moa.savingGroup.*'
```

다른 PC에서는 Java 17 경로를 수정한다. 프론트에서는 다음을 실행한다.

```powershell
# AI작성: front 폴더에서 실행. 실제 API를 호출하지 않는다.
node --test src/api/savingGroups.test.js src/api/savingGroupAccount.test.js src/components/savingGroup/SavingGroupAccountCard.test.js
npm run build
```

선택형 MySQL 테스트는 별도 명시적 요청 후 `MOA_TEST_DB_ENABLED=true`, `MOA_TEST_DB_URL`, `MOA_TEST_DB_USER`, `MOA_TEST_DB_PASSWORD`로 실행한다. 기존 방식과 같이 이름이 `moa_join_test_*`이고 테이블·뷰가 없는 전용 DB만 허용한다. 이 테스트는 테스트용 계좌·이력 테이블을 생성·정리하므로 공용·운영 DB를 지정하지 않는다. 이번 작업에서는 강제로 비활성화했다.

### DB 반영 후 사용자가 수행할 수동 확인

1. DB 담당자가 이력 테이블·UNIQUE 제약과 기존 계좌 암호문 저장 공간을 반영했는지 확인한다. 기존 평문·중복 계좌가 있으면 담당자와 먼저 처리한다.
2. 테스트 상점에서 허용된 은행과 예금주를 확인하고 위 환경 변수 6개를 서버 실행 환경에 설정한다. 활성화 전까지 기본 false를 유지한다. 키는 출력·공유하지 않고 서버를 재시작한다.
3. 테스트 회원으로 개설 신청 후 관리자 심사 상세에서 해당 신청 번호를 확인한다. 팀의 승인된 수동 테스트 절차에서 관리자 승인 버튼 또는 기존 PATCH를 한 번 호출한다.
4. 아래 상태 쿼리와 토스 개발자센터의 해당 상점 테스트 결제내역을 대조한다. 계좌 원문을 조회·복사하지 않는다. 정상은 신청 APPROVED, 모임 01, 계주 APPROVED, 이력 COMPLETED, 계좌 저장됨이다.
5. 승인 모임원으로 상세 화면의 은행·마스킹 계좌·0원·테스트 안내를 확인한다. 비회원·가입 대기·탈퇴 회원은 API에서 403, 비로그인은 401인지 확인한다.
6. 같은 신청 재승인으로 계좌가 늘어나지 않는지 확인한다. UNKNOWN 또는 오래 남은 REQUESTED는 승인 버튼을 반복하거나 DB 상태를 강제로 바꾸지 말고 해당 주문의 외부 처리 결과부터 확인한다.
7. 작은 화면·오류 재조회·로그아웃·모임과 계정 전환 시 이전 계좌가 숨겨지는지 브라우저에서 확인한다. 실제 입금·출금·개발자센터 입금 이벤트는 실행하지 않는다.

```sql
-- AI작성: 1을 확인할 심사 번호로 바꾼다. 계좌 원문·암호문·예금주를 출력하지 않는다.
SET @approval_id = 1;
SELECT ma.approval_id, ma.group_id, ma.status AS approval_status,
       sg.status AS group_status, gm.membership_status AS owner_status,
       i.status AS issue_status, i.error_code,
       CASE WHEN ga.account_id IS NULL THEN '없음' ELSE '저장됨' END AS account_saved,
       CASE WHEN ga.account_number LIKE 'v1:%' THEN '암호문 형식'
            WHEN ga.account_id IS NULL THEN '없음' ELSE '확인 필요' END AS storage_format
FROM meeting_approval ma
JOIN `groups` g ON g.group_id = ma.group_id
JOIN saving_group sg ON sg.group_id = ma.group_id
LEFT JOIN group_member gm ON gm.group_id = ma.group_id
    AND gm.member_id = g.member_id AND gm.member_role = 'OWNER'
LEFT JOIN saving_group_account_issue i ON i.approval_id = ma.approval_id
LEFT JOIN group_account ga ON ga.group_id = ma.group_id
WHERE ma.approval_id = @approval_id;
```

`암호문 형식`은 문자열 형식 확인일 뿐 복호화 성공을 증명하지 않는다. 회원 계좌 조회가 정상적으로 마스킹 결과를 반환하는지도 확인한다. 위 쿼리는 이 작업에서 실행하지 않았다.

### 단계별 변경과 커밋

| 단계 | 주요 작업 파일·내용 | 커밋 |
|---|---|---|
| 2 | 발급 요청·결과·이력·암호문 DTO, VirtualAccountStorageMapper/XML, Mapper 테스트 | `110017d` feat: 계모임 가상계좌 저장 구조와 발급 이력 Mapper 구현 |
| 3 | AccountEncryptionProperties, AccountNumberCipher, 발급·저장 Service/Impl, 별도 Mock, 테스트 | `0fc9401` feat: 계모임 계좌 암호화 저장과 Mock 발급 구현 |
| 4 | TossVirtualAccountProperties, TossVirtualAccountResponse, 테스트 클라이언트와 HTTP Mock 테스트 | `5fa4e08` feat: 토스 테스트 가상계좌 발급 클라이언트 구현 |
| 5 | SavingGroupCreationServiceImpl, 발급 승인·상태 Service/Impl, 승인 순서·실패 테스트 | `6497103` feat: 관리자 계모임 승인에 가상계좌 발급 저장 연결 |
| 6 | SavingGroupAccountResponse, 기존 SavingGroupService/Impl·Controller, 조회 테스트·기존 동시성 테스트 생성자 | `e37bc64` feat: 승인 모임원 전용 계모임 계좌 조회 API 구현 |
| 7 | savingGroups.js, savingGroupAccount.test.js: 조회·오류·승인 대기 시간 | `2ca4552` feat: 계모임 계좌 조회 요청 API와 승인 오류 처리 연결 |
| 8 | SavingGroupAccountCard.jsx, SavingGroupDetail.jsx, SavingGroupDetailPage.jsx, 관리자 상세 페이지 | `897f710` feat: 계모임 테스트 계좌와 내부 장부 잔액 화면 구현 |
| 9 | 아래 7개 파일: 보안·트랜잭션·환경 변수·MySQL 선택형·SSR 및 은행 코드 보완 | `cd7abe0` test: 계모임 가상계좌 승인 저장 조회 통합 검증 |
| 10 | 이 API 문서와 docs/ai-usage-log.md에 정책·검증·수동 절차 일괄 기록 | docs: 계모임 테스트 가상계좌 연동 및 회원 조회 정책 정리 |

9단계 변경 파일:

- `back/src/main/java/com/nightstudy/moa/savingGroup/service/TossVirtualAccountIssueServiceImpl.java`
- `back/src/test/java/com/nightstudy/moa/savingGroup/service/TossVirtualAccountIssueServiceImplTest.java`
- `back/src/test/java/com/nightstudy/moa/savingGroup/controller/SavingGroupAccountSecurityIntegrationTest.java`
- `back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupAccountApprovalMySqlIntegrationTest.java`
- `back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupAccountApprovalTransactionTest.java`
- `back/src/test/java/com/nightstudy/moa/savingGroup/service/VirtualAccountEnvironmentBindingTest.java`
- `front/src/components/savingGroup/SavingGroupAccountCard.test.js`

남은 작업은 담당자의 DB 적용, 상점 은행·예금주 확인과 환경 변수 주입, 실제 MySQL·브라우저·승인된 수동 테스트 발급 검증, 불확실 이력의 복구 정책, 향후 키 교체 정책이다. 입금·출금·거래 생성·웹훅은 별도 요청으로 진행한다.

<!-- AI작성: 2026-11-05 사용자 확정 정책에 따른 계원 목록·역할 관리 계약 및 검증 기록. -->
## 계주 전용 계원 목록 및 역할 관리 (2026-11-05)

### 공통 정책

- 두 API의 첫 경로 ID는 `saving_group.saving_group_id`다. 상세 화면 URL과 같은 식별자를 사용한다.
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

# 2026-11-05 계금 요청 API

<!-- AI작성: 실제 금융 호출 없이 내부 요청과 증빙만 저장한다. DB 담당자의 신규 컬럼 적용이 선행되어야 한다. -->

기본 경로 `/saving-groups`와 `/api/saving-groups`는 동일하다. `{savingGroupId}`는 `saving_group.saving_group_id`다. 인증 사용자 ID는 JWT 속성에서만 사용한다.

| 메서드·경로 | 입력 | 결과 |
|---|---|---|
| GET `/{savingGroupId}/payout-requests/options` | 없음 | 현재 사용자의 유형·현재 회차·확정 금액·증빙 필요 여부 |
| POST `/{savingGroupId}/payout-requests` | multipart/form-data | 요청 ID·유형·금액·PENDING |
| GET `/{savingGroupId}/payout-requests/expenses/{expenseId}/receipt` | 없음, OWNER 전용 | 증빙 파일 다운로드, no-store |

POST는 순번계의 경우 빈 multipart를 보내고, 친목계는 `amount`(1원 이상·13자리 이하 정수), `title`(공백 제외 필수·최대 100자), `receipt`(JPG/PNG·5MB 이하·각 변 4096픽셀 이하)를 보낸다. 사용자·상태·계좌·이체 정보는 입력하지 않는다. 순번계 금액·제목·증빙 입력은 거절한다.

```json
{
  "success": true,
  "data": {
    "groupType": "순번계",
    "roundNumber": 3,
    "amount": 502000,
    "receiptRequired": false,
    "requestId": 10,
    "status": "PENDING"
  },
  "meta": null,
  "error": null
}
```

options에서는 requestId·status가 null이다. 친목계 options에서는 roundNumber·amount도 null이며 receiptRequired는 true다. 생성 후 친목계 amount는 입력한 금액이다. requestId는 유형에 따라 payout_request_id 또는 expense_id다. 큰 ID 직렬화는 기존 Java Long 계약을 따른다.

대표 오류: 401 UNAUTHORIZED, 404 NOT_FOUND, 403 PAYOUT_FORBIDDEN / PAYOUT_NOT_YOUR_TURN / RECEIPT_FORBIDDEN, 409 PAYOUT_INACTIVE / PAYOUT_NOT_STARTED / PAYOUT_ROUND_MISSING / PAYOUT_SNAPSHOT_MISSING / PAYOUT_DUPLICATE / PAYOUT_INVALID_TYPE, 400 PAYOUT_INVALID_INPUT / INVALID_RECEIPT / 기존 Validation 오류. 인증 필터에서 차단되는 `/api` 미인증 응답은 기존 LOGIN_REQUIRED 정책을 따른다.

진행중 APPROVED 회원만 요청 가능하다. 친목계 OWNER·MANAGER, 순번계 현재 순번 본인만 허용한다. 미납·잔액·예산은 접수 차단 조건이 아니다. 승인·거절·지급 완료·요청 목록 API는 이번에 추가하지 않았다.

회차 금액 보존, 중복·증빙 접근 정책, 필요한 DB 컬럼과 검증 절차는 [계금 요청 설계·검증 기록](saving-group-payout-request-design.md)을 참고한다. 회차 생성과 추가 회비 부과 기능은 별도 구현이 필요하다.

## 계주 계금·지출 요청 목록 조회 (2026-11-05)

<!-- AI작성: 사용자 확정 정책과 기존 내부 테이블을 기준으로 한 조회 전용 API다. -->

### 경로·인증·권한

`GET /api/saving-groups/{savingGroupId}/payout-requests`

기존 Controller 별칭 `/saving-groups/{savingGroupId}/payout-requests`도 지원한다.
경로의 ID는 `saving_group.saving_group_id`이며 공통 `groups.group_id`가 아니다.
서버 인증 처리에서 설정한 `memberId`만 사용한다. 개설자 `groups.member_id` 일치와 해당 모임의
`OWNER + APPROVED` 관계를 모두 검증한다. MANAGER·MEMBER·비회원·가입 대기·거절·탈퇴·추방 회원과
다른 모임의 OWNER는 조회할 수 없다. 모집중(01)·진행중(02)에서만 조회한다.
`/api/**`의 기존 Spring Security 인증 정책을 유지하고, Service에서도 인증 회원 누락을 차단한다.

### 조회 조건

| 파라미터 | 기본값 | 규칙 |
|---|---|---|
| status | 미지정(전체 상태) | PENDING / APPROVED / REJECTED / COMPLETED |
| page | 1 | 1 이상인 int, 페이지 크기는 10 고정 |

친목계에는 COMPLETED가 없으므로 해당 필터는 400이다. 빈 문자열·미정의 상태와 잘못된 페이지는 400이다.
사용자 지정 정렬은 받지 않으며 요청일 내림차순·식별자 내림차순으로 안정 정렬한다.
요청 유형은 클라이언트에서 받지 않고 DB의 `saving_group_type`으로 결정한다.
순번계는 `payout_request` → `meeting_round.group_id`, 친목계는 `expenses.group_id`로 범위를 제한한다.
전체 개수와 목록에 동일한 상태·요청자 조건을 사용한다.

### 응답과 개인정보

기존 `{ success, data: [], meta: { page, size, total, totalPages }, error }` 구조를 사용한다.
결과 없음과 범위 초과 페이지도 200·빈 배열이며, 요청 페이지를 임의로 변경하지 않는다.
식별자는 BIGINT 정밀도 손실을 막기 위해 문자열로 제공하고, 금액은 BigDecimal을 사용한다.
화면에서 천 단위 콤마와 원 단위를 적용한다.

| 응답 필드 | 공통/순번계/친목계 | 의미 |
|---|---|---|
| requestId | 공통 | 해당 유형 테이블의 요청 식별자(문자열) |
| requesterName | 공통 | 요청자 실명(사용자 확정으로 마스킹하지 않음) |
| amount | 공통 | 요청 금액 |
| status | 공통 | 해당 요청 테이블의 기존 상태 |
| requestedAt | 공통 | 계금 requested_at / 지출 created_at |
| roundNumber | 순번계 | meeting_round.round_number(회차) |
| reviewerName | 순번계 | 현재 승인 관계를 유지하는 미삭제 처리자의 실명 |
| reviewedAt | 순번계 | 검토일이며 지급 완료일과 다름 |
| title | 친목계 | 기존 지출 제목, 별도 사용처로 해석하지 않음 |
| hasEvidence | 친목계 | 증빙 경로가 null·공백이 아닌지에 따른 첨부 여부 |

적용되지 않거나 null인 필드는 응답에서 생략한다. 순번계 미검토는 reviewedAt 부재로 판단한다.
계좌번호·거래 식별자·증빙 URL·원본·요청 사유·지급 완료일·회원 순번은 제공하지 않는다.
요청자는 서비스 미삭제 및 해당 모임 APPROVED 관계를 유지하는 경우에만 목록과 개수에 포함한다.
모임 탈퇴·추방·서비스 탈퇴 요청자의 원본 요청은 삭제하지 않고 조회에서 제외한다.
처리자만 탈퇴하면 요청과 검토일은 유지하고 이름을 생략한다. 화면은 `이름 비공개`로 표시한다.
이는 현재 관계 기준이다. 탈퇴 후 재가입하여 APPROVED로 바뀌면 기존 요청도 다시 포함된다.
과거 탈퇴 이력을 계속 숨기는 별도 이력 정책·테이블은 이번 범위에서 추가하지 않는다.

### 예외

| HTTP | 코드 | 조건 |
|---|---|---|
| 400 | BAD_REQUEST | 잘못된 ID·페이지·상태 또는 친목계 COMPLETED 필터 |
| 401 | LOGIN_REQUIRED / UNAUTHORIZED | 기존 보안 필터 또는 Service에서 미인증 판단 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 대상 모임 개설자·승인 OWNER 조건 불충족 |
| 404 | SAVING_GROUP_NOT_FOUND | 계모임 없음 또는 공통 모임 삭제 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중·진행중 외 상태 |
| 409 | SAVING_GROUP_TYPE_INVALID | 미정의/누락된 모임 유형(임의 추정 차단) |

### 프론트 연결·검증

기존 가입 신청 관리의 `계금 요청` 탭에서 조회한다. 목록·페이지는 서버 결과를 사용하고 기존 금융 Mock은 참조하지 않는다.
친목계에는 지출 제목·증빙 여부, 순번계에는 회차·검토일·검토자를 표시한다.
상태 변경은 1페이지로 돌아가며 이전 요청 취소·오래된 응답 차단·재시도를 지원한다.
승인·거절·지급 버튼은 제공하지 않는다. 기존 가입 신청 승인·거절 흐름은 유지한다.

Postman에서 테스트 계정의 기존 인증을 사용해 순번계·친목계 각각
`GET /api/saving-groups/{savingGroupId}/payout-requests?status=PENDING&page=1`을 호출한다.
계주 정상 조회, MANAGER/MEMBER 등 권한 거절, 01/02 외 상태, page=0, 친목계 COMPLETED,
빈 결과·2페이지·탈퇴 요청자 제외·탈퇴 처리자 이름 부재를 확인한다.
브라우저에서는 계주 로그인 → 계모임 상세 → 가입 신청 관리 → 계금 요청 탭에서
상태 필터·페이지·빈 결과·재시도·모바일 배치와 실명 표시를 확인한다.

선택 MySQL 테스트는 기존 `MOA_TEST_DB_ENABLED=true`와 `MOA_TEST_DB_URL`, `MOA_TEST_DB_USER`,
`MOA_TEST_DB_PASSWORD` 환경변수를 사용한다. 자격증명은 파일·명령 기록에 작성하지 않는다.
반드시 비어 있는 `moa_join_test_*` 전용 테스트 DB가 필요하며 테스트가 만든 테이블만 종료 시 정리한다.
일반 프로젝트 DB와 실제 금융 서비스는 사용하지 않는다.

### 남은 TODO

- 기간·요청자 검색·사용자 지정 정렬·증빙 원본 열람은 정책 확정 후 별도 작업.
- 탈퇴 후 재가입한 회원의 과거 요청을 영구 제외하려면 별도 이력 정책 검토 필요.
- 실제 MySQL 실행, 인증된 브라우저·Postman 상호작용은 별도 검증 필요.
- 승인·거절·수정·취소·실제 지급·이체와 요청 등록 화면의 금융 Mock 교체는 범위 밖.

### 실행 결과 (2026-11-05)

- 신규 백엔드 테스트: 42건 통과, 전용 MySQL 테스트 1건은 환경변수 미설정으로 생략.
- 계모임 회귀: 403건 중 393 통과·8 생략·2 실패(마지막 응답 테스트 추가 전 실행). 실패한 기존 SavingGroupDeletionApplicationServiceTest 2건은 승인 OWNER 관계 Mock 누락이다. 이번 변경에서는 해당 테스트·삭제 로직을 수정하지 않았다.
- 일반 Gradle test는 기존 다른 도메인의 import 13건으로 테스트 전체 컴파일이 막힌다. 추적하지 않는 임시 init script에서 sourceSets.test.java.setIncludes(['com/nightstudy/moa/savingGroup/**'])로 계모임만 분리했다. 본문 컴파일은 통과했다.
- 실행 명령(back 폴더): gradlew.bat -I build/payout-tests.init.gradle -Dorg.gradle.java.installations.paths=C:/dev/jdk-17.0.12_windows-x64_bin/jdk-17.0.12 test --tests '*SavingGroupPayoutRequest*' --offline. 임시 init script의 내용은 allprojects { plugins.withId('java') { sourceSets.test.java.setIncludes(['com/nightstudy/moa/savingGroup/**']) } } 이다. 전체 계모임 회귀는 --tests 옵션 없이 실행했다.
- 프론트(front 폴더): node --test src/api/*.test.js src/components/savingGroup/SavingGroupPayoutApprovalList.test.js — 111건 통과. npm run build 통과(번들 크기 경고). 변경 파일 oxlint 오류 없음, 기존 상세 화면 미사용 변수 2건 경고 유지.
- 실제 MySQL·인증 브라우저·Postman·JWT 필터 통합 검증은 실행하지 않았다. XML 바인딩·Mock Service·MVC·SSR 테스트 결과와 구분한다.


### 2026-11-05 병합 호환성 확인

<!-- AI작성: 계원 관리·계금 요청 등록·목록 조회를 모두 보존하여 충돌을 해결했다. -->

- 등록 POST와 options·영수증·목록 GET은 savingGroupId를 사용한다. HTTP 메서드와 하위 경로가 달라 별도의 핸들러로 유지한다.
- 기준 브랜치의 계금 산정·지출 검토 컬럼을 유지한다. 목록 응답은 사용자 확정 범위대로 유지하며 새 컬럼을 자동 노출하지 않는다.
- 충돌 해결 후 프론트 테스트 121건·빌드 통과. 백엔드 계모임 회귀 581건 중 566 통과·13 생략·기존 삭제 신청 테스트 2건 실패. 계원 관리 및 계금 등록·목록 테스트에는 실패 없음. 일반 전체 테스트 컴파일의 기존 import 오류 13건·실제 DB/브라우저 미검증은 AI 사용 기록에 별도 기재했다.

## 계금 요청 승인·거절 (2026-11-05)

<!-- AI작성: 사용자 확정 정책과 feature/apply-request-savinggroup-thepool 구현을 반영한다. 실제 지급 API가 아니다. -->

### 경로와 데이터 계약

- `PATCH /api/saving-groups/{savingGroupId}/payout-requests/{payoutRequestId}/approve`
- `PATCH /api/saving-groups/{savingGroupId}/payout-requests/{payoutRequestId}/reject`
- 기존 Controller의 `/saving-groups` 별칭도 지원한다. 인증은 기존 JWT Authorization Bearer 방식이다.
- savingGroupId는 `saving_group.saving_group_id`이며 공통 `groups.group_id`가 아니다. 서버가 내부 group_id로 변환한다.
- 모임 유형은 DB의 saving_group_type으로 판별한다. 순번계 요청 ID는 payout_request.payout_request_id, 친목계 요청 ID는 expenses.expense_id다. 클라이언트가 유형·처리자·처리 상태를 지정하지 않는다.
- 요청 생성 작업 a2c30ef 및 목록 조회 작업 5709a5f/e6e967d의 유형별 저장 테이블·목록 ID 계약을 확인했다. 다른 브랜치의 코드는 병합하지 않았다.

### 요청·응답

승인은 요청 본문이 없다. 거절은 JSON `{ "rejectReason": "증빙을 다시 확인해 주세요." }`를 보낸다. 사유는 공백만 있는 값 불가, 입력 문자열 최대 500자이며 저장 시 앞뒤 공백을 제거한다.
성공은 HTTP 200 및 기존 ApiResponse.ok() 봉투(`success=true`, `data=null`, `meta=null`, `error=null`)다. 실패는 기존 ApiResponse.fail의 `error.code`와 `error.message`를 따른다.

### 권한·상태·검증

| 항목 | 정책 |
|---|---|
| 처리자 | 로그인한 해당 모임의 APPROVED OWNER이며 groups.member_id와 일치해야 함 |
| 본인 심사 | OWNER 자신의 요청 승인·거절 허용 |
| 모임 | 삭제되지 않은 모집중(01)·진행중(02)만 허용 |
| 요청 | 해당 모임에 속하는 PENDING 한 건만 처리 |
| 순번계 요청자 | 현재 APPROVED OWNER·MANAGER·MEMBER |
| 친목계 요청자 | 현재 APPROVED OWNER·MANAGER |
| 탈퇴·추방·대기·회원 삭제 | 요청자 처리 불가. 처리자의 회원 자체 상태는 기존 JWT 세션 정책을 따름 |
| 순번계 승인 | 현재 요청자 sequence_number가 양수이고 요청 meeting_round.round_number와 동일 |
| 친목계 승인 | receipt_img_url이 NULL·빈 문자열이 아닌 요청만 허용. 영수증 내용은 계주가 판단 |
| 거절 | 순번 일치·영수증 첨부 조건 미적용. 나머지 인증·권한·모임·소속·상태·요청자 자격은 유지 |

별도의 현재 회차 자동 판정, 약정 금액 재계산, 금액 한도·예산·잔액·증빙 진위 판단 정책은 추가하지 않았다. receipt_sha256은 요청 생성에서의 중복 판별이며 심사에서 파일을 다시 업로드하거나 해시를 계산하지 않는다.

### 저장과 트랜잭션

Service의 승인·거절 진입점에 @Transactional을 적용한다. 공통 모임을 먼저 잠그고 계주 관계, 대상 요청·회차, 요청자 관계와 회원 행을 확인한다. 순번은 잠근 요청자 관계에서 확인되는 현재 값을 사용한다. 변경 SQL은 요청 ID·모임 소속·PENDING을 다시 제한하고 변경 행 수가 1이 아니면 409로 롤백한다.

- 승인: status=APPROVED, reviewer_id=인증 계주, reviewed_at=DB CURRENT_TIMESTAMP, reject_reason=NULL.
- 거절: status=REJECTED, reviewer_id=인증 계주, reviewed_at=DB CURRENT_TIMESTAMP, reject_reason=검증한 사유.
- 실제 이체·출금·금융 API·자동 지급 호출 없음. completed_at·transaction_id·순번·잔액·거래·승인 이력·알림은 변경하거나 생성하지 않는다.
- 친목계 UPDATE는 DB 담당자가 제안서의 expenses.reviewer_id BIGINT NULL, reviewed_at DATETIME NULL, reject_reason VARCHAR(500) NULL을 적용한 후 사용해야 한다. 운영 DB에서 적용 여부를 확인하지 않았다.
- schema.sql·마이그레이션은 변경하지 않았다. 회차 산정 5개 컬럼과 영수증 해시 컬럼은 이번 심사 SQL에서 참조하지 않는다. 회차 생성 및 요청 생성 작업과의 연결은 별도다.

### 오류

| HTTP | 코드 | 조건 |
|---|---|---|
| 400 | BAD_REQUEST | 잘못된 경로·거절 사유; 세부 입력 예외 봉투는 기존 공통 처리 적용 |
| 401 | 기존 JWT 오류 / UNAUTHORIZED | 토큰·세션 오류 또는 인증 회원 없음 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 해당 모임의 승인된 계주가 아님 |
| 404 | NOT_FOUND | 계모임 없음·삭제됨 |
| 404 | PAYOUT_REQUEST_NOT_FOUND | 요청 없음 또는 다른 모임에 속함 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중·진행중이 아님 |
| 409 | PAYOUT_GROUP_TYPE_UNSUPPORTED | 지원하지 않는 모임 유형 |
| 409 | PAYOUT_REQUEST_ALREADY_PROCESSED | 이미 처리됐거나 조건부 UPDATE 실패 |
| 409 | PAYOUT_REQUESTER_INELIGIBLE | 요청자의 현재 회원 상태·역할 부적합 또는 회원 삭제 |
| 409 | PAYOUT_SEQUENCE_MISMATCH | 승인 시 순번 미지정·불일치 |
| 409 | PAYOUT_EVIDENCE_REQUIRED | 승인 시 영수증 미첨부 |

### 검증 및 Postman 확인 방법

1. 테스트 DB에 승인된 OWNER와 유형별 적격 요청자, 모집중 또는 진행중 모임, PENDING 요청을 준비한다. 순번계는 sequence_number=round_number, 친목계는 접근 정책에 맞는 영수증을 준비한다.
2. OWNER의 유효한 JWT로 approve를 호출하고 200 및 대상 테이블의 status/reviewer_id/reviewed_at/reject_reason을 확인한다. transaction_id/completed_at과 잔액·거래·순번은 변경되지 않아야 한다.
3. 별도 대기 요청에 rejectReason JSON으로 reject를 호출한다. 사유 앞뒤 공백 제거와 처리 기록을 확인한다.
4. 같은 요청 재처리는 409, 다른 모임 요청은 404, 회계·일반회원·비회원·대기·탈퇴 계주의 처리는 403 또는 기존 인증 정책의 401인지 확인한다.
5. 순번 불일치·영수증 누락은 승인 409, 거절은 다른 자격 조건 충족 시 200인지 확인한다. 요청자 탈퇴·추방은 승인·거절 모두 409다.
6. 모든 확인은 내부 테스트 데이터만 사용하며 실제 자금 이동은 수행하지 않는다. Postman·브라우저 검증은 이번 작업에서 실행하지 않았다.

내부 테스트는 Mapper XML 바인딩, Service 정책, Controller 입력·응답, 실제 JWT 필터 연결(서명 파서·세션 대역), 실제 Spring/JDBC 트랜잭션 경계(commit/rollback)를 검증한다. 전체 SecurityFilterChain·서명 파서·실제 DB 잠금 경합 검증과 구분한다.
선택 실행 SavingGroupPayoutDecisionMySqlTest는 MOA_PAYOUT_TEST_DB_ENABLED=true 및 MOA_TEST_DB_URL/USER/PASSWORD가 필요하다. moa_payout_test_* 테스트 카탈로그에서만 실행하며 연결 전용 임시 테이블로 실제 MySQL의 저장·소속 제한·중복 UPDATE·롤백·커밋을 확인한다. 이 테스트의 임시 테이블은 운영 스키마 적용 여부를 증명하지 않는다.

로컬 JDK 17이 없어 저장소 설정을 바꾸지 않고 JDK 21 임시 Gradle init을 사용했다. 기존 타 도메인 테스트의 import 오류 때문에 테스트 소스 컴파일 범위는 savingGroup으로 제한했다. JDK 17 및 전체 저장소 테스트는 별도 확인이 필요하다. 실제 DB 정보가 없어 MySQL 테스트는 건너뛰었다.

재현용 임시 Gradle init 내용(저장소 설정에 반영하지 않음):

```groovy
allprojects {
    afterEvaluate {
        plugins.withId('java') {
            java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
            sourceSets.test.java.setIncludes(['**/savingGroup/**/*Test.java'])
        }
    }
}
```

back 폴더에서 `gradlew.bat -I <임시-init-절대경로> test --tests '*SavingGroupPayoutDecision*Test' --tests '*SavingGroupNotice*Test' --tests '*SavingGroupJoinRejection*Test' --console=plain`으로 실행한다. DB 테스트 환경변수는 자격 증명을 문서·명령 이력·코드에 직접 쓰지 않고 별도로 주입한다.

남은 TODO: DB 담당자의 친목계 심사 기록 컬럼 적용 확인, 실제 MySQL 동시 승인·거절 잠금 검증, JDK 17 재검증, 요청 생성·조회 브랜치 통합 시 충돌 및 계약 확인, 계주 전용 영수증 열람·프론트 승인 UI 연결. 이번 범위는 승인·거절 백엔드 API다.

최종 회귀 검증 결과: 계금 심사·기존 공지·가입 거절 관련 373건 통과, 실패 0건, MySQL 테스트 클래스 1개 건너뜀(2026-11-05, JDK 21 임시 설정). 신규 심사 테스트만은 68건 통과다. `git diff --check` 통과.

## 승인·거절 병합 확인 (2026-11-05)

<!-- AI작성: 위 개별 브랜치 작업 당시의 미검증 사항과 구분하여 현재 병합 검증 결과를 기록한다. -->

- feature/savinggroup(9dbd9ae)에 승인·거절 작업(86a3d6b)을 병합하면서 계원관리·요청 등록·목록·승인·거절 API와 SQL을 모두 보존했다. 등록·옵션·증빙·목록·승인·거절은 savingGroupId를 사용한다. 승인·거절의 requestId는 해당 유형의 목록 응답 request_id와 같다.
- 기준 schema.sql에 expenses.reviewer_id/reviewed_at/reject_reason이 반영되어 있음을 확인했다. 위 심사 브랜치 작성 당시의 컬럼 적용 대기 기록은 이 스키마 정의 확인으로 보완되지만, 운영·개발 DB에 실제 적용됐는지는 별도 확인이 필요하다.
- 이번에는 설치된 JDK 17 경로를 명시하여 본문·테스트를 컴파일했다. 계모임 회귀 649건 중 634 통과·13 생략·기존 삭제 신청 테스트 2건 실패. 신규 승인·거절 내부 테스트 68건은 전부 통과했다. 기존 두 실패는 승인 OWNER 관계 Mock 누락이며 이전 병합 기록과 동일하다.
- 실제 MySQL 저장·동시 잠금·브라우저·Postman 검증은 실행하지 않았다. 계주 전용 영수증 조회는 기존 등록 작업의 구현을 보존했으며 이번에 API를 새로 만들거나 프론트 승인 버튼을 연결하지 않았다. 커밋은 사용자 진행 예정이다.

## 계금 요청 심사 프론트 연결·검증 (2026-11-05)

<!-- AI작성: 기존 병합 시점의 프론트 미연결 기록 이후 수행한 프론트 작업과 검증 범위를 기록한다. -->
- 기존 계주 전용 신청 목록 > 계금 요청에서 대기 요청에 승인·거절 버튼을 제공한다. 실제 심사에는 기존 PATCH API와 savingGroupId·목록 요청 ID를 사용한다.
- 승인 확인창은 신청자·금액과 실제 지급 별도 안내를 표시한다. 거절은 필수 사유·공백 검증·최대 500자·글자 수를 제공한다. 거절 실패 시 입력 사유를 유지한다.
- 처리 중 승인·거절·필터·페이지 이동을 막는다. 성공 후 현재 필터로 서버 목록을 재조회하고 페이지 범위 초과 시 마지막 유효 페이지(전체 0건이면 1페이지)로 이동한다.
- 인증·권한·대상 없음, 이미 처리됨, 모임 제한·유형 오류·요청자 자격 변경 오류는 목록 재조회를 유도한다. 거절창은 이 오류에서 사유를 유지하고 확인 버튼을 목록 확인으로 바꿔 동일 요청 재심사를 막는다. 일반 통신 오류는 오류 안내 후 재시도할 수 있다.
- 현재 화면은 API가 변환한 한글 상태 '대기'로 처리 버튼을 표시한다. 기존 상위 화면의 계주 전용 진입을 재사용하며, 역할·모임 상태를 별도 props로 전달하는 보강과 원본 상태 코드 필드 분리는 이번 작업에 포함하지 않았다. 최종 권한·상태·소속·순번·증빙 검증은 서버가 수행한다.
- 계주 전용 영수증 원본 열람 버튼은 연결하지 않았다. 현재 증빙 표시는 첨부 유무만 제공하므로 증빙 내용을 확인하는 심사 흐름은 후속 작업이다.

### 자동 검증과 한계

- front에서 `node --test src/api/savingGroups.test.js src/api/savingGroupPayout.test.js src/api/savingGroupPayoutRequests.test.js src/components/savingGroup/SavingGroupPayoutApprovalList.test.js`: 55개 통과.
- API 대역 테스트: 승인 본문 없음, 큰 ID 보존, 거절 사유만 전송, 500자 허용·501자 및 공백 차단, 잘못된 ID 차단, HTTP 오류·실패 봉투·통신 오류·취소 보존.
- SSR 화면 테스트: 순번계·친목계의 대기 전용 버튼, 처리 완료·미정의 상태에서 버튼 미표시, 처리 중 버튼·필터·페이지 비활성, 로딩·실패 상태에서 심사 버튼 미표시.
- SSR 테스트는 실제 모달 클릭·비동기 상태 전이·중복 클릭·페이지 재조회·실패 시 사유 유지 동작을 실행하지 않는다. 해당 항목은 아래 브라우저 검증이 필요하다.

### 브라우저·Postman 수동 검증 (미실행)

1. 테스트용 승인 계주와 순번계·친목계의 대기 요청을 준비한다. 승인 확인 취소 시 PATCH가 없고, 확인 시 한 번 호출되는지 Network로 확인한다.
2. 거절 사유 빈 값·공백 제출이 차단되고 500자 사유가 전송되는지 확인한다. 실패 응답 후 사유 유지·재시도, 취소 시 미요청을 확인한다.
3. 대기 필터에서 성공 요청이 사라지고 전체 필터에서는 새 상태가 표시되는지 확인한다. 마지막 페이지의 마지막 요청 처리 후 유효 페이지 이동을 확인한다.
4. 지연 응답 중 연속 클릭과 승인·거절 중복 실행이 차단되는지 확인한다. 다른 창에서 먼저 처리한 요청은 409 안내와 목록 갱신을 확인한다.
5. 비계주·로그아웃·모임 상태 변경·요청자 탈퇴·순번 불일치·증빙 누락의 서버 오류를 확인한다. 거절 사유 유지와 목록 확인 동작을 확인한다.
6. Postman과 테스트 DB에서 처리 상태·검토자·검토일·거절 사유를 확인하고 실제 지급·잔액·거래가 변경되지 않는지 확인한다. 실제 금융 API를 호출하지 않는다.

<!-- AI작성: 진행 전 계원 추방 프론트 4단계 구현과 검증 범위를 기록한다. -->
## 진행 전 계원 추방 프론트 연결

- 기존 계원 관리 화면을 재사용한다. APPROVED OWNER이고 모임 상태가 01일 때 MEMBER·MANAGER에 추방 버튼이 보인다. 역할 편집 중에는 추방 버튼을 비활성화한다. 진행중·종료 및 비OWNER는 추방 UI를 제공하지 않으며 최종 권한은 서버가 판단한다.
- 회원 조회·역할 변경·추방은 savingGroupId를 사용한다. 추방은 기존 JWT client의 /api 기본 경로와 PATCH /saving-groups/{savingGroupId}/members/{memberId}/expel을 사용하고 본문은 보내지 않는다.
- 기존 ConfirmModal에서 '추방된 계원은 이 계모임에 다시 가입할 수 없습니다.'를 안내한다. 취소는 요청 없이 닫는다. 확인 중 검색·페이지·역할 변경·추방·확인창 닫기를 차단하고 ref 잠금으로 중복 요청을 방지한다.
- 성공 시 안내 후 현재 검색과 페이지로 재조회하고 마지막 페이지가 사라지면 남은 페이지를 조회한다. 실패 시 목록을 유지하고 확인창 안에 오류를 표시한다. 다시 열면 이전 오류를 지운다. 모임·역할·회원 상태·진행 상태 변경 시 패널을 재생성하여 이전 상태를 폐기한다.
- 자동 검증: 계모임 API·회원 관리·계금 요청 API·목록 SSR 테스트 61건 통과, 프론트 빌드 성공 및 대상 lint 오류 없음(기존 경고 2건). 브라우저 클릭 검증은 앱 내 브라우저의 로컬 서버 연결 시간 초과 및 Windows URL 확인 정책으로 수행하지 못했다. 실제 JWT·MySQL 연동 및 모바일 브라우저도 미검증이다.
- 수동 확인: 서로 다른 groupId/savingGroupId를 가진 모집중 모임의 OWNER로 계원 관리 진입 → MEMBER·MANAGER 추방 버튼 확인 → 취소 시 PATCH 없음 → 재가입 불가 문구 확인 후 추방 → 해당 회원 제외 및 성공 안내 확인. 마지막 페이지 1명을 추방한 경우 이전 페이지 이동을 확인한다. 403/404/409/통신 실패 시 목록 유지·확인창 오류, 빠른 연속 클릭 시 PATCH 1건, 진행중·비OWNER의 버튼 미노출, 검색·역할 변경 회귀도 확인한다.


## 계모임 공용 계좌 테스트 입금 (2026-11-06)

AI작성: 실제 돈 이동·입금 확인·금융 API·발급·웹훅 없이 내부 원장만 변경한다. 상세 설계·DB 담당자 적용 전제·단계별 변경 파일·실제 커밋은 [테스트 입금 작업 기록](saving-group-deposit-design.md)을 참고한다. 신규 보조 테이블은 담당자 적용 대기이며 이번 코드 작업에서 schema.sql과 실제 DB는 변경하지 않았다.

### 경로와 인증

- POST /api/saving-groups/{savingGroupId}/account/deposits
- POST /api/saving-groups/{savingGroupId}/account/deposits/confirm
- 기존 /saving-groups 별칭도 지원한다.
- 경로 ID는 계좌 조회 API와 같은 saving_group_id다.
- JWT 필터의 memberId 속성을 사용한다. 입력 회원 ID·역할·계좌번호는 신뢰하거나 사용하지 않는다.
- APPROVED인 현재 OWNER/MANAGER/MEMBER, 미삭제 회원, 삭제되지 않은 진행중 상태 02 계모임, 존재하는 공용 계좌가 필요하다.

### 최초·반복 입금 요청

```json
{
  "amount": 1000,
  "requestKey": "mock-request-key-0001"
}
```

- amount: 필수 JSON 숫자, 1원 이상·원 단위 정수·최대 13자리. DECIMAL 저장 범위 검증이며 별도 업무상 한도는 아니다.
- requestKey: 필수, 영문·숫자·밑줄·하이픈 16~64자. 프론트는 crypto.randomUUID()로 생성한다. 인증용 비밀정보가 아니며 같은 시도의 재전송에서는 동일한 키를 사용한다.
- 서버가 대상 모임의 계좌를 결정한다. 전체 계좌번호·은행 키·계좌 ID·처리자 ID·입금 유형·회비 회차·메모 입력은 받지 않는다.
- 기존 회비·순번계 납부 상태·알림을 변경하지 않는다.

일반 완료 응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "amount": 1000,
    "balance": 1000,
    "confirmationToken": null,
    "confirmationExpiresAt": null,
    "message": "테스트 입금이 등록되었습니다. 실제 돈은 이동하지 않습니다."
  }
}
```

응답 예시는 핵심 필드만 표시했다. 공통 ApiResponse의 다른 필드 직렬화는 기존 설정을 따른다.

### 중복 확인 필요 응답

같은 회원·공용 계좌·금액의 첫 일반 성공 입금부터 5분 미만이면 HTTP 200의 성공 봉투 안에 PENDING_CONFIRMATION을 반환한다. 거래는 생성하지 않고 잔액도 변경하지 않는다.

| data 필드 | 타입·설명 |
|---|---|
| status | PENDING_CONFIRMATION |
| amount | 요청 금액 숫자 |
| balance | null. 확인 대기 응답에서 잔액 갱신하지 않음 |
| confirmationToken | URL-safe 43자 임의 일회용 확인값. 로그·영구 저장 금지 |
| confirmationExpiresAt | LocalDateTime ISO 문자열. DB 기준 발급 후 5분이며 시간대 오프셋은 포함하지 않음 |
| message | 최근 5분 이내 동일 계좌·동일 금액 입금 이력과 추가 등록 확인 안내 |

확인값은 DB에 해시만 저장한다. 같은 대기 요청을 재전송하면 새 확인값과 기한을 발급하여 이전 확인값은 무효화한다. 보조 테이블의 상태는 은행 입금 상태가 아니라 내부 요청 처리 상태다.

### 추가 입금 확인 요청

본문은 동일 requestKey와 대기 응답에서 받은 confirmationToken이다. 확인값은 테스트용 고정 문자열이 아니라 실제 직전 응답 값을 사용한다.

- 서버가 본인·현재 권한·모임 상태·대상 계좌·요청·확인 해시·만료를 재검증한다.
- 유효한 확인이면 원장 거래 1건을 생성하고 대기 요청을 COMPLETED로 변경한다. 완료 응답은 일반 완료와 같다.
- 확인 기한에 도달하면 만료다. 만료 시 같은 requestKey로 최초 경로에 다시 요청하여 안내를 재발급할 수 있다.
- 완료된 동일 키 재전송은 거래를 추가하지 않고 기존 완료 결과와 현재 내부 잔액을 반환한다. 다른 금액·계좌로 같은 키를 재사용하면 409다.
- 추가 확인 입금은 최초 5분 기준을 연장하지 않는다. 정확히 5분 이후의 새 요청 키는 새 일반 입금이 된다.
- 취소는 클라이언트 화면을 닫는 동작이며 서버 원장 변경은 없다. 별도 취소 API·즉시 확인값 실효는 구현하지 않았다.

### 잔액과 저장

기존 group_transaction에 DEPOSIT, 금액, DB 시각 기반 거래일시, 테스트 입금 적요를 저장한다. 요청자는 보조 테이블 member_id로 연결한다. 기존 원장의 입금·출금 합산으로 잔액을 조회하며 balance_after·source_provider·source_transaction_id는 이번 기능에서 생성하지 않는다. 원장에 없는 증빙·은행 상태를 만들어 저장하지 않는다.

모임→회원 관계→계좌→요청 순서로 잠그고 원장 생성·보조 기록·확인 소비를 같은 트랜잭션으로 처리한다. 기존 방식의 Mock 지연 300~800ms는 잠금 전에 적용한다. 외부 금융 Service·SDK는 의존하지 않는다.

### 오류

| HTTP | 대표 코드 | 상황 |
|---|---|---|
| 400 | 기존 Validation 오류 / BAD_REQUEST / DEPOSIT_INVALID_INPUT | 잘못된 경로·금액·요청 키·확인값 형식 |
| 401 | 기존 필터 LOGIN_REQUIRED / Service UNAUTHORIZED | 미인증 |
| 403 | SAVING_GROUP_ACCOUNT_FORBIDDEN | 현재 승인된 역할·회원 조건 불충족 |
| 403 | DEPOSIT_CONFIRMATION_INVALID | 일치하지 않는 확인값 |
| 404 | NOT_FOUND | 삭제되었거나 없는 계모임 |
| 404 | DEPOSIT_ACCOUNT_NOT_FOUND | 공용 계좌 없음 |
| 404 | DEPOSIT_REQUEST_NOT_FOUND | 본인의 대상 계좌 확인 요청 없음 |
| 409 | DEPOSIT_INACTIVE | 진행중이 아님 |
| 409 | DEPOSIT_REQUEST_CONFLICT | 동일 요청 키에 다른 계좌·금액 |
| 409 | DEPOSIT_CONFIRMATION_EXPIRED | 확인 만료 |
| 409 | DEPOSIT_SAVE_CONFLICT | 조건부 저장 영향 행 수 불일치 |
| 500 | DEPOSIT_SAVE_FAILED | 원장 생성 키 확인 실패 |
| 503 | DEPOSIT_INTERRUPTED | Mock 지연 중 작업 중단 |

DB 테이블 미적용·기타 DB 장애는 기존 공통 예외 처리 대상이며 성공으로 대체하지 않는다. 네트워크·서버 장애로 결과를 확신할 수 없으면 새 키를 만들지 않고 같은 키로 결과를 확인한다. 현재 화면을 닫으면 메모리 요청 정보는 사라지므로 다시 입금하기 전에 거래내역부터 확인한다.

### Postman·브라우저 검증

1. DB 담당자의 테이블 적용과 기존 계좌 준비를 확인한다. 실제 자금 이동은 하지 않는다.
2. 테스트 진행중 모임의 OWNER/MANAGER/MEMBER JWT로 각자의 요청 키를 사용하여 최초 요청 성공·원장·입금자 연결·잔액 증가를 확인한다.
3. 같은 회원·금액의 새 키로 5분 이내 요청하면 PENDING_CONFIRMATION만 생성되는지 확인한다.
4. 확인값을 직전 응답에서 복사해 confirm을 호출하고 거래 1건 추가, 같은 확인 재전송에서 추가 없음, 확인 해시 소비를 확인한다.
5. 만료·위조 확인, 다른 회원·모임, 비회원·LEFT·PENDING·EXPELLED·REJECTED, 잘못된 금액·없는 계좌·비진행 모임을 확인한다.
6. 첫 성공부터 정확히 5분 경계, 추가 확인으로 시간 연장 안 됨, 동시 요청과 저장 실패 롤백을 전용 DB에서 확인한다.
7. 브라우저에서 마스킹·입력 잠금·확인 취소·실패 재시도·잔액 및 거래내역 갱신·계정 전환·모바일·키보드를 확인한다.

실행 결과: 신규 입금 테스트 24건 통과·전용 MySQL 1건 건너뜀. 기존 계좌·거래내역 포함 관련 백엔드 총 67건 통과·MySQL 2건 건너뜀. 프론트 API·계모임 SSR 회귀 145건 통과, 프로덕션 빌드 통과. 실제 DB·브라우저 클릭 검증은 미실시다. 전체 테스트 컴파일은 기존 무관한 패키지 참조·생성자 불일치 문제로 불가하여 임시 Gradle 소스 범위로 관련 테스트를 분리했다.

## 순번계 모의 순번 추천 조회 (2026-11-06)

<!-- AI작성: 사용자 확정 정책·구현·단계별 커밋과 검증 방법을 기록한다. -->

### API와 권한

`GET /api/saving-groups/{savingGroupId}/turn-orders/recommendations`

기존 `/saving-groups/{savingGroupId}/turn-orders/recommendations` 별칭도 제공한다. 경로 ID는 saving_group.saving_group_id이며 공통 groups.group_id가 아니다. 추가 요청 파라미터·본문은 없다. 기존 JWT 인증의 memberId 속성만 사용하며 쿼리의 memberId는 인증에 사용하지 않는다.

삭제되지 않은 순번계에서 모집중(01)·진행중(02)일 때 해당 모임의 APPROVED OWNER만 조회한다. MANAGER·MEMBER·비회원·PENDING·REJECTED·LEFT·EXPELLED·삭제 회원은 차단한다. 이번 사용자 확정에 따라 관계의 OWNER 역할을 기준으로 하며 별도 개설자 ID 일치 조건은 추가하지 않았다.

### 추천과 금액 정책

- 해당 계모임의 현재 승인된 OWNER·MANAGER·MEMBER 전체를 매 호출 무작위 배치한다. 검색어·계원 목록 페이지에 영향을 받지 않는다.
- 기존 순번 유무·과거 지급 여부와 관계없이 현재 승인 회원 전원을 모의 배치한다. 가입일·나이·주소·납부·금융 이력을 추천 기준으로 사용하지 않는다.
- 1부터 현재 대상 인원 수까지 회원당 하나의 순번을 제안한다. 재조회 시 우연히 같은 결과가 나올 수 있다.
- 기본 회비 B, 승인된 미삭제 회원 수 N, 추천 순번 k일 때 예상 지급 금액은 `B × N + floor(B × 0.01) × (k - 1)`이다.
- 인원·기본 회비 유지, 앞 순번 전원 지급을 가정한다. 이는 확정 회차의 payout_amount나 실제 지급 약속이 아니다. 모집중에도 현재 값으로 계산하며 인원·회비 변경 시 재조회해야 한다.
- BigDecimal로 원 단위 계산한다. 기본 회비 미설정·0 이하·소수 회비 또는 예상 금액 13자리 초과는 409다. 대상 없음은 성공 빈 배열로 반환한다.
- 순번·회원·회차·회비·지급 상태·추천 이력을 저장하거나 변경하지 않는다. 읽기 전용 REPEATABLE_READ 트랜잭션에서 일관된 조회 결과로 계산하며 SELECT만 사용한다. DB·마이그레이션·의존성 변경 없음.

### 응답 예시

Cache-Control: no-store를 적용한다. 기존 공통 ApiResponse를 사용하며 페이지 메타는 없다.

```json
{
  "success": true,
  "data": [
    { "memberId": "30", "nickname": "테스트계주", "recommendedTurnOrder": 1, "expectedPayoutAmount": 30000 },
    { "memberId": "50", "nickname": "테스트계원가", "recommendedTurnOrder": 2, "expectedPayoutAmount": 30100 },
    { "memberId": "60", "nickname": "테스트계원나", "recommendedTurnOrder": 3, "expectedPayoutAmount": 30200 }
  ]
}
```

응답은 회원 식별자 문자열·닉네임·추천 순번·예상 지급 금액만 제공한다. 예시는 기본 회비 10,000원·3명 기준이다. 예상 금액은 JSON 숫자이고 화면에서 천 단위 콤마와 원을 표시한다.

| HTTP | 코드 | 조건 |
|---|---|---|
| 400 | BAD_REQUEST | 잘못된 계모임 ID |
| 401 | UNAUTHORIZED 또는 기존 JWT 오류 코드 | 미인증·무효 세션·토큰 |
| 403 | SAVING_GROUP_OWNER_REQUIRED | 현재 승인된 미삭제 OWNER가 아님 |
| 404 | SAVING_GROUP_NOT_FOUND | 계모임 없음·삭제 |
| 409 | SAVING_GROUP_TYPE_INVALID | 순번계가 아님 |
| 409 | SAVING_GROUP_ACTIVITY_RESTRICTED | 모집중·진행중이 아님 |
| 409 | SAVING_GROUP_DUES_INVALID | 회비 또는 예상 금액 범위 오류 |

### 화면 흐름

계모임 상세 → 계원 관리 → 순번 추천 버튼 → 패널의 추천받기 → 결과 확인 → 다시 추천받기.

추천 버튼은 APPROVED OWNER·순번계·모집중/진행중에만 표시한다. 별도 탭은 없다. 추천 결과가 실제 순번에 저장·적용되지 않는다는 안내와 예상 금액 가정을 표시한다. 첫 진입은 자동 조회하지 않는다. 처리 중 중복 클릭을 차단하고 새 요청 시작 시 이전 결과를 지운다. 오류·빈 결과를 구분하며 이전 성공 결과를 실패 후 표시하지 않는다. 닫기·화면 이동·모임·로그인 변경 시 요청을 취소하고 결과를 폐기한다. 계원 관리 변경 후 재조회 시 추천 패널 결과도 초기화한다. 계원 역할·추방 처리 중에는 추천 실행을 막는다.

### 단계별 작업과 커밋

Java 경로는 `back/src/main/java/com/nightstudy/moa/savingGroup/`, 테스트는 `back/src/test/java/com/nightstudy/moa/savingGroup/` 기준이다. 각 단계에 `docs/ai-usage-log.md`를 포함한다.

| 단계 | 변경 파일 | 작업 내용 | 커밋 |
|---|---|---|---|
| 1 | dto/SavingGroupTurnOrderContextRow.java, dto/SavingGroupTurnOrderMemberRow.java, mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, 테스트 service/SavingGroupTurnOrderMapperTest.java | 모임·전체 승인 회원 조회 | 7314889 — feat: 순번계 순번 추천 조회 DTO와 Mapper 추가 |
| 2 | dto/SavingGroupTurnOrderRecommendationResponse.java, service/SavingGroupService.java, service/SavingGroupServiceImpl.java, 테스트 service/SavingGroupTurnOrderServiceTest.java | 권한·유형·상태·무작위 제안·예상액 | 3b15aac — feat: 순번계 모의 순번 추천 서비스와 권한 검증 추가 |
| 3 | controller/SavingGroupController.java, 테스트 controller/SavingGroupTurnOrderControllerTest.java | GET·인증 속성·공통 응답·캐시 금지 | 15e3201 — feat: 순번계 순번 추천 조회 API 추가 |
| 4 | front/src/api/savingGroups.js, front/src/api/savingGroupTurnOrder.test.js | 인증 client·응답 검증·오류·취소 | 2168ecc — feat: 순번계 순번 추천 프론트 API 추가 |
| 5 | front/src/components/savingGroup/SavingGroupMembersPanel.jsx, SavingGroupDetail.jsx, SavingGroupTurnOrderRecommendationPanel.jsx, SavingGroupTurnOrderRecommendationPanel.test.js(이상 동일 디렉터리), front/src/api/savingGroupTurnOrder.test.js | 계원 관리 버튼·추천 패널·상태·큰 글씨·금액·API 연결 | e4850e9 — feat: 순번계 모의 순번 추천 화면과 API 연동 구현 |
| 6 | 테스트 controller/SavingGroupTurnOrderSecurityTest.java, service/SavingGroupTurnOrderTransactionTest.java, docs/saving-groups-api.md, docs/ai-usage-log.md | JWT 필터·읽기 전용 트랜잭션 검증·명세·수동 검증 | test: 순번계 순번 추천 화면 연동 검증 및 API 문서화 |

### 수동 검증 절차

1. 테스트 DB에 모집중·진행중 순번계, 승인 OWNER·MANAGER·MEMBER와 대기·탈퇴·추방 회원을 준비한다. 실명·실제 금융정보는 사용하지 않는다.
2. OWNER로 상세 → 계원 관리 → 순번 추천을 열고 추천받기를 클릭한다. Network에서 위 GET 한 건과 회원 전체의 순번·닉네임·예상 금액을 확인한다.
3. 예를 들어 기본 회비 10,000원·승인 회원 3명이면 순번별 예상 금액이 30,000원·30,100원·30,200원인지 확인한다. 재추천은 같은 결과가 나올 수도 있다.
4. 검색·페이지와 무관하게 OWNER 포함 전체 대상이 반환되는지 확인한다. 대기·탈퇴·추방·삭제 회원은 없어야 한다.
5. Postman에서 MANAGER·MEMBER·비회원·탈퇴·대기 계정은 403, 로그아웃은 401, 친목계는 409, 없는 모임은 404인지 확인한다. 종료·정지 상태도 409다.
6. 호출 전후 group_member의 sequence_number·회원 상태, meeting_round, payout_request의 상태·transaction_id·completed_at 및 fee_payment를 비교한다. 변경 없어야 한다. 저장·적용 POST/PATCH가 없어야 한다.
7. 느린 네트워크에서 중복 클릭·닫기·모임/계정 전환, 오류·빈 결과를 확인한다. 실패 후 이전 결과가 노출되지 않아야 한다. 모바일·키보드·금액 가독성을 확인한다.

추천안 저장·비교·실제 적용은 후속 기능이며 이번에 테이블·API·버튼을 추가하지 않았다.

### 실행한 검증과 제한

- 백엔드: Mapper 1건, Service 22건, Controller 9건, JWT 필터 연결 3건, Spring 읽기 전용 트랜잭션 2건으로 총 37건 통과.
- Java 17로 실행했고 기존 전체 테스트 소스의 삭제/이동 클래스 참조 오류를 피하도록 build 아래 임시 init script에서 관련 테스트 소스만 선택했다. 프로젝트 빌드 설정·의존성은 변경하지 않았다.
- 프론트: 추천 API·계원 API·기존 savingGroups API·계원 목록 SSR·추천 패널 SSR 총 64건 통과. npm run build 통과. 변경 파일 oxlint 오류 없음. 기존 Detail의 미사용 선언 2건과 번들 크기 경고는 유지한다.
- 무변경 검증은 조회 Mapper 호출만 발생함·원본 회원 목록 불변·추천 SQL이 SELECT임을 자동 확인한 수준이다. 실제 MySQL 호출 전후 테이블 비교, 브라우저 클릭·계정 전환·모바일 검증은 미실시다. 위 수동 절차로 확인이 필요하다.

### 2026-11-06 순번 추천 버튼 미노출 수정

<!-- AI작성: 사용자 제보로 상세 응답의 groupType 누락을 확인했다. -->

추천 패널의 표시 조건은 groupType을 사용했으나 실제 GET /api/saving-groups/{groupId}의 상세 조회·응답에 해당 필드가 없어 버튼이 숨겨졌다. 상세 SELECT에 saving_group_type AS group_type을 추가하고 SavingGroupRow → Service → SavingGroupDetailResponse에 groupType을 전달했다. 기존 생성자는 유지한다. 프론트는 기존 상세 응답을 그대로 보존하므로 화면의 권한·유형 제한을 완화하지 않았다.

실제 JWT 필터·상세 Service·Controller 응답에 순번계/친목계 유형이 포함되는 회귀 테스트와 상세 화면 전체 SSR의 버튼 노출/비노출 테스트를 추가했다. 백엔드 재시작 후 승인 OWNER로 순번계 상세의 계원 관리에서 순번 추천 버튼을 확인한다. Network의 상세 응답 data.groupType이 순번계인지 확인하면 된다.

## 회비 납부 모달·거래 후 잔액 수정 (2026-11-06)

AI작성: 사용자 승인한 후속 수정이다. 이 절은 위 초기 입금 명세 중 balance_after 미저장을 대체한다. 입금/확인 경로·본문·JWT·권한·5분 정책은 유지한다. 추가 DB 컬럼·마이그레이션·금융 API 호출은 없다.

### 새 입금의 거래 후 잔액

기존 모임·계좌 잠금과 트랜잭션 안에서 현재 원장 합산 잔액 + 입금액을 계산하여 group_transaction.balance_after에 함께 저장한다. 최초·일반·추가 확인 입금에 모두 적용한다. 중복 안내와 완료 요청 재전송은 새 스냅샷·거래를 만들지 않는다.

DECIMAL(15,2) 저장 범위를 벗어나면 HTTP 409 DEPOSIT_BALANCE_OVERFLOW이며 원장 저장 전에 거절한다. 별도 업무상 입금 한도를 추가하지 않았다. 거래 저장과 보조 기록 저장 실패는 함께 롤백한다. 과거 NULL 잔액을 현재 잔액으로 채우는 보정은 하지 않는다.

완료 응답 balance는 응답 시점의 현재 장부 잔액이고, 목록 balanceAfter는 각 거래 직후의 스냅샷이다. 둘은 의미가 다르다. 이전 요청을 재전송해 받은 현재 잔액을 과거 거래의 잔액으로 덮어쓰지 않는다.

### 화면 흐름

- 진행중·현재 승인 역할·발급 계좌 조회 성공 시 계좌 아래 회비 납부 버튼을 제공한다.
- 버튼 클릭 시 입력 모달에서 마스킹된 공용 계좌 선택과 금액 입력을 제공하고 취소 / 입금 버튼을 표시한다.
- 같은 모달에서 중복 안내·추가 확인·처리중·실패를 전환한다. 중복 확인 취소는 입력 단계로 돌아간다.
- 성공 시 입력 모달을 닫고 별도 테스트 입금 완료 모달에 금액·현재 잔액·실제 돈 이동 없음 안내를 표시한다.
- 잔액 재조회와 완료 모달 상태는 분리한다. 조회 실패가 입금 실패로 표시되지 않는다.
- 확정 실패는 입력과 오류를 유지한다. 응답 유실·서버 장애는 결과 확인 필요로 표시하고 같은 키로 다시 확인한다.
- 모임·계정 변경 시 이전 상태를 숨긴다. 처리중·불확실 상태에서는 요청 정보가 사라지는 취소를 막는다.
- 이름은 회비 납부지만 금액은 자율 테스트 입금이다. fee_payment.is_paid·순번계 납부 상태를 변경하지 않는다.

### 검증 결과와 확인 방법

입금·계좌·거래내역 백엔드 72건 중 70건 통과·전용 MySQL 2건 건너뜀. 프론트 API·계모임 SSR 회귀 전체와 프로덕션 빌드 통과. 기존 대형 번들 경고 유지, 변경 프론트 oxlint 오류 없음. 실제 DB·브라우저 상호작용은 미실시다. 전체 백엔드의 기존 무관한 컴파일 오류를 피하기 위해 저장소 설정을 바꾸지 않고 임시 init script로 관련 테스트만 실행했다.

Postman에서는 입금·확인 요청 후 계좌 조회에서 장부 잔액을 확인한다. 다른 회원·출금·연결 없는 거래·정확히 5분 경계·확인 재전송·경쟁 요청도 전용 DB에서 확인한다. 브라우저에서는 회비 납부 → 입력 → 입금 → 완료 모달 → 잔액 갱신 및 중복 취소·실패·결과 불명확·모바일·키보드를 확인한다.

단계별 모든 파일·실제 커밋과 후속 검증은 [테스트 입금 설계·작업 기록](saving-group-deposit-design.md)의 최신 수정 이력을 따른다.

## 순번계 순번 지정·재지정 및 다음 달 예약 적용 (2026-11-06)

AI작성: 사용자 승인 A안에 따라 GET/PUT /api/saving-groups/{savingGroupId}/turn-orders를 추가했다. /saving-groups 별칭도 지원한다. savingGroupId는 saving_group.saving_group_id다.

- 현재 승인된 OWNER만 조회·지정한다. OWNER를 포함한 미지급 승인 회원 전원에게 남은 실제 회차를 중복 없이 배정한다.
- 지급 완료 회원은 재배정에서 제외하고 같은 운영 기간 중 재수령 요청·승인을 차단한다.
- 모집중은 즉시 저장, 진행중은 다음 달 예약 저장이며 같은 적용월 예약 전체를 교체한다.
- PUT 본문은 {"turnOrders":[{"memberId":"30","sequenceNumber":3},{"memberId":"40","sequenceNumber":4}]} 형식이다. 다음 달이 3회차이고 미지급 회원이 2명인 예시다.
- 응답은 기존 ApiResponse 안에 effectiveMonth, startRoundNumber, scheduled, needsReassignment, members를 반환한다. members는 memberId·nickname·role·paid·currentSequenceNumber·plannedSequenceNumber다. 회원 ID는 문자열이며 nullable 순번은 누락될 수 있다.
- 기존 JWT memberId만 사용하며 요청 본문·쿼리로 요청자 신원을 받지 않는다. GET은 no-store다.
- 기존 미래 지급 요청과 충돌하거나 예약 이후 대상·지급 상태가 달라지면 자동 취소·자동 재배정하지 않는다.
- 지급 요청과 승인에는 해당 회차의 적용월 순번을 사용한다. 회차 생성·지급 상태 변경·실제 금융 호출은 하지 않는다.
- DB 담당자가 A안 테이블 2개를 적용해야 한다. 이번 작업에서 schema.sql·마이그레이션·실제 DB를 변경하지 않았다.

최종 백엔드 관련 224건 중 221건 통과·MySQL 3건 건너뜀, 프론트 관련 6건 통과. 실제 DB·브라우저 상호작용은 미실시다. 전체 계모임 검사에는 별도 삭제 신청 테스트 2건 실패가 있어 전체 통과로 간주하지 않는다.

전체 요청·응답·오류·DB 계약·단계별 파일/커밋·실행 명령·수동 검증·남은 TODO는 [순번 지정 설계와 작업 기록](saving-group-turn-order-design.md)을 따른다.

## 계모임 공용 통장 거래내역 조회 (2026-11-08)

`GET /api/saving-groups/{savingGroupId}/account/transactions?page=1`

- 경로에는 공용 계좌 조회와 같은 `saving_group_id`를 사용한다.
- 로그인한 사용자 중 해당 모임의 현재 `APPROVED` 관계가 있는 `OWNER`, `MANAGER`, `MEMBER`가 조회할 수 있다.
- `page`는 1부터 시작하며 기본값은 1이다. 페이지 크기는 10건이다. 거래일시 최신순, 동일 시각은 내부 거래 PK 내림차순이다.
- 해당 공용 계좌 ID의 내부 `group_transaction`만 조회한다. 실제 금융 호출은 없다.
- 공통 성공 응답의 `data`는 목록이고 `meta`는 `page`, `size`, `total`, `totalPages`를 제공한다. 거래가 없거나 마지막 페이지를 넘으면 목록은 빈 배열이다.

| 응답 필드 | 형식 및 의미 |
|---|---|
| transactedAt | ISO 로컬 날짜·시간 문자열 |
| transactionType | DEPOSIT 또는 WITHDRAWAL |
| amount | 숫자, 거래 금액 |
| balanceAfter | 숫자, 거래 후 잔액. 0은 유효하다. |

거래 메모는 필수 데이터로 내부 검증하며 원문과 계좌번호·거래 식별자는 공개 응답에서 제외한다. 거래 목적·분류는 화면에서 대시로 표시하고, 필터·분류 수정은 후속 작업으로 남긴다.

| HTTP | 조건 |
|---|---|
| 400 | 모임 ID 또는 페이지가 유효하지 않음 |
| 401 | 미인증 |
| 403 | 해당 모임의 조회 권한 없음 |
| 404 | 모임 없음 또는 공용 계좌 누락. 계좌 누락 코드는 DEPOSIT_ACCOUNT_NOT_FOUND |
| 500 | 요청 페이지의 거래에 메모 누락 또는 공백, 거래 후 잔액 누락. 코드는 SAVING_GROUP_TRANSACTION_INVALID_DATA |

### 수동 검증

1. 테스트 DB의 해당 공용 계좌에 유효한 메모·거래 후 잔액을 가진 입금 및 출금 거래를 11건 이상 준비하고 다른 계좌에도 거래를 준비한다.
2. 승인 모임원의 JWT로 Postman에서 기본 페이지와 `page=2`를 요청한다. 10건 단위, 최신순, 다른 계좌 행 미포함, 원본 메모·식별자 미노출을 확인한다.
3. 빈 장부, 미인증, 다른 모임 사용자, 계좌 없는 모임, 메모나 거래 후 잔액이 비어 있는 행을 각각 확인한다.
4. 브라우저에서 계모임 상세의 거래 내역 메뉴를 열어 다음 페이지, 입금·출금 금액, 0원 잔액, 모바일 카드, 빈 목록과 오류 안내를 확인한다.


## 계주 전용 월별 회비 납부 현황

GET /api/saving-groups/{savingGroupId}/dues/payment-status?month=2026-11&page=1

- AI작성: 현재 승인 계주만 조회할 수 있다. 필수 월 형식은 YYYY-MM, 기본 페이지는 1, 페이지 크기는 10이다.
- 공통 data 및 meta를 사용한다. 항목은 memberName, duesAmount, depositedAmount, remainingAmount, paid를 반환한다.
- 해당 모임 원장에 연결된 완료 테스트 입금만 회원 ID별로 합산한다. 선택 월 시작 이상, 다음 월 시작 미만인 거래만 포함한다. 거래일시는 저장된 현지 DATETIME을 사용하므로 운영 DB 시각이 한국 시간 기준인지 확인한다.
- 회비 이상 입금하면 납부, 부족하면 미납이다. 초과분은 다음 달에 이월하지 않는다. 입금 없는 현재 승인 계원은 합계 0원으로 표시한다.
- 선택 월까지 적용된 회비 변경 이력을 사용한다. 금액 변경 이력이 없고 모임 시작일이 확인되면 현재 회비를 사용한다. 최초 적용 이력 이전의 금액이나 시작일을 확인할 수 없으면 기준 확인 필요로 표시하고 입금 합계를 제공한다.
- 기존 테스트 입금 요청 방식과 DB 스키마를 사용한다. 이 조회에서 거래나 회비 행의 상태를 변경하지 않는다.

### 수동 검증

1. 테스트 모임의 월 회비를 12,000원으로 설정하고 승인 계원으로 같은 달 2,000원과 10,000원을 테스트 입금한다.
2. 계주로 선택 월을 조회해 입금 합계 12,000원, 부족액 0원, 납부를 확인한다.
3. 다른 달 입금은 선택 월 합계에 포함되지 않는지, 회계·일반 계원 JWT로 같은 API 호출 시 403인지 확인한다.
4. 브라우저에서 월 변경 시 첫 페이지로 이동하고 화면 폭을 줄이면 모바일 카드로 표시되는지 확인한다.

JDK 17로 관련 백엔드 테스트 38개 통과, 전용 DB 테스트 2개 건너뜀. 프론트 테스트 57개와 빌드 통과.

## 계모임 월간 캘린더 (2026-11-09)

<!-- AI작성: 공유 일정의 API 계약과 검증 결과를 기록한다. -->

승인된 계주(OWNER), 회계(MANAGER), 계원(MEMBER)이 같은 계모임의 일정을 조회한다. 등록은 승인된 계주와 회계만 가능하다. 경로의 savingGroupId는 계모임 고유 ID다. 실제 로그인 회원은 서버 인증 속성에서 가져온다.

| 동작 | 메서드와 경로 | 성공 |
| --- | --- | --- |
| 월간 일정 조회 | GET /api/saving-groups/{savingGroupId}/calendar?month=2026-11 | 200 |
| 선택일에 일정 추가 | POST /api/saving-groups/{savingGroupId}/calendar/events | 201 |

조회 응답 예시

```json
{"success":true,"data":{"canCreate":true,"events":[{"id":"42","title":"정기 모임","date":"2026-11-20","time":"19:00"}]}}
```

날짜별 상세는 월간 응답에서 선택일의 일정을 표시한다. 일정이 없는 월에는 events가 빈 배열이다. 승인된 일반 계원에게는 canCreate가 false다. 조회 응답에는 Cache-Control: no-store를 설정한다.

등록 요청 및 성공 응답 예시

```json
{"title":"정기 모임","date":"2026-11-20","time":"19:00"}
```

```json
{"success":true,"data":{"id":"42","title":"정기 모임","date":"2026-11-20","time":"19:00"}}
```

제목은 필수이며 최대 100자다. 날짜와 시각은 한국 현지 날짜와 시각으로 합쳐 저장한다. 시각은 분 단위다. 400은 잘못된 모임 번호, 월, 날짜, 시각 또는 제목, 401은 인증 누락, 403은 대상 모임 미승인·비회원 또는 계원의 등록, 404는 조회할 수 없는 모임에 해당한다. 오류는 기존 공통 error.code와 error.message로 반환한다.

### DB 적용

기존 back/src/main/resources/schema.sql의 group_event를 사용한다. group_id와 writer_id, title, event_date를 저장하며 location은 NULL이다. 대상 MySQL에서 기존 group_event 정의와 FK 적용 여부를 확인한다. 기존 스키마가 적용되어 있으면 별도 DDL이나 이관 작업이 없다. 조회는 group_id와 월 시작 이상·다음 월 시작 미만 범위를 함께 적용한다.

### 검증 결과와 재현 절차

- 백엔드 compileJava와 분리 실행한 캘린더 MockMvc 4건 통과. 계원 조회와 등록 차단, 계주·회계 등록, 날짜·시각 응답, 인증 누락, 잘못된 요청과 교차 모임 접근 차단을 확인했다. 전체 백엔드 테스트 컴파일은 기존 테스트의 누락된 타입 참조로 실패했다.
- 프런트 npm run build 및 node --test src/api/savingGroups.test.js 50건 통과. 빌드에서 번들 크기 경고가 출력됐다.
- 실제 MySQL 통합 테스트는 MOA_TEST_DB_ENABLED 설정이 없어 건너뛰었다. 격리된 빈 moa_join_test_* DB에 프로젝트의 기존 테스트 환경변수를 주입하면 생성 키와 저장 후 재조회, 월 경계 및 모임별 분리를 검증할 수 있다.
- 브라우저에서는 승인 회원으로 상세의 캘린더 탭을 열어 월 이동, 오늘 복귀, 날짜 선택을 확인한다. 계주나 회계로 일정을 등록한 뒤 같은 달을 다시 열어 새로고침 전후를 비교하고, 같은 모임의 다른 승인 계정에서 공유된 일정을 확인한다.
- Postman에서는 기존 Bearer 인증으로 두 API의 정상 응답, 잘못된 월·빈 제목의 400, 계원의 POST 403, 비회원이나 다른 모임 ID를 사용한 GET·POST 403을 확인한다. 실제 로그인 계정을 사용하는 브라우저와 Postman 검증은 미실행이다.

### 선택일 등록 상한과 등록 폼

<!-- AI작성: 하루 10개 제한과 등록 폼의 동작을 기록한다. -->

하루 최대 10개는 계모임 ID와 일정 날짜별로 적용한다. 계주와 회계의 등록을 합산한다. 등록 트랜잭션에서 기존 모임 행을 잠근 후 선택일 시작 이상·다음 날 시작 미만의 일정 수를 확인한다. 기존 일정이 10개 이상인 날짜도 전체 목록을 조회할 수 있다. 별도 DB 변경은 없다.

11번째 등록 요청은 HTTP 409를 반환한다.

```json
{"success":false,"error":{"code":"SAVING_GROUP_DAILY_EVENT_LIMIT","message":"하루에 최대 10개까지 등록할 수 있습니다."}}
```

등록 폼은 선택일 제목 아래와 일정 목록 위에 열리며 저장 성공 또는 취소 시 닫힌다. 등록 실패 시 입력을 유지한다. 선택일 일정이 10개 이상이면 등록 버튼을 비활성화한다. 다른 회원의 등록으로 409를 받으면 입력을 유지한 채 해당 월의 목록을 새로 조회한다.

- 검증 결과는 캘린더 MockMvc 5건 및 프런트 빌드 통과다. 전용 MySQL 설정이 없어 계모임·날짜별 집계와 동시 요청 통합 테스트 2건은 건너뛰었다. 실제 브라우저와 Postman 검증은 미실행이다.
- 브라우저에서는 일정 9개가 있는 날짜에서 폼이 목록 위에 열리는지, 저장·취소 시 닫히는지, 10번째 일정 저장 후 버튼과 한도 안내가 바뀌는지 확인한다.
- Postman에서는 같은 모임·날짜의 10번째 등록이 201, 11번째가 409인지 확인한다. 다른 날짜와 모임에는 별도 한도가 적용된다. 동시 요청은 일정 9개가 있는 상태에서 계주와 회계로 각각 등록을 보내 한 건만 201이 되는지 확인한다.

### 계모임 일정 물리 삭제 (2026-11-09)

<!-- AI작성: 계모임 일정 삭제 API와 검증 결과를 기록한다. -->

`DELETE /api/saving-groups/{savingGroupId}/calendar/events/{scheduleId}`에 기존 Bearer 인증을 사용한다. 요청 본문은 없다. 승인된 계주·회계가 작성자에 관계없이 해당 모임의 일정을 삭제할 수 있다. 예를 들어 `DELETE /api/saving-groups/10/calendar/events/42`가 성공하면 HTTP 200과 공통 응답 `{"success":true}`를 반환한다.

| 상태 | 조건 |
| --- | --- |
| 400 | 모임 또는 일정 ID가 양의 정수가 아님 |
| 401 | 로그인 정보 없음 |
| 403 | 해당 모임 비회원·미승인 회원 또는 일반 계원 |
| 404 | 대상 계모임 또는 그 모임에 속한 일정이 없음. 재삭제와 타 모임 일정 ID도 포함 |

삭제는 등록과 같은 모임 행 잠금을 사용하고, 기존 group_event의 group_id와 schedule_id가 일치하는 행을 물리 삭제한다. DB 구조 변경은 없다. 삭제 성공 후 화면에서 배지와 상세 목록을 함께 갱신한다. 선택일이 10개 미만이 되면 등록 버튼이 다시 활성화된다. 확인창 취소 또는 삭제 실패 시 기존 목록을 유지한다.

- 캘린더 MockMvc 8건과 프런트 빌드, 기존 계모임 API 회귀 테스트 50건이 통과했다. 실제 행 삭제와 한도 회복을 다루는 MySQL 테스트 2건은 전용 DB 설정이 없어 건너뛰었다.
- 브라우저 수동 검증은 계주·회계로 제목과 시각 확인, 취소, 삭제 성공, 새로고침 후 유지, 한도 회복을 확인하고 계원 화면의 삭제 버튼 노출 여부를 확인한다.
- Postman 수동 검증은 정상 삭제 200, 일반 계원·비회원 403, 타 모임 일정 ID 및 재삭제 404를 확인한다. 실제 계정으로 브라우저와 Postman을 사용한 검증은 미실행이다.

## 계모임 채팅 (2026-11-09)

승인된 계모임 회원은 계모임 상세의 **채팅방** 탭에서 텍스트를 주고받는다. 화면은 날짜별로 대화를 구분하고, 내 메시지는 오른쪽에 표시한다. 채팅방에 접속하면 최근 대화를 불러오고 실시간 메시지와 메시지 ID 기준으로 합친다.

| 구분 | 경로 | 동작 |
|---|---|---|
| REST GET | `/api/saving-groups/{savingGroupId}/chat-room` | 계모임 상세 ID로 채팅방 ID를 조회한다. 응답의 `data`는 방 ID다. |
| REST GET | `/api/v1/chat/rooms/{roomId}/messages` | 이전 대화를 조회한다. `size` 기본값은 30, 허용 범위는 1~100이다. `beforeId`를 지정하면 해당 ID보다 앞선 메시지를 조회한다. 결과는 메시지 ID 오름차순이다. |
| WebSocket | `/ws` | STOMP CONNECT의 `Authorization: Bearer <accessToken>` 헤더로 인증한다. |
| STOMP SEND | `/app/chat/rooms/{roomId}` | JSON 본문 `{"content":"안녕하세요"}`를 전송한다. 공백만 있는 본문과 1,000자 초과 본문을 거절한다. |
| STOMP SUBSCRIBE | `/topic/chat/rooms/{roomId}` | 저장된 메시지를 받는다. |
| STOMP SUBSCRIBE | `/user/queue/errors` | 메시지 처리 실패 시 공통 오류 응답을 받는다. |

메시지에는 `messageId`, `roomId`, `senderId`, `senderName`, `content`, `createdAt`이 포함된다. REST는 공통 응답 봉투의 `data`에 메시지 배열을 반환하고, 방 구독은 메시지 객체를 전달한다. 인증 실패는 STOMP 오류 프레임의 `AUTH_REQUIRED`, `TOKEN_EXPIRED`, `TOKEN_INVALID`로 구분한다. 허용되지 않은 방 또는 경로에는 `CHAT_FORBIDDEN`을 사용한다.

채팅방과 참여자 저장은 DB 담당자가 적용하는 `chat_room`, `chat_room_member`, `chat_message`를 전제로 한다. 개설 신청에서 방을 만들고, 개설·가입 승인 시 참여 권한을 등록한다. 탈퇴·제외 시 권한을 회수한다. 기존 계모임의 방과 승인 회원 데이터 반영은 DB 담당자 적용 범위다.

### 검증과 재현

- JavaScript 문법 검사와 `git diff --check`는 통과했다. 프론트 빌드는 `@stomp/stompjs` 미설치로 실패했고, 백엔드 테스트는 Gradle이 JDK 17을 찾지 못해 실행되지 않았다.
- DB 반영, 의존성 설치 및 JDK 17 설정 후 서로 다른 브라우저 프로필에서 같은 계모임의 승인 회원 두 명으로 접속한다. 메시지가 양쪽에 한 번씩 도착하고 새로고침 후에도 조회되는지 확인한다.
- 비회원 계정으로 방 ID·대화 조회와 직접 구독·전송이 거절되는지 확인한다. 방 이동과 재연결 시 메시지 혼입이나 중복이 없는지도 확인한다. 브라우저와 실제 DB 검증은 미실행이다.
