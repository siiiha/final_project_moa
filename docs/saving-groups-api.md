<!-- AI작성: 2026-10-20 확정 설계에 따른 구현·검증 명세. -->
# 계모임 목록 조회 API
# 계모임 조회 API

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

## 요청

`GET /saving-groups` (기존 프론트 프록시용 별칭: `GET /api/saving-groups`)

공개 조회이며 토큰을 요구하지 않는다. 현재 저장소에는 Spring Security/JWT 구현·의존성이 없다.
인증 기능 도입 시 두 GET 경로의 공개 접근 및 만료 토큰 정책을 따로 확인해야 한다.

| 파라미터 | 타입 | 기본값 | 검증 |
|---|---|---|---|
| keyword | String | 조건 없음 | 앞뒤 공백 제거 후 최대 20 Java 문자열 길이, 공백만 있으면 생략 |
| regionCode | String | 조건 없음 | 01~10만 허용, 전체는 파라미터 생략 |
| page | Integer | 1 | 1 이상, 빈 값·문자·소수·범위 초과 거부 |

지역: 01 서울 / 02 경기 / 03 강원 / 04 충북 / 05 충남 / 06 전북 / 07 전남 /
08 경북 / 09 경남 / 10 제주도.
DB ID와 코드를 동일시하지 않으며 `region.region_name`을 해당 명칭과 비교한다.
상세 지역과 광역시 확장·매핑은 이번 범위에 없다. 지역 데이터는 API가 생성하지 않는다.

예: `/api/saving-groups?keyword=산책&regionCode=01&page=1`

## 응답

기존 게시글과 동일하게 `ApiResponse.ok(items, pageMeta)`를 사용한다.
`data`는 카드 배열, `meta`는 기존 `PageMeta`이다. 공통 클래스는 변경하지 않았다.
페이지·크기·전체 개수·전체 페이지·offset은 모두 int로 계산한다.
기존 `ApiResponse.ok(data)`를 사용한다. 고정 크기 목록 전용 DTO 안에 페이징을 담으므로
공통 `meta`는 null이다. 기존 PageMeta와 다른 긴 정수 개수·offset을 지원하면서 공통 인터페이스는 바꾸지 않았다.

```json
{
  "success": true,
  "data": {
    "items": [{
      "savingGroupId": 101,
      "groupName": "산책 모임",
      "description": "이웃과 함께하는 모임",
      "regionName": "서울",
      "maxMembers": 8,
      "memberCount": 5,
      "duesAmount": 50000.00,
      "duesDay": 25,
      "status": "01"
    }],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 1,
    "pageSize": 6
  },
  "meta": null,
  "error": null
}
```

- PageMeta의 기존 getOffset()도 JSON의 offset 필드로 직렬화된다.
- 카드 필드는 위 예시와 같다. description/maxMembers/duesAmount/duesDay는 null 가능하다.
- 결과 0건: items=[], totalElements=0, totalPages=0, currentPage=요청 페이지, pageSize=6.
- 전체 7건에 page=3: items=[], totalElements=7, totalPages=2, currentPage=3, pageSize=6.
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
- 상단·상세 지역은 현재 상위 지역명만 사용한다.
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
   Java 17로 컴파일·대상 테스트를 검증하며 버전 업그레이드는 하지 않는다.

## 공식 근거

- https://mybatis.org/mybatis-3/sqlmap-xml.html
- https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://dev.mysql.com/doc/refman/8.4/en/string-comparison-functions.html
- https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html
