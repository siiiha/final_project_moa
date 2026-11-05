<!-- AI작성: 기존 소모임 구현과 사용자 확정 정책을 기준으로 작성한 상세·설정·가입 신청·투표·후기 API 설계 문서입니다. -->
# 소모임 상세·공지·설정·가입 신청·투표·후기 API 설계

<!-- AI작성: 소모임 생성 지역 선택 2단계의 화면 동작과 기존 요청 연결을 기록한다. -->
<!-- AI작성: 공지 구현 이후의 최종 계약을 먼저 안내한다. -->
## 공지 최종 적용 안내 (2026-11-04)

공지 작성·목록·상세·수정·논리 삭제·고정 기능을 구현했다. 메인 공지 영역은 일반 공지 4건 페이징과 고정 공지 최대 2건을 분리한다. 전체 보기 버튼·별도 전체 목록 화면은 없다. 목록 요약은 공개하며 본문은 승인 OWNER·MEMBER만 조회한다. 작성·고정은 해당 소모임장, 수정·삭제는 현재 해당 소모임장이면서 작성자 본인만 가능하다. 상세한 변경 파일·실제 커밋명·검증 결과와 수동 확인 방법은 [공지 작업 기록](small-group-notice-work-log.md)을 따른다. 아래 단계별 작성 기록은 구현 당시 기록이며 최종 계약은 이 안내와 문서 하단의 공지 API 절을 기준으로 한다.

## 생성 화면 지역 선택 (2026-11-03, 2단계)

- 생성 화면은 계모임·검색과 같은 `/api/auth/regions`에서 상위 지역 정보를 조회한다. 기존 `/api/small-groups/regions` 계약은 변경하지 않는다.
- 시·도와 시·군·구는 필수, 읍·면·동은 선택이다. 시 변경 시 구·동, 구 변경 시 동을 초기화한다.
- 상위 선택 전 하위 선택을 비활성화하고 세종은 구 단계에서 `세종특별자치시 전체`를 선택한다.
- 최종 구 또는 동의 regionName을 폼의 기존 region 필드로 전달해 POST `/api/small-groups`의 regionName 계약을 유지한다.
- 조회 중·실패·빈 목록에서는 생성을 막고 실패 시 다른 입력을 유지하면서 지역 조회를 재시도한다.
- 동 데이터가 없으면 동을 비활성화하고 구 단위 생성을 허용한다. 실제 지역 데이터 추가는 이번 범위에 포함하지 않는다.
- 이번 작업은 화면 단계다. 서버 지역 검증 강화·요청 테스트는 3단계, 저장 후 상세·새로고침 확인은 4단계다.

## 범위

- 엔드포인트: `GET /api/small-groups/{id}`
- `{id}`는 `small_group.small_group_id`이며 양수여야 한다.
- 소모임 상세 자체는 비로그인 사용자와 비회원도 조회할 수 있다.
- 공지 일반 목록·고정 목록의 요약과 페이지 메타를 포함한다. 공지 본문은 별도 인증 API에서 조회하며 일정 목록·가입 신청 내역은 포함하지 않는다.

## 요청과 응답

요청 본문은 없다. 응답은 기존 공통 응답 형식인 `ApiResponse`를 사용한다.

```json
{
  "success": true,
  "data": {
    "smallGroupId": 1,
    "groupId": 10,
    "groupName": "주말 걷기 모임",
    "description": "함께 걷고 이야기하는 소모임입니다.",
    "regionName": "강남구",
    "likeCategory": "운동",
    "meetDate": "2026-11-01",
    "location": "강남역 1번 출구",
    "maxMembers": 10,
    "memberCount": 4,
    "creatorNickname": "모아회원",
    "createdAt": "2026-10-28T10:00:00",
    "myRole": "MEMBER",
    "notices": [
      {
        "noticeId": 21,
        "title": "첫 모임 안내",
        "writerNickname": "운영진",
        "createdAt": "2026-10-28T10:00:00"
      }
    ],
    "pinnedNotices": []
  },
  "meta": { "page": 1, "size": 4, "total": 1, "totalPages": 1 }
}
```

`myRole`은 로그인한 요청자의 승인된 소모임 역할만 반환한다. 비로그인 사용자, 비회원, `PENDING` 또는 `REJECTED` 상태는 `null`이다.

`notices`는 삭제·고정 공지를 제외한 일반 공지 한 페이지(최대 4건)다. `noticeSort=LATEST|OLDEST`로 작성일·공지 ID 순서를 선택한다. `pinnedNotices`는 일반 목록과 별도로 최신순 고정 공지 요약을 제공한다. 비회원도 제목·작성자·작성일은 볼 수 있지만 본문과 상세 이동은 APPROVED OWNER·MEMBER만 가능하다. 응답 `meta`는 일반 공지만 집계한 PageMeta다.

## 권한과 관계 데이터 검증

| 조건 | 처리 |
| --- | --- |
| 비로그인 또는 비회원 | 상세 조회 성공, `myRole: null` |
| `APPROVED` 상태의 `OWNER` 또는 `MEMBER` | 상세 조회 성공, 해당 역할 반환 |
| `PENDING` 또는 `REJECTED` 상태의 `OWNER` 또는 `MEMBER` | 상세 조회 성공, `myRole: null` |
| 상태와 무관한 `MANAGER` 관계 | `403 ACCESS_DENIED` |
| 한 사용자·소모임 관계가 2건 이상 존재 | `500 INTERNAL_ERROR` |

참여자 수는 `APPROVED` 상태의 `OWNER`와 `MEMBER`만 중복 없이 계산한다. `MANAGER`는 참여자 수에 포함하지 않는다.

## 조회 대상과 예외

| 상황 | 응답 |
| --- | --- |
| 소모임이 없거나 소모임·개설자 삭제 상태 | `404 NOT_FOUND` |
| 요청자 관계에 `MANAGER`가 존재 | `403 ACCESS_DENIED` |
| 요청자 관계 데이터가 중복되었거나 알 수 없는 역할값 | `500 INTERNAL_ERROR` |

인증 헤더는 필수가 아니다. 다만 잘못되었거나 만료된 토큰의 처리는 기존 JWT 공통 예외 처리 규칙을 따른다.

## 보안 경로

Spring Security에서 소모임 전체 목록, 숫자 ID의 `GET /api/small-groups/{id}`, `GET /api/small-groups/{smallGroupId}/reviews`를 공개한다. 같은 후기 경로의 POST 등록과 설정·투표·가입 신청 등 보호 경로에는 기존 인증 정책을 적용한다. 인증 헤더가 있으면 공개 경로에서도 JWT·세션 검증을 수행한다.

## 상세 화면 탭

| 조회자 역할 | 노출 탭 |
| --- | --- |
| 비로그인·비회원·미승인·모임 탈퇴 회원 | 공지, 후기 |
| `MEMBER` | 공지, 장소 찾기, 투표, 후기 |
| `OWNER` | MEMBER 탭, 설정, 신청 목록 |
| `MANAGER` | API에서 403 처리하므로 화면을 표시하지 않음 |

비회원에게도 모임 날짜와 장소를 포함한 기본 상세 정보를 노출한다.

<!-- AI작성: 장소 검색 구현에 맞춰 탭 문구와 후속 기능 범위를 갱신한다. -->
장소 찾기는 승인된 OWNER·MEMBER만 사용할 수 있으며 내부 탭 식별자 `directions`를 유지한다.
검색 계약과 위치 이용 흐름은 [소모임 장소 검색 API와 화면](small-group-places-api.md)을 따른다.
길찾기와 검색 결과의 모임 장소 저장은 후속 작업이다.

## 데이터와 검증

- 기존 `small_group`, `groups`, `group_member`, `members`, `region`, `group_notice` 및 SmallGroup MyBatis Mapper를 재사용한다.
- 상세 조회만을 위해 DB 마이그레이션, 컬럼, 상태값은 추가하지 않는다.
- 서비스 단위 테스트는 정상 조회, 비회원, 승인 회원, 대기 회원, MANAGER 403, 중복 관계 500, 없는 소모임 404, 최근 공지 요약과 빈 공지 목록을 다룬다.
- 보안 통합 테스트는 비로그인 상세 조회 허용과 보호 경로의 인증 요구를 확인한다.
- 프론트 API는 최근 공지에서 공지 ID·제목·작성자 닉네임·작성일만 화면으로 전달하며, 필수 필드가 없으면 `INVALID_RESPONSE`로 처리한다.

## 수동 확인

1. 비로그인 상태에서 `GET /api/small-groups/{id}`가 200이고 `myRole`이 `null`인지 확인한다.
2. OWNER, MEMBER, 비회원, PENDING/REJECTED, MANAGER 계정으로 각각 조회해 응답과 오류 코드를 확인한다.
3. 프론트 상세 화면에서 역할별 탭, 모임 날짜·장소, 최근 공지 0·1·4건 표시를 확인한다.
4. 공지 전체 보기 버튼이 제거되고 메인에서 정렬·페이지 이동이 가능한지 확인한다. 일정 확인·길찾기는 기존 준비 중 상태를 유지한다.
5. Java 17 환경에서 백엔드 소모임 테스트를 실행한다.

---

## 소모임 가입 신청 API

### 범위

- 엔드포인트: `POST /api/small-groups/{smallGroupId}/join-requests`
- `{smallGroupId}`는 `small_group.small_group_id`이며, 요청 본문은 없다.
- Spring Security와 JWT 필터가 인증한 회원 ID만 사용하며, 클라이언트는 회원 ID·신청 메시지·추가 입력값을 전달하지 않는다.
- 신청 성공은 가입 완료가 아닌 `group_member`의 `MEMBER`·`PENDING` 관계 생성 또는 재신청 갱신이다.
- 소모임 가입 신청 승인·거절과 OWNER의 신청 목록 조회는 아래 별도 API 범위에서 처리한다. 취소·일괄 처리는 포함하지 않는다.

### 성공 응답

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 가입 가능 검증과 저장

Service는 하나의 트랜잭션에서 회원, 소모임, 기존 관계, 승인 인원을 순서대로 잠가 조회한다.

1. 회원이 활성 상태이고 본인 인증을 완료했는지 확인한다.
2. `system_role`이 `ADMIN`이면 신청을 차단한다.
3. 삭제되지 않은 소모임인지와 정원 설정 여부를 확인한다.
4. 개설자, `OWNER`, `MANAGER` 관계면 본인 소모임 신청을 차단한다.
5. 기존 관계 상태를 확인한다.
   - `APPROVED`, `PENDING`, `REJECTED`, `EXPELLED`은 신청을 차단한다.
   - `LEFT`만 기존 행을 `PENDING`으로 되돌려 재신청을 허용한다.
