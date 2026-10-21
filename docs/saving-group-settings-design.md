# 계모임 정보 수정 기능 설계안

> 상태: 정책 확인 반영 · 구현 전 검토용 초안  
> 범위: 계주가 계모임의 기본 정보와 회비·납부일을 수정하는 기능

## 0. 확인한 현재 구조와 전제

- 프론트 설정 화면은 `SavingGroupSettingsPage.jsx` 및 `SavingGroupSettingsForm.jsx`에 있으나 현재 Mock 데이터를 사용한다.
- 백엔드는 `SavingGroupController`의 공개 목록 조회만 구현되어 있다. 설정 조회, 상세 조회, 수정 API는 새로 설계해야 한다.
- 현재 설정 탭은 모든 사용자에게 보이므로, 프론트 노출 제어와 서버 권한 검증을 함께 추가해야 한다. 설정 접근 권한과 상태별 편집 권한은 분리한다.
- 공통 응답은 `ApiResponse`, 업무 예외는 `ApiException`, 전역 처리는 실제 클래스명인 `GlobalExceptionHandler`를 사용한다.
- 지정 범위에서는 JWT/Spring Security, 상태 변경, 가입 승인, 회차 생성의 실제 구현 파일을 찾지 못했다. 이 연계 코드는 구현 전 추가 확인이 필요하다.

---

## 1. 처리 흐름

### 1.1 설정 초기 조회

1. 프론트가 인증 토큰을 포함해 `GET /saving-groups/{id}/settings`를 호출한다.
2. 서버는 인증 사용자와 `group_member` 관계로 계주인지 확인한다.
3. 계주인 경우에만 다음 정보를 반환한다.
   - 현재 적용값: 이름, 소개글, 정원, 월 회비, 납부일
   - 상태 코드, 승인 계원 수, 설정 접근 여부와 수정 가능 필드
   - 진행중 모임의 다음 달 변경 예정값과 적용 시점
4. 프론트는 현재값과 예정값을 별도 표시한다.

### 1.2 상세·설정 접근

- 일반 공개 상세 조회: 삭제되지 않은 `01(개설)`, `02(진행중)`만 허용한다.
- 계주 예외: 본인 모임의 `00(심사중)`, `03(정지)` 상세와 설정에 접근할 수 있다.
- `03(정지)`는 설정 화면을 읽기 전용으로 표시하고, 어떤 필드도 수정할 수 없다.
- `04(종료)`는 수정 불가이며 설정 탭을 숨긴다.
- 프론트는 `canAccessSettings`가 true일 때만 설정 탭을 표시하고, `canEditSettings`가 false이면 모든 입력과 저장 버튼을 비활성화한다. 탭 숨김·비활성화는 보조 UX이며 서버 PATCH에서도 같은 권한과 상태를 반드시 검증한다.

### 1.3 부분 수정 저장

1. 프론트는 초기값과 입력값을 정규화해 비교하고 실제 달라진 필드만 PATCH 본문에 넣는다.
2. 저장 버튼은 변경이 없거나 저장 중이면 비활성화한다.
3. Controller는 `@Valid` 요청 DTO와 인증 사용자 ID를 Service로 전달한다.
4. ServiceImpl은 한 트랜잭션에서 모임·계주 관계·상태·승인 계원 수·실질적 변경 여부를 확인한다.
5. 즉시 적용 필드는 현재 테이블에 UPDATE하고, 다음 달 적용 필드는 예정 변경 테이블에 저장한다.
6. 하나라도 실패하면 전체 롤백한다.
7. 성공 후 프론트는 해당 상세 화면의 `공지` 탭으로 이동한다.

### 1.4 상태별 필드 정책

| 상태 | 즉시 수정 | 다음 달 1일 적용 | 수정 불가 |
|---|---|---|---|
| `00` 심사중 | 이름, 소개글, 정원, 회비, 납부일 | 없음 | 없음 |
| `01` 개설 | 이름, 소개글, 정원, 회비, 납부일 | 없음 | 없음 |
| `02` 진행중 | 이름, 소개글 | 회비, 납부일 | 정원 |
| `03` 정지 | 없음 | 없음 | 전체 |
| `04` 종료 | 없음 | 없음 | 전체 |

### 1.5 다음 달 반영·정지·종료

