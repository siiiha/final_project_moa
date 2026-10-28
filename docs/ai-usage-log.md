<!-- AI작성: 사용자 승인 경로에 작업·검증 기록을 작성한다. -->
# AI 사용 기록

## 2026-10-28 — 소모임 설정 정보 조회 API

- 목적: 소모임 개설자가 설정 화면을 열기 전에 현재 수정 가능한 모임 정보와 승인 참여 인원을 안전하게 조회할 수 있게 한다.
- 사용자 확정: 경로는 `GET /api/small-groups/{id}/settings`이며, `id`는 `small_group.small_group_id`를 사용한다. 수정 가능 정보는 모임명·관심사 카테고리·소개글·정원·만남일·장소이고, 소모임에는 상태별 수정·조회 제한을 추가하지 않는다. 설정 조회와 수정 권한은 개설자이면서 `OWNER`·`APPROVED` 관계인 사용자로 한정한다.
- AI 작성: 설정 조회 응답·권한 DTO, MyBatis 권한·현재값 조회, Service의 개설자/승인 OWNER 이중 권한 검증, GET Controller, Service·Mapper XML·Controller·Security 테스트, API 명세를 추가했다.
- 응답·권한: 성공 시 기존 `ApiResponse`의 `data`에 6개 수정 가능 필드와 `approvedMemberCount`를 반환한다. 승인 인원은 `APPROVED` 상태의 `OWNER`, `MEMBER`만 중복 없이 집계한다. 없는·삭제 소모임은 `404 SMALL_GROUP_NOT_FOUND`, 개설자 또는 승인 OWNER가 아닌 요청은 `403 SMALL_GROUP_SETTINGS_FORBIDDEN`으로 처리한다. 인증이 없으면 Spring Security 공통 `401 LOGIN_REQUIRED`를 사용한다.
- DB·의존성·외부 연동: 기존 `small_group`, `groups`, `group_member`와 MyBatis Mapper만 재사용했다. DB 마이그레이션, 상태값, 외부 호출, 의존성·설정 변경은 하지 않았다.
- 검증: `git diff --check`를 통과했다. Gradle 대상 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 환경에서 소모임 Mapper XML·Service·Controller·Security 테스트를 실행하고, 실제 JWT로 OWNER·비OWNER·비로그인·삭제 소모임의 200/401/403/404 응답을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 다음 단계에서 프런트 설정 폼이 이 API의 데이터와 승인 인원 수를 사용하도록 연결하기 전 응답 필드를 검토한다.

## 2026-10-28 — 소모임 공개 상세 조회 API 구현 및 문서화

