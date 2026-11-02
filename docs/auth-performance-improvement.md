# 인증 조회 성능 개선 기록

## 1. 목적

원격 Aiven MySQL을 사용하는 환경에서 로그인한 보호 API 요청마다 반복되던 인증 상태 조회를 줄인다.

MySQL은 회원·세션·Refresh Token·로그아웃 이력의 원본 저장소로 유지한다. Aiven Valkey는
반복 인증 조회를 위한 보조 캐시로만 사용한다.

## 2. 확인된 기존 상황

- Aiven MySQL의 CPU, Load average, 디스크 사용량은 병목 수준이 아니었다.
- Workbench에서 원격 DB `SELECT 1`과 계모임 목록 COUNT 조회는 약 0.2~0.3초가 걸렸다.
- MySQL Query statistics의 DB 엔진 평균 시간은 수 ms 수준이었다.
- 따라서 데이터 건수나 DB CPU보다 로컬 백엔드와 Aiven 간 네트워크 왕복 및 인증용 반복 조회의 영향이 컸다.

기존 보호 API 인증 흐름은 다음과 같았다.

```text
JWT 서명·만료 확인
→ MySQL: revoked_token 조회
→ MySQL: member_auth_session + members 조회
→ 실제 API 처리
```

## 3. 적용한 개선

### 공개 목록 API 인증 조회 제외

로그인 여부에 따라 결과가 달라지지 않는 공개 목록 API는 JWT 필터를 건너뛴다.

- `GET /api/saving-groups`
- `GET /api/small-groups`
- `GET /api/community/list`
- `GET /api/post/{postId}/comments`

만료되었거나 로그아웃한 Access Token이 함께 전달돼도 위 공개 목록은 익명 요청으로 조회된다.

### MySQL 인증 조회 단일화

보호 API의 MySQL 대체 조회는 세션, 회원 상태, 폐기 토큰을 하나의 쿼리에서 확인한다.

```text
member_auth_session + members
AND NOT EXISTS (revoked_token)
```

### Valkey 인증 캐시

Valkey에는 원문 Access Token·Refresh Token·비밀번호를 저장하지 않는다.

| 키 | 값 | TTL |
| --- | --- | --- |
| `auth:session:{sessionId}` | sessionId, memberId, systemRole, expiresAtMs | 세션 만료 시각까지 |
| `auth:member-sessions:{memberId}` | 해당 회원의 sessionId 집합 | 가장 늦게 만료되는 세션까지 |
| `auth:revoked-token:{tokenId}` | `1` | Access Token 만료 시각까지 |

로그인 또는 Refresh Token 재발급 시에는 MySQL 트랜잭션이 성공적으로 커밋된 뒤 Valkey 세션 캐시를 저장한다.

로그아웃 또는 전체 세션 폐기 시에는 오래된 캐시가 인증에 재사용되지 않도록 Valkey 캐시를 먼저 무효화하고,
그 뒤 기존 MySQL 세션 폐기·폐기 토큰 기록을 처리한다.

## 4. 현재 인증 흐름

```text
보호 API 요청
→ JWT 서명·만료 확인
→ Valkey: 폐기 tokenId 확인
   → 존재하면 401
→ Valkey: sessionId의 회원 번호·역할·만료 시각 확인
   → JWT와 일치하면 인증 통과
→ 캐시 미스·정보 불일치·Valkey 장애
   → MySQL 단일 인증 조회
   → 정상 결과면 Valkey 세션 캐시 복구
```

Valkey 읽기 장애 시 보호 API는 MySQL 단일 인증 조회로 대체된다.

로그아웃·전체 세션 폐기처럼 인증 상태를 바꾸는 요청에서 Valkey 캐시 무효화에 실패하면,
오래된 캐시를 허용하지 않도록 해당 요청을 실패 처리한다. 연결이 복구된 뒤 다시 요청해야 한다.

## 5. 로컬 실행 설정

IntelliJ의 Spring Boot 실행 설정에만 아래 환경변수를 등록한다. 비밀번호는 Git, 코드, 문서, 채팅에 기록하지 않는다.

```text
SPRING_DATA_REDIS_HOST=moa-auth-cache-finalproject-db.aivencloud.com
SPRING_DATA_REDIS_PORT=23752
SPRING_DATA_REDIS_USERNAME=default
SPRING_DATA_REDIS_PASSWORD=개인_Aiven_Valkey_비밀번호
SPRING_DATA_REDIS_SSL_ENABLED=true
```

`SPRING_DATA_REDIS_SSL_ENABLED=true`은 Aiven의 `rediss://` TLS 연결에 필요하다.

## 6. 검증 절차

1. 환경변수를 등록한 뒤 백엔드를 재시작한다.
2. 로그인한다.
3. 보호 API(예: `/api/auth/me`)를 호출한다.
4. MyBatis 로그에서 `countActiveSession` 인증 SQL이 호출되지 않는지 확인한다.
5. 로그아웃 후 기존 Access Token으로 보호 API를 호출해 `401`인지 확인한다.
6. 공개 목록 API를 만료·로그아웃 토큰과 함께 호출해도 `200`으로 익명 조회되는지 확인한다.
7. Valkey 연결을 일시적으로 끊은 상태에서 보호 API가 MySQL 단일 인증 조회로 정상 동작하는지 확인한다.

자동 검증은 `AuthSessionServiceImplTest`에 다음 네 경우를 포함한다.

- 캐시된 폐기 토큰 차단
- 일치하는 캐시 세션의 MySQL 조회 생략
- 캐시 미스 후 MySQL 조회 및 캐시 복구
- Valkey 장애 시 MySQL 대체

## 7. 검증 상태와 주의사항

- 코드 형식 검사는 `git diff --check`로 통과했다.
- Gradle 테스트는 Gradle Wrapper 잠금 파일 접근 거부로 아직 실행 결과를 확보하지 못했다.
- 실제 백엔드 재시작, Aiven Valkey 연결, API 응답 시간 비교는 아직 수행 전이다.
- 현재 백엔드는 로컬 PC에서 실행되고 Valkey는 Aiven에 있으므로, Valkey도 네트워크 왕복이 발생한다.
  따라서 로컬 체감 속도 향상은 실측으로 확인해야 하며, 가장 큰 네트워크 지연 개선은 백엔드를 Aiven과 가까운 리전에 배포할 때 기대할 수 있다.
- 현재 구현은 폐기 토큰 확인과 세션 확인을 각각 수행한다. 실측에서 Redis 왕복 시간이 병목이면
  두 조회를 파이프라인 또는 단일 Lua 스크립트로 묶는 후속 최적화를 검토한다.

## 8. 관련 커밋

```text
feat: 공개 목록 API 인증 DB 조회 제외
refactor: 인증 상태 DB 조회를 단일 쿼리로 통합
feat: Valkey 인증 캐시 연결 의존성 추가
feat: 인증 상태 캐시 인터페이스 추가
feat: Valkey 인증 상태 캐시 구현
feat: 로그인 세션 Valkey 캐시 저장
feat: 로그아웃 인증 캐시 무효화 처리
refactor: JWT 인증을 Valkey 우선 조회로 개선
test: Valkey 인증 캐시 동작 검증 추가
```
