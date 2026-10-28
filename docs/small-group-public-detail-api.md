<!-- AI작성: 기존 계모임·소모임 구현과 사용자 확정 정책을 기준으로 작성한 소모임 공개 상세 조회 설계 문서입니다. -->
# 소모임 공개 상세 조회 API 설계

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