- 목적: 비로그인 사용자와 비회원도 조회할 수 있는 소모임 상세 API와 상세 화면 연동을 구현하고, 역할별 응답·탭 노출 규칙을 기록했다.
- 사용자 확정: 경로는 `GET /api/small-groups/{id}`로 통일했다. `MANAGER` 관계는 `membership_status`와 무관하게 403 처리하며, 중복 관계는 500 처리한다. 모집 상태는 상세 응답에 포함하지 않는다. 모임 날짜·장소와 개설자 닉네임은 기본 상세에 포함한다. 비회원도 최근 공지 4건의 제목·작성자·작성일은 볼 수 있지만, 공지 본문·공지 목록·공지 상세 이동은 후속 작업으로 남긴다.
- AI 제안·작성: 상세 응답 DTO, Service 계약·구현, MyBatis 조회·관계 검증, 공개 보안 경로, Controller 응답, 백엔드 테스트, 프론트 API 모듈·테스트, 역할별 상세 탭과 상세 페이지 API 연동을 작성했다. 이후 `group_notice` 기반 최근 공지 요약을 상세 응답에 추가하고, 공지 탭에 장소·다음 모임 일정·최근 공지를 구성했다. 중복되는 모임 정보 카드 대신 개설자·생성일을 상단 기본 정보 옆에 표시하도록 정리했다. API 설계 문서는 `docs/small-group-public-detail-api.md`에 작성했다.
- 권한·데이터: 비로그인·비회원은 `myRole: null`로 상세를 조회한다. 승인된 OWNER/MEMBER만 역할을 받으며, PENDING/REJECTED는 공개 상세만 조회한다. 참여자 수는 승인된 OWNER/MEMBER만 중복 없이 계산한다.
- DB·의존성: 기존 테이블과 MyBatis Mapper를 재사용했고 DB 마이그레이션, 새 상태값, 외부 연동, 패키지 추가는 하지 않았다.
- 검증: 프론트 `node --test src/api/smallGroups.test.js` 10개 테스트 통과, `npm run build` 통과, `git diff --check` 통과를 확인했다. 백엔드 Gradle 테스트는 로컬에 Java 17 toolchain이 없어 테스트 컴파일 전 실행되지 못했다.
- 직접 확인 필요: Java 17 환경에서 `back\\gradlew.bat test --tests "com.nightstudy.moa.smallGroup.*" --no-daemon`을 실행하고, 실제 DB 데이터로 비로그인·비회원·OWNER·MEMBER·MANAGER·중복 관계, 최근 공지 0·1·4건 조회를 확인한다. 브라우저에서는 공지 전체 보기·일정 확인·길찾기가 후속 기능 전까지 이동하지 않는지 확인한다.
- 개발자 코드 검토·수정: 대기 중. 특히 실제 `group_member` 생성 로직이 개설자 OWNER/APPROVED 행을 생성하는지와 운영 환경의 JWT 오류 응답을 확인한다.

## 2026-10-27 — 커뮤니티 화면 상태 유지 및 작성 임시저장

- 목적: 게시글·댓글 화면에서 새로고침이나 화면 이동이 발생해도 작성 중인 내용과 적용된 검색 조건을 잃지 않도록 한다.
- 사용자 확정: JWT와 회원 번호는 브라우저를 다시 열어도 로그인 상태가 유지되도록 기존 Zustand `localStorage` 방식을 유지한다. 소셜 로그인은 다른 담당자의 범위이므로 변경하지 않는다. 입력 중인 검색어는 유지하지 않고, 검색을 실행해 적용된 조건만 URL에 반영한다.
- AI 제안·작성: 신규 댓글·대댓글 상태에 응답의 `memberId`를 포함해 등록 직후에도 본인 수정·삭제 버튼이 표시되도록 했다. 회원·게시글별 댓글과 대댓글 초안, 새 글과 게시글 수정 초안을 `sessionStorage`에 저장하고 새로고침 시 복원하며 등록 성공·명시적 취소 시 삭제하도록 했다.
- 검색·페이징: 적용된 검색어·카테고리·검색 조건·정렬·페이지를 URL 쿼리 파라미터로 관리한다. 새 검색 조건은 1페이지부터 조회하고, 뒤로 가기와 새로고침 시 URL 상태를 복원한다. 현재 페이지가 전체 페이지 수를 넘으면 마지막 페이지로 보정하고, 결과가 없으면 `page`를 제거해 기본 1페이지 상태로 정리한다.
- 작성 취소: 기존 공통 `ConfirmModal`을 재사용한다. 작성 또는 수정 내용이 있으면 계속 작성하거나 임시저장을 삭제하고 나가도록 선택하게 하며, 내용이 없으면 바로 원래 화면으로 이동한다. 새로고침이나 일반 화면 이탈에서는 임시저장을 유지한다.
- 변경 파일: `front/src/pages/PostDetailPage.jsx`, `front/src/pages/PostCreatePage.jsx`, `front/src/pages/PostListPage.jsx`. 인증 저장소, 백엔드, DB, API 계약, 의존성, 외부 서비스는 변경하지 않았다.
- 검증: 각 변경 후 `npm run build`를 통과했고 대상 파일별 `oxlint`와 `git diff --check`를 통과했다. Vite의 기존 500 kB 초과 번들 경고와 npm의 `min-release-age` 환경 설정 경고는 유지된다.
- 직접 확인 필요: 브라우저에서 댓글·대댓글 및 게시글 초안의 새로고침 복원과 등록·취소 후 삭제, 검색·카테고리·정렬·페이지의 새로고침·뒤로 가기 복원, 존재하지 않는 페이지의 마지막 페이지 보정, 작성 취소 확인창의 두 선택지를 확인해야 한다.
- 개발자 코드 검토·수정: 최종 검토 대기.