6. 승인된 `OWNER`, `MEMBER` 수가 정원보다 적은지 확인한다.
7. 신규 신청은 `group_member`에 `MEMBER`, `PENDING`, `requested_at`을 저장한다.

`LEFT` 재신청 시에는 이전 `approved_by`, `approved_at`, `joined_at`, `left_at`, `application_message`를 초기화한다. 유니크 제약 충돌은 서버 오류가 아닌 중복 신청으로 처리한다.

### 오류 응답

| 상황 | 응답 |
| --- | --- |
| 인증 정보 없음 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임 ID가 0 이하 | `400 BAD_REQUEST` |
| 비활성·정지·탈퇴 회원 | `403 MEMBER_ACCOUNT_UNAVAILABLE` |
| 본인 인증 미완료 | `403 IDENTITY_VERIFICATION_REQUIRED` |
| 관리자 계정 | `403 SMALL_GROUP_JOIN_ADMIN_NOT_ALLOWED` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 개설자, OWNER, MANAGER 신청 | `403 SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED` |
| 이미 가입한 MEMBER | `409 ALREADY_JOINED_SMALL_GROUP` |
| 대기 신청이 이미 있음 | `409 JOIN_REQUEST_ALREADY_PENDING` |
| REJECTED·EXPELLED 재신청 | `409 JOIN_REAPPLICATION_FORBIDDEN` |
| 정원 미설정 | `409 SMALL_GROUP_CAPACITY_NOT_CONFIGURED` |
| 정원 도달 | `409 SMALL_GROUP_CAPACITY_FULL` |
| 저장 행 수 불일치 | `500 JOIN_REQUEST_CREATE_FAILED` |
| 유니크 제약 충돌 | `409 JOIN_REQUEST_DUPLICATED` |

### 프론트 연결

- `requestSmallGroupJoin(smallGroupId)`는 요청 본문 없이 API를 호출하고 서버 오류 코드·메시지를 그대로 보존한다.
- 소모임 상세에서는 승인된 `OWNER`, `MEMBER`에게 가입 신청 버튼을 노출하지 않는다.
- 그 외 역할·비회원에게는 버튼을 표시하되, 실제 권한·중복·정원 판단은 서버 응답을 기준으로 한다.
- 성공 시 완료 모달을 표시하고, 본인 인증 미완료는 인증 화면 이동 모달로 안내하며, 나머지 실패는 서버 메시지로 표시한다.

### 데이터·후속 TODO

- 기존 `members`, `groups`, `small_group`, `group_member`와 SmallGroup MyBatis Mapper를 재사용하며 DB 마이그레이션을 추가하지 않는다.
- 소모임에는 모집 상태, 공개 범위, 자동 승인 여부 데이터가 없으므로 해당 가입 제한을 임의로 추가하지 않는다.
- 신청 메시지는 `group_member.application_message` 컬럼이 있어도 이번 API에서 입력·저장하지 않는다.
- OWNER 전용 가입 신청 목록 조회·개별 승인·개별 거절은 각각 `GET /api/small-groups/{smallGroupId}/join-requests`, `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/approve`, `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/reject`로 제공한다. 취소·일괄 처리 정책은 별도 범위에서 결정한다.

### 테스트와 수동 확인

- Service 단위 테스트는 정상 신청, 계정·본인 인증·관리자·개설자·OWNER·MANAGER·기존 상태·정원·중복 키 예외를 검증한다.
- Security 통합 테스트는 비로그인 `POST` 요청의 `401`을 검증한다.
- 프론트 API 테스트는 본문 없는 POST 요청과 서버 오류 보존을 검증한다.
- 브라우저에서 비회원, 일반 회원, OWNER, 이미 신청한 회원, 본인 인증 미완료 회원, 관리자 계정으로 버튼·모달·오류를 확인한다.

---

## 소모임 개설자 전용 가입 신청 목록 API

### 범위

- 엔드포인트: `GET /api/small-groups/{smallGroupId}/join-requests`
- `{smallGroupId}`는 `small_group.small_group_id`이며 양수여야 한다.
- JWT 필터가 인증해 request attribute에 저장한 회원 ID만 사용한다. 클라이언트는 회원 ID를 전달하지 않는다.
- 소모임의 `groups.member_id`와 일치하고, `group_member`에서 `OWNER`·`APPROVED` 관계인 개설자만 조회할 수 있다.
- `MANAGER`를 포함한 운영진은 조회 권한이 없다. 상세 화면의 신청 목록 탭도 `OWNER`에게만 노출한다.
- 목록 조회는 읽기 전용이다. 개별 승인·거절 API는 아래 별도 범위에서 제공하며, 취소·일괄 처리는 포함하지 않는다.

### 요청

| 파라미터 | 타입 | 규칙 |
| --- | --- | --- |
| `smallGroupId` | Long 경로 변수 | 양수, `small_group.small_group_id` |
| `status` | String 쿼리 | 생략하면 전체, `PENDING` / `APPROVED` / `REJECTED`만 허용 |
| `page` | int 쿼리 | 기본 1, 1 이상, 고정 페이지 크기 10 |

기존 계모임 가입 신청 목록과 동일하게 검색어와 페이지 크기 쿼리 파라미터는 제공하지 않는다.

### 성공 응답

기존 `ApiResponse`의 `data` 배열과 `meta` 페이징 구조를 사용한다.

```json
{
  "success": true,
  "data": [{
    "crewMemberId": "9007199254740993",
    "applicant": "가**",
    "regionName": "서울",
    "identityVerified": true,
    "regionVerified": false,
    "applicationMessage": "함께하고 싶습니다.",
    "membershipStatus": "PENDING"
  }],
  "meta": {
    "page": 1,
    "size": 10,
    "total": 1,
    "totalPages": 1,
    "offset": 0
  },
  "error": null
}
```

- `crewMemberId`는 BIGINT 정밀도 손실 방지를 위해 문자열이다.
- `applicant`는 SQL에서 첫 글자를 제외한 나머지를 `*`로 마스킹한다. 회원 ID·연락처·생년월일·계좌 정보·인증 시각 원문은 반환하지 않는다.
- `applicationMessage`는 기존 `group_member.application_message`만 반환한다. 관심사 정보는 데이터 구조에 없으므로 목록에 추가하지 않는다.

### 조회 기준·정렬·오류

- 기존 `small_group`, `groups`, `group_member`, `members`, `region`과 `SmallGroupMapper`만 재사용하며 DB 마이그레이션·컬럼·상태값은 추가하지 않는다.
- `PENDING`, `APPROVED`, `REJECTED` 관계만 포함하고 `OWNER`, `LEFT`, `EXPELLED`, 삭제 회원은 제외한다.
- 목록과 전체 수에 같은 조건을 적용하며 `requested_at DESC, crew_member_id DESC`로 정렬한다.
- 결과가 없거나 범위 초과 페이지면 성공 응답의 빈 배열을 반환한다.

| 상황 | 응답 |
| --- | --- |
| 인증 정보 없음 | `401 UNAUTHORIZED` |
| 소모임 ID·신청 상태·페이지가 올바르지 않음 | `400 BAD_REQUEST` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 개설자가 아니거나 승인 OWNER 관계가 아님 | `403 SMALL_GROUP_OWNER_REQUIRED` |

### 프론트 연결·검증

- `getSmallGroupJoinRequests(smallGroupId, { status, page, signal })`는 서버 필터·정렬·페이징을 그대로 사용하고 Mock 데이터나 클라이언트 재정렬을 하지 않는다.
- OWNER 신청 목록 탭은 로딩·오류·재시도·빈 결과·페이지 이동을 표시한다. 개별 승인·거절 성공 뒤에는 서버 목록을 다시 조회하며, 선택 승인은 비활성으로 표시한다.
- Service 테스트는 정상·빈 결과·페이지·상태 필터·비개설자·없는 소모임·비로그인·잘못된 상태·페이지를 검증한다. Controller와 Security 테스트는 JWT request attribute 전달, 공통 응답, 인증 누락을 검증한다.
- 실제 MySQL과 JWT 환경에서 개설자·비개설자·MANAGER·삭제 소모임, 이름 마스킹·정렬·범위 초과 페이지를 확인해야 한다.

---

## 소모임 개설자 전용 가입 신청 승인 API

### 범위

- 엔드포인트: `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/approve`
- 요청 본문·쿼리 파라미터는 없으며, `{smallGroupId}`는 `small_group.small_group_id`, `{crewMemberId}`는 `group_member.crew_member_id`다.
- JWT 필터가 request attribute에 저장한 인증 회원 ID만 승인자로 사용한다. 클라이언트가 회원 ID·처리자·역할을 전달할 수 없다.
- `groups.member_id`가 로그인 회원과 일치하고 `group_member`에 `OWNER`·`APPROVED` 관계가 있는 개설자만 승인할 수 있다. `MANAGER` 등 운영진에게는 승인 권한을 부여하지 않는다.

### 성공 응답

성공 시 기존 계모임 승인 API와 같이 본문 없이 공통 성공 봉투를 반환한다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 승인 검증과 저장

Service는 하나의 트랜잭션에서 신청 식별 → 신청 회원 → 소모임 → 신청 행 재조회 순서로 잠가 최신 상태를 검증한다.

1. 가입 신청, 신청 회원, 삭제되지 않은 소모임이 존재하는지 확인한다.
2. 잠금 재조회한 신청의 회원이 최초 식별한 회원과 같고, 해당 소모임에 속하는지 확인한다.
3. 개설자와 승인된 `OWNER` 관계를 함께 확인한다.
4. 신청 상태가 `PENDING`이고, 소모임 정원이 양수이며 승인된 `OWNER`·`MEMBER` 수가 정원보다 적은지 확인한다.
5. `crew_member_id`, `group_id`, `PENDING` 조건이 모두 일치하는 기존 `group_member` 행 하나만 `APPROVED`, `MEMBER`로 갱신한다. `joined_at`, `approved_at`, `approved_by`도 함께 저장한다.

소모임에는 모집 상태와 가입 모임 수 제한 정책이 없으므로 승인 조건에 추가하지 않는다. 기존 신청 행을 갱신하므로 별도 회원 행을 INSERT하지 않는다.

### 오류