- 서비스 기준 시간대는 `Asia/Seoul`로 고정한다.
- 진행중 모임에서 회비·납부일을 저장하면 다음 달 1일 `00:00:00 +09:00`을 적용 시점으로 기록한다.
- 반영 작업은 `PENDING` 예정 변경을 잠근 뒤 현재 모임 상태가 `02`인지 재확인하고, 현재값 UPDATE와 예정 행 `APPLIED` 전환을 하나의 트랜잭션으로 처리한다.
- `02 → 03` 상태 변경 시 아직 반영되지 않은 예정값을 `HELD`로 보류한다.
- `03 → 02`로 복구될 때는 기존 적용일이 아직 지나지 않았다면 그 적용일에 반영하고, 이미 지났다면 다음 달 1일로 적용일을 재설정한다.
- 종료 상태 전환 시 예정값을 `CANCELED`로 취소한다.
- 이미 `APPLIED` 상태인 행은 다시 선택하지 않으므로 재실행해도 중복 적용되지 않는다.

---

## 2. 클래스·파일별 책임

| 대상 | 책임 |
|---|---|
| `SavingGroupController` | 설정 조회와 PATCH 엔드포인트, `@Valid`, 인증 사용자 전달, `ApiResponse` 반환 |
| `SavingGroupService` | 설정 조회·부분 수정 계약 정의 |
| `SavingGroupServiceImpl` | 권한·상태·정원·변경 여부 검증, 즉시/예정 변경 분기, 트랜잭션 |
| `SavingGroupMapper` | 잠금 조회, 계주 확인, 승인 인원 집계, UPDATE, 예정값 저장·반영 |
| `SavingGroupMapper.xml` | MyBatis 동적 UPDATE 및 `FOR UPDATE` SQL |
| 요청 DTO | 필드 전달 여부, null, 빈 요청을 구분하고 Bean Validation 수행 |
| 응답 DTO | 현재값, 예정값, 수정 가능 여부, 저장 결과 반환 |
| `SavingGroupSettingsPage` | 설정 조회, 로딩·오류·저장 상태, 성공 후 공지 탭 이동 |
| `SavingGroupSettingsForm` | 상태별 비활성화, 입력 정규화, 변경 필드 추출, 중복 저장 방지 |
| `SavingGroupDetail`/`SavingGroupMenuTabs` | `canAccessSettings`일 때만 설정 탭 표시, `canEditSettings`가 false이면 읽기 전용 표시 |
| 예정값 반영 처리 | KST 기준 반영, 보류·취소·재처리 |

### 예상 변경 파일

- `front/src/pages/SavingGroupSettingsPage.jsx`
- `front/src/components/savingGroup/SavingGroupSettingsForm.jsx`
- `front/src/pages/SavingGroupDetailPage.jsx`
- `front/src/components/savingGroup/SavingGroupDetail.jsx`
- `front/src/components/savingGroup/SavingGroupMenuTabs.jsx`
- `front/src/api/savingGroups.js`
- `back/src/main/java/com/nightstudy/moa/savingGroup/controller/SavingGroupController.java`
- `back/src/main/java/com/nightstudy/moa/savingGroup/service/SavingGroupService.java`
- `back/src/main/java/com/nightstudy/moa/savingGroup/service/SavingGroupServiceImpl.java`
- `back/src/main/java/com/nightstudy/moa/savingGroup/mapper/SavingGroupMapper.java`
- `back/src/main/resources/mappers/SavingGroupMapper.xml`
- `back/src/main/resources/schema.sql` — 예정 변경 테이블 승인이 난 경우에만

### DTO 권장안

- `SavingGroupSettingsPatchRequest`: `@Getter`, `@NoArgsConstructor`, 필드 전달 여부 플래그와 `@JsonSetter`를 사용한다.
- `SavingGroupSettingsResponse`, `PendingSettingChangeResponse`, `SavingGroupSettingsUpdateResponse`: `@Getter`, `@AllArgsConstructor`, Validation 없음.
- 일반 nullable 필드만으로는 JSON의 **미전달**과 **명시적 null**을 구분할 수 없으므로, setter가 호출됐는지 저장하는 boolean 플래그가 필요하다.
- 형식 검증은 DTO의 Bean Validation에서 처리하고, DB 상태·계주 권한·승인 인원·기존값 비교는 ServiceImpl의 업무 검증으로 분리한다.

---

## 3. DTO 및 API 명세