## 2026-10-23 — 계모임 계원 자진 탈퇴 API

- 목적: 로그인한 계원이 계모임 시작 전인 모집중(`01`) 상태에서 본인의 계원 관계를 탈퇴 처리한다.
- 사용자 확정: 탈퇴는 모집중(시작 전)에만 허용하고, 나머지 상태는 차단한다. 구현은 Mapper → Service → Controller → 테스트 → 문서 단계로 나누어 진행했다.
- AI 작성: 기존 MyBatis 잠금 조회를 재사용해 `APPROVED` 관계만 `LEFT` 및 `left_at = CURRENT_TIMESTAMP`로 조건부 변경하는 Mapper, Service 트랜잭션, DELETE API, Service·Controller 단위 테스트, API 명세를 작성했다.
- 권한·데이터: 탈퇴 대상은 서버 request attribute `memberId`로만 정하며 URL·쿼리·헤더·본문에서 회원 ID를 받지 않는다. 계모임 상태는 `01`만 허용하고, 승인 관계가 없으면 저장하지 않는다.
- DB·외부 서비스·의존성·설정 변경: 없음. 기존 `group_member.membership_status`의 `LEFT`, `left_at`을 사용했으며 새 테이블·컬럼·DTO·외부 연동을 추가하지 않았다.
- 검증: `git diff --check`를 통과했다. `gradlew.bat test --tests "*SavingGroupLeave*" --offline` 실행은 Java 17 toolchain 부재로 테스트 컴파일 전에 중단되어 통과 여부는 미확정이다.
- 직접 확인 필요: Java 17 준비 후 탈퇴 대상 테스트 실행, 별도 MySQL에서 `FOR UPDATE`·조건부 UPDATE·`left_at` 저장·동시 탈퇴 롤백, JWT 인터셉터 병합 뒤 401·성공·비계원·진행중 요청을 확인한다.
- 개발자 코드 검토·수정: 최종 검토 대기.

## 2026-10-23 — 계주 승인 계원 추방 API

- 목적: 계주가 자신의 계모임에서 승인된 계원을 `EXPELLED` 상태로 변경할 수 있게 한다.
- 사용자 확정: 요청 식별자는 `groupId`와 대상 `memberId`를 URL 경로로 받고, 계주 본인 추방은 차단한다.
- AI 작성: 기존 서버 인증 request attribute와 계주 이중 검증을 재사용한 Service·MyBatis 조건부 UPDATE, Controller API, Service·Controller 단위 테스트, API 명세를 작성했다. 추방 성공 뒤에는 알림 TODO만 남겼다.
- 권한·데이터: `groups.member_id`와 `OWNER` + `APPROVED` 관계를 모두 확인한다. 대상은 `APPROVED` 상태일 때만 `EXPELLED`로 변경하며, `left_at`·역할·승인 이력·알림 데이터는 바꾸지 않는다.
- DB·외부 서비스·의존성·설정 변경: 없음. 기존 `group_member.membership_status`의 `EXPELLED` 상태값을 사용했고 실제 알림 발송·저장·외부 연동은 구현하지 않았다.
- 검증: `git diff --check`와 MyBatis Mapper XML 구조 확인을 통과했다. Gradle 대상 테스트 실행은 Java 17 toolchain 부재로 컴파일 전에 중단되어 통과 여부는 미확정이다.
- 직접 확인 필요: Java 17을 준비한 뒤 Service·Controller 테스트, 실제 MySQL에서 `FOR UPDATE`와 조건부 UPDATE, JWT 인터셉터 병합 뒤 401·403·성공 요청을 검증한다.
- 개발자 코드 검토·수정: 최종 검토 대기.

