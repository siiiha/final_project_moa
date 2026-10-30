<!-- AI작성: 기존 계모임·소모임 구현과 사용자 확정 정책을 기준으로 작성한 소모임 상세·설정·가입 신청 API 설계 문서입니다. -->
# 소모임 상세·설정·가입 신청 API 설계

## 범위

- 엔드포인트: `GET /api/small-groups/{id}`
- `{id}`는 `small_group.small_group_id`이며 양수여야 한다.
- 소모임 상세 자체는 비로그인 사용자와 비회원도 조회할 수 있다.
- 최근 공지 요약은 포함하지만, 공지 본문·공지 목록·공지 상세·일정 목록·가입 신청 내역은 포함하지 않는다.

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
    ]
  }
}
```

`myRole`은 로그인한 요청자의 승인된 소모임 역할만 반환한다. 비로그인 사용자, 비회원, `PENDING` 또는 `REJECTED` 상태는 `null`이다.

`notices`는 작성일 내림차순, 같은 작성 시각에서는 공지 ID 내림차순으로 정렬된 최신 4건 이하의 목록이다. 비회원도 공지 ID·제목·작성자 닉네임·작성일은 볼 수 있지만, 공지 본문과 공지 상세 이동은 이 API 범위에 포함하지 않는다.

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

Spring Security에서 숫자 ID의 `GET /api/small-groups/{id}`만 공개한다. 소모임 생성, 지역·카테고리 목록 등 다른 `/api/small-groups/**` 경로에는 기존 보안 정책을 그대로 적용한다.

## 상세 화면 탭

| 조회자 역할 | 노출 탭 |
| --- | --- |
| 비로그인·비회원 | 공지 |
| `MEMBER` | 공지, 길찾기, 투표, 후기 |
| `OWNER` | MEMBER 탭, 설정, 신청 목록 |
| `MANAGER` | API에서 403 처리하므로 화면을 표시하지 않음 |

비회원에게도 모임 날짜와 장소를 포함한 기본 상세 정보를 노출한다.

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
4. 공지 전체 보기·일정 확인·길찾기는 후속 API가 구현되기 전까지 이동하지 않는지 확인한다.
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