### 3.1 API

| 메서드·경로 | 권한 | 목적 |
|---|---|---|
| `GET /saving-groups/{id}` | 공개, 계주 예외 | 상세 조회 |
| `GET /saving-groups/{id}/settings` | 인증된 계주 | 설정 초기값·예정값 조회 |
| `PATCH /saving-groups/{id}` | 인증된 계주 | 변경한 필드만 수정 |

### 3.2 PATCH 필드 규칙

| 필드 | 타입 | 미전달 | null | 빈 문자열 | 검증 |
|---|---|---|---|---|---|
| `groupName` | string | 기존값 유지 | 오류 | 오류 | 정규화 후 2~20자 |
| `description` | string | 기존값 유지 | 오류 권장 | 삭제 | 정규화 후 최대 40자 |
| `maxMembers` | integer | 기존값 유지 | 오류 | 해당 없음 | 4~20, 승인 인원 이상 |
| `duesAmount` | integer | 기존값 유지 | 오류 | 해당 없음 | 10,000~1,000,000, 정수 |
| `duesDay` | integer | 기존값 유지 | 오류 | 해당 없음 | 1~28, 정수 |

### 3.3 정규화 및 소개글 삭제

- 이름: 기존 생성 규칙 확인 전 권장안은 앞뒤 공백을 제거하고 내부 공백은 유지하는 방식이다.
- 소개글: 앞뒤 공백 제거 후 공백만 남으면 빈 문자열로 저장한다.
- 소개글 삭제: `{ "description": "" }`를 권장한다.
- 미전달: 해당 필드를 변경하지 않는다.
- 명시적 `null`: 소개글을 포함해 오류로 처리하는 방식을 권장한다. 빈 문자열과 역할이 겹치지 않아 API 의미가 명확해진다.

### 3.4 요청 예시

이름 수정:

```json
{ "groupName": "성동 알뜰 계모임" }
```

소개글 삭제:

```json
{ "description": "" }
```

진행중 모임의 다음 달 회비·납부일 변경:

```json
{ "duesAmount": 50000, "duesDay": 15 }
```

### 3.5 설정 조회 성공 응답 권장안

```json
{
  "success": true,
  "data": {
    "savingGroupId": 12,
    "status": "02",
    "canAccessSettings": true,
    "canEditSettings": true,
    "current": {
      "groupName": "성동 알뜰살뜰 계모임",
      "description": "",
      "maxMembers": 10,
      "approvedMemberCount": 8,
      "duesAmount": 30000,
      "duesDay": 10
    },
    "pending": {
      "duesAmount": 50000,
      "duesDay": 15,
      "effectiveAt": "2026-11-01T00:00:00+09:00",
      "status": "PENDING"
    },
    "editableFields": ["groupName", "description", "duesAmount", "duesDay"]
  },
  "meta": null,
  "error": null
}
```

### 3.6 PATCH 성공·실패 응답 예시

저장 결과는 즉시 반영과 예정 반영을 구분해야 하므로 결과 DTO 반환을 권장한다.

```json
{
  "success": true,
  "data": {
    "immediatelyUpdated": ["groupName"],
    "scheduledUpdated": ["duesAmount", "duesDay"],
    "effectiveAt": "2026-11-01T00:00:00+09:00"
  },
  "meta": null,
  "error": null
}
```

```json
{
  "success": false,
  "data": null,
  "meta": null,
  "error": {
    "code": "SAVING_GROUP_NO_CHANGES",
    "message": "실제 변경된 정보가 없습니다."
  }
}
```

데이터가 전혀 필요 없는 엔드포인트라면 기존 규칙대로 `ApiResponse.ok(null)`을 사용한다.

---

## 4. DB 변경 사항

### 4.1 현재 스키마 근거

| 테이블 | 관련 컬럼·용도 |
|---|---|
| `groups` | `group_name`, `description`, `max_members`, `member_id`, `deleted_at` |
| `saving_group` | `dues_amount`, `dues_day`, `status` |
| `group_member` | `member_role`, `membership_status`, `member_id`, `group_id` |
| `meeting_round` | 회차와 납부 기준일 |
| `fee_payment` | 회차별 납부 금액·납부일·납부 상태 |