## 2026-10-22 — 계모임 개설 신청 및 관리자 심사 실제 API 연동

- 목적: 개설 신청을 `meeting_approval`의 CREATE/PENDING 이력으로 저장하고, 관리자 승인·반려 및 React 화면을 실제 API로 연결한다.
- 사용자 선택: `meeting_approval` 중심의 정석 구현을 선택했고, 로그인 미구현 상태에서는 임시 `memberId=1` 전송 방식을 승인했다.
- AI 작성: 신청 DTO 검증·지역명 조회·그룹/계모임/심사 이력 트랜잭션 생성, 심사 목록·상세·승인·반려 API와 MyBatis SQL, 신청·관리자 화면의 로딩/오류/중복 처리 방지, 반려 사유 입력을 작성했다. 승인 시에만 계모임 상태를 `01`로 바꿔 공개 목록에 노출한다.
- 보안·권한: 임시 신청자 ID는 API 모듈 상수로만 사용하며 실제 인증 도입 때 서버 인증 ID로 교체해야 한다. 관리자 API는 아직 Spring Security 권한 검증이 없으므로 JWT 병합 전 외부 공개 배포를 금지한다.
- 데이터·외부 서비스·의존성·설정: 스키마·공용 DB·외부 금융 서비스·의존성·설정 변경 없음. 이미지 저장은 기존 `FileStorageService`를 재사용한다.
- 검증: 프론트 `npm run lint` 통과(기존 미사용 변수 경고 4건 유지), `npm run build` 통과. 백엔드 `gradlew.bat test`는 Windows 환경에 Java 17 toolchain이 없어 컴파일 전에 중단되어 미검증이다.
- 직접 확인 필요: 회원 ID 1과 지역 데이터가 있는 별도 테스트 DB에서 multipart 신청, 목록/상세, 승인 후 공개 목록 노출, 반려 사유 저장, 동시 심사와 파일 저장 실패 시 고아 파일 처리를 검증한다.

## 2026-10-22 — 계모임 목록 공통 응답 형식 정정

- 목적: 목록 조회 실패를 해결하기 위해 `GET /api/saving-groups` 응답을 공통 목록 형식으로 통일한다.
- 사용자 선택: `data`에는 카드 배열, `meta`에는 `PageMeta`를 반환하는 방식을 선택했다.
- AI 제안·작성: Controller가 Service 결과의 `items`와 `pageMeta`를 `ApiResponse.ok(items, pageMeta)`로 반환하도록 정정하고, 프론트 API 테스트와 API 명세를 같은 구조로 맞췄다.
- 개발자 검토·수정: 최종 검토 대기.
- 데이터·외부 서비스·의존성·설정 변경: 없음.
- 검증 결과: `node --test src/api/savingGroups.test.js` 7개 통과. JDK 17을 지정한 `gradlew.bat test --offline`도 통과.

## 2026-10-20 — 계모임 공개 상세 조회 구현

<!-- AI작성: 구현 중 사용자 정정·승인과 실제 검증 결과를 기록한다. -->

- 목적: GET /saving-groups/{group_id}와 SavingGroupDetailPage를 실제 공개 API로 연결한다.
- 사용자 최종 확정: 가입 이력은 group_member.membership_status에서 관리한다.
  기존 OWNER/MANAGER/MEMBER·APPROVED 코드를 유지하고 JWT 연결은 후속으로 남긴다.
- AI 제안·작성: Controller/Service/Mapper 상세 확장, DTO 2개, Long 양수 검증·404,
  승인 인원 중복 제거·공지 최신 네 제목·역할 조회 준비, 목록 groupId 추가,
  공개 상세 UI·로딩/오류/취소/재시도·금액 포맷·역할별 탭, 정상·실패 테스트와 API 문서.