| 상황 | 응답 |
| --- | --- |
| 인증되지 않은 HTTP 요청 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임·신청 번호가 올바르지 않음 | `400 BAD_REQUEST` |
| 신청 회원이 없거나 삭제됨 | `404 JOIN_REQUEST_MEMBER_NOT_FOUND` |
| 소모임이 없거나 삭제됨 | `404 SMALL_GROUP_NOT_FOUND` |
| 신청이 없거나 다른 소모임 소속 | `404 JOIN_REQUEST_NOT_FOUND` |
| 개설자가 아니거나 승인 OWNER 관계가 아님 | `403 SMALL_GROUP_OWNER_REQUIRED` |
| 신청이 이미 처리됨 | `409 JOIN_REQUEST_NOT_PENDING` |
| 정원이 없거나 가득 참 | `409 SMALL_GROUP_CAPACITY_NOT_CONFIGURED` 또는 `409 SMALL_GROUP_CAPACITY_FULL` |
| 동시 변경으로 조건부 갱신이 실패함 | `409 JOIN_REQUEST_APPROVAL_FAILED` |

### 프론트 연결·후속 TODO

- `approveSmallGroupJoinRequest(smallGroupId, crewMemberId)`는 두 식별자만 경로로 보내고 서버 오류 코드·메시지를 그대로 보존한다.
- OWNER 신청 목록은 실제 GET API 데이터만 렌더링하며 승인·거절 성공 후 서버 목록을 재조회한다. 로컬 Mock 상태를 처리 결과로 바꾸지 않는다.
- 취소, 선택 승인, 알림 생성, 역할 추가 부여, 회비 설정은 이번 범위에 포함하지 않는다.

### 테스트와 수동 확인

- Service 테스트는 정상 승인, 비개설자, 소모임·신청 없음, 다른 소모임 신청, 처리 완료 신청, 정원 초과, 조건부 갱신 실패를 검증한다.
- Controller·Security 테스트는 인증 ID 전달, 공통 성공·업무 오류 응답, 잘못된 경로 ID, 비로그인 401을 검증한다.
- 실제 MySQL에서는 승인 후 `group_member`의 상태·역할·승인 처리자·시각, 정원 경합과 트랜잭션 롤백을 확인한다.

---

## 소모임 설정 정보 조회 API

### 범위

- 엔드포인트: `GET /api/small-groups/{id}/settings`
- `{id}`는 `small_group.small_group_id`이며 양수여야 한다.
- 설정 조회는 공개 API가 아니다. JWT 인증을 통과한 뒤, 소모임의 개설자이면서 `OWNER`·`APPROVED` 관계인 사용자만 조회할 수 있다.
- 소모임에는 모집·진행·완료 상태 정책이 없으므로 상태에 따른 조회 제한은 두지 않는다.

### 성공 응답

```json
{
  "success": true,
  "data": {
    "groupName": "주말 걷기 모임",
    "likeCategory": "운동",
    "description": "함께 걷고 이야기하는 소모임입니다.",
    "maxMembers": 10,
    "meetDate": "2026-11-01",
    "location": "한강공원",
    "approvedMemberCount": 4
  },
  "meta": null,
  "error": null
}
```

`approvedMemberCount`는 `APPROVED` 상태인 `OWNER`, `MEMBER`만 중복 없이 집계한다. 이후 정원 축소 요청의 프런트 1차 검증과 서버 최종 검증 기준으로 사용한다.

### 권한·오류

| 상황 | 응답 |
| --- | --- |
| 인증 정보 없음 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임이 없거나 `groups.deleted_at`이 설정됨 | `404 SMALL_GROUP_NOT_FOUND` |
| 로그인 회원이 개설자가 아니거나 승인 OWNER가 아님 | `403 SMALL_GROUP_SETTINGS_FORBIDDEN` |
| `id`가 0 이하 또는 숫자가 아님 | `400 BAD_REQUEST` |

### 데이터와 테스트

- 기존 `small_group`, `groups`, `group_member`와 `SmallGroupMapper`를 재사용하며 DB 마이그레이션은 추가하지 않는다.
- Service 단위 테스트는 정상 조회, 비로그인, 없는·삭제된 소모임, 비개설자, 승인 OWNER 관계가 없는 개설자를 검증한다.
- Controller 테스트는 JWT 요청 속성 전달, 공통 성공·업무 오류 응답, 양수가 아닌 경로 ID를 검증한다.
- Security 통합 테스트는 설정 조회 API가 인증 없이는 접근할 수 없음을 검증한다.

---

## 소모임 설정 수정 API

### 범위

- 엔드포인트: `PATCH /api/small-groups/{id}`
- `{id}`는 `small_group.small_group_id`이며 양수여야 한다.
- 설정 조회와 같이 JWT 인증을 통과한 개설자이면서 `OWNER`·`APPROVED` 관계인 사용자만 수정할 수 있다.
- `groups.deleted_at`이 설정된 소모임은 수정할 수 없다.
- 소모임에는 모집·진행·완료 상태 정책이 없으므로 상태에 따른 수정 제한을 두지 않는다.

### 요청과 성공 응답

요청 본문에는 실제로 바꿀 필드만 포함한다. 빈 객체는 허용하지 않는다.

```json
{
  "groupName": "주말 걷기 모임",
  "likeCategory": "운동",
  "description": "함께 걷고 이야기하는 소모임입니다.",
  "maxMembers": 10,
  "meetDate": "2026-11-01",
  "location": "한강공원"
}
```

성공 시 기존 계모임 설정 수정과 동일하게 데이터를 반환하지 않는다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 수정 가능 필드와 검증

| 필드 | 검증 |
| --- | --- |
| `groupName` | 공백 제거 후 2~20자 |
| `likeCategory` | 공백 불가, 최대 50자 |
| `description` | 공백 제거 후 최대 40자 |
| `maxMembers` | 2~20명 |
| `meetDate` | 오늘 이후, 즉 내일부터 |
| `location` | 공백 불가, 최대 40자 |

식별자, 개설자, 생성일, 지역은 요청 DTO에 없어 수정할 수 없다. 전달된 문자열은 Service에서 공백을 제거해 저장한다.

`maxMembers`를 변경할 때는 `APPROVED` 상태의 `OWNER`, `MEMBER` 수를 잠금 조회한다. 새 정원이 이 인원보다 작으면 변경하지 않고 `409 SMALL_GROUP_CAPACITY_BELOW_MEMBERS`를 반환한다.

### 오류와 처리

| 상황 | 응답 |
| --- | --- |
| 인증 정보 없음 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 잘못된 ID·요청 본문·값 범위 | `400 BAD_REQUEST` 또는 잘못된 JSON 날짜 형식의 `400 INVALID_REQUEST` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 개설자 또는 승인 OWNER가 아님 | `403 SMALL_GROUP_SETTINGS_FORBIDDEN` |
| 새 정원이 승인 참여자 수보다 작음 | `409 SMALL_GROUP_CAPACITY_BELOW_MEMBERS` |

공통 모임 필드(`group_name`, `description`, `max_members`)와 소모임 전용 필드(`like_category`, `meet_date`, `location`)는 하나의 Service 트랜잭션에서 수정한다. DB 마이그레이션은 추가하지 않는다.

---

## 소모임 개설자 전용 가입 신청 거절 API

### 범위

- 엔드포인트: `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/reject`
- `{smallGroupId}`는 `small_group.small_group_id`, `{crewMemberId}`는 `group_member.crew_member_id`이며 모두 양수여야 한다.
- 요청 본문·쿼리 파라미터는 없다. JWT 필터가 request attribute에 저장한 인증 회원 ID만 처리자로 사용한다.
- `groups.member_id`가 로그인 회원과 일치하고 `group_member`에 `OWNER`·`APPROVED` 관계가 있는 소모임 개설자만 거절할 수 있다. `MANAGER` 등 운영진에게는 거절 권한을 부여하지 않는다.

### 성공 응답

성공 시 기존 계모임 가입 신청 거절 API와 같이 본문 없이 공통 성공 봉투를 반환한다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 거절 검증과 저장

Service는 하나의 트랜잭션에서 신청 식별 → 신청 회원 → 소모임 → 신청 행 재조회 순서로 잠가 최신 상태를 검증한다.

1. 가입 신청, 신청 회원, 삭제되지 않은 소모임이 존재하는지 확인한다.
2. 잠금 재조회한 신청의 회원이 최초 식별한 회원과 같고, 해당 소모임에 속하는지 확인한다.
3. 요청자가 개설자이며 승인된 `OWNER` 관계인지 확인한다.
4. 가입 신청 상태가 `PENDING`인지 확인한다.
5. `crew_member_id`, `group_id`, `membership_status = 'PENDING'`가 모두 일치하는 기존 `group_member` 행 하나만 `REJECTED`로 갱신한다.

거절은 회원 관계를 새로 생성하지 않으며 승인 이력·가입 시각도 변경하지 않는다. 기존 계모임과 소모임 구조에는 거절 사유, 처리자, 처리일 저장 컬럼·정책이 없으므로 요청 본문과 응답 필드를 추가하지 않는다. 알림 생성도 연계하지 않는다. DB 마이그레이션·새 상태값·외부 연동·의존성·보안 설정 변경은 하지 않는다.

### 오류

| 상황 | 응답 |
| --- | --- |
| 인증 정보 없음 | `401 UNAUTHORIZED` |
| 소모임·신청 번호가 올바르지 않음 | `400 BAD_REQUEST` |
| 신청 회원이 없거나 삭제됨 | `404 JOIN_REQUEST_MEMBER_NOT_FOUND` |
| 소모임이 없거나 삭제됨 | `404 SMALL_GROUP_NOT_FOUND` |
| 신청이 없거나 다른 소모임 소속 | `404 JOIN_REQUEST_NOT_FOUND` |
| 개설자가 아니거나 승인 OWNER 관계가 아님 | `403 SMALL_GROUP_OWNER_REQUIRED` |
| 신청이 이미 승인·거절 등 처리됨 | `409 JOIN_REQUEST_NOT_PENDING` |
| 동시 변경으로 조건부 갱신이 실패함 | `409 JOIN_REQUEST_REJECTION_FAILED` |

### 프론트 연결·검증