- `groups.group_name`은 100자, `description`은 50자이므로 요구사항의 20자/40자는 DTO Validation으로 제한할 수 있다.
- `saving_group.dues_amount`는 `DECIMAL(12,2)`이나 API는 쉼표 없는 정수만 받고 정수 값만 저장한다.
- `saving_group.dues_day`의 DB 제약은 1~31이므로, 요구사항의 1~28은 서버 Validation으로 더 좁혀야 한다.

### 4.2 계주·승인 인원 확인

계주 권한은 클라이언트 입력이나 단순 `groups.member_id`만 신뢰하지 않는다. 다음 관계를 우선 확인한다.

```text
group_member.member_id = 인증 사용자 ID
AND member_role = 'OWNER'
AND membership_status = 'APPROVED'
```

승인 인원은 다음 기준으로 집계한다.

```text
COUNT(DISTINCT member_id)
WHERE group_id = :groupId
  AND membership_status = 'APPROVED'
```

모임장이 승인 계원 행으로 이미 등록되어 있으므로 `+1`을 하지 않는다. `PENDING`, `REJECTED`, `LEFT`는 제외한다.

### 4.3 예정값 저장 방식 비교

| 방식 | 장점 | 단점 | 판단 |
|---|---|---|---|
| `saving_group`에 예정 컬럼 추가 | 단순 | 보류·취소·이력·중복 반영 관리가 약함 | 비권장 |
| 별도 예정 변경 테이블 | 현재값·예정값 분리, 상태·재처리 관리 가능 | 스키마 추가 필요 | 권장 |
| 회차 테이블 직접 갱신 | 회차와 연결 가능 | 설정과 회차 생성 책임이 섞임 | 정책 확정 전 보류 |

### 4.4 권장 신규 테이블

테이블명은 예시이며 기존 명명 규칙 확인 후 확정한다.

```text
saving_group_setting_change
- setting_change_id
- group_id
- dues_amount nullable
- dues_day nullable
- effective_at
- status: PENDING / HELD / CANCELED / APPLIED
- applied_at nullable
- canceled_at nullable
```

- 한 모임·한 적용일에 하나의 예정 레코드만 두도록 `(group_id, effective_at)` 고유 제약을 권장한다.
- 같은 적용일에 다시 저장하면 전달된 예정 필드만 덮어쓰고, 미전달 예정 필드는 유지한다.
- **확정:** 현재값으로 되돌리면 해당 필드의 예정값만 제거하고, 두 예정 필드가 모두 비면 예정 레코드를 취소한다.

### 4.5 부분 UPDATE와 동시성

- MyBatis `<set>`과 바인딩 파라미터를 사용해 허용·변경된 필드만 UPDATE한다.
- 조회와 저장 사이 상태가 바뀌지 않도록 대상 모임을 `SELECT ... FOR UPDATE`로 잠근다.
- 잠금 순서는 **모임 → 계원 관계/인원 → 예정 변경**으로 고정한다.
- 정원 변경과 가입 승인은 같은 모임 행 또는 동일한 잠금 순서를 공유해야 정합성을 보장할 수 있다.
- 영향 행 수는 MyBatis/MySQL의 기존 설정을 확인한 뒤 기대값과 비교한다. 변경사항 없음 판단은 UPDATE 결과가 아니라 저장 전 기존값 비교로 처리한다.

### 4.6 기존 납부 정보 연계

- 현재 회차와 과거 `fee_payment`, 거래 내역은 수정하지 않는다.
- 다음 회차 납부 예정 데이터가 이미 만들어지는 구조라면, 아직 납부되지 않은 미래 회차를 언제 갱신할지 정책이 필요하다.
- **확정:** 현재·과거 내역은 유지하고, 아직 생성되지 않은 다음 회차부터 새 기준을 사용한다.

---

## 5. 예외 케이스와 응답

> 아래 업무 에러 코드는 현재 상수로 확인된 값이 아니라 제안이다.

