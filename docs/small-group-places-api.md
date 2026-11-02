<!-- AI작성: 사용자 확정 범위와 기존 회원 지역 인증·소모임 코드 및 카카오 공식 명세를 기준으로 작성했다. -->
# 소모임 장소 검색 API와 화면

## 범위

- `GET /api/small-groups/places`: 로그인한 해당 소모임의 승인된 `OWNER`·`MEMBER`만 장소를 검색한다.
- 검색어, 현재 위치, 반경, 관련도순·거리순, 페이지 이동을 제공한다.
- 길찾기 API, 검색 결과의 모임 장소 저장, 지도 SDK 추가, 추천·카테고리 필터는 구현하지 않는다.
- 기존 DB·Mapper·인증·환경 변수를 재사용한다. 신규 의존성·키·설정·SQL·마이그레이션은 없다.

## 재사용 구성

| 구성 | 사용 방식 |
| --- | --- |
| `KakaoApiClient` | 기존 `RestClient`, `JdkClientHttpRequestFactory`, 연결 5초·읽기 10초 제한 재사용 |
| `KakaoOAuthProperties` | 기존 `oauth.kakao.client-id`와 `KakaoAK` 인증 재사용. OAuth 시크릿은 검색에 전송하지 않음 |
| `SmallGroupMapper` | `selectSmallGroupDetail`, `selectSmallGroupMemberRelations` 재사용 |
| `SecurityConfig`·`JwtAuthenticationFilter`·`AuthSessionService` | 기존 `/api/**` 인증·회원 상태·세션 검증 그대로 적용 |
| `ApiResponse`·`ApiException`·`GlobalExceptionHandler` | 공통 성공·실패 봉투와 Validation 처리 재사용 |
| 프론트 `client.js` | 기존 Axios 인증·재발급·취소 처리 사용 |
| 프론트 `RegionMap`·`loadKakaoMap` | 선택한 검색 장소의 지도와 마커 표시. 기존 지도 SDK·설정만 재사용 |
| JUnit·Mockito·MockMvc·Spring Test | 기존 테스트 의존성으로 Mock 검증. 지역 인증 전용 Mock 테스트는 저장소에서 확인되지 않아 새 테스트에 Mock 서버 연결 |