- `rejectSmallGroupJoinRequest(smallGroupId, crewMemberId)`는 두 식별자만 경로로 보내고 서버 오류 코드·메시지를 그대로 보존한다.
- OWNER 신청 목록은 실제 GET API 데이터만 렌더링한다. 대기 신청의 거절 성공 후 서버 목록을 다시 조회하며, 로컬 Mock 상태를 거절 결과로 바꾸지 않는다.
- 승인·거절 처리 중에는 다른 처리 버튼과 상태 필터·페이지 이동을 막아 중복 요청을 방지한다.
- Service 테스트는 정상 거절, 비개설자, 승인 OWNER 관계가 없는 개설자, 소모임·신청 없음, 다른 소모임 신청, 처리 완료 신청, 조건부 갱신 실패를 검증한다.
- Controller 테스트는 인증 ID 전달, 공통 성공·업무 오류 응답, 잘못된 경로 ID, 비로그인 401을 검증한다.

### 수동 확인과 TODO

- 실제 MySQL과 JWT 환경에서 개설자·비개설자·MANAGER·비로그인, 없는·삭제 소모임, 다른 소모임 신청, 처리 완료 신청, 동시 거절을 확인해야 한다.
- 브라우저에서는 OWNER 신청 목록의 거절 중 상태, 성공 후 목록 재조회, 서버 오류 표시, 승인과 거절의 중복 클릭 차단을 확인해야 한다.
- 취소, 선택 승인, 재신청 정책 변경, 알림, 거절 사유·처리자·처리일 저장은 이번 범위에 포함하지 않는다.

---

## 소모임 투표 목록 조회 API

### 범위

- 엔드포인트: `GET /api/small-groups/{smallGroupId}/votes`
- `{smallGroupId}`는 `small_group.small_group_id`이며 양수여야 한다.
- 요청 본문과 쿼리 파라미터는 없다. 검색·상태 필터·정렬 파라미터·페이징은 제공하지 않는다.
- Spring Security와 JWT 필터가 인증한 회원 ID만 사용하며, 클라이언트는 회원 ID를 전달하지 않는다.
- 삭제되지 않은 소모임의 `group_member`에서 `APPROVED` 상태인 `OWNER` 또는 `MEMBER`만 조회할 수 있다.
- `PENDING`, `LEFT`, `EXPELLED`, 비회원, `MANAGER` 관계는 조회할 수 없다.
- 투표 상세, 보기, 참여 여부, 참여자, 득표 수, 결과 공개 여부는 목록 응답에 포함하지 않는다. 각 목록 항목의 `smallGroupVoteId`로 상세 API를 호출한다.

### 성공 응답

기존 `ApiResponse`의 `data` 배열을 반환하며, `meta`는 `null`이다.

```json
{
  "success": true,
  "data": [
    {
      "smallGroupVoteId": 11,
      "title": "11월 정기 모임 날짜",
      "status": "OPEN",
      "endsAt": "2026-11-02T18:00:00"
    }
  ],
  "meta": null,
  "error": null
}
```

- 목록 항목에는 상세 이동용 `smallGroupVoteId`, `title`, `status`, `endsAt`만 포함한다.
- `endsAt`은 마감일을 설정하지 않은 투표인 경우 `null`일 수 있다.
- 결과가 없으면 성공 응답의 `data: []`를 반환한다.

### 조회 기준·정렬·진행 중 투표 한도

- 기존 `small_group`, `groups`, `group_member`, `small_group_vote`와 `SmallGroupMapper`를 재사용한다. DB 마이그레이션, 상태값, 외부 연동, 의존성, Security 설정은 추가하지 않는다.
- 소모임이 없거나 `groups.deleted_at`이 설정되면 목록을 반환하지 않는다.
- 해당 `small_group_id`의 진행 투표와 종료 후 24시간 이내 투표를 `created_at DESC, small_group_vote_id DESC` 순으로 반환한다.
- 목록 조회 자체에는 `LIMIT`, 검색, 상태 조건, 페이지 조건을 적용하지 않는다.
- 투표 생성 시에는 `closed_at IS NULL`인 진행 중 투표 수만 집계하며, 같은 소모임에서 5개 이상이면 새 투표를 생성할 수 없다. 종료된 투표(`closed_at IS NOT NULL`)는 이 한도에 포함하지 않는다.
- **TODO:** 현재 목록 SQL의 24시간 제외 기준은 `closed_at`이다. `ends_at` 경과 투표도 정확히 같은 시점에 숨기려면 종료 처리에서 `closed_at`을 저장하거나, 목록 SQL에 마감 시각 기준을 함께 반영해야 한다.

### 오류

| 상황 | 응답 |
| --- | --- |
| 인증되지 않은 HTTP 요청 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임 ID가 0 이하 또는 형식이 올바르지 않음 | `400 BAD_REQUEST` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 승인된 `OWNER`·`MEMBER` 관계가 아님 | `403 SMALL_GROUP_VOTE_LIST_FORBIDDEN` |

### 프런트 연결·검증

- `getSmallGroupVotes(smallGroupId, { signal })`는 `GET /small-groups/{smallGroupId}/votes`를 호출하고, `smallGroupVoteId`·제목·상태·마감일 배열을 검증해 반환한다.
- 소모임 상세의 투표 탭은 탭 선택 시에만 목록을 요청하고, 화면 전환 시 이전 요청을 취소한다.
- 투표 생성 성공 뒤에는 목록을 다시 조회한다. 프런트에서 목록을 임의 정렬하거나 추가하지 않는다.
- 탭에는 로딩, 재시도 가능한 오류, 빈 목록, 마감일 미설정 상태를 표시한다.
- Service·Controller·Security·Mapper XML 테스트는 정상 OWNER/MEMBER, 빈 목록, 비회원·대기·탈퇴·추방·MANAGER, 없는 소모임, 잘못된 ID, 인증 누락을 다룬다.

### 수동 확인

1. 승인 OWNER와 MEMBER로 각각 조회해 제목·등록일·마감일 순서가 최신 등록순인지 확인한다.
2. 진행 중 투표 5개가 있을 때 생성이 `409 SMALL_GROUP_OPEN_VOTE_LIMIT_EXCEEDED`인지, 종료된 투표는 생성 한도에 포함되지 않는지 확인한다.
3. 비회원, `PENDING`, `LEFT`, `EXPELLED`, `MANAGER`, 비로그인 계정으로 403 또는 401 응답을 확인한다.
4. 없는·삭제 소모임의 404, 투표가 없는 승인 회원의 빈 배열을 확인한다.
5. 브라우저에서 투표 탭의 로딩·오류 재시도·빈 목록·마감일 미설정·생성 후 목록 갱신을 확인한다.

---

## 소모임 투표 상세 조회 API

### 범위

- 엔드포인트: `GET /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}`
- 두 식별자는 각각 `small_group.small_group_id`, `small_group_vote.small_group_vote_id`이며 1 이상이어야 한다.
- 요청 본문과 쿼리 파라미터는 없다. JWT 필터가 저장한 `memberId`만 사용한다.
- 삭제되지 않은 소모임의 승인 `OWNER` 또는 `MEMBER`만 조회할 수 있다. 비회원, `PENDING`, `REJECTED`, `LEFT`, `EXPELLED`, `MANAGER`는 조회할 수 없다.
- 투표는 요청한 `smallGroupId`에 속해야 한다. 다른 소모임의 투표 ID는 존재하더라도 상세를 반환하지 않는다.
- `status = CLOSED`이거나 `endsAt`이 지난 투표는 결과 조회 상태로 반환한다. 마감·종료 시각 후 24시간이 지나면 목록과 상세에서 숨긴다.
- 투표 참여 저장, 선택 변경·취소, 제목·보기 수정, 삭제 API는 이 상세 조회 범위에 포함하지 않는다.

### 성공 응답

성공 시 기존 `ApiResponse`의 `data`에 아래 구조를 반환하며 `meta`는 `null`이다.

```json
{
  "success": true,
  "data": {
    "smallGroupVoteId": 11,
    "title": "11월 모임 장소",
    "creatorNickname": "모임장",
    "multipleChoice": true,
    "anonymous": false,
    "createdAt": "2026-10-30T10:00:00",
    "endsAt": "2026-10-31T18:00:00",
    "status": "OPEN",
    "participantCount": 2,
    "myParticipation": {
      "participated": true,
      "selectedOptionIds": [101]
    },
    "options": [
      {
        "smallGroupVoteOptionId": 101,
        "content": "한강공원",
        "displayOrder": 1,
        "voteCount": 2,
        "voterNicknames": ["걷기좋아", "모아회원"]
      }
    ]
  },
  "meta": null,
  "error": null
}
```

- `participantCount`는 저장된 투표 참여 이력 기준의 총 참여 회원 수다.
- `myParticipation.participated`가 `false`이면 `selectedOptionIds`는 빈 배열이다.
- `options`는 `displayOrder ASC` 순서이며, 선택자가 없는 보기도 `voteCount: 0`으로 포함한다.
- 단일·복수 선택 여부와 관계없이 상세 조회는 이미 저장된 선택 보기 ID 목록을 그대로 반환한다.

### 공개·익명 정책

| 조건 | `voterNicknames` |
| --- | --- |
| 익명 투표 | 항상 빈 배열 |
| 공개 투표, 미참여 요청자 | 항상 빈 배열 |
| 공개 투표, 참여 완료 요청자 | 보기별 참여자 닉네임 목록 |

투표 생성자를 포함해 익명 투표의 참여자 이름과 선택 내역은 반환하지 않는다. 항목별 득표 수는 익명 여부와 관계없이 반환한다.

### 오류 응답

| 상황 | 응답 |
| --- | --- |
| 인증되지 않은 HTTP 요청 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임 ID 또는 투표 ID가 0 이하·형식 오류 | `400 BAD_REQUEST` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 없는 투표, 다른 소모임 투표, 삭제 투표, 마감 후 24시간 경과 투표 | `404 SMALL_GROUP_VOTE_NOT_FOUND` |
| 승인된 `OWNER`·`MEMBER` 관계가 아님 | `403 SMALL_GROUP_VOTE_DETAIL_FORBIDDEN` |

### 프런트 연결·검증

- `getSmallGroupVoteDetail(smallGroupId, smallGroupVoteId, { signal })`는 GET 요청과 응답 구조를 검증하고 서버 오류 코드·메시지를 보존한다.
- 목록의 `smallGroupVoteId`를 사용해 `/small-groups/{smallGroupId}/votes/{voteId}`로 이동한다.
- 상세 페이지는 로딩·오류·재시도·존재하지 않는 투표 상태를 표시한다.
- 상세 화면은 투표 참여 API를 호출하지 않는다. 참여 UI·저장은 별도 기능 범위에서 연결한다.