| 발생 조건 | HTTP | 제안 코드 | 사용자 메시지 |
|---|---:|---|---|
| 미인증/JWT 오류 | 401 | `UNAUTHORIZED` | 로그인이 필요합니다. |
| 계주 아님 | 403 | `SAVING_GROUP_SETTINGS_FORBIDDEN` | 계주만 설정을 변경할 수 있습니다. |
| 모임 없음·삭제됨 | 404 | `SAVING_GROUP_NOT_FOUND` | 계모임 정보를 찾을 수 없습니다. |
| 정지·종료 상태 | 409 | `SAVING_GROUP_NOT_EDITABLE` | 현재 상태에서는 계모임 정보를 수정할 수 없습니다. |
| 상태별 수정 불가 필드 | 409 | `SAVING_GROUP_FIELD_NOT_EDITABLE` | 현재 상태에서는 해당 항목을 수정할 수 없습니다. |
| 정원 < 승인 인원 | 409 | `SAVING_GROUP_CAPACITY_BELOW_MEMBERS` | 승인된 계원 수보다 작게 설정할 수 없습니다. |
| 빈 PATCH/null/형식 오류 | 400 | `BAD_REQUEST` | 입력 항목을 확인해주세요. |
| 동일값만 전달 | 409 | `SAVING_GROUP_NO_CHANGES` | 실제 변경된 정보가 없습니다. |
| 동시 수정·상태 충돌 | 409 | `SAVING_GROUP_UPDATE_CONFLICT` | 정보가 변경되었습니다. 다시 확인 후 저장해주세요. |
| UPDATE 행 수 불일치 | 409 또는 500 | `SAVING_GROUP_UPDATE_CONFLICT` | 저장 중 상태가 변경되었습니다. |
| 예상 밖 저장 실패 | 500 | `INTERNAL_ERROR` | 서버 오류가 발생했습니다. |

`ApiException`으로 업무 예외를 던지고 `GlobalExceptionHandler`가 `ApiResponse.fail(code, message)`로 감싼다. JWT 401/403은 실제 Security 필터의 응답 방식과 통일해야 한다.

---

## 6. 기술 선택지와 권장안

| 항목 | 선택지 | 권장안 |
|---|---|---|
| 부분 수정 DTO | 일반 nullable DTO / 외부 `JsonNullable` / 전달 플래그 DTO | 새 라이브러리 없이 전달 플래그 DTO |
| 소개글 삭제 | `null` / 빈 문자열 | 빈 문자열 |
| 예정값 저장 | 본 테이블 미래 컬럼 / 별도 테이블 | 별도 테이블 |
| 반영 실행 | DB Event / Spring 스케줄러 / 수동 처리 | 영속 예정값 + Spring 스케줄러 |
| 적용 시점 | 1일 단발 실행 / 미적용 건 주기 처리 | KST 기준 주기 처리로 재시도 지원 |
| 동시성 | 행 잠금 / 버전 컬럼 | 우선 행 잠금, 필요 시 버전 컬럼 검토 |
| 저장 응답 | `ok(null)` / 결과 DTO | 즉시·예정 결과 DTO |
| 정지 설정 UI | 숨김 / 읽기 전용 | **확정: 계주만 읽기 전용으로 표시** |
| 종료 설정 UI | 숨김 / 읽기 전용 | **확정: 숨김** |

---

## 7. 작업 분할 및 커밋 계획

### 7.1 작업 원칙

- **하나의 커밋은 한 가지 확인 가능한 결과만 포함**한다. 예를 들어 “설정 조회 API”와 “설정 폼 화면 연결”은 별도 커밋이다.
- 백엔드 API를 먼저 완성·검증한 후 해당 API를 사용하는 프론트 작업을 진행한다.
- 프론트 커밋과 백엔드 커밋은 섞지 않는다. 각 작업이 끝난 시점에 개발자가 직접 커밋한다.
- 아래 커밋 제목은 제안이며, 실제 커밋 생성은 하지 않는다.
- 실제 JWT·상태 변경·가입 승인·회차 생성 코드가 없으므로, 해당 기반 기능은 설정 기능보다 먼저 구현되어야 한다.

### 7.2 선행 작업: 다른 브랜치의 상세 조회 재사용

상세 조회는 다른 브랜치에서 이미 작업 중이므로 이 기능에서 새로 구현하지 않는다. 해당 브랜치가 병합된 뒤 기존 API와 페이지 조회 흐름을 그대로 재사용한다.

| 순서 | 구분 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|---|
| 0-1 | 통합 확인 | 다른 브랜치의 상세 조회 API 경로, 응답 필드, 상세 페이지의 재조회 시점을 확인한다. | 저장 직후 상세 페이지 진입 시 최신 정보 조회가 자동으로 발생함을 확인한다. | 커밋 없음 — 병합 후 확인 |
| 0-2 | Backend | JWT에서 로그인 회원 ID를 추출하는 공통 방식을 구현하거나, 상세 조회 브랜치의 기존 인증 방식을 재사용한다. | 요청 DTO의 회원 ID 없이 인증 사용자 ID를 Service에 전달할 수 있다. | `feat(auth): 로그인 회원 식별 기반 추가` — 필요할 때만 |