- 개인정보·범위: 공개 응답만 제공하며 모든 HTTP 요청의 myRole은 null.
  검증되지 않은 사용자 ID·역할 입력은 사용하지 않는다. Mock 업무 패널은 준비 중 안내로 대체한다.
- 개발자 검토·수정: 사용자 설계 정정은 반영. 구현 코드의 개발자 직접 검토 여부는 아직 확인되지 않음.
- 데이터·외부 서비스: 실제 DB 조회·변경, 금융 API, JWT 구현, 의존성 추가·설정 변경 없음.
  기존 Mapper XML의 사용자 수정도 보존함.
- 검증 결과: JDK 17 Gradle 대상 테스트 37개 통과, 별도 MySQL 환경 미설정으로 5개 항목 건너뜀.
  Node 테스트 8개 통과(실제 상세 JSX 렌더 포함), lint·프로덕션 빌드 통과.
- 테스트 보완 과정: Controller 실패 테스트의 Mockito 재설정을 doThrow로 수정하고,
  Node SSR의 styled-components 기본 내보내기 차이는 테스트 전용 변환으로 해결함.
- 남은 직접 확인: 실제 MySQL SQL 실행, 브라우저/Postman 통합·모바일, JWT 후속 연결,
  실제 스키마·인덱스·격리 수준·계주 승인 등록·중복 가입·BIGINT 응답 정밀도.
- 파일별 변경 이유·재현 방법: [계모임 API 명세](saving-groups-api.md)의 상세 확장 절.
## 2026-10-22 — 계주 전용 실제 신청 목록 및 프론트 개별 승인 연결

- 목적: Mock 신청 ID로 실제 승인을 호출하지 않고 계주 전용 목록 응답의 crewMemberId로 개별 승인한다.
- 사용자 승인: 실제 목록 API를 먼저 연결하는 권장안, 공유한 17개 파일·개별 승인 우선·선택 승인/거절 비활성화·JWT TODO 유지 범위에 대해 수정 승인받았다.
- AI 작성: 권한 검증을 재사용하는 목록 Service/Controller, 상태·페이지 검증, 마스킹 SQL·문자열 ID·공통 meta DTO, 단위/Controller/선택 실행 MySQL 테스트, 프론트 목록·승인 함수·오류 보존·API 테스트, 화면 로딩/오류/빈 결과/재시도·개별 승인·중복 클릭 차단·승인 후 재조회.
- 개인정보: SQL에서 이름을 마스킹하고 회원 ID·연락처·계좌정보를 반환하지 않는다. 지역·인증 완료 여부·기존 신청 메시지만 제공한다. 자유 입력 메시지의 자동 개인정보 탐지 정책은 별도 범위다.
- 권한·데이터: groups.member_id와 OWNER + APPROVED 관계 모두 확인한다. 목록은 해당 모임의 PENDING/APPROVED/REJECTED 중 OWNER·삭제 회원을 제외한다. LEFT/EXPELLED는 포함하지 않는다. 목록 조회는 기존 잠금 조회를 재사용한 트랜잭션으로 처리한다.
- 프론트: 가입 신청 Mock import와 로컬 승인/거절 상태 변경을 제거했다. 서버 ID를 문자열로 유지하고 승인 요청 본문을 보내지 않는다. 화면 그룹 변경 시 패널을 재생성하고 이전 조회 취소·응답 무시를 적용했다. 미구현 선택 승인·거절·상세 보기는 비활성화했다.
- 검증: Java 17 컴파일·테스트 컴파일 및 Controller 31개, 승인 Service 12개, 목록 Service/실제 XML 파싱·바인딩 22개, 총 65개 통과. 프론트 API 테스트 15개, lint·build·git diff --check 통과. 기존 번들 크기 경고는 유지한다.
- 미검증: 별도 MySQL 접속 및 스키마 생성 허용 설정이 없어 MySQL 통합 테스트 9개를 건너뛰었다. 실제 SQL 실행·DB 마스킹 결과·잠금·동시성·브라우저 클릭 동작·실제 JWT 통합은 미검증이다. 단위 테스트의 성공을 실제 통합 성공으로 보고하지 않는다.
- 남은 TODO: JWT 브랜치 병합과 GET 두 경로/PATCH 두 경로 인증 등록·공통 client 토큰 제공, 실제 MySQL 및 브라우저 검증, 거절/선택 승인 API 정책, 기존 상세 페이지의 Mock 모임 정보·존재 여부 판정을 실제 조회로 교체. Mock에 없는 실제 모임 ID는 아직 상위 상세 페이지에서 차단된다. 기존 계금 요청 Mock은 별도 탭에 유지한다.
- 변경하지 않음: schema.sql·공용 DB·인증 구현·의존성·설정·외부 서비스. git commit은 수행하지 않았으며 개발자 최종 검토 대기다.
- 파일별 목록·API 계약·재현 방법: saving-groups-api.md의 '계주 전용 가입 신청 목록과 프론트 승인 연결' 절.