### 테스트·수동 확인

- Service 단위 테스트는 승인 OWNER/MEMBER, 비회원·대기·탈퇴·추방·MANAGER, 없는 소모임·투표, 다른 소모임 투표, 익명·공개 투표의 닉네임 범위, 마감 뒤 24시간 이내·경과 상태를 검증한다.
- Controller 테스트는 JWT `memberId` 전달, 공통 응답, 잘못된 Path Variable, Service의 401·404 오류 보존을 검증한다.
- 프런트 API 테스트는 상세 경로·취소 신호·필수 응답 필드·서버 오류 보존, 목록의 상세 식별자 보존을 검증한다.
- 실제 MySQL·JWT 환경에서 소모임별 투표 격리, 승인 회원 권한, 종료 시각과 24시간 숨김 경계, 공개·익명 닉네임 공개 범위를 확인해야 한다.

---

## 소모임 투표 참여·현황 조회 API

### 투표 참여

- 엔드포인트: `POST /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}/participations`
- JWT 필터가 저장한 `memberId`만 사용하며, 승인된 `OWNER` 또는 `MEMBER`만 참여할 수 있다.
- `PENDING`, `REJECTED`, `LEFT`, `EXPELLED`, `MANAGER`, 비회원은 참여할 수 없다.
- 삭제된 소모임·투표, 다른 소모임의 투표, `CLOSED` 상태 또는 마감 시각이 지난 투표는 참여할 수 없다.
- 단일 선택은 정확히 한 개, 복수 선택은 한 개 이상이며 해당 투표의 전체 보기 수 이하를 선택해야 한다.
- 중복 보기 ID 또는 해당 투표에 속하지 않는 보기 ID는 허용하지 않는다.

#### 요청 본문

```json
{
  "selectedOptionIds": [101, 102]
}
```

#### 성공 응답

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

- 최초 참여는 `small_group_vote_participation`과 `small_group_vote_selection`에 저장한다.
- 기존 참여자가 같은 보기를 다시 전송하면 성공 응답만 반환하고 DB 이력과 참여 시각은 바꾸지 않는다.
- 기존 참여자가 다른 보기를 전송하면 이전 선택을 교체하고 `participated_at`을 갱신한다.

### 투표 현황 조회

- 엔드포인트: `GET /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}/status`
- 승인된 `OWNER` 또는 `MEMBER` 중 해당 투표의 참여 이력이 있는 회원만 조회할 수 있다.
- 마감 전에는 화면에서 `현황 보기`, 마감 후에는 `결과 보기`로 표시한다. 두 경우 모두 참여 완료 회원은 조회할 수 있다.
- 성공 응답은 기존 투표 상세 조회와 같은 `SmallGroupVoteDetailResponse` 구조를 반환한다.
- `participantCount`는 해당 투표의 참여 이력 수이며 화면에는 `N명 참여`로 표시한다.
- 공개 투표는 참여 완료 회원에게 보기별 참여자 닉네임을 반환한다. 익명 투표는 생성자·운영자를 포함해 닉네임과 선택 내역을 반환하지 않는다.

### 오류 응답

| 상황 | 응답 |
| --- | --- |
| 인증되지 않은 요청 | `401 LOGIN_REQUIRED` |
| 잘못된 소모임·투표·보기 식별자 또는 선택 수 | `400 BAD_REQUEST` 및 세부 오류 코드 |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 없는·삭제 투표 또는 다른 소모임 투표 | `404 SMALL_GROUP_VOTE_NOT_FOUND` |
| 마감·종료 투표 참여 | `409 SMALL_GROUP_VOTE_CLOSED` |
| 승인 회원이 아닌 참여 요청 | `403 SMALL_GROUP_VOTE_PARTICIPATION_FORBIDDEN` |
| 참여 이력이 없는 현황 요청 | `403 SMALL_GROUP_VOTE_STATUS_FORBIDDEN` |

### 검증 항목

- 최초 참여, 단일·복수 선택, 같은 선택 재요청, 선택 변경 재투표를 확인한다.
- 마감 투표, 다른 소모임의 보기, 중복 보기, 비회원·탈퇴 회원의 참여 실패를 확인한다.
- 참여 완료 회원의 공개·익명 투표 현황, 미참여 회원의 현황 조회 차단을 확인한다.
- 실제 MySQL·JWT 환경에서 행 잠금과 동시 참여·선택 변경을 확인한다.

---

## 소모임 투표 삭제 API

- 엔드포인트: `DELETE /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}`
- 요청 본문·쿼리 파라미터는 없다.
- JWT 필터가 저장한 `memberId`만 사용한다.
- `{smallGroupId}`와 `{smallGroupVoteId}`는 모두 양수여야 한다.

### 삭제 권한·상태 정책

- 로그인 회원은 투표의 `creator_member_id`, 소모임 공통 모임의 개설자(`groups.member_id`), 승인된 `OWNER` 관계가 모두 동일해야 한다.
- `OPEN`, `CLOSED` 상태 모두 삭제할 수 있다. 마감 시각과 참여 이력 유무는 삭제 제한 조건이 아니다.
- `small_group_vote.deleted_at`만 현재 시각으로 갱신하는 논리 삭제를 사용한다.
- `small_group_vote_option`, `small_group_vote_participation`, `small_group_vote_selection`은 삭제하거나 수정하지 않아 기존 보기·참여·선택 이력을 보존한다.
- 논리 삭제된 투표는 목록·상세·참여 대상에서 제외되며, 진행 중 투표 5개 생성 한도에도 포함하지 않는다.

### 성공 응답

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

### 오류 응답

| 상황 | 응답 |
| --- | --- |
| 인증되지 않은 HTTP 요청 | `401 LOGIN_REQUIRED` — Spring Security 공통 응답 |
| 소모임 ID 또는 투표 ID가 0 이하·형식 오류 | `400 BAD_REQUEST` |
| 없는·삭제 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 없는 투표, 다른 소모임 투표, 이미 삭제된 투표 | `404 SMALL_GROUP_VOTE_NOT_FOUND` |
| 투표 작성자가 아니거나 개설자·승인 OWNER 관계가 아님 | `403 SMALL_GROUP_VOTE_DELETE_FORBIDDEN` |
| 잠금 검증 후 논리 삭제 갱신 실패 | `500 SMALL_GROUP_VOTE_DELETE_FAILED` |

### 프런트 연결·검증

- `deleteSmallGroupVote(smallGroupId, smallGroupVoteId)`는 DELETE 요청을 보내고 서버 오류 코드·메시지를 보존한다.
- 소모임 상세 API에서 `myRole`이 `OWNER`인 경우에만 투표 상세의 삭제 버튼을 표시한다. 투표 작성자 여부는 서버가 최종 검증한다.
- 삭제 확인 모달에서 성공하면 투표 목록으로 돌아가며, 목록 재조회 시 삭제 투표는 표시되지 않는다.
- Service 테스트는 OPEN·CLOSED 삭제, 권한 없음, 없는 소모임·투표, 다른 소모임 투표와 이미 삭제된 투표를 검증한다.
- 실제 MySQL·JWT 환경에서 보기·참여·선택 이력 보존과 목록 제외, 삭제 후 생성 한도 반영을 확인한다.

---

<!-- AI작성: 사용자 확정 정책 및 2~7단계 실제 구현을 기준으로 후기 등록 API와 DB·저장 모델을 정리한다. -->
## 소모임 후기 등록 API

### 구현 범위와 선행 조건

- 엔드포인트: `POST /api/small-groups/{smallGroupId}/reviews`
- `{smallGroupId}`는 `small_group.small_group_id`이며 양수다. `groups.group_id`와 구분한다.
- DTO·Mapper/XML·Service·Controller 및 테스트 코드는 추가되어 있다. 컴파일·테스트 통과와 실제 DB 적용 완료를 의미하지 않는다.
- 프론트 작성 모달과 공개 후기 목록을 구현했다. 작성 화면도 별점 0~5점·0.5 단위 및 코멘트 필수·1~30자를 검증한다. 후기 상세·수정·삭제·신고·댓글 기능은 포함하지 않는다.
- 후기 테이블은 DB 담당자가 반영한다. 현재 저장소 `schema.sql`에는 `small_group_review`가 없으며 실제 DB 반영 여부도 확인되지 않았다. 테이블·제약 반영 후 API를 사용해야 한다.
- 신규 의존성·마이그레이션 도구·외부 API 연동은 추가하지 않는다.

### 인증 및 요청

`Authorization: Bearer <access-token>`과 `Content-Type: application/json`을 사용한다. 기존 Spring Security·JWT 필터가 검증하고 요청 속성에 저장한 `memberId`만 작성자로 사용하며 본문에서 작성자 ID를 받지 않는다.

```json
{
  "rating": 4.5,
  "content": "함께해서 즐거웠어요"
}
```

| 필드 | Java 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| rating | BigDecimal | 예 | 0~5점, 0.5 단위. 0·0.5·1·…·5 허용 |
| content | String | 예 | 공백 문자열 불가, 원문 길이 1~30자 |

요청 DTO는 `SmallGroupReviewCreateRequest`다. `@NotNull`, `@DecimalMin`, `@DecimalMax`, `@AssertTrue`로 별점을, `@NotBlank`, `@Size`로 코멘트를 검증한다. Service 직접 호출도 같은 정책으로 방어한다. 코멘트는 앞뒤 공백을 제거하지 않고 저장한다. 현재 Java Validation과 Service 길이 검사는 UTF-16 코드 단위 기준이므로 이모지 등 일부 문자는 화면상 글자 수와 다를 수 있다. 별점을 DB가 반올림하기 전에 원본 값의 단위를 검사한다. 평점 외 이미지·제목·공개 범위 등 추가 필드는 사용하지 않는다.

### 등록 정책 및 처리 순서

Service `createSmallGroupReview`가 하나의 트랜잭션에서 아래를 처리한다. Controller는 요청 바인딩·Validation·인증 회원 ID 전달·응답만 담당한다.

