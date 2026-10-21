# 계모임 가입 신청 기능 구현 전 설계안

## 1. 문서 목적과 작업 범위

이 문서는 계모임 상세 화면의 **가입 신청** 기능을 구현하기 직전에 사용하는 작업 기준서이다.

- 가입 신청은 가입 완료가 아니라 `PENDING` 상태의 대기 등록이다.
- 승인, 거절, 자진 탈퇴, 추방 자체는 이번 작업 범위에 포함하지 않는다.
- JWT 인증, 로그인, 계모임 개설 시 모임장 생성, 공통 모달 변경 등 연계 기능은 다른 브랜치 또는 다른 담당자가 구현한다.
- 이 기능 작업에서는 연계 기능을 새로 만들지 않고, 기존 구현이 제공하는 인증 사용자 정보와 공통 UI 흐름에만 연결한다.
- 요청받은 세부 작업만 수정하며, 커밋은 개발자가 직접 수행한다.

> `SavingGroupApplicationPage.jsx`는 계모임 **개설 신청** 화면이다. 가입 신청 기능의 프론트엔드 대상은 `SavingGroupDetailPage.jsx`의 `가입 신청하기` 버튼이다.

## 2. 확정 정책

### 2.1 신청 가능 조건

가입 신청은 아래 조건을 모두 만족해야 한다.

1. JWT로 인증된 로그인 회원이다.
2. 회원 계정 상태가 `ACTIVE`이다.
3. `identity_verified_at`이 존재하는 본인 인증 완료 회원이다.
4. 계모임이 존재하며 삭제되지 않았다.
5. 계모임 상태가 `'01'`이다.
6. 계모임 정원(`max_members`)이 설정되어 있다.
7. 승인된 계원 수가 정원 미만이다.
8. 최대 5개 가입 제한을 넘지 않는다.

### 2.2 계모임 상태별 신청 가능 여부

| 상태 코드 | 상태 | 가입 신청 |
| --- | --- | --- |
| `'00'` | 심사중 | 불가 |
| `'01'` | 개설 | 가능 |
| `'02'` | 진행중 | 불가 |
| `'03'` | 정지 | 불가 |
| `'04'` | 종료 | 불가 |

### 2.3 기존 관계 상태별 처리

| `membership_status` | 처리 |
| --- | --- |
| 관계 없음 | `PENDING` 행 신규 저장 |
| `APPROVED` | 차단 — 이미 가입한 계모임 |
| `PENDING` | 차단 — 이미 가입 신청한 계모임 |
| `REJECTED` | 차단 — 재신청 불가 |
| `EXPELLED` | 차단 — 재신청 불가 |
| `LEFT` | 재신청 허용 — 기존 행을 `PENDING`으로 갱신 |

### 2.4 최대 5개 가입 수 집계 기준

다음 조건의 승인 관계만 집계한다.

```text
group_member.membership_status = APPROVED
AND groups.deleted_at IS NULL
AND saving_group.status IN ('00', '01', '02', '03')
```

포함 대상:

- 승인된 일반 계원 모임
- 승인된 계주 모임
- 심사중(`'00'`), 개설(`'01'`), 진행중(`'02'`), 정지(`'03'`) 모임

제외 대상:

- 대기(`PENDING`), 거절(`REJECTED`), 자진 탈퇴(`LEFT`), 추방(`EXPELLED`) 관계
- 종료(`'04'`) 모임
- 삭제된 모임

대기 신청은 5개 제한에 포함하지 않는다. 단, 추후 승인 기능은 승인 시점에 동일 기준으로 다시 검증해야 한다.

## 3. API 설계

### 3.1 API

```http
POST /api/saving-groups/{savingGroupId}/join-requests
Authorization: Bearer {JWT}
```

요청 본문은 없다.