> 상세 조회 API와 상세 화면 API 연결을 이 작업에서 중복 구현하지 않는다. JWT 기반도 다른 브랜치에 이미 있다면 새로 만들지 않고 기존 방식을 사용한다.

### 7.3 세부 기능 1: 기존 계모임 정보 조회

#### Backend 작업

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 1-BE-1 | 설정 조회 응답 DTO를 추가한다. 현재 적용값, 승인 계원 수, 상태, `canAccessSettings`, `canEditSettings`, 예정값을 정의한다. | 응답에 화면 초기화에 필요한 값만 포함된다. | `feat(saving-group): 설정 조회 응답 DTO 추가` |
| 1-BE-2 | `GET /saving-groups/{id}/settings`의 Mapper 조회를 추가한다. 계주 관계, 삭제 여부, 승인 인원, 현재값을 조회한다. | 계주가 아닌 사용자는 설정 데이터를 받을 수 없다. | `feat(saving-group): 설정 조회 Mapper 추가` |
| 1-BE-3 | Controller·Service·ServiceImpl에 설정 조회 API와 접근·상태 정책을 연결한다. | `03` 계주는 접근 가능하지만 `canEditSettings=false`, `04`는 접근 불가다. | `feat(saving-group): 설정 조회 API 추가` |

#### Frontend 작업

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 1-FE-1 | `api/savingGroups.js`에 설정 조회 함수와 응답 변환을 추가한다. | 공통 `ApiResponse` 실패를 화면용 오류로 변환한다. | `feat(saving-group): 설정 조회 API 모듈 추가` |
| 1-FE-2 | `SavingGroupSettingsPage`에서 Mock 조회를 제거하고 로딩·오류·빈 상태를 처리한다. | 직접 URL 접근도 서버 권한 결과에 맞게 처리된다. | `feat(saving-group): 설정 페이지 조회 연결` |
| 1-FE-3 | `SavingGroupSettingsForm`에 현재값과 예정값을 표시한다. | 진행중 회비·납부일에는 현재값과 예정값이 구분되어 보인다. | `feat(saving-group): 설정 폼 초기값 표시` |
| 1-FE-4 | 상세 메뉴에서 `canAccessSettings`를 기준으로 설정 탭을 노출한다. | 비계주는 탭을 보지 못하고, `03` 계주는 읽기 전용 화면을 본다. | `feat(saving-group): 설정 탭 접근 상태 반영` |

### 7.4 세부 기능 2: 변경된 값만 수정

#### Backend 작업: 즉시 반영 필드

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 2-BE-1 | 부분 수정 요청 DTO를 추가한다. 미전달·null·빈 문자열을 구분하고 형식 Validation을 적용한다. | 빈 PATCH와 잘못된 값이 `400`으로 처리된다. | `feat(saving-group): 부분 수정 요청 DTO 추가` |
| 2-BE-2 | 모임 잠금 조회, 계주 확인, 승인 계원 수 집계 Mapper를 추가한다. | 정원과 가입 승인 경쟁 시 일관된 검증 기반을 갖춘다. | `feat(saving-group): 설정 수정 검증 Mapper 추가` |
| 2-BE-3 | 이름·소개글·정원의 상태별 업무 검증과 동적 UPDATE를 ServiceImpl에 구현한다. | `00`, `01`의 허용 필드와 `02`의 이름·소개글만 수정된다. `03`, `04`는 전체 차단된다. | `feat(saving-group): 즉시 설정 수정 API 추가` |
| 2-BE-4 | 저장 전 정규화·기존값 비교와 영향 행 수 검증을 추가한다. | 동일값만 전달하면 `409`, 실제 변경이 하나라도 있으면 해당 필드만 저장된다. | `feat(saving-group): 설정 변경 검증 추가` |