1. 인증 회원 ID, 소모임 ID 및 입력값을 검증한다.
2. `selectSmallGroupReviewCreateTargetForUpdate`로 소모임·공통 모임·개설자 연결 행을 잠금 조회한다. 삭제된 공통 모임 또는 삭제된 개설자는 조회 대상에서 제외한다.
3. `groups.member_id`와 요청자가 같으면 개설자 작성 금지로 거절한다. 관계 역할이 잘못 저장되어 있어도 개설자는 허용하지 않는다.
4. 기존 `selectSmallGroupMembershipForUpdate`로 해당 `group_id`와 인증 회원의 관계를 잠금 조회한다. `MANAGER`는 상태와 무관하게 기존 403 제한을 적용한다. 정확히 `MEMBER`·`APPROVED`인 관계만 허용한다. 비회원, OWNER 관계, PENDING·REJECTED·LEFT·EXPELLED는 거절한다.
5. `meet_date`가 NULL이면 거절한다. `Asia/Seoul` 기준 오늘보다 모임 날짜가 이전일 때만 허용하므로 모임 당일·미래는 거절하고 다음 날부터 허용한다. 별도의 참석 이력·활동 종료 조건은 추가하지 않는다.
6. `selectSmallGroupReviewId`로 같은 소모임·작성자의 기존 후기를 확인한다. 기존 후기나 INSERT 시 DB UNIQUE 위반은 동일한 중복 오류로 처리한다.
7. `SmallGroupReviewCreateCommand`에 경로 소모임 ID, 인증 작성자 ID, 별점·코멘트를 설정하고 저장한다. 저장 건수 1건 및 생성 ID를 확인한 뒤 후기 ID를 반환한다.

### 성공 응답

기존 일반 게시글 등록과 같은 HTTP 200, `ApiResponse<Long>` 형식이다. `data`는 생성된 후기 ID다.

```json
{
  "success": true,
  "data": 50,
  "meta": null,
  "error": null
}
```

### 실패 응답

`ApiException`과 기존 `GlobalExceptionHandler` 또는 인증 필터의 공통 응답을 사용한다. 인증·요청 검증이 먼저 수행될 수 있으므로 여러 조건이 동시에 잘못되었다면 아래 오류의 우선순위를 보장하지 않는다.

| 상황 | HTTP | error.code |
| --- | --- | --- |
| 인증 없는 HTTP 요청 | 401 | LOGIN_REQUIRED |
| 만료된 JWT | 401 | ACCESS_TOKEN_EXPIRED |
| 잘못된 JWT·유효하지 않은 세션 | 401 | INVALID_TOKEN |
| Service에 인증 회원 ID가 없거나 0 이하 | 401 | UNAUTHORIZED |
| 숫자가 아닌 경로, 0 이하 소모임 ID, 입력 검증 실패 | 400 | BAD_REQUEST |
| 요청 본문 누락·잘못된 JSON·역직렬화 실패 | 400 | INVALID_REQUEST |
| 소모임이 없거나 공통 모임·개설자가 삭제됨 | 404 | SMALL_GROUP_NOT_FOUND |
| 소모임 개설자 | 403 | SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED |
| MANAGER 관계 | 403 | ACCESS_DENIED |
| 승인된 일반 모임원이 아님 | 403 | SMALL_GROUP_REVIEW_CREATE_FORBIDDEN |
| 모임 날짜 없음 | 409 | SMALL_GROUP_REVIEW_MEET_DATE_REQUIRED |
| 모임 당일·미래 날짜 | 409 | SMALL_GROUP_REVIEW_NOT_AVAILABLE_YET |
| 이미 등록한 후기·DB 중복 키 오류 | 409 | SMALL_GROUP_REVIEW_ALREADY_EXISTS |
| 저장 건수가 1이 아니거나 생성 키 없음 | 500 | SMALL_GROUP_REVIEW_CREATE_FAILED |

```json
{
  "success": false,
  "data": null,
  "meta": null,
  "error": {
    "code": "SMALL_GROUP_REVIEW_ALREADY_EXISTS",
    "message": "이미 이 소모임에 후기를 등록했습니다."
  }
}
```

### DB 및 저장 모델 — 담당자 반영 요청

다음은 전달한 테이블 설계이며 실제 DB 반영 확인 전이다. Mapper는 아래 이름을 참조한다.

| 컬럼 | 타입·기본값 | 용도 |
| --- | --- | --- |
| small_group_review_id | BIGINT NOT NULL AUTO_INCREMENT, PK | 후기 식별자 |
| small_group_id | BIGINT NOT NULL | small_group.small_group_id FK |
| writer_id | BIGINT NOT NULL | members.member_id FK |
| rating | DECIMAL(2,1) NOT NULL | 별점 |
| content | VARCHAR(30) NOT NULL | 코멘트 |
| created_at | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 작성일, INSERT에서 생략 |
| updated_at | DATETIME NULL | 기존 게시글 감사 필드 관례, 등록 시 NULL |

- `uq_small_group_review_group_writer`: UNIQUE(small_group_id, writer_id). 동시 등록에도 회원당 소모임별 1건을 보장한다.
- `fk_small_group_review_small_group`, `fk_small_group_review_writer`: 위 소모임·작성자 FK. 연쇄 삭제·갱신을 추가하지 않는다.
- `chk_small_group_review_rating`: CHECK(rating BETWEEN 0.0 AND 5.0 AND MOD(rating * 10, 5) = 0).
- `chk_small_group_review_content`: CHECK(CHAR_LENGTH(content) BETWEEN 1 AND 30 AND CHAR_LENGTH(TRIM(content)) > 0).
- 기존 InnoDB·utf8mb4·utf8mb4_unicode_ci 관례를 따른다. CHECK가 강제되는 MySQL 버전과 SQL 모드를 확인한다.
- DB CHECK는 저장값에 적용된다. DECIMAL 변환 전 별점, 탭·개행 공백 문자열, 코멘트 길이 검증은 서버·화면에서도 수행한다.
- 현재 승인 상태·개설자·날짜 조건은 Service에서 검증한다. FK만으로 이 조건이 보장되지는 않는다.
- SQL은 `useGeneratedKeys="true"`, `keyProperty="command.smallGroupReviewId"`로 후기 ID를 반환한다. 수정·삭제 상태나 기능은 추가하지 않는다.

### 테스트 및 실제 검증 상태

- `SmallGroupReviewCreateServiceTest`: 정상 저장값·생성 ID, 인증·소모임·개설자·회원 상태·MANAGER, 날짜 NULL·당일·미래, 중복·DB 중복 오류 변환, 별점·코멘트 경계, 저장 실패·생성 키 누락 테스트.
- `SmallGroupReviewCreateControllerTest`: 요청 바인딩·Validation·서버 인증 ID 사용, 성공·실패 응답, 잘못된 JSON·본문 누락·경로 형식 테스트. Service Mock을 사용하므로 실제 업무 권한 검증 통합 테스트는 아니다.
- `SmallGroupSecurityIntegrationTest`: 인증 속성을 임의 전달하더라도 인증 없는 후기 등록 요청이 401로 차단되는지 테스트. 실제 발급 JWT의 성공 흐름은 별도 확인이 필요하다.
- `SmallGroupMapperXmlTest`: 신규 statement 3개와 생성 키 경로 파싱 확인. 실제 SQL 실행·잠금·DB UNIQUE 동시성 검증은 아니다.
- 작업 중 `git diff --check`와 별도 XML 구문 파싱·매핑 정적 확인은 통과했다.
- Gradle 컴파일 및 위 대상 테스트 실행은 Java 17 toolchain 부재로 컴파일 전에 중단되었다. 테스트 통과 건수는 없으며 실행 성공으로 기록하지 않는다.

Java 17 준비 후 `back` 폴더에서 실행한다.

```powershell
.\gradlew.bat test --tests com.nightstudy.moa.smallGroup.SmallGroupReviewCreateServiceTest --tests com.nightstudy.moa.smallGroup.SmallGroupReviewCreateControllerTest --tests com.nightstudy.moa.smallGroup.SmallGroupSecurityIntegrationTest --tests com.nightstudy.moa.smallGroup.SmallGroupMapperXmlTest
```

### Postman·DB 수동 확인

1. DB 담당자의 테이블·FK·UNIQUE·CHECK 반영과 `SHOW CREATE TABLE small_group_review` 결과를 확인한다. 공용 DB 적용과 schema.sql·ERD 갱신은 DB 담당자가 수행한다.
2. 전날 이전 모임의 승인된 일반 모임원 토큰으로 위 예시를 POST하고 200, 후기 ID 및 DB 작성자·별점·코멘트·작성일을 확인한다. 별점 0·0.5·5, 코멘트 1·30자도 확인한다.
3. 같은 회원·소모임으로 재요청해 409 및 행 수 1건 유지를 확인한다. 실제 동시 요청도 같은 제약을 유지하는지 확인한다.
4. 비로그인, 비회원, 개설자, MANAGER, 가입 대기·거절·탈퇴·강제 탈퇴 회원, 없는·삭제된 소모임을 각각 요청해 위 오류를 확인한다.
5. 모임 당일·미래·날짜 NULL을 거절하고 다음 날부터 허용하는지 한국 날짜 경계에서 확인한다.
6. 별점 누락·범위 초과·0.1 단위, 코멘트 누락·빈 값·공백·31자, 잘못된 JSON 요청이 거절되고 행이 추가되지 않는지 확인한다.

이 문서는 구현·테스트 코드와 확인된 실행 제한을 기록한 것이며 실제 MySQL·Postman·브라우저 검증 완료를 의미하지 않는다.

---

<!-- AI작성: 사용자 확정 정책과 후기 목록 1~10단계 실제 구현·검증 결과를 정리한다. -->
## 소모임 후기 목록 조회 API

### 요청·공개 범위

- `GET /api/small-groups/{smallGroupId}/reviews?page=1`
- 경로 ID는 `small_group.small_group_id`이며 양수다. 요청 본문은 없다.
- 쿼리는 `page`만 사용한다. 생략 시 1, 정수 1 이상이다. 크기는 서버에서 8로 고정하고 검색·필터·정렬 옵션은 추가하지 않는다.
- 비로그인·비회원·가입 대기·거절·모임 탈퇴·강퇴 관계도 조회할 수 있다. 승인 OWNER·MEMBER도 조회할 수 있다.
- 인증된 요청자의 해당 소모임 관계에 MANAGER가 있으면 가입 상태와 관계없이 `403 ACCESS_DENIED`다. 비로그인 요청에서는 사용자 역할을 식별하지 않는다.
- 회원 계정 탈퇴·정지 등 유효하지 않은 로그인 세션은 JWT 공통 정책을 따른다. 모임 탈퇴 관계와 계정 탈퇴를 구분한다.
- 등록 전용 조건인 모임 다음 날·승인 MEMBER·개설자 작성 금지는 목록 조회에 적용하지 않는다.
- 인증 없는 GET만으로도 조회 가능하며 같은 경로 POST는 인증 필수다. 만료·잘못된 인증 헤더는 공개 조회에서도 기존 JWT 오류를 반환한다.

