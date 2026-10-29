<!-- AI작성: 기존 계모임·소모임 구현과 사용자 확정 정책을 기준으로 작성한 소모임 공개 상세 조회 설계 문서입니다. -->
# 소모임 상세·설정 조회 API 설계

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