#### Frontend 작업: 변경 필드 PATCH

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 2-FE-1 | 폼의 초기값 스냅샷과 입력값을 비교해 변경 필드만 만드는 로직을 추가한다. | 미전달 필드가 PATCH 본문에 포함되지 않는다. | `feat(saving-group): 변경 필드 추출 추가` |
| 2-FE-2 | 이름·소개글·정원 입력 검증과 소개글 빈값 안내를 연결한다. | 클라이언트 오류를 보여도 입력값은 유지된다. | `feat(saving-group): 설정 폼 입력 검증 추가` |
| 2-FE-3 | PATCH 요청, 저장 중 중복 클릭 방지, 서버 오류 표시를 연결한다. | 저장 중 버튼이 비활성화되고 실패 시 폼을 유지한다. | `feat(saving-group): 설정 수정 API 연결` |
| 2-FE-4 | `03`은 모든 입력·저장 버튼을 비활성화하고 읽기 전용 안내를 표시한다. | URL을 직접 열어도 정지 모임을 프론트에서 수정할 수 없다. | `feat(saving-group): 정지 모임 설정 읽기 전용 처리` |

### 7.5 세부 기능 3: 진행중 회비·납부일의 다음 달 적용

#### Backend 작업

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 3-BE-1 | 예정 변경 테이블 DDL과 예정값 DTO를 추가한다. | 현재값과 예정값을 별도로 저장할 수 있다. | `feat(saving-group): 회비 변경 예정값 스키마 추가` |
| 3-BE-2 | 진행중 모임의 회비·납부일 PATCH를 예정값 upsert로 구현한다. | 해당 필드는 현재 `saving_group` 값이 아닌 다음 달 예정값으로 저장된다. | `feat(saving-group): 진행중 회비 변경 예약 저장` |
| 3-BE-3 | KST 기준 예정값 반영 작업과 중복 적용 방지를 구현한다. | 재실행해도 `APPLIED` 예정값은 다시 반영되지 않는다. | `feat(saving-group): 회비 변경 예정값 반영 처리` |
| 3-BE-4 | 상태 변경에 `PENDING → HELD`, 종료 시 `CANCELED`, 복구 시 적용일 재설정을 연계한다. | 정지·종료·복구 정책이 확정된 규칙대로 동작한다. | `feat(saving-group): 예정값 상태 변경 연계` |

#### Frontend 작업

| 순서 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|
| 3-FE-1 | 진행중 모임에서 회비·납부일 아래에 다음 회차 적용 안내를 표시한다. | 사용자가 즉시 적용되지 않음을 알 수 있다. | `feat(saving-group): 예정 변경 안내 표시` |
| 3-FE-2 | 예정값이 있으면 현재값과 예정값을 함께 표시하고 재수정 값을 PATCH에 포함한다. | 전달하지 않은 다른 예정 필드는 유지된다. | `feat(saving-group): 예정 회비 설정 화면 연결` |
| 3-FE-3 | 현재값으로 되돌렸을 때 해당 예정 필드만 취소하는 화면 상태를 연결한다. | 다른 예정 필드는 사라지지 않는다. | `feat(saving-group): 예정 변경 취소 화면 연결` |

### 7.6 세부 기능 4: 저장 후 상세 메인 페이지의 공지 탭 이동

| 순서 | 구분 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|---|
| 4-FE-1 | Frontend | PATCH 성공 후 `navigate('/saving-groups/{id}')`로 이동한다. | 저장 API를 다시 호출하지 않고 상세 메인 페이지로 이동한다. | `feat(saving-group): 저장 후 상세 페이지 이동` |
| 4-FE-2 | 통합 확인 | 이동한 상세 페이지의 기존 조회 흐름이 API를 다시 호출하고, 공지 탭이 기본으로 선택되는지 확인한다. | 수정한 즉시값과 적용된 예정값이 최신 상세 데이터로 보인다. | 커밋 없음 — 기존 상세 조회 재사용 |

> 현재 상세 페이지의 기본 탭이 공지이므로 별도 `?tab=notice` 처리 없이 상세 메인 경로로 이동한다. 다른 브랜치 구현에서 기본 탭 정책이 달라진 경우에만 쿼리 파라미터 방식을 추가 검토한다.

### 7.7 통합 확인과 문서화