### 응답

HTTP 200, 기존 `ApiResponse<List<SmallGroupReviewListItemResponse>>`와 `PageMeta`를 사용한다.

```json
{
  "success": true,
  "data": [
    {
      "smallGroupReviewId": 50,
      "writerNickname": "모아회원",
      "content": "함께해서 즐거웠어요",
      "createdAt": "2026-11-01T12:00:00",
      "rating": 4.5
    }
  ],
  "meta": { "page": 1, "size": 8, "total": 1, "totalPages": 1 },
  "error": null
}
```

별점은 Java BigDecimal로 조회하며 JSON 숫자로 반환한다. 공개 응답은 후기 식별자·닉네임·내용·작성일·별점만 포함한다. 작성자 회원 ID·실명·연락처·이미지·수정 여부는 추가하지 않는다. 빈 결과는 `data: []`, `total: 0`, `totalPages: 0`이다. 범위 초과 페이지도 요청 페이지 번호를 유지한 빈 목록과 실제 전체 건수·페이지 수를 반환한다.

### 처리 구조·정렬

1. Controller는 경로·선택적 인증 회원 ID·검증된 SmallGroupReviewSearchCondition을 Service에 전달하고 items·pageMeta를 공통 응답으로 반환한다.
2. Service는 ID·페이지를 방어 검증하고 기존 selectSmallGroupDetail로 모임 존재를 확인한다. 공통 모임 또는 개설자가 삭제된 경우도 기존 상세 SQL에 따라 조회 대상에서 제외한다.
3. 로그인한 요청은 기존 selectSmallGroupMemberRelations·resolveMemberRole을 재사용한다. MANAGER는 403, 중복 관계·미정의 역할은 기존 500 정책을 따른다. 승인 여부를 조회 제한으로 사용하지 않는다.
4. SmallGroupReviewListCondition으로 해당 소모임의 건수와 목록을 조회한다. 크기는 8, offset은 공통 PageMeta로 계산한다. 빈 결과·범위 초과는 offset 계산 전에 반환한다.
5. SQL은 `created_at DESC, small_group_review_id DESC`로 정렬한다. 목록과 건수는 동일한 소모임 WHERE 조건을 공유한다. 작성자 닉네임은 members LEFT JOIN으로 조회하고 작성자의 모임 관계·상태로 후기를 제외하지 않는다. 읽기 트랜잭션이며 등록용 잠금 쿼리는 사용하지 않는다.

| 상황 | HTTP | error.code |
| --- | --- | --- |
| 잘못된 ID·페이지 형식, 0 이하 ID·페이지, 페이지 빈 값 | 400 | BAD_REQUEST |
| 소모임이 없거나 공통 모임·개설자가 삭제됨 | 404 | NOT_FOUND |
| 인증 요청자의 해당 소모임 MANAGER 관계 | 403 | ACCESS_DENIED |
| 중복 관계·미정의 역할 | 500 | INTERNAL_ERROR |
| Service 직접 호출에 0 이하 인증 회원 ID 전달 | 401 | UNAUTHORIZED |
| 만료된 JWT | 401 | ACCESS_TOKEN_EXPIRED |
| 잘못된 JWT·유효하지 않은 세션 | 401 | INVALID_TOKEN |

### 화면·등록 후 갱신

- SmallGroupMenuTabs는 비회원에게도 공지·후기 탭을 제공한다. 작성 버튼·기존 모달은 기존 등록 정책을 유지한다.
- SmallGroupReviewsPanel은 처음 8개를 조회하고 하단이 보이면 다음 페이지를 추가한다. 페이지 번호 버튼 없이 기존 목록을 유지하며 닉네임·내용 전체·날짜·숫자 별점을 표시한다.
- 중복 요청과 중복 후기 ID 표시를 방지한다. 탭·소모임·역할 전환 시 이전 요청을 취소하고 상태를 초기화한다.
- 로딩·빈 결과·마지막 페이지를 안내하고, 추가 조회 실패 시 기존 목록 아래에서 같은 페이지를 재시도한다. IntersectionObserver 미지원 환경은 후기 더 보기 버튼을 제공한다.
- 실제 등록 성공 콜백의 created=true인 경우만 목록을 다시 마운트하고 첫 페이지부터 재조회한다. 중복 등록 안내(created=false)는 기존 목록을 유지한다. 재조회 실패 시 등록 성공 안내와 별도로 목록 재시도를 제공한다.
- offset 페이징이므로 페이지 요청 사이 새 등록·삭제가 발생하면 페이지 구성이 변할 수 있다. ID 중복은 제거하지만 조회 시점 전체의 일관성은 보장하지 않는다.

### 후기 별점 미선택 시 0점 처리 — 2026-11-03

<!-- AI작성: 사용자 확정에 따라 화면 미선택은 0점으로 전송하며 API의 별점 필수 계약은 유지한다. -->
- 작성 모달은 `별점`과 `별점을 선택하지 않으면 0점으로 등록됩니다.`를 표시한다. 별점 미선택 시 코멘트 검증을 통과하면 `rating: 0`으로 등록 API를 호출한다. 미선택 오류나 별점으로의 강제 초점 이동은 발생하지 않는다.
- 별점 선택 시 별점 오류만 해제한다. 코멘트·서버 오류는 별점 선택으로 지우지 않는다. 오류와 도움말은 별점 영역의 접근성 설명으로 연결한다.
- `0점 선택`은 유효한 입력이다. 기존 0~5점·0.5점 단위, 코멘트 필수·1~30자 및 서버 검증 정책을 유지한다.
- API 본문의 rating은 여전히 필수다. 화면에서 미선택을 숫자 0으로 변환하므로 서버·DB 계약 변경은 없다.
- 브라우저 검증: 별점 미선택 상태에서 정상 코멘트로 등록해 Network 요청의 `rating: 0`과 등록 후 0점 표시를 확인한다. 직접 선택한 0점·0.5점·5점도 정상 코멘트로 등록을 확인한다. 별점 미선택·선택 각각에서 코멘트 누락·공백 입력은 요청이 차단되어야 한다.
- 실제 브라우저·서버 등록 검증은 미실행이며 개발자 확인이 필요하다.

### DB 전제·남은 TODO

- 기존 후기 등록과 같은 small_group_review 테이블을 사용한다. 현재 저장소 schema.sql에는 테이블이 없으며 실제 DB 반영은 확인하지 않았다. DB 담당자의 최종 DDL·적용 결과를 받아야 한다. 이번 목록 작업은 마이그레이션·컬럼·상태를 추가하지 않았다.
- 전달된 DDL에는 후기 삭제 컬럼이 없다. 현재 SQL은 삭제 조건을 적용하지 않는다. 물리 삭제된 행은 조회되지 않으며, 논리 삭제 예정이면 최종 컬럼을 확인해 건수·목록 양쪽에 제외 조건을 반영해야 한다. 삭제 제외 요구사항은 이 정책 확정 전까지 미검증이다.
- 비공개·신고 숨김 정책은 관리자 기능 후속 작업이다. 임의 상태나 필터를 추가하지 않는다.
- 작성자 계정 삭제 시 닉네임 처리 정책은 별도 확정이 필요하다. 현재 SQL은 members.nickname을 그대로 조회하고 프론트는 문자열 닉네임을 요구한다.

### 최종 검증 기록 — 2026-11-02

- `front`에서 `node --test src/api/smallGroups.test.js`: 51개 통과. 목록 요청·응답·빈 결과·별점 경계·오류·취소와 기존 등록 등 API 회귀 테스트다. HTTP 클라이언트를 대체한 단위 테스트로 실제 서버 통신 검증은 아니다.
- `front`에서 `npm run build`: 통과. 번들 500 kB 초과 경고가 있으며 이번 작업에서 분할 설정은 변경하지 않았다.
- 후기 등록·목록 Service/Controller, SmallGroupSecurityIntegrationTest, SmallGroupMapperXmlTest를 지정한 Gradle 테스트를 시도했으나 Java 17 toolchain 부재로 컴파일 전에 중단됐다. 백엔드 테스트 통과로 간주하지 않는다.
- `git diff --check`로 문서 변경을 검증한다. 실제 MySQL·Postman·브라우저 수동 검증은 수행하지 않았다.

### 후속 수동 검증 절차

1. DB 담당자의 최종 테이블 정의·제약·삭제 정책과 Java 17 환경을 준비하고 위 백엔드 테스트를 다시 실행한다.
2. Postman에서 인증 헤더 없이 목록 GET이 200인지 확인한다. 비회원·대기·탈퇴 관계·OWNER·MEMBER 토큰으로도 조회하고 MANAGER 토큰은 403인지 확인한다. 비로그인 POST 등록은 401이어야 한다.
3. 테스트 모임에 후기 0·1·8·9·17건과 다른 모임 후기를 준비해 해당 모임만 조회되는지 확인한다. 날짜가 같은 후기의 ID 내림차순, 페이지별 8·8·1건, 범위 초과 빈 목록을 확인한다.
4. 없는·삭제된 소모임은 404, `page=0`, `page=-1`, `page=abc`, 빈 page와 잘못된 ID는 정책에 맞는 오류인지 확인한다. 음수·문자 ID의 비로그인 요청은 공개 숫자 경로와 일치하지 않아 Security에서 먼저 401이 될 수 있다.
5. 브라우저에서 비회원 후기 탭, 본문 전체·날짜·0·0.5·5점, 스크롤 추가 조회·마지막 안내·실패 재시도·탭/모임 전환 중 요청 취소를 확인한다.
6. 승인 일반 모임원으로 등록해 모달 종료·완료 안내·첫 페이지 재조회·최상단 새 후기를 확인한다. 중복 등록은 목록을 초기화하지 않고, 일반 등록 실패는 모달 입력을 유지하는지 확인한다.

<!-- AI작성: 계모임과 동일하게 적용한 공개 소모임 지역 검색 계약입니다. -->
## 공개 소모임 목록 — 시·구·동 검색

`GET /api/small-groups`는 기존 검색어·관심사·페이지와 아래 지역 조건을 함께 받는다. 응답의 data/meta 및 공개 조회 정책은 유지한다.