공식 근거: [카카오 Local REST API](https://developers.kakao.com/docs/ko/local/dev-guide).
Local은 키워드 검색·주소/좌표 변환을 제공하며 검색 결과에 장소 상세 페이지 링크가 포함된다.
별도 장소 상세 조회 API나 길찾기 API를 추가하지 않는다.

## 요청 계약

회원 ID는 JWT 요청 속성에서 읽는다. `memberId` 같은 임의 요청값은 바인딩하지 않는다.

| 파라미터 | 필수·기본값 | 검증 |
| --- | --- | --- |
| `smallGroupId` | 필수 | 양의 정수, `small_group.small_group_id` |
| `query` | 필수 | 앞뒤 공백 제거 후 1~100자. 100자는 프로젝트 정책이며 외부 API의 공식 최대 길이를 의미하지 않음 |
| `page` | 기본 1 | 1~45 |
| `size` | 기본 15 | 1~15 |
| `latitude` | 선택 | 유한한 숫자, -90~90 |
| `longitude` | 선택 | 유한한 숫자, -180~180 |
| `radius` | 선택, 생략 시 제한 없음 | 정수 0~20,000m, 좌표 필수 |
| `sort` | 기본 `accuracy` | `accuracy` 또는 `distance`. 거리순에는 좌표 필수 |

위도·경도는 둘 다 생략하거나 둘 다 전달한다. 검색 좌표를 외부 요청의 `x=longitude`, `y=latitude`로 변환한다.
검색어는 URI 변수로 인코딩해 `&`·`+`·`#` 등이 추가 파라미터로 해석되지 않도록 한다.
소모임 ID와 인증 회원 ID는 외부 API에 보내지 않는다. 별도 카테고리·지도 영역 조건은 받지 않는다.

Service도 Validation을 수행해 Controller 외 호출의 잘못된 입력을 차단한다.
입력 검증 후 삭제되지 않은 소모임과 단일 `APPROVED`·`OWNER`/`MEMBER` 관계를 확인하고 외부 Client를 호출한다.
외부 응답 대기 동안 DB 트랜잭션이나 행 잠금을 유지하지 않는다.

## 성공 응답

`200 OK`, `Cache-Control: no-store`. 아래는 실제 장소나 사용자의 위치가 아닌 가상 예시다.

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "placeId": "test-1",
        "placeName": "가상 장소",
        "roadAddress": "가상 도로",
        "address": "가상 주소",
        "category": "가상 분류",
        "latitude": 0.0,
        "longitude": 0.0,
        "placeUrl": "https://place.map.kakao.com/1",
        "distanceMeters": null
      }
    ],
    "page": 1,
    "size": 15,
    "totalCount": 1,
    "pageableCount": 1,
    "end": true
  },
  "meta": null,
  "error": null
}
```

- 외부 DTO를 직접 반환하지 않고 `SmallGroupPlaceSearchResponse`로 변환해 공통 `ApiResponse.data`에 담는다.
- 페이지 정보는 `data` 안에 담는다. 외부 API의 검색 종료 여부를 유지하기 위해 `end`를 제공하며 일반 DB 목록의 `PageMeta.totalPages`로 대체하지 않는다.
- `end=true` 또는 페이지 45일 때 다음 페이지를 비활성화한다. `totalCount`로 추가 페이지를 추정하지 않는다.
- `latitude`·`longitude`는 검색된 장소의 좌표다. 검색 기준인 사용자 좌표는 응답에 별도로 포함하지 않는다.
- `distanceMeters`는 기준 좌표가 있고 외부 API가 유효한 거리를 반환했을 때만 설정한다. 자체 계산하지 않는다.
- 전화번호와 외부 응답의 기타 필드는 제외한다. 도로명 주소가 없으면 화면에서 지번 주소를 사용한다.
- 좌표·거리·필수 장소 정보가 잘못되면 502로 처리한다. 상세 링크는 카카오 장소 도메인과 숫자 경로만 허용하고 HTTPS로 반환한다.

## 실패 응답

실패는 `ApiResponse.fail(code, message)` 형식이다. 빈 검색어와 검색 결과가 없는 경우를 구분한다.

| 상황 | 상태·코드 |
| --- | --- |
| 빈 검색어·길이 초과·범위 오류·잘못된 좌표 조합·형식 오류 | `400 BAD_REQUEST` |
| 비로그인 | `401 LOGIN_REQUIRED` |
| 무효·종료 세션 또는 토큰 | 기존 인증 정책의 `401 INVALID_TOKEN` 등 |
| 소모임 없음·조회 대상에서 제외된 소모임 | `404 SMALL_GROUP_NOT_FOUND` |
| 미가입·미승인·탈퇴·추방·MANAGER·미정의 역할·중복 관계 | `403 SMALL_GROUP_PLACE_FORBIDDEN` |
| 정상 외부 응답에 검색 결과 없음 | `404 SMALL_GROUP_PLACE_NOT_FOUND` |
| 외부 HTTP 실패·연결 실패·타임아웃·변환 실패 | `502 KAKAO_PLACE_REQUEST_FAILED` |
| 외부 응답 구조·장소 데이터 이상 | `502 KAKAO_PLACE_RESPONSE_INVALID` |

정상 응답의 빈 목록을 성공으로 처리하지 않는다. 외부 오류 원문·키·사용자 좌표는 오류 메시지나 로그에 추가하지 않는다.
권한 검증 실패 시 외부 Client를 호출하지 않는다. 관리자라도 해당 소모임의 승인 관계가 없으면 검색할 수 없다.

## 화면 흐름과 위치 이용

1. 승인된 `OWNER`·`MEMBER`의 기존 `directions` 탭을 화면상 **장소 찾기**로 표시한다. 내부 식별자는 유지한다.
2. 기본 화면은 위치 수집 없이 검색어·관련도순·반경 제한 없음으로 검색한다.
3. **내 위치 사용** 설명에서 검색 시 카카오로 위치를 전송하며 저장하지 않는다는 목적을 안내한다. 버튼을 누를 때만 브라우저 위치 요청을 실행한다.
4. 기존 회원 인증의 브라우저 옵션(정밀 위치, 15초 제한, 캐시 미사용)과 정확도 기준(500m 이하)을 따른다.
5. 위치 확인 후 반경 `1·3·5·10·20km`와 거리순을 사용할 수 있다. 좌표 자체는 화면에 표시하지 않는다.
6. 위치 거부·실패·시간 초과·정확도 미달이면 설명을 표시하고 검색어 기반 검색을 계속 제공한다.
7. 데스크톱은 왼쪽 장소 목록·오른쪽 선택 장소 지도, 모바일은 지도·목록 순서로 표시한다. 첫 결과를 기본 선택하고 목록을 누르면 해당 장소의 지도·상세 정보로 전환한다. 기존 RegionMap을 재사용하며 검색된 장소 좌표만 전달한다. 장소·주소·분류·제공된 거리·상세 링크를 표시하고 상세 링크는 새 창으로 열며 referrer를 보내지 않는다.
8. 페이지 이동은 마지막으로 제출한 검색 조건을 유지한다. 조건 변경 후 검색은 1페이지부터 시작한다.
9. 새 검색·위치 해제·탭/모임/역할/계정 전환은 이전 요청을 취소하고 오래된 응답을 무시한다. 위치 해제는 결과·반경·정렬도 초기화한다.
10. 접근 불가 응답이면 결과와 위치를 제거하고 검색을 비활성화한다. 위치·결과·이력은 DB·persist 저장소에 저장하지 않는다.

기존 공지 화면의 길찾기 버튼은 준비 중 상태를 유지한다. 장소 검색으로 기존 `location` 값을 변경하지 않는다.

## 검증 결과와 재실행

2026-11-02, 기존 설치 JDK 17 사용. 테스트 중 실제 외부 API와 실제 위치는 사용하지 않았다.

| 검증 | 결과 |
| --- | --- |
| `SmallGroupPlaceSearchRequestValidationTest` | 28건 통과 |
| `KakaoPlaceApiClientTest` | 9건 통과 |
| `SmallGroupPlaceServiceTest` | 24건 통과 |
| `SmallGroupPlaceControllerTest` | 보안 필터 포함 24건 통과 |
| 프론트 장소 검색·기존 소모임 요청 API 테스트 | 58건 통과 |
| 프론트 lint·build | 오류 없이 통과. 기존 lint 경고와 번들 크기 경고 존재 |
| 헤드리스 Chrome 가상 데이터 UI | 17개 상호작용 통과. 390px 모바일에서도 통과, 문서 너비 390px·패널 360px 확인 |
| 전체 소모임 + 장소 Client 회귀 테스트 | 330건 중 17건 실패. 시작 커밋 `af98364`의 소모임 테스트 273건에서도 동일한 17건 실패 재현 |

기존 실패는 가입 승인 Service 2건, Mapper XML 1건, 후기 등록 Service 8건,
설정 Controller 2건, 투표 삭제·상세·목록·참여 Controller 각 1건이다.
실패 테스트의 클래스·이름 목록을 시작 커밋과 대조해 동일함을 확인했다. 이번 작업에서 관련 없는 수정은 하지 않았다.

백엔드(JDK 17이 Gradle에 등록된 환경, `back`에서):

```powershell
.\gradlew.bat test --tests "com.nightstudy.moa.smallGroup.SmallGroupPlaceSearchRequestValidationTest" --tests "com.nightstudy.moa.member.service.KakaoPlaceApiClientTest" --tests "com.nightstudy.moa.smallGroup.SmallGroupPlaceServiceTest" --tests "com.nightstudy.moa.smallGroup.SmallGroupPlaceControllerTest" --offline --no-daemon
```

이 환경에서는 기존 설치를 인식시키기 위해 다음 실행 옵션만 사용했고 저장소 설정은 변경하지 않았다:
`-Dorg.gradle.java.installations.paths=C:/dev/jdk-17.0.12_windows-x64_bin/jdk-17.0.12`.

프론트(`front`에서):

```powershell
node --test src/api/smallGroupPlaces.test.js src/api/smallGroups.test.js
npm run lint
npm run build
```

브라우저 검증은 임시 화면에 실제 상세 컴포넌트·테마를 연결하고 Client·위치 응답을 가상 값으로 교체해 실행했다.
빈 입력, 성공, 페이지 이동/종료, 빈 결과, 외부 실패/재시도, 위치 활성화/해제/거부,
최신 요청 우선, 모임·계정 변경, 권한 실패, 비회원 탭 미노출, 좁은 화면을 확인했다.
임시 화면·서버는 검증 후 제거했으며 자동화 harness는 제품 소스에 포함하지 않았다.

<!-- AI작성: 후속 UI 요청에 대한 검증을 초기 기능 검증과 구분해 기록한다. -->
2026-11-02 UI 개선: 모아 테마를 유지하며 네이버지도와 유사한 검색창·필터·번호 목록·선택 지도 구성을 적용했다.
장소 검색 API 테스트 5건·lint·build 통과. 가상 검색 및 가상 지도 객체로 데스크톱 1280px과 모바일 390px에서
각 14개 시나리오(초기 지도 미호출, 검색 검증, 기본/다른 장소 선택, 지도 전환, 페이지 변경/종료,
빈 결과의 지도 제거, 위치 사용, 거리 표시, 화면 배치, 가로 넘침 없음, 외부 호출 없음)를 확인했다.
실제 카카오 지도나 외부 API를 호출한 검증은 아니다. 임시 검증 화면·서버는 제거했다.

Postman 확인 시 미인증 요청은 401, 인증 후 `query` 누락·공백 또는 잘못된 조건은 400을 확인한다.
실제 외부 호출 없는 성공·실패·권한 검증은 위 Mock 테스트로 재실행할 수 있다.

## 남은 TODO

- 길찾기는 별도 사용자 요청 이후 설계·구현한다. Local 검색의 상세 링크를 경로 조회 결과로 취급하지 않는다.
- 기존 소모임 `location`은 40자 문자열이며 주소·좌표 전용 필드가 없다. 임의 좌표 추정·장소 자동 선택·DB 변경은 하지 않는다.
- 실제 계정·DB에서의 가입 상태 변경과 실제 브라우저 위치 권한 동작은 개발자가 최종 확인한다. 가상 데이터 검증은 실제 위치 동의·권한 확인을 대신하지 않는다.
- 기존 회귀 테스트 실패 17건의 정리는 별도 작업으로 남긴다.