| 값 | 전달 또는 결정 방식 |
| --- | --- |
| 계모임 ID | URL 경로 변수 |
| 신청 회원 ID | JWT Principal에서 서버가 조회 |
| 본인 인증 여부 | 회원 데이터에서 서버가 조회 |
| 신청 상태 | 서버가 `PENDING`으로 결정 |
| 역할 | 서버가 `MEMBER`로 결정 |
| 신청 시각 | DB 기본값 또는 서버 시간 |

클라이언트는 `memberId`, `memberRole`, `membershipStatus`를 보내지 않는다.

### 3.2 성공 응답

기존 `ApiResponse` 규칙을 따르며, 응답 데이터가 없으므로 `ApiResponse.ok(null)`을 사용한다.

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "error": null
}
```

## 4. 처리 흐름

### 4.1 프론트엔드

```text
가입 신청 버튼 클릭
→ 로그인 여부 확인
→ 비로그인: 로그인 화면으로 이동하며 현재 상세 경로 전달
→ 로그인 성공: 기존 계모임 상세 화면으로 복귀
→ 사용자가 다시 가입 신청 버튼 클릭
→ 요청 중 버튼 비활성화
→ 가입 신청 API 호출
→ 성공: “가입 신청이 완료되었습니다.” 모달 표시
→ 확인: 현재 상세 화면 유지
```

로그인 성공 직후 가입 신청 API를 자동 호출하지 않는다.

본인 인증 미완료 오류를 받으면 다음 흐름을 사용한다.

```text
본인 인증 미완료 오류 수신
→ 안내 모달 표시
→ “본인 인증 완료 후 신청이 가능합니다.” 안내
→ 사용자가 “인증하러 가기” 버튼 선택
→ /member/verify 이동
```

### 4.2 백엔드

```text
JWT Principal에서 로그인 회원 ID 확인
→ 회원 존재, ACTIVE 상태, 본인 인증 여부 확인
→ 회원 행 잠금
→ 계모임 행 잠금
→ 계모임 존재, 삭제, 상태('01'), 정원 확인
→ 기존 group_member 관계 확인
→ 승인 계원 수 확인
→ 최대 5개 가입 수 확인
→ 신규 PENDING INSERT 또는 LEFT → PENDING UPDATE
→ 영향 행 수 확인
→ ApiResponse.ok(null) 반환
```

## 5. 트랜잭션 및 동시성

가입 신청 ServiceImpl의 업무 검증과 저장은 하나의 트랜잭션으로 처리한다.

```text
회원 잠금
→ 계모임 잠금
→ 기존 관계 검증
→ 정원 검증
→ 가입 수 검증
→ INSERT 또는 UPDATE
```

신청과 승인 기능은 모두 아래 순서로 잠금을 사용한다.

```text
회원 → 계모임
```

이 기준은 동일 회원의 여러 모임 동시 처리, 동일 계모임 동시 신청, 정원 초과, 최대 5개 초과를 방지하기 위한 것이다. 단순한 `SELECT` 후 `INSERT`만으로는 동시 중복 신청을 막을 수 없다.

## 6. 저장 정책

### 6.1 신규 신청

```text
member_id             = JWT 인증 회원 ID
group_id              = saving_group 조회 결과의 group_id
member_role           = MEMBER
membership_status     = PENDING
application_message   = NULL
requested_at          = 현재 시각
joined_at             = NULL
left_at               = NULL
```

현재 `member_role`이 `NOT NULL`이므로 대기 상태에도 `MEMBER`를 저장한다. 단, 실제 모임 기능의 권한 확인은 역할만 보지 않고 반드시 `membership_status = APPROVED`를 함께 확인해야 한다.

### 6.2 자진 탈퇴 후 재신청

이력 보존 없이 기존 `LEFT` 행을 아래 값으로 갱신한다.

```text
membership_status     = PENDING
member_role           = MEMBER
requested_at          = 재신청 시각
joined_at             = NULL
left_at               = NULL
application_message   = NULL
```

### 6.3 추후 승인 기능의 책임

승인 기능은 본 작업 범위 밖이지만, 다음을 승인 직전에 다시 검증해야 한다.

```text
PENDING 상태인지 확인
→ 계모임 상태 확인
→ 정원 확인
→ 최대 5개 제한 확인
→ APPROVED 변경
→ MEMBER 역할 확정
→ joined_at 저장
```

## 7. DB 의존 조건

다음 DB 반영은 다른 담당 작업의 결과물로 전제한다. 가입 신청 구현 작업에서 임의로 반영하지 않는다.

```text
membership_status에 EXPELLED 추가
UNIQUE(member_id, group_id) 추가
group_member(group_id, membership_status) 인덱스 검토
group_member(member_id, membership_status) 인덱스 검토
```

또한 계모임 개설 시 모임장이 아래 관계로 저장되어 있어야 한다.

```text
member_role = OWNER
membership_status = APPROVED
```

그래야 모임장이 정원과 최대 5개 가입 수에 정상 포함된다.

## 8. 예외 정책

| 조건 | HTTP | 코드 | 메시지 |
| --- | ---: | --- | --- |
| JWT 없음 또는 유효하지 않음 | 401 | `UNAUTHORIZED` | 로그인이 필요합니다. |
| 회원 없음, 비활성, 정지 | 403 | `MEMBER_ACCOUNT_UNAVAILABLE` | 현재 계정 상태에서는 가입 신청할 수 없습니다. |
| 본인 인증 미완료 | 403 | `IDENTITY_VERIFICATION_REQUIRED` | 본인 인증 완료 후 가입 신청할 수 있습니다. |
| 잘못된 계모임 ID | 400 | `BAD_REQUEST` | 계모임 번호가 올바르지 않습니다. |
| 계모임 없음 또는 삭제됨 | 404 | `SAVING_GROUP_NOT_FOUND` | 계모임을 찾을 수 없습니다. |
| 상태가 `'01'`이 아님 | 409 | `SAVING_GROUP_JOIN_UNAVAILABLE` | 현재 가입 신청할 수 없는 계모임입니다. |
| 정원 미설정 | 409 | `SAVING_GROUP_CAPACITY_NOT_CONFIGURED` | 계모임 정원이 설정되지 않았습니다. |
| 이미 가입한 회원 또는 계주 | 409 | `ALREADY_JOINED_SAVING_GROUP` | 이미 가입한 계모임입니다. |
| 이미 대기 신청함 | 409 | `JOIN_REQUEST_ALREADY_PENDING` | 이미 가입 신청한 계모임입니다. |
| 거절 또는 추방 이력 | 409 | `JOIN_REAPPLICATION_FORBIDDEN` | 재신청할 수 없는 계모임입니다. |
| 정원 도달 | 409 | `SAVING_GROUP_CAPACITY_FULL` | 계모임 정원이 모두 찼습니다. |
| 최대 5개 가입 | 409 | `SAVING_GROUP_JOIN_LIMIT_EXCEEDED` | 가입 가능한 계모임 수는 최대 5개입니다. |
| 저장 영향 행 수 불일치 | 500 | `JOIN_REQUEST_CREATE_FAILED` | 가입 신청 처리에 실패했습니다. |
| 유니크 제약 충돌 | 409 | `JOIN_REQUEST_DUPLICATED` | 이미 가입 신청한 계모임입니다. |

## 9. 요청 가능한 세부 구현 작업

아래 번호 중 필요한 작업만 개별 요청한다. 연계 기능은 구현하지 않는다.

### BE-01. 가입 신청 조회 모델과 Mapper 계약

**범위**

- 가입 신청에 필요한 회원, 계모임, 기존 관계 조회 모델 정의
- `SavingGroupMapper` 조회·저장 메서드 계약 추가

**예상 파일**

```text
SavingGroupMapper.java
가입 신청 조회 DTO 파일들(신규, 이름은 기존 규칙 확인 후 확정)
```

### BE-02. 가입 신청 Mapper XML SQL

**범위**

- 회원·계모임 잠금 조회
- 기존 관계, 정원, 최대 5개 집계 SQL
- 신규 신청 INSERT 및 LEFT 재신청 UPDATE SQL

**예상 파일**

```text
SavingGroupMapper.xml
```

### BE-03. Service 계약

**범위**

- 가입 신청 Service 메서드 선언만 추가

**예상 파일**

```text
SavingGroupService.java
```

### BE-04. ServiceImpl 가입 신청 업무 로직

**범위**

- 트랜잭션, 업무 검증, 예외 처리, INSERT/UPDATE 영향 행 검증

**예상 파일**

```text
SavingGroupServiceImpl.java
```

### BE-05. 가입 신청 Controller API

**범위**

- `POST /saving-groups/{savingGroupId}/join-requests` 추가
- 인증 Principal에서 회원 ID를 받아 Service에 전달
- `ApiResponse.ok(null)` 반환

**예상 파일**

```text
SavingGroupController.java
```

### FE-01. 가입 신청 API 함수

**범위**

- `requestSavingGroupJoin(savingGroupId)` API 함수 추가

**예상 파일**

```text
front/src/api/savingGroups.js
```

### FE-02. 가입 버튼 중복 클릭 방지

**범위**

- 요청 중 버튼 비활성화와 로딩 상태 표시

**예상 파일**

```text
front/src/components/savingGroup/SavingGroupDetail.jsx
```

### FE-03. 상세 페이지 가입 신청 API 연결

**범위**

- 실제 API 호출
- 성공 시에만 성공 모달 표시
- 성공 모달 종료 뒤 상세 페이지 유지
- 일반 오류 표시

**예상 파일**

```text
front/src/pages/SavingGroupDetailPage.jsx
```

### FE-04. 본인 인증 필요 안내

**범위**

- `IDENTITY_VERIFICATION_REQUIRED` 오류 시 안내 모달
- 사용자가 선택하면 `/member/verify` 이동

**예상 파일**

```text
front/src/pages/SavingGroupDetailPage.jsx
```

공통 모달 기능 변경이 필요할 경우에는 해당 변경을 별도 작업으로 분리한다.

### FE-05. 로그인 후 상세 페이지 복귀 연결

**범위**

- 비로그인 사용자의 상세 경로 전달
- 로그인 완료 후 상세 페이지 복귀
- 자동 가입 신청 금지

**의존 조건**

```text
다른 담당자가 로그인 성공 처리와 인증 상태 제공을 구현한 상태
```

## 10. 작업 요청 예시

```text
BE-04만 구현해줘.
SavingGroupServiceImpl에서 가입 신청 검증과 LEFT 재신청 처리만 수정해줘.
다른 파일과 연계 기능은 수정하지 마.
```

```text
FE-03만 구현해줘.
SavingGroupDetailPage.jsx에서 가입 신청 API 성공 후 모달을 표시하도록 수정해줘.
다른 화면이나 로그인 기능은 수정하지 마.
```

## 11. 작업 완료 시 검증 기준

- 정상 신청은 `PENDING`으로 저장된다.
- `APPROVED`, `PENDING`, `REJECTED`, `EXPELLED` 상태는 각각 정책대로 처리된다.
- `LEFT` 상태는 기존 행을 갱신해 재신청할 수 있다.
- 정원은 승인 계원만 포함해 계산한다.
- 최대 5개는 확정된 상태·삭제 기준으로 계산한다.
- 대기 신청은 정원을 예약하거나 5개 제한에 포함하지 않는다.
- 성공 모달 문구는 `가입 신청이 완료되었습니다.`이다.
- 성공 모달을 닫은 뒤 현재 상세 화면에 남는다.