| 파라미터 | 검증·의미 |
|---|---|
| keyword | 기존과 동일, 공백 제거 후 최대 20자 |
| likeCategory | 기존과 동일, 공백 제거 후 최대 50자 |
| page | 기본 1, 1 이상 |
| regionCode | 기존 호환용 01~10. 새 지역 조건과 동시 사용 금지 |
| provinceName | 시·도 정규명, 공백 제거 후 최대 30자 |
| districtName | 시·군·구 정규명, 공백 제거 후 최대 50자. provinceName 필요 |
| townName | 읍·면·동 정규명, 공백 제거 후 최대 50자. provinceName·districtName 필요 |

- 지역 미선택은 전체, 시만 선택하면 그 시의 모든 구·동 모임, 시·구까지 선택하면 해당 구의 모든 동 모임, 동까지 선택하면 해당 동이다. 선택한 단계까지만 region의 province_name·district_name·town_name을 AND 비교한다. 목록과 전체 건수는 동일한 smallGroupListWhere를 재사용한다.
- 기존 regionCode는 기존 고정 지역명 일치 검색을 유지한다. 기존 코드와 새 지역 조건을 함께 보내거나 상위 조건 없이 하위 조건을 보내면 HTTP 400이다. 길이 초과도 400이다. 존재하지 않거나 서로 맞지 않는 지역명 조합은 200 빈 목록이며 지역 존재 여부를 별도 조회하지 않는다.
- 회원가입·지역 인증의 getMemberRegions(`/api/auth/regions`)를 재사용한다. 구 선택값은 regionId로 관리하고 실제 요청에는 정규 지역명을 전달한다. 세종은 districtName 미전달과 빈 문자열을 구분하며, 빈 구 이름의 행을 선택해야 동을 선택할 수 있다.
- 지역 변경은 첫 페이지 재조회, 시 변경은 구·동 초기화, 구 변경은 동 초기화한다. 구·동은 필수가 아니다. 기존 검색어·관심사와 결합하며 요청 취소·오래된 응답 무시를 유지한다. 지역 목록 로딩·조회 실패·재시도를 표시한다.
- DB 스키마·데이터를 변경하지 않는다. 모임이 참조하는 region 행에 상위 지역 컬럼이 채워져 있어야 검색되며 실제 동 데이터 준비 여부는 별도 확인해야 한다. 생성·상세·후기·권한 정책은 변경하지 않는다.

예: `/api/small-groups?provinceName=서울특별시&likeCategory=운동&page=1`
또는 `/api/small-groups?provinceName=서울특별시&districtName=강남구&page=1`

수동 검증: 전체·시·시/구·시/구/동 결과와 건수, 검색어·관심사 조합, 2페이지 이후 지역 변경 시 첫 페이지, 하위 초기화, 지역 조회 실패 후 재시도, 세종 빈 구 이름·동 선택, 모바일 줄바꿈을 확인한다. Postman에서 기존 regionCode 단독 성공과 새 조건 동시 입력·상위 누락 시 400을 확인한다.

<!-- AI작성: 5단계 검색 초기화·결과 건수 표시 동작만 기록한다. -->
## 검색 초기화와 결과 건수 표시

- 정상 조회 결과가 0건이면 기존 빈 결과 안내 아래 `검색 조건 초기화` 버튼을 표시한다. 범위 초과 페이지는 기존 첫 페이지 이동 버튼을 유지한다.
- 초기화하면 입력 중인 검색어·제출한 검색어·시·구·동·관심사·검색어 오류·페이지를 초기 상태로 되돌리고 전체 목록을 다시 조회한다. 이미 전체 조건이더라도 재조회한다.
- 목록 로딩·실패 시 결과 건수와 최신순 헤더를 숨겨 이전 조건의 건수나 오류에 따른 0건을 표시하지 않는다. 성공 후 현재 조건의 건수를 표시한다.
- 브라우저 검증: 결과 없는 검색 후 초기화하여 검색어·지역이 비워지고 관심사는 전체, 페이지는 1로 돌아가 전체 조회가 실행되는지 확인한다. 전체 결과가 0건인 경우에도 초기화 재조회가 실행되는지 확인한다. 느린 네트워크에서 이전 건수가 숨겨지고 오류를 0건으로 표시하지 않는지 확인한다.
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

### 소모임 일정 기준

- 서버의 Asia/Seoul 날짜를 기준으로 meet_date가 오늘이거나 미래면 날짜 조건을 충족한다. 일정 다음 날부터 신청·승인을 차단한다.
- meet_date가 null이면 SMALL_GROUP_DATE_NOT_CONFIGURED, 과거면 SMALL_GROUP_MEETING_ENDED로 차단한다. 당일 시간은 LocalDate 데이터에 없으므로 시간 단위 마감은 적용하지 않는다.
- 실제 신청·승인은 잠금 조회한 meet_date로 재검사한다. 일정이 지났거나 없어졌더라도 심사 대기 상태를 자동 변경하지 않으며 해당 승인 요청을 거절한다.
- small_group.status의 ACTIVE/RESTRICTED는 운영 상태로 유지하고 모집 마감 의미로 재사용하지 않는다. DB 컬럼·마이그레이션은 추가하지 않는다.
- 기존 관리자·개설자·운영진 신청 제한과 공개 상세의 MANAGER·중복 관계 오류 처리는 유지한다.

수동 확인: GET /api/small-groups/{smallGroupId}와 POST /api/small-groups/{smallGroupId}/join-requests에 대해 어제·오늘·내일·일정 없음, 빈자리 있음·정원 도달, 본인 관계 상태를 조합해 확인한다. 신청 후 일정이 지났을 때 승인 PATCH도 차단되는지 확인한다.

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


## AI작성: 소모임 공지 작성 (2026-11-04)
- POST /api/small-groups/{smallGroupId}/notices
- 제목 필수·100자 이하, 본문 필수·5,000자 이하. 공백만 입력 불가.
- 작성자는 인증 memberId로 결정한다. 삭제되지 않은 소모임의 개설자이면서 APPROVED OWNER만 허용한다.
- 소모임 ID를 groups ID로 변환해 기존 group_notice에 저장하며 성공 응답은 공통 success=true, data=null이다.
- 브라우저: 모임장 작성·취소·중복 클릭 방지·오류 시 입력 보존·저장 후 조회 재시도 확인.


## AI작성: 소모임 공지 목록 (2026-11-04)
- GET /api/small-groups/{id}?noticePage=1&noticeSort=LATEST
- 목록 요약은 기존 공개 상세와 같은 접근 정책이다. 공지 본문은 제외한다.
- noticePage는 1 이상의 정수, noticeSort는 LATEST 또는 OLDEST. 페이지당 4건, 초과 페이지는 마지막 유효 페이지로 보정한다. 응답 meta는 PageMeta다.
- 메인 공지에서 정렬·3페이지 번호 묶음을 제공한다. 별도 전체 보기 버튼·화면은 없다. 작성 성공 시 최신순 1페이지, URL로 목록 상태를 보존한다.


## AI작성: 소모임 공지 상세 (2026-11-04)
- GET /api/small-groups/{smallGroupId}/notices/{noticeId}
- 로그인한 APPROVED OWNER·MEMBER만 본문을 조회한다. 소모임 기존 MANAGER 제한은 유지한다.
- 소모임 ID를 공통 모임 ID로 변환한 뒤 noticeId와 함께 조회한다. 없는 공지·다른 모임 공지·삭제된 공지는 404다.
- 제목·본문·작성자 닉네임·작성일을 표시하며 목록 복귀 시 페이지·정렬을 유지한다. 본문은 HTML로 해석하지 않는다.


## AI작성: 소모임 공지 수정 (2026-11-04)
- PUT /api/small-groups/{smallGroupId}/notices/{noticeId}, body: title·content. 성공 data는 noticeId다.
- 현재 개설자·APPROVED OWNER이면서 공지 작성자 본인만 수정 가능하다. 제목·본문 제한은 작성과 같다.
- 모임→회원 관계→공지 잠금 순서로 검증하고 SQL에서도 writer_id를 제한한다. 같은 내용은 성공 처리하며 고정·작성자·작성일을 유지한다.
- 브라우저: 수정 폼의 공지 수정 제목, 우측 저장·취소 버튼, 수정 중 목록 복귀 숨김, 권한 변경 오류 시 차단 확인.


## AI작성: 소모임 공지 삭제 (2026-11-04)
- DELETE /api/small-groups/{smallGroupId}/notices/{noticeId}, 성공 data=null.
- 현재 개설자·APPROVED OWNER이면서 작성자 본인만 삭제 가능하다. 수정과 같은 모임→회원 관계→공지 잠금 순서다.
- deleted_at만 갱신하며 최근 목록·페이지 목록·건수·상세에서 제외한다. 재삭제는 404다.
- 삭제 확인창을 거친 뒤 기존 정렬·페이지로 복귀하고 마지막 페이지가 사라지면 서버가 보정한 페이지로 URL을 갱신한다.


## AI작성: 소모임 공지 고정·해제 최종 계약 (2026-11-04)

- PATCH /api/small-groups/{smallGroupId}/notices/{noticeId}/pin, JSON 요청은 pinned: true 또는 false다. 누락·null은 400이다.
- 삭제되지 않은 소모임의 개설자이면서 APPROVED OWNER만 고정을 관리한다. 일반 MEMBER·비회원은 고정 변경을 할 수 없다.
- 작성자 조건은 수정·삭제에 적용하며 고정은 해당 소모임장이 소속 공지에 적용한다.
- 모임→회원 관계→공지 잠금 순서로 같은 모임의 고정 요청을 직렬화한다. 최대 2개를 넘으면 409 SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED다. 같은 상태 재요청은 성공 처리한다.
- 일반 목록·건수는 is_pinned=0 AND deleted_at IS NULL, 고정 목록은 is_pinned=1 AND deleted_at IS NULL을 적용한다. 고정 공지를 삭제하면 고정 목록과 개수에서 제외되어 자리가 확보된다.
- 고정 변경 성공 후 목록만 다시 조회하며 재조회 실패 시 PATCH를 반복하지 않는다. 현재 목록을 확인할 때까지 작성·고정·정렬·페이징을 막고 조회 재시도 버튼을 제공한다.
- 행 우측 44px 핀 버튼, 고정 배지·별도 영역, 최대 개수 초과 모달을 사용한다. PC만 이번 개선 대상으로 했으며 모바일 전용 개선은 하지 않았다.
- 스키마는 기존 group_notice의 is_pinned·updated_at·deleted_at을 그대로 재사용하며 DB 변경은 없다.