## 2026-10-22 — 가입 승인 3단계: Controller 연결 (JWT 연동 TODO)

- 목적: 기존 승인 Service를 PATCH API에 연결하고 공통 응답·예외 처리 계약을 검증한다.
- 사용자 지시·승인: 다른 브랜치의 JWT 로그인은 병합하지 않고 TODO 주석으로 남겨 제외한 상태로 구현. 안내한 Controller·신규 테스트·API 명세·본 기록 4개 파일 범위에서 작업했다.
- AI 작성: 두 Controller 기본 경로 아래 /{savingGroupId}/join-requests/{crewMemberId}/approve 추가, 서버 request attribute memberId를 계주 ID로 사용, 인증 속성 누락·0 이하 차단, 기존 승인 Service와 ApiResponse.ok 연결, 테스트·명세 작성.
- 인증 확인: 다른 브랜치 LoginInterceptor가 JWT 검증 후 memberId(Long) 요청 속성을 설정한다. 현재 브랜치에는 인증 구현이 없으며 이를 임의로 새로 만들거나 Principal 형식을 가정하지 않았다.
- TODO: JWT 브랜치 병합 후 두 승인 경로에 LoginInterceptor를 등록하고 공개 GET은 유지한다. 실제 JWT·만료·변조·비로그인·경로별 보호를 통합 검증하고 인증 브랜치 오류 코드도 명세에 반영한다.
- 현재 동작 제한: JWT 미연결 상태에서는 실제 HTTP 요청에 인증 속성이 없어 401을 반환한다. 정상 승인 응답 테스트는 테스트 전용 requestAttr 주입으로 인증 완료 상황을 모사한다. 클라이언트 입력으로 인증을 우회하거나 임시 회원 ID를 사용하지 않는다.
- 검증: Java 17 컴파일·테스트 컴파일 통과. Controller 테스트 24개와 기존 Service 단위 테스트 12개, 총 36개 통과. git diff --check 통과.
- 테스트 내용: 두 경로 본문 없는 승인 성공·공통 응답, 인증 정보 없음·잘못된 값의 차단, 클라이언트 ID 무시, 문자열·Long 범위 초과·0·음수 경로, 권한·대상 없음·승인 불가 상태 등의 오류 응답.
- 미검증: 실제 MySQL 테스트 7개는 별도 테스트 서버 환경설정이 없어 건너뛰었다. JWT 검증, 실제 앱 기동·Postman 성공 승인, 실제 DB 저장·롤백·동시성은 미검증이다.
- 변경하지 않은 범위: 인증 구현·WebConfig·Service·Mapper·DB 스키마·공용 DB·공통 응답/예외·의존성·설정. 실제 git commit은 수행하지 않았다. 개발자 최종 검토 대기.
- 공식 근거: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestattrib.html (서버가 미리 설정한 요청 속성 사용).

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

## 2026-10-22 — 계모임 기능 브랜치 병합 충돌 해결