| 순서 | 구분 | 작업 내용 | 완료 기준 | 제안 커밋 제목 |
|---:|---|---|---|---|
| 5-1 | Backend | 상태·권한·동시성·예정값 반영 시나리오를 검증한다. | API 응답·DB 결과가 설계안과 일치한다. | `test(saving-group): 설정 수정 시나리오 추가` |
| 5-2 | Frontend | 로딩·오류·읽기 전용·중복 클릭·상세 복귀를 브라우저에서 검증한다. | 사용자 흐름이 끊기지 않는다. | `test(saving-group): 설정 화면 흐름 검증` |
| 5-3 | Docs | API/DB 명세와 AI 사용 기록을 팀 규칙에 맞춰 갱신한다. | 구현 결과와 문서가 일치한다. | `docs(saving-group): 설정 수정 명세 갱신` |

---

## 8. 테스트 시나리오

- [ ] 계주 정상 수정과 비계주 접근 차단
- [ ] 비계주에게 설정 탭 미표시
- [ ] `00`, `03` 상태 계주의 상세·설정 접근
- [ ] 상태별 허용 필드와 `03` 정지·`04` 종료 수정 차단
- [ ] `03` 정지 계주의 설정 화면 읽기 전용 표시와 PATCH 직접 호출 차단
- [ ] 진행중 정원 변경 차단
- [ ] 삭제된 모임과 존재하지 않는 ID
- [ ] 이름·소개글·정원·회비·납부일의 정상·실패·경계값
- [ ] 소개글 앞뒤 공백 제거, 공백만 입력, 삭제, 화면 안내 문구
- [ ] 회비 정수 검증과 `10,000원` 표시
- [ ] 승인 인원과 같은 정원 허용 및 그보다 작은 정원 차단
- [ ] 모임장 포함, 대기·탈퇴 제외 인원 집계
- [ ] 미전달 필드 보존, null·빈 문자열 구분
- [ ] 빈 요청·동일값 요청 오류와 저장 버튼 비활성화
- [ ] 이름·소개글 즉시 반영
- [ ] 회비·납부일 다음 달 1일 KST 반영과 현재 회차 내역 유지
- [ ] 예정값 재수정 및 미전달 예정 필드 유지
- [ ] 정지 시 보류, 종료 시 취소
- [ ] 월말 저장·월초 반영 시간 경계
- [ ] 반영 작업 중복 실행과 실패 후 재처리
- [ ] 동시 수정, 정원 변경·가입 승인 충돌, 저장 실패 롤백
- [ ] 안내 문구, 중복 저장 방지, 오류 시 입력값 유지
- [ ] 저장 후 상세 공지 탭 이동
- [ ] `ApiResponse` 및 예외 응답 형식 준수

---

## 9. 정책 확인이 필요한 항목

### 9.1 이미 생성된 다음 회차 납부 예정 정보 — 확정

- **결정:** 현재·과거 내역은 유지하고, 아직 생성되지 않은 다음 회차부터 새 회비·납부일을 사용한다.
- **구현 영향:** 기존 다음 회차 정보를 수정하지 않으므로, 향후 회차 생성 시점에 적용 중인 회비·납부일을 읽도록 설계한다.

### 9.2 정지 해제 후 보류 예정값 — 확정

- **결정:** `03 → 02` 복구 시 기존 적용일이 지나지 않았다면 기존 적용일에 반영하고, 지났다면 다음 달 1일로 적용일을 재설정한다.
- **구현 영향:** 상태 복구 트랜잭션에서 HELD 예정값의 `effective_at`을 KST 기준으로 비교·갱신한다.

### 9.3 현재값으로 되돌리는 예정 변경 취소 — 확정

- **결정:** 해당 예정 필드만 취소하고, 다른 예정 필드는 유지한다.
- **구현 영향:** 두 예정 필드가 모두 없어진 경우에만 예정 변경 레코드를 `CANCELED` 처리한다.

### 9.4 연계 코드 현황

현재 해당 구현이 없으므로, 아래 기능은 이번 구현 범위에서 새로 설계해야 한다.

1. JWT/Spring Security 인증 사용자 ID 추출 — 계주 권한 및 401/403 처리의 선행 기능
2. 계모임 상태 변경 — 정지 보류·종료 취소 연계 기능
3. 가입 승인 — 정원 변경과 동시성 제어의 연계 기능
4. 다음 회차·납부 예정 생성 — 확정된 새 회비·납부일을 미래 회차에 적용하는 기능
5. 계모임 생성 요청 DTO·검증 — 이름 공백 처리 규칙을 이번 수정 DTO와 일치시키기 위한 참고 기능
