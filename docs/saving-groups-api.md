<!-- AI작성: 2026-10-20 확정 설계에 따른 구현·검증 명세. -->
# 계모임 목록 조회 API

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