- 목적: 계모임 개설 신청·승인, 목록·상세 조회, 정보 수정, 가입 신청 기능을 한 브랜치에서 함께 동작하도록 충돌을 통합한다.
- AI 제안·작성: Settings와 가입 신청의 Mapper·Service 계약 및 SQL을 모두 보존하고,
  상세 응답에 `savingGroupId`를 포함해 `groupId` 기반 상세 URL과 가입 신청 API의 식별자를 구분했다.
  또한 실제 가입 신청 POST 엔드포인트와 프론트의 요청 중·성공·실패 상태를 연결했다.
- 개발자 검토·수정: 최종 검토 대기. 인증 연결 전에는 기존 설정 API와 같은 임시 회원 ID를 사용한다.
- 데이터·외부 서비스·의존성·설정 변경: 없음.
- 검증 결과: `node --test src/api/savingGroups.test.js` 9개 통과, `npm run build` 통과.
  백엔드 Gradle 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 컴파일 전 중단됐다.
- 직접 확인 필요: Java 17을 준비한 뒤 `back\\gradlew.bat test` 실행 및 MySQL에서
  가입 신청의 잠금·중복·정원 검증, JWT 병합 후 실제 로그인 회원 ID 전달을 확인한다.

## 2026-10-22 — 계모임 가입 신청 승인 기능 병합 충돌 해결

- 목적: 기존 가입 신청 등록·상세·설정 흐름을 보존하면서 계주 전용 신청 목록 조회와 개별 승인 기능을 통합한다.
- AI 제안·작성: Controller·Service·Mapper 계약을 함께 병합하고, 신청 목록은 마스킹된 정보와 문자열 신청 ID만 반환하며,
  승인은 `crewMemberId`와 PENDING 상태를 다시 검증한 뒤에만 처리하도록 유지했다.
- 개발자 검토·수정: 최종 검토 대기. 신청 목록과 승인은 인증 인터셉터가 `memberId` 요청 속성을 제공할 때만 동작한다.
- 데이터·외부 서비스·의존성·설정 변경: 없음.
- 검증 결과: `node --test src/api/savingGroups.test.js` 22개 통과, `npm run build` 통과.
  백엔드 Gradle 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 준비 후 `back\\gradlew.bat test` 실행, JWT 병합 뒤 계주·비계주·인증 없음 요청과
  MySQL에서 동시 승인·정원·최대 가입 수 조건을 통합 검증한다.

## 2026-10-22 — 계모임 서비스 병합 문법 오류 수정

- 목적: 가입 신청 승인 기능 병합 뒤 발생한 `SavingGroupServiceImpl` 컴파일 오류를 해결한다.
- AI 제안·작성: `getEditableFields` 메서드의 누락된 종료 중괄호를 복구해 가입 신청 메서드가 클래스 범위에 선언되도록 수정했다.
- 검증: `git diff --check` 실행. Gradle 테스트는 실행 환경의 Java 17 toolchain 부재로 컴파일 전에 중단된다.

## 2026-10-22 — 계모임 패키지 및 Spring 빈 이름 통합

- 목적: `saving_group`과 `savingGroup`에 중복된 Controller·Service·Mapper 이름 때문에 발생한 Spring 빈 등록 충돌을 해소한다.
- AI 제안·작성: 기존 `savingGroup` 패키지로 개설 요청 DTO를 옮기고, 개설 전용 Controller·Service·ServiceImpl을
  `SavingGroupCreation*`으로 분리했다. 개설 Mapper 메서드는 기존 `SavingGroupMapper`와 XML namespace로 통합하고,
  XML의 생성 키 프로퍼티를 Java 필드명 `groupId`로 정정했으며 테스트의 DTO 참조도 함께 갱신했다.
- 개발자 검토·수정: 최종 검토 대기. API URL과 DB 스키마는 변경하지 않았다.
- 데이터·외부 서비스·의존성·설정 변경: 없음.
- 검증: `git diff --check`, Gradle 컴파일 및 테스트 실행 결과를 확인해야 한다.
