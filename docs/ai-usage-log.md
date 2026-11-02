<!-- AI작성: 사용자 승인 경로에 작업·검증 기록을 작성한다. -->
# AI 사용 기록

<!-- AI작성: 계모임 지역 선택 6단계의 상세 조회 점검과 최종 검증 한계를 기록한다. -->
## 2026-11-03 — 계모임 상세 지역 표시 점검 (6단계)

- 목적·승인: 사용자 요청에 따라 저장 지역의 상세 표시 경로와 재조회를 점검했다.
- AI 제안·작성: groups.region_id 저장, 상세 Mapper의 region 조인, Service 응답의 regionName 전달, 상세 화면 상단·모임 정보의 원문 표시를 코드에서 확인했다. 기존 표시 구조가 요구사항을 충족하므로 운영 코드는 수정하지 않았다. savingGroups.test.js에 구·동·세종·기존 광역 지역의 상세 재조회 원문 유지 테스트를 추가했다.
- 변경 파일: front/src/api/savingGroups.test.js, docs/saving-groups-api.md, 본 기록.
- 검증: API 단위 테스트 총 33개 통과, 변경 테스트 oxlint 통과. Java 실행 경로는 JDK 21이고 5단계에서 필요한 Java 17 부재가 확인돼 서버 테스트를 반복하지 않았다. 8080·5173·3306 로컬 수신 포트는 확인되지 않았다.
- 표시 검증 한계: 임시 Vite SSR 렌더링은 React CJS·styled-components 모듈 호환 오류로 실패했다. 프로젝트 설정·의존성은 변경하지 않았다. 실제 HTML 표시·브라우저 클릭·새로고침·DB·관리자 승인 흐름은 검증하지 못했다. API 테스트는 client.get을 대체한 단위 테스트다.
- 개발자 검토·수정: 검토 대기. 명세의 최종 수동 검증 순서에 따라 Java 17 서버 테스트, 구/동 신청·저장·승인·상세 새로고침을 확인해야 한다. 모든 단계의 통합 검증이 완료된 상태는 아니다. 커밋 미수행.

<!-- AI작성: 계모임 개설 지역 선택 5단계 서버 검증·저장 작업을 기록한다. -->
## 2026-11-03 — 계모임 개설 서버 지역 검증 (5단계)

- 목적·승인: 사용자 요청에 따라 구 또는 동 단위 지역만 개설 요청에 저장하도록 서버 검증을 추가했다.
- AI 제안·작성: SavingGroupMapper에 selectCreationRegionsByName을 추가하고 기존 MemberRegionDto로 지역 ID·상위 정보를 조회한다. Service에서 이름 공백 제거, 단일 행 여부, 시·구 정보 필수, 세종의 빈 구 예외를 검증한다. 입력 regionId는 신뢰하지 않고 조회된 ID로 덮어쓴다. 기존 메서드명·API 필드·저장 SQL·트랜잭션·DB 스키마는 유지한다.
- 변경 파일: back/src/main/java/com/nightstudy/moa/savingGroup/service/SavingGroupCreationServiceImpl.java, back/src/main/java/com/nightstudy/moa/savingGroup/mapper/SavingGroupMapper.java, back/src/main/resources/mappers/SavingGroupMapper.xml, back/src/test/java/com/nightstudy/moa/savingGroup/service/SavingGroupCreationServiceImplTest.java, docs/saving-groups-api.md, 본 기록.
- 테스트 작성: 구·동·세종 구/동 정상 저장과 클라이언트 지역 ID 덮어쓰기, 존재하지 않는 지역·중복 이름·광역 지역만 선택·상위 정보 누락·구 공백·ID 누락·지역명 누락 거절을 검증하도록 추가했다.
- 실행 결과: Gradle 테스트는 기본 실행에서 캐시 잠금 접근 거부, 권한 재시도 후 Java 17 toolchain 부재로 컴파일 전에 중단됐다. 테스트 통과·Java 컴파일·MyBatis 실행·실제 DB 저장은 확인하지 못했다. Mapper XML 파싱과 git diff --check는 통과했다. 의존성·도구 설치·프로젝트 설정 변경은 하지 않았다.
- Postman 검증 방법: 인증된 multipart POST /api/saving-group에서 DB에 등록된 구 이름·동 이름·세종 이름은 정상 신청, 시 이름만·지역 누락·존재하지 않는 이름은 400인지 확인한다. regionId를 다른 값으로 보내도 저장된 groups.region_id가 선택 지역의 실제 ID인지 확인한다. 이 검증은 미실행이다.
- 개발자 검토·수정: 검토 대기. 실제 지역 데이터의 상위 정보 정확성·중복 이름 여부를 확인해야 한다. 관리자 승인 후 상세 지역 표시는 6단계다. 커밋 미수행.

<!-- AI작성: 계모임 생성 요청 지역 전달 4단계의 작업·검증 결과를 기록한다. -->
## 2026-11-03 — 계모임 개설 요청 지역 전달 (4단계)

- 목적·승인: 사용자 요청에 따라 기존 생성 요청의 지역 전달을 확인했다. 3단계에서 최종 선택 regionName 연결이 완료돼 운영 코드·API 계약 변경은 필요하지 않았다.
- AI 제안·작성: savingGroups.test.js에 구·동·부산·세종 지역명과 기존 입력·이미지 전달, 같은 FormData의 지역 변경 후 이전 값 제거, 서버 지역 오류·네트워크 실패 전달 검증 3개를 추가했다.
- 변경 파일: front/src/api/savingGroups.test.js, docs/saving-groups-api.md, 본 기록.
- 검증: node --test src/api/savingGroups.test.js 총 32개 통과, 변경 테스트 파일 oxlint 통과. 운영 코드 변경이 없어 빌드는 반복하지 않았다.
- 검증 한계·방법: client.post 대체 단위 테스트다. 브라우저에서 상위 지역 변경 후 구·동을 다시 선택하고 Network의 POST /api/saving-group Form Data에서 최종 regionName 하나만 포함되는지 확인해야 한다. 실제 HTTP·DB·브라우저 검증은 미실행이다.
- 개발자 검토·수정: 검토 대기. 서버 지역 검증 강화는 5단계, 승인 후 상세 표시 확인은 6단계다. 커밋은 수행하지 않았다.

<!-- AI작성: 계모임 개설 지역 선택 3단계 작업과 검증 결과를 기록한다. -->
## 2026-11-03 — 계모임 개설 지역 선택 (3단계)

- 목적·승인: 사용자 승인에 따라 시·구 필수, 동 선택 및 세종 전체 선택을 개설 화면에 적용했다.
- AI 제안·작성: SavingGroupApplicationPage.jsx에서 getMemberRegions를 재사용해 지역 로딩·실패·빈 목록·재시도를 처리하고 이전 요청 응답을 무시한다. SavingGroupApplicationForm.jsx에서 시 변경 시 구·동, 구 변경 시 동을 초기화한다. 상위 선택 전 하위 선택을 비활성화하고 시·구에 required를 적용한다. 최종 구 또는 동의 regionName을 기존 FormData에 전달한다. 동 데이터가 없으면 구 단위 신청을 허용한다.
- 변경 파일: front/src/pages/SavingGroupApplicationPage.jsx, front/src/components/savingGroup/SavingGroupApplicationForm.jsx, docs/saving-groups-api.md, 본 기록.
- 검증: 변경 화면 두 파일 oxlint 통과, npm run build 통과(500kB 초과 번들 경고). 브라우저·실제 API·DB 검증은 미실행이다.
- 브라우저 검증 방법: 필수 지역 미선택 제출 차단, 시 변경 시 구·동 초기화, 구 변경 시 동 초기화, 동 미선택 허용, 세종 전체 선택, 지역 조회 실패 후 재시도를 확인한다. Network에서 최종 선택 지역만 regionName에 포함되는지 확인한다.
- 개발자 검토·수정: 검토 대기. 서버 지역 검증 강화 및 관리자 승인 후 상세 지역 확인은 후속 단계다. 동 선택에는 실제 지역 API의 동 데이터가 필요하다. DB·서버·의존성 변경 없음. 커밋은 수행하지 않았다.

<!-- AI작성: 사용자 승인 범위의 선택 장소 현재 위치 길찾기 구현과 검증을 기록한다. -->
## 2026-11-02 — 선택 장소의 현재 위치 기반 카카오맵 길찾기

- 사용자 요청: 카드의 장소 상세 보기 (새 창)를 현 위치에서 길찾기로 바꾸고 카카오맵 길찾기를 연다. 기존에 제시한 1단계 작업과 문서 갱신 범위로 진행하며 커밋명 추천을 요청했다.
- AI 제안·작성: SmallGroupPlacesPanel.jsx에서 클릭할 때마다 현재 위치를 확인하고 선택 장소와 함께 공식 출발지→도착지 길찾기 URL로 이동하도록 구현했다. 기존 위치 옵션·500m 정확도 기준·좌표 범위를 재사용하고 장소명은 URL 인코딩한다.
- 새 창: 클릭 시 about:blank를 확보해 비동기 팝업 차단을 피하고 opener 분리·no-referrer를 설정했다. 팝업 차단 시 위치를 수집하지 않는다. 위치 권한 거부·조회 실패·시간 초과·정확도 미달·창 닫힘·동기 오류 시 대기 창을 닫고 안내한다.
- 상태 처리: 버튼 비활성화와 ref로 중복 실행을 방지한다. 선택 장소·검색·페이지·위치 해제·탭·소모임·계정 전환과 언마운트 후의 늦은 콜백은 외부 이동을 실행하지 않는다. 출발 좌표를 상태·DB·로그·persist 저장소에 추가하지 않는다.
- 변경 범위: 제품 코드 1파일과 docs/small-group-places-api.md, 본 기록. 기존 placeUrl 응답 DTO는 유지하며 신규 API·SDK·키·환경 변수·백엔드·DB 변경은 없다. 공지 화면의 저장된 모임 장소 길찾기는 후속 작업이다.
- 검증: lint·build 통과, 기존 lint·번들 크기 경고 유지. 가상 위치·지도·새 창 객체로 헤드리스 Chrome 1280px 데스크톱과 390px 모바일 각각 26개 시나리오를 통과했다. 좌표 순서·인코딩·중복 실행·팝업 차단·권한/정확도 실패·창 닫힘·오래된 콜백 취소를 확인했다. 실제 사용자 위치 수집·외부 API·카카오맵 이동 없이 검증했고 임시 파일·서버는 정리했다.
- 개발자 검토: 실제 브라우저에서 위치 허용 후 출발지·목적지 반영과 팝업 차단 안내를 최종 확인해야 한다. 이번 기록은 실제 위치·외부 카카오맵 연동 검증 완료를 뜻하지 않는다.

<!-- AI작성: 기존 장소 검색 기능의 후속 UI 개선 요청과 검증을 기록한다. -->
## 2026-11-02 — 소모임 장소 찾기 목록·지도 UI 개선

- 사용자 요청: 네이버지도와 비슷한 탐색 구성을 적용하되 모아 프로젝트의 디자인을 따른다.
- AI 제안·작성: SmallGroupPlacesPanel.jsx 한 코드 파일에 상단 검색창·필터, 왼쪽 번호 목록과 오른쪽 선택 장소 지도·상세 카드, 모바일 지도 우선 배치, 선택 결과 강조를 구성했다. 프로젝트 테마·Button·react-icons와 기존 RegionMap을 재사용했다.
- 선택 상태는 화면에서만 유지하고 첫 결과를 기본 선택하며 새 검색·페이지 변경·초기화 시 선택을 재설정한다. 기존 인증·검색·위치 이용·권한·저장 정책은 유지한다. 실제 사용자 좌표 대신 선택 장소의 좌표만 지도에 전달한다.
- 신규 지도 API·SDK·패키지·키·환경 변수·DB 변경은 없다. 기존 지도 컴포넌트와 기존 설정을 그대로 사용한다.
- 검증: 장소 검색 프론트 API 테스트 5건, 전체 lint·build 통과. 기존 lint·번들 크기 경고는 남아 있다. 별도 임시 Chrome 프로필에서 가상 검색·지도 객체로 1280px 데스크톱과 390px 모바일 각각 14개 UI 시나리오를 통과하고 캡처를 확인했다. 실제 외부 API·실제 위치는 사용하지 않았다. 임시 검증 파일·서버는 제거했다.
- 문서: docs/small-group-places-api.md의 재사용 구성·화면 흐름·UI 검증 결과를 마지막에 갱신했다.
- 개발자 검토: 실제 기존 카카오 지도 설정에서의 지도 표시와 실제 계정·권한 동작은 개발자의 최종 확인이 필요하다. 길찾기는 기존 TODO로 유지한다.

<!-- AI작성: 사용자 요청에 따라 마지막 단계에서 장소 검색 1~8단계 작업·검증을 모아 기록한다. -->
## 2026-11-02 — 소모임 장소 검색 1~9단계

- 목적·사용자 확정: 기존 회원 위치 API를 재사용해 승인된 소모임 OWNER·MEMBER만 장소를 검색한다. 현재 위치·반경·거리순과 화면을 포함하고 빈 결과는 오류로 처리한다. 길찾기·장소 저장·DB 변경은 제외한다. 문서화는 마지막 단계에 한 번에 진행하고 단계별 로컬 커밋을 요청받았다.
- 1단계(`af98364`, 사용자 커밋): 검색 요청·외부/공개 응답 DTO, Service 인터페이스, 검색어 100자·좌표/반경/정렬 조합 Validation 테스트를 추가했다.
- 2단계(`508e1b0`): 기존 KakaoApiClient의 RestClient·REST 키·타임아웃·ApiException으로 장소 검색을 추가했다. 검색어 인코딩과 선택적 좌표 전달, 외부 실패·응답 검증을 Mock 서버로 확인했다.
- 3단계(`8664589`): SmallGroupPlaceServiceImpl에 기존 상세/회원 관계 Mapper 기반 접근 검증, 외부 호출, 장소 데이터 변환·안전한 상세 링크·빈 결과 오류 처리를 추가했다. 외부 호출 중 DB 잠금은 유지하지 않는다.
- 4단계(`d0d15d4`): Service 단위 테스트로 승인 OWNER·MEMBER, 미승인·탈퇴·미가입·MANAGER·중복 관계, 빈 결과, 외부 실패·응답 이상을 확인했다.
- 5단계(`af51c9a`): GET /api/small-groups/places를 별도 Controller에 추가했다. JWT 회원 ID·허용 필드 바인딩·공통 ApiResponse와 no-store를 적용했고 기존 보안 설정은 변경하지 않았다.
- 6단계(`8f4c78e`): 기존 SecurityFilterChain·JWT 세션 Mock을 포함해 인증·입력 바인딩·공통 오류·개인정보 미노출을 검증했다.
- 7단계(`b5ae1de`): 기존 프론트 인증 client에 searchSmallGroupPlaces를 추가하고 허용 조건만 전달하며 서버 오류·취소·응답 필드를 검증했다. 신규 요청 API 테스트를 추가했다.
- 8단계(`7d05817`): SmallGroupPlacesPanel과 상세 탭 연결을 추가했다. 기존 directions 식별자를 유지하며 문구는 장소 찾기로 변경했다. 위치 사용 목적 안내·명시적 요청·정확도 500m 기준·반경/정렬·페이지·오류/재시도·요청 취소와 모임/역할/계정 변경 시 상태 초기화를 구현했다.
- 9단계: docs/small-group-places-api.md에 계약·화면 흐름·검증·길찾기 TODO를 작성하고 기존 상세 API 탭 문서와 본 기록을 갱신했다.
- 재사용: KakaoApiClient, KakaoOAuthProperties, 기존 HTTP Client·인증 키·환경 변수, SmallGroupMapper 상세·관계 조회, SecurityConfig·JWT·AuthSessionService, ApiResponse·ApiException·공통 Validation/예외 처리, 프론트 client·테마·버튼·회원 위치 요청 옵션, 기존 JUnit·Mockito·MockMvc·Spring Test·Node 테스트 도구.
- 검증: 기존 설치 JDK 17을 실행 옵션으로 지정했다. 장소 검색 요청 28건·Client 9건·Service 24건·보안 포함 Controller 24건, 합계 85건 통과. 프론트 관련 API 테스트 58건과 lint·build 통과. 기존 lint·번들 크기 경고는 남아 있다.
- 회귀 확인: 전체 소모임+Client 330건 중 기존 테스트 17건이 실패했다. 시작 커밋 af98364를 별도 임시 폴더로 내보내 소모임 273건을 실행했으며 동일한 17건 실패를 클래스·테스트 이름 기준으로 대조했다. 관련 없는 코드나 테스트는 수정하지 않았다.
- 화면 검증: Computer Use 앱 제어 승인이 시간 초과되어 UI 입력은 수행하지 못했다. 기존 Chrome의 별도 임시 프로필·가상 Client/위치 응답으로 검색·빈 결과·외부 오류·위치 거부/해제·페이지·요청 경합·모임/계정/권한 전환 등 17개 시나리오를 확인했다. 390px 모바일에서도 통과하고 캡처를 확인했다. 실제 외부 API·실제 키·실제 위치를 사용하지 않았고 임시 화면·서버는 제거했다.
- 개발자 검토·수정: 개발자의 코드 검토와 실제 계정·DB·브라우저 위치 권한 검증은 아직 미완료다. 이 기록은 개발자 승인이나 실제 외부 연동 성공을 뜻하지 않는다.
- 남은 TODO: 길찾기 별도 설계, 기존 location 문자열만 있는 목적지 데이터 제약 확인, 기존 회귀 실패 17건 별도 정리. 새 의존성·SDK·키·환경 변수·DB 변경은 없다.

<!-- AI작성: 사용자 요청에 따라 마지막 문서화 단계에서 1~8단계 이력을 모아 기록한다. -->
## 2026-11-02 — 소모임 모임원 후기 등록 1~8단계

- 목적: 승인된 소모임 일반 모임원이 개설자를 제외하고 후기 1건을 등록하는 API를 단계별로 구현한다.
- 사용자 확정: 별도 후기 테이블, 별점 0~5점·0.5 단위, 코멘트 필수·1~30자, 회원당 소모임별 1회, 모임 다음 날부터 허용, meet_date NULL 시 제한, 기존 MANAGER 403 유지. 문서화는 마지막 단계에 모아서 진행한다.
- 1단계: 후기 테이블 PK·FK·회원별 UNIQUE·별점·코멘트 CHECK 설계를 작성했다. 사용자가 DB 담당자 반영으로 결정하여 다운로드 폴더에 `소모임_후기_테이블_추가_요청.md`를 전달용으로 작성했다. 현재 저장소 schema.sql에는 후기 테이블 정의가 없고 실제 DB 적용 여부도 확인되지 않았다.
- 2단계: SmallGroupReviewCreateRequest·CreateCommand·CreateTargetRow와 기존 SmallGroupMapper에 등록 대상 잠금 조회·후기 ID 조회·INSERT 계약을 추가했다. 요청 별점은 BigDecimal과 Validation으로 검증한다.
- 3단계: 기존 SmallGroupMapper.xml에 해당 SQL 3개를 추가했다. 기존 회원 관계 잠금 조회를 재사용하며 저장 시 생성 키를 반환한다.
- 4단계: SmallGroupService·Impl에 트랜잭션 기반 인증·존재·일반 승인 회원·개설자·MANAGER·한국 날짜·중복·입력값 검증과 저장을 추가했다. DB 중복 오류를 동일한 409 업무 오류로 변환한다.
- 5단계: SmallGroupReviewCreateServiceTest에 정상 등록, 권한·상태·날짜·중복·입력값 및 저장 실패 테스트를 추가했다.
- 6단계: 기존 SmallGroupController에 POST /api/small-groups/{smallGroupId}/reviews를 추가했다. JWT 회원 ID와 검증한 본문만 전달하며 HTTP 200 ApiResponse<Long>으로 생성 후기 ID를 반환한다.
- 7단계: SmallGroupReviewCreateControllerTest를 추가하고 기존 SmallGroupSecurityIntegrationTest·SmallGroupMapperXmlTest에 인증 차단·statement·생성 키 파싱 검증을 추가했다.
- 8단계: docs/small-group-public-detail-api.md에 확정 정책, 요청·응답, 오류 코드, DB·저장 모델, 테스트 범위, 재실행 명령과 Postman 검증 절차를 기록했다. 운영 코드·schema.sql은 이 단계에서 수정하지 않았다.
- 검증 결과: 단계별 git diff --check 및 XML 정적 파싱·statement ID·생성 키 매핑 확인 통과. Gradle compileJava와 대상 테스트 실행은 Java 17 toolchain 부재로 컴파일 전에 중단되었다. JUnit 테스트 실행·통과 결과, 실제 MySQL DDL·INSERT·동시성, 발급 JWT 성공 흐름, Postman·브라우저 검증은 미확인이다.
- 남은 확인: DB 담당자의 최종 테이블·제약·schema.sql·ERD 반영, Java 17 환경 대상 테스트 실행, 실제 인증·DB 통합 확인. 프론트 작성 화면의 코멘트 검증은 후속 범위다.
- 개발자 검토·수정: 단계별 구현 요청·정책 승인은 받았으며 최종 코드·동작 검토 완료 여부는 확인되지 않았다.
- 외부 서비스·의존성: 외부 API 호출·설정 변경·추가 패키지 설치·공용 DB 변경 없음. AI가 커밋을 실행하지 않았다.

## 2026-11-01 — 소모임 투표 논리 삭제 API 및 화면 연동

- 목적: 투표 작성자이면서 소모임 개설자·승인된 `OWNER`만 소속 투표를 삭제하고, 투표 보기·참여·선택 이력은 보존하도록 한다.
- 사용자 확정: 경로는 `DELETE /api/small-groups/{smallGroupId}/votes/{voteId}`를 사용한다. `OPEN`, `CLOSED` 모두 삭제 가능하며 `small_group_vote.deleted_at`만 갱신한다. 이미 삭제됐거나 다른 소모임 투표는 존재를 구분하지 않고 `404 SMALL_GROUP_VOTE_NOT_FOUND`로 처리한다.
- AI 작성: 기존 `SmallGroupVoteDetailRow`, 소모임·회원 잠금 조회 Mapper를 재사용해 논리 삭제 Mapper 계약, Service 트랜잭션과 권한 검증, Controller API·Security 테스트, React 삭제 요청 API·확인 화면을 추가했다. 삭제된 투표가 목록과 진행 중 투표 생성 한도에서 제외되도록 기존 SQL에도 `deleted_at IS NULL` 조건을 보완했다.
- API·데이터: 삭제 권한은 투표 작성자, `groups.member_id` 개설자, 승인된 `OWNER` 관계가 모두 같은 로그인 회원일 때만 허용한다. `small_group_vote_option`, `small_group_vote_participation`, `small_group_vote_selection`은 삭제·수정하지 않는다. DB 마이그레이션, 상태값, 외부 연동, 의존성·Security 설정은 추가하지 않았다.
- 검증: `git diff --check`와 프런트 `node --test src/api/smallGroups.test.js` 40개, `npm run lint`, `npm run build`를 통과했다. lint의 기존 경고와 번들 크기 경고는 유지됐다. 백엔드 Gradle 대상 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17·실제 MySQL·JWT 환경에서 작성자 OWNER의 OPEN·CLOSED 삭제, 일반 회원·다른 OWNER·MANAGER의 403, 없는·삭제 소모임과 다른 소모임·이미 삭제된 투표의 404, 보기·참여·선택 이력 보존, 삭제 후 목록 제외·생성 한도 반영을 E2E로 확인한다.
- 개발자 코드 검토·수정: 대기 중.

## 2026-11-01 — 소모임 투표 참여·재투표 및 현황 조회

- 목적: 승인된 소모임 회원이 상세 화면에서 보기를 선택해 투표하고, 참여 후 항목별 득표 수·공개 투표의 참여자 닉네임·참여 인원을 확인하도록 한다.
- 사용자 확정: 단일 선택은 1개, 복수 선택은 1개 이상 전체 보기 수 이하로 제한한다. 같은 선택으로 재요청하면 DB를 바꾸지 않고 성공 처리하며, 선택을 바꾸면 기존 선택을 교체한다. 마감 전에는 `현황 보기`, 마감 후에는 `결과 보기`를 표시한다. 익명 투표는 생성자·운영자를 포함해 닉네임을 숨긴다.
- AI 작성: 기존 `small_group_vote_participation`, `small_group_vote_selection`을 재사용하는 참여 요청 DTO·Mapper SQL·Service 트랜잭션·Controller API·React 요청 API와 상세 화면 연결을 추가했다. 별도 DB 마이그레이션, 의존성, Security 설정은 추가하지 않았다.
- API·데이터: `POST /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}/participations`는 최초 참여를 저장하고, 같은 선택은 무변경 성공 처리하며, 달라진 선택만 교체하고 참여 시각을 갱신한다. `GET /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}/status`는 참여 이력이 있는 승인 OWNER·MEMBER만 기존 상세 응답 구조로 반환한다.
- 검증: 프런트 `node --test src/api/smallGroups.test.js` 37개 및 `npm run build`를 통과했다. `npm run lint`는 이번 변경과 무관한 기존 경고 10건만 보고했다. 백엔드 Gradle 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17과 실제 MySQL·JWT 환경에서 참여 행 잠금, 동일 선택 무변경, 선택 변경, 마감 경계, 공개·익명 닉네임 노출과 비회원·미참여 회원의 401·403 응답을 확인한다.

## 2026-10-30 — 소모임 투표 상세 조회 및 목록-상세 화면 연동

- 목적: 승인된 소모임 OWNER·MEMBER가 자기 소모임 투표의 결과와 참여 상태를 안전하게 조회하고, 투표 목록에서 상세 화면으로 이동하도록 구현했다.
- 사용자 확정: 투표 생성자는 OWNER만 가능하다. 상세 조회와 참여는 승인 OWNER·MEMBER로 제한한다. 복수 선택과 익명 투표를 지원하며, 익명 투표는 생성자를 포함해 누구도 참여자 이름·선택을 볼 수 없다. 공개 투표도 참여 완료한 회원에게만 보기별 닉네임을 공개한다. 마감 투표는 참여할 수 없고 결과는 24시간 동안 조회한 뒤 목록·상세에서 숨긴다.
- AI 작성: 투표 상세 응답·내 참여·보기·Mapper 내부 DTO, MyBatis 상세·보기·득표·참여·닉네임 조회 계약, Service 권한·소속·마감·공개 범위 조합, GET Controller와 Service·Controller 테스트, React 상세 요청 API·응답 검증·오류 보존, 상세 페이지·표시 컴포넌트, 목록의 `smallGroupVoteId` 응답과 상세 라우트·클릭 이동을 작성했다.
- API·데이터: `GET /api/small-groups/{smallGroupId}/votes/{smallGroupVoteId}`는 기존 `ApiResponse` 형식을 사용한다. 기존 투표·보기·참여·선택 테이블과 Mapper를 재사용했다. 삭제 투표 제외를 위한 `small_group_vote.deleted_at`은 별도 0단계 작업의 선행 조건이며, 이 작업에서는 외부 API·의존성·Security 설정을 변경하지 않았다.
- 검증: `git diff --check`를 통과했다. 프런트 `node --test src/api/smallGroups.test.js` 33개, `npm run build`를 통과했다. `npm run lint`는 이번 변경과 무관한 기존 경고만 반환했다. 백엔드 Gradle 대상 테스트는 실행을 시도했으나 현재 환경에서 Java 17 toolchain을 찾지 못해 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 환경에서 `SmallGroupVoteDetailServiceTest`, `SmallGroupVoteDetailControllerTest`, `SmallGroupVoteListControllerTest`, `SmallGroupMapperXmlTest`를 실행한다. 실제 MySQL·JWT로 소모임별 투표 격리, 삭제 투표, 마감 후 24시간 경계, 익명·공개 닉네임 범위와 목록에서 상세 이동을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 투표 참여 저장, 선택 변경·취소, 투표 수정·삭제, 종료 처리와 알림은 별도 요구사항으로 구현한다.

## 2026-10-30 — 소모임 투표 목록 조회 API 및 투표 탭 연동

- 목적: 승인된 소모임 회원이 해당 소모임의 투표 제목·등록일·마감일을 검색·필터·페이징 없이 조회하고, 상세 화면의 투표 탭에서 확인하게 한다.
- 사용자 확정: 경로는 `GET /api/small-groups/{smallGroupId}/votes`이며, 승인된 `OWNER`·`MEMBER`만 조회할 수 있다. 목록에는 제목·등록일·마감일만 포함하고, 결과·보기·참여 정보는 포함하지 않는다. 투표 생성 한도는 `closed_at IS NULL`인 진행 중 투표 5개이며 종료된 투표는 한도에 포함하지 않는다.
- AI 작성: 투표 목록 응답·대상 DTO, MyBatis 대상·목록 조회 계약과 XML 파싱 검증, Service의 소모임 존재·승인 회원 검증, GET Controller와 Controller·Security 테스트, React 목록 요청·응답 검증·단위 테스트, 투표 탭의 로딩·오류 재시도·빈 목록·생성 후 재조회 UI를 작성했다.
- API·데이터: 기존 `small_group`, `groups`, `group_member`, `small_group_vote`와 `SmallGroupMapper`를 재사용했다. 목록은 `created_at DESC, small_group_vote_id DESC` 순으로 `title`, `createdAt`, `endsAt`만 반환하며 빈 결과는 성공 배열이다. DB 마이그레이션, 상태값, 외부 연동, 의존성, Security 설정은 변경하지 않았다.
- 검증: `git diff --check`를 통과했다. 프런트 `node --test src/api/smallGroups.test.js`는 29개 테스트가 통과했다. 백엔드 Gradle 대상 테스트와 Mapper XML 테스트는 실행을 시도했으나 현재 환경에 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 미검증: `npm run build`는 이번 변경과 무관하게 `VerificationPage.jsx`의 `@portone/browser-sdk/v2` import를 해석하지 못해 실패했다. 실제 MySQL·JWT 환경에서 소모임별 격리, 승인 회원 권한, `closed_at` 기준 진행 중 투표 5개 제한과 종료 투표 제외를 확인해야 한다. 브라우저에서 투표 탭의 목록·오류·빈 상태·생성 후 갱신도 직접 확인해야 한다.
- 개발자 코드 검토·수정: 대기 중. 투표 상세, 보기·참여·결과, 마감 처리, 결과 공개와 알림은 후속 요구사항 확정 후 구현한다.

## 2026-10-29 — 소모임 개설자 가입 신청 거절 API 및 화면 연동

- 목적: 소모임 개설자가 본인이 개설한 소모임의 대기 가입 신청만 거절하고, OWNER 신청 목록 화면에서 실제 처리 결과를 확인하게 한다.
- 사용자 확정: 기존 계모임 가입 신청 거절의 인증·권한·예외·응답·상태 전이를 따른다. 처리자는 JWT request attribute `memberId`만 사용하며, 개설자이면서 `OWNER`·`APPROVED` 관계인 회원으로 제한한다. `MANAGER`에게 권한을 부여하지 않고, 거절 사유·처리자·처리일·알림·재신청 정책 변경은 추가하지 않는다.
- AI 작성: 기존 소모임 가입 신청·목록·승인 DTO와 잠금 조회 계약을 재사용해 거절 Mapper 계약·조건부 UPDATE SQL, Service 트랜잭션 검증, PATCH Controller, Service·Controller 테스트, 프론트 거절 API 함수·테스트, OWNER 신청 목록의 거절 버튼과 성공 후 목록 재조회 처리를 작성했다. 기존 실제 목록 API와 화면 연결은 이미 구현되어 있어 중복 구현하지 않았다.
- API·데이터: `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/reject`는 요청 본문 없이 공통 성공 응답을 반환한다. `group_member`의 같은 `crew_member_id`, `group_id`, `PENDING` 행 한 건만 `REJECTED`로 갱신하며 회원 관계 INSERT·승인 이력 변경은 하지 않는다. DB 마이그레이션, 새 DTO·상태값, 외부 연동, 의존성·Security 설정 변경은 하지 않았다.
- 검증: Java 17 경로를 지정한 `compileJava`를 통과했다. `SmallGroupJoinRejectionServiceTest` 7개, `SmallGroupJoinRejectionControllerTest` 4개, 기존 가입 신청 목록 Controller·Service 대상 테스트를 통과했다. 프론트 `node --test src/api/smallGroups.test.js` 23개, `npm run lint`, `npm run build`, `git diff --check`를 통과했다. lint의 경고와 번들 크기 경고는 이번 변경 파일과 무관한 기존 경고로 유지했다.
- 직접 확인 필요: 실제 MySQL에서 `FOR UPDATE`와 조건부 UPDATE 영향 행, 동시 승인·거절 경합과 트랜잭션 롤백을 확인한다. 실제 JWT로 개설자·비개설자·MANAGER·비로그인·없는/삭제 소모임·다른 소모임 신청·처리 완료 신청의 HTTP 응답을 확인한다. 브라우저에서 OWNER 신청 목록의 거절 중 상태, 성공 후 재조회, 서버 오류 표시, 승인·거절 중복 클릭 차단을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 실제 git commit은 수행하지 않았으며, 취소·선택 승인·알림·거절 사유/처리자/처리일 저장과 재신청 정책 변경은 별도 요구사항 확정 후 진행한다.

## 2026-10-29 — 소모임 개설자 가입 신청 승인 API 및 화면 연동

- 목적: 소모임 개설자가 본인이 개설한 소모임의 대기 가입 신청을 승인하고, 신청 목록 화면에서 실제 승인 결과를 확인하게 한다.
- 사용자 확정: 소모임에는 모집 상태가 없으므로 삭제 여부·정원·승인 참여자 수만 검증한다. 승인자는 `groups.member_id`가 일치하고 `OWNER`·`APPROVED` 관계인 개설자로 제한하며, `MANAGER`에게 권한을 부여하지 않는다. 승인 식별자는 기존 계모임과 같이 `crewMemberId`를 사용한다.
- AI 작성: 기존 소모임 가입 신청·목록 흐름을 재사용해 승인 대상 DTO, Mapper 계약·조건부 승인 SQL, Service 트랜잭션과 권한·소속·상태·정원 검증, PATCH Controller, Service·Controller·Security 테스트, 프론트 승인 API와 승인 후 목록 재조회 UI를 작성했다.
- API·데이터: `PATCH /api/small-groups/{smallGroupId}/join-requests/{crewMemberId}/approve`는 요청 본문 없이 공통 성공 응답을 반환한다. 기존 `group_member` PENDING 행을 APPROVED·MEMBER로 갱신하고 `joined_at`, `approved_at`, `approved_by`를 저장한다. DB 마이그레이션, 새 상태값, 외부 연동, 의존성·Security 설정 변경은 하지 않았다.
- 범위와 TODO: 거절·취소·선택 승인·알림·회비·추가 역할 부여는 구현하지 않았다. 소모임 모집 상태와 가입 모임 수 제한 정책이 없으므로 승인 조건에 추가하지 않았다.
- 검증: Mapper XML statement 확인과 `git diff --check`를 통과했다. 프론트 `node --test src/api/smallGroups.test.js` 22개와 `npm run build`를 통과했다. 백엔드 Gradle 테스트는 실행을 시도했으나 로컬 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 환경에서 `SmallGroupJoinApprovalServiceTest`, `SmallGroupJoinApprovalControllerTest`, `SmallGroupSecurityIntegrationTest`, `SmallGroupMapperXmlTest`를 실행한다. 실제 MySQL·JWT에서 개설자·비개설자·MANAGER·비로그인·없는/삭제 소모임·다른 소모임 신청·처리 완료 신청·정원 초과 및 동시 승인을 확인한다. 브라우저에서는 OWNER 신청 목록의 승인 중 상태, 성공 후 목록 재조회, 오류 표시와 거절·선택 승인 비활성을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 실제 운영 DB의 `approved_by`, `approved_at`, 유니크 제약과 잠금 경합은 별도 테스트 DB에서 검토해야 한다.

## 2026-10-29 — 소모임 개설자 전용 가입 신청 목록 조회 API

- 목적: 소모임 개설자가 본인이 개설한 소모임의 가입 신청 현황을 계모임과 동일한 상태 필터·정렬·페이징·마스킹 규칙으로 조회하게 한다.
- 사용자 확정: `status`, `page`, 고정 크기 10을 사용하며 `PENDING`·`APPROVED`·`REJECTED`만 조회한다. 개설자이면서 `OWNER`·`APPROVED` 관계인 회원만 신청 목록 탭과 API를 사용할 수 있고, `MANAGER`를 포함한 운영진은 권한이 없다. 승인·거절·취소·일괄 처리는 범위에서 제외한다.
- AI 작성: 가입 신청 목록 DTO·Mapper 계약과 SQL, Service 권한·조건·페이징 조합, GET Controller와 Service·Controller·Security 테스트, 프론트 API 모듈·테스트, 실제 API를 쓰는 OWNER 목록 패널을 작성했다. 기존 Mock 목록 파일과 로컬 승인·거절 상태 변경은 제거했다.
- API·데이터: `GET /api/small-groups/{smallGroupId}/join-requests`는 `ApiResponse`의 data 배열과 meta를 반환한다. 기존 `small_group`, `groups`, `group_member`, `members`, `region`만 재사용하고 DB 마이그레이션, 상태값, 외부 연동, 의존성·보안 설정 변경은 하지 않았다. 신청자 이름은 SQL에서 마스킹하고 BIGINT 신청 ID는 문자열로 반환한다.
- 검증: 프론트 `node --test src/api/smallGroups.test.js` 21개 통과, `npm run build` 통과, `npm run lint` 완료(기존 경고 유지), `git diff --check` 통과를 확인했다. 백엔드 Gradle 대상 테스트는 실행을 시도했으나 현재 환경에서 Java 17 toolchain을 찾지 못해 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 환경에서 `SmallGroupServiceImplTest`, `SmallGroupJoinRequestControllerTest`, `SmallGroupSecurityIntegrationTest`를 실행한다. 실제 MySQL·JWT에서 개설자·비개설자·MANAGER·비로그인·삭제 소모임, 상태 필터·페이지·빈 결과·마스킹·정렬을 확인한다. 브라우저에서 OWNER 탭의 로딩·오류 재시도·필터·페이지 및 비OWNER 탭 미노출을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 승인·거절·일괄 처리 정책과 API가 확정되면 현재 비활성 UI를 실제 서버 요청으로 교체한다.

## 2026-10-29 — 소모임 가입 신청 API와 상세 화면 연결

- 목적: 일반 회원이 소모임 상세에서 가입 신청을 제출하고, 서버가 기존 관계·정원·권한을 검증해 대기 상태로 저장하도록 한다.
- 사용자 확정: 소모임은 삭제되지 않았고 정원이 남으면 신청 가능하다. ADMIN은 신청할 수 없고 개설자·OWNER·MANAGER도 자신의 소모임에 신청할 수 없다. 재신청은 `LEFT`만 허용하며, `REJECTED`, `EXPELLED`은 재신청할 수 없다. 가입 메시지는 처리하지 않으며, 승인·거절은 후속 OWNER 전용 기능으로 분리한다.
- AI 작성: 가입 신청 내부 DTO·MyBatis 잠금 조회 및 INSERT/UPDATE 계약·SQL, Service 트랜잭션과 예외 처리, POST Controller, Service·Mapper XML·Security 테스트, 프론트 API 모듈·테스트, 상세 가입 신청 버튼과 완료·본인 인증·실패 모달을 작성했다.
- 권한·데이터: 인증 회원 ID는 JWT 필터 request attribute만 사용한다. 활성·본인 인증 완료 USER만 신청할 수 있고, 기존 `group_member`에 `MEMBER`·`PENDING`으로 저장한다. `LEFT` 재신청은 승인·가입·탈퇴 이력을 초기화한다. DB 마이그레이션, 새 상태값, 외부 연동, 의존성·설정 변경은 하지 않았다.
- API·오류: `POST /api/small-groups/{smallGroupId}/join-requests`는 성공 시 `ApiResponse.ok()`을 반환한다. 인증 없음은 Security 공통 401, 없는 소모임은 `404 SMALL_GROUP_NOT_FOUND`, 관리자·운영 관계는 403, 중복·정원 문제는 409로 처리한다. 구체적 오류 코드는 `docs/small-group-public-detail-api.md`에 기록했다.
- 프론트: 승인된 OWNER·MEMBER에게 가입 신청 버튼을 노출하지 않는다. 나머지 사용자는 요청할 수 있고, 성공·본인 인증 필요·일반 실패를 모달로 구분한다. OWNER 신청 목록 화면의 Mock 데이터는 승인·거절 API가 후속 범위이므로 유지한다.
- 검증: Java 17 경로를 지정한 `gradlew.bat test --tests "com.nightstudy.moa.smallGroup.SmallGroupServiceImplTest" --tests "com.nightstudy.moa.smallGroup.SmallGroupSecurityIntegrationTest" --no-daemon`에서 35개 테스트가 통과했다. 프론트 `node --test src/api/smallGroups.test.js` 18개 통과, `npm run build` 통과, `git diff --check` 통과를 확인했다.
- 직접 확인 필요: 실제 MySQL에서 `FOR UPDATE`, 유니크 제약, `LEFT` 재신청 초기화와 동시 요청을 확인한다. 실제 JWT로 일반 회원·ADMIN·개설자·OWNER·MANAGER·대기 신청·정원 초과의 HTTP 응답과 브라우저 버튼·모달을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 모집 상태·공개 범위·자동 승인 정책 및 OWNER의 신청 목록·승인·거절 요구사항이 확정되면 후속 API와 화면을 설계한다.

## 2026-10-28 — 소모임 설정 수정 API

- 목적: 소모임 개설자가 설정 화면에서 모임명·관심사·소개글·정원·만남일·장소를 부분 수정할 수 있게 한다.
- 사용자 확정: 경로는 `PATCH /api/small-groups/{id}`이고 ID는 `small_group.small_group_id`이다. 수정은 삭제되지 않은 소모임에서만 가능하며, 개설자이면서 `OWNER`·`APPROVED` 관계인 사용자만 허용한다. 정원은 2~20명이고 승인 OWNER·MEMBER 수보다 작게 줄일 수 없다. 소모임은 상태 정책이 없으므로 상태 기반 수정 제한을 추가하지 않는다.
- AI 작성: 전달 필드 감지·Bean Validation 요청 DTO, 공통/소모임 전용 MyBatis UPDATE DTO와 잠금 SQL, Service 트랜잭션·권한·정원 검증, PATCH Controller, Service·Mapper XML·Controller·Security 테스트, API 명세를 추가했다.
- 응답·오류: 성공 시 기존 `ApiResponse.ok(null)`을 반환한다. 없는·삭제 소모임은 `404 SMALL_GROUP_NOT_FOUND`, 권한 없음은 `403 SMALL_GROUP_SETTINGS_FORBIDDEN`, 승인 인원보다 작은 정원은 `409 SMALL_GROUP_CAPACITY_BELOW_MEMBERS`, 요청 검증 실패는 공통 `400 BAD_REQUEST`를 사용한다.
- DB·의존성·외부 연동: 기존 `groups`, `small_group`, `group_member`와 MyBatis Mapper를 재사용했다. DB 마이그레이션, 상태값, 외부 호출, 의존성·설정 변경은 하지 않았다.
- 검증: `git diff --check`를 통과했다. Gradle 대상 테스트는 현재 실행 환경에 Java 17 toolchain이 없어 테스트 컴파일 전에 중단됐다.
- 직접 확인 필요: Java 17 환경에서 PATCH Controller·Security·Service·Mapper XML 테스트를 실행하고, 실제 JWT와 MySQL에서 부분 수정, 401·403·404·409, 잠금·롤백, 삭제된 소모임 차단을 확인한다.
- 개발자 코드 검토·수정: 대기 중. 프런트 저장 연결 전 사용자에게 표시할 400·403·404·409 오류 메시지를 확인한다.

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

## 2026-10-27 — 로그아웃 정지 및 네이버 재인증 수정

- 목적: 로그인 후 로그아웃 중 표시가 유지되는 오류와 네이버 계정의 자동 인증 흐름을 수정한다.
- AI작성: Header의 존재하지 않는 clearAuth 호출을 기존 clearToken으로 교체하고, 기존 logoutMember API로 access token과 refresh 쿠키를 함께 전달한다. 성공·인증 실패·네트워크 실패 시 현재 브라우저 인증 상태를 정리하고 이동하며, 서버 폐기를 확인하지 못한 오류는 사용자에게 안내한다.
- AI작성: NaverOAuthServiceImpl의 인증 URL에 auth_type=reauthenticate를 추가한다. 카카오는 현재 서버의 리다이렉트에 prompt=login이 포함되어 있음을 확인했다.
- 검증: 프론트 빌드와 백엔드 compileJava 통과. 현재 Header 핸들러의 성공·401·네트워크 실패·중복 클릭 분기 실행 검사 통과. 실제 브라우저의 로그아웃 및 제공사 인증 화면은 서버 재시작 후 사용자 확인이 필요하다.
- 참고: https://developers.naver.com/docs/login/devguide/devguide.md 의 사용자 재인증 절, https://developers.kakao.com/docs/ko/kakaologin/rest-api 의 prompt 설명.
- 개발자 검토: 최종 확인 대기. DB·외부 앱 설정·의존성 변경 없음.

## 2026-11-02 — Valkey 기반 인증 조회 성능 개선

- 목적: 원격 Aiven MySQL을 사용하는 보호 API의 반복 인증 조회를 줄이고, 공개 목록 API의 불필요한 인증 DB 조회를 제거한다.
- AI 제안·작성: 공개 목록 API JWT 필터 제외, 세션·회원 상태·폐기 토큰 MySQL 단일 조회,
  Aiven Valkey 연결 의존성, 인증 캐시 계약·구현, 로그인·재발급 캐시 저장, 로그아웃·전체 세션 폐기 캐시 무효화,
  Valkey 우선 인증과 MySQL 대체, 단위 테스트 및 성능 개선 기록 문서.
- 보안 정책: MySQL은 원본 저장소로 유지하고 Refresh Token 원문·비밀번호는 Valkey에 저장하지 않는다.
  Valkey 읽기 장애는 MySQL 단일 인증 조회로 대체한다. 로그아웃·전체 세션 폐기에서 캐시 무효화에 실패하면
  오래된 캐시 허용을 막기 위해 요청을 실패 처리한다.
- 의존성 변경: `spring-boot-starter-data-redis` 추가. Aiven Valkey의 Host·Port·User·Password는
  IntelliJ 환경변수로만 설정하며 비밀번호는 코드·Git·문서에 기록하지 않는다.
- 검증: `git diff --check` 통과. `AuthSessionServiceImplTest`에 캐시 적중·캐시 미스·폐기 토큰·장애 대체를 추가했다.
  Gradle 테스트는 Wrapper 잠금 파일 접근 거부로 실행 결과를 확보하지 못했다.
- 개발자 확인 필요: 백엔드 재시작 후 Aiven Valkey TLS 연결, 로그인·로그아웃·공개 목록·장애 대체 동작과
  실제 API 응답 시간 및 MyBatis 인증 SQL 호출 여부를 확인한다.
- 상세 기록: [인증 조회 성능 개선 기록](auth-performance-improvement.md)
## 2026-10-28 — 관리자 전체 댓글 목록 API 및 화면 연결

- 목적: 관리자가 전체 댓글을 검색·작성자 지역·공개/삭제 상태로 필터링하고 페이징 조회할 수 있게 한다.
- AI 제안·작성: `GET /api/admin/comments` 요청 DTO·응답 DTO·Service·MyBatis 조회 SQL을 추가하고, 기존 게시글별 댓글 조회·삭제 URL은 유지했다. 프론트 관리자 댓글 화면은 Mock 대신 실제 API와 로딩·오류·빈 상태를 사용한다.
- 사용자 확정: 댓글 숨김·공개 기능은 제외하고 `deleted_at` 기준의 `VISIBLE`/`DELETED` 필터만 제공한다. 댓글 삭제 버튼 API 연결은 후속 작업으로 분리한다.
- DB·의존성·외부 서비스 변경: 없음.
- 검증: 관리자 댓글 Controller·Service 테스트 통과, `npm run build` 통과. Vite 청크 크기 경고는 기존 빌드 구조의 비차단 경고이다.
- 개발자 검토: MySQL 실제 데이터로 검색·지역·공개/삭제 필터와 페이지 전환을 확인해야 한다.

## 2026-10-28 — 관리자 댓글 삭제 화면 연결

- 목적: 관리자 댓글 목록의 삭제 버튼을 기존 백엔드 소프트 삭제 API와 연결한다.
- AI 제안·작성: 게시글·댓글 번호를 사용하는 삭제 API 함수, 기존 `ConfirmModal` 확인 창, 요청 중 중복 제출 차단, 성공·실패 안내, 성공 후 현재 필터·페이지 재조회를 연결했다.
- 사용자 확정: 댓글 숨김·공개는 구현하지 않고 기존 `deleted_at` 기반 삭제만 제공한다.
- DB·의존성·외부 서비스 변경: 없음.
- 검증: 기존 관리자 댓글 Controller·Service 삭제 테스트 통과, `npm run build` 통과, `npm run lint` 통과. 린트의 기존 경고 7건은 이번 작업과 관계없다.
- 개발자 검토: 브라우저에서 삭제 확인·취소·성공·이미 삭제된 댓글 재요청 흐름을 확인해야 한다.

## 2026-10-29 — 관리자 회원 이용 제한·해제 및 실제 API 연결

- 목적: 관리자가 회원 상세에서 7일·30일·영구 제한과 제한 해제를 처리하고 회원 목록·상세 Mock을 실제 API로 교체한다.
- AI 제안·작성: 기존 신고 제재의 기간 계산을 `MemberRestrictionPolicy`로 공통화하고, 직접 제한·해제 상태 변경과 `restriction_history` 이력 저장을 하나의 트랜잭션으로 처리했다. 인증된 관리자 번호로 본인 제한을 막고 기존 `AuthSessionService`로 대상 회원의 로그인 세션을 폐기한다.
- AI 제안·작성: 프론트 회원 목록·상세 API 모듈, 검색·상태·지역·페이징, 제한 기간 선택, 필수 사유, 중복 제출 방지, 성공 후 상세 재조회, 로딩·오류·빈 상태를 연결했다.
- DB·의존성·외부 서비스 변경: 없음. 기존 `members.account_status`, `members.suspended_until`, `restriction_history`를 재사용했다.
- 검증: 백엔드 `compileJava`, 프론트 `npm run build`, `npm run lint` 통과. 전체 백엔드 테스트는 기존 `SmallGroupSecurityIntegrationTest` 생성자 인자 불일치로 테스트 소스 컴파일 단계에서 중단된다.
- 개발자 검토: MySQL 연결 환경에서 직접 제한·해제 이력과 기존 JWT·재발급 차단을 Postman 및 브라우저로 최종 확인해야 한다.

## 2026-10-29 — 관리자 계모임 이용 제한·해제 및 실제 API 연결

- 목적: 관리자가 진행 중인 계모임을 기간 없이 활동 정지하고 문제 해결 후 해제하며, 관리자 계모임 Mock 화면을 실제 API로 교체한다.
- 사용자 확정: 기존 `02(진행 중) ↔ 03(일시정지)` 상태를 재사용하고, 최근 `restriction_history`가 미해제 `RESTRICT`인 경우에만 관리자 제한 해제를 허용한다.
- AI 제안·작성: 기존 `SavingGroupService.changeSavingGroupStatus`를 재사용해 회비 설정 보류·재개 로직의 중복을 피하고, 상태 변경과 제한·해제 사유 저장을 하나의 트랜잭션으로 처리했다. 계모임은 기간제가 아니므로 `restriction_type`과 `restricted_until`은 NULL로 저장한다.
- AI 제안·작성: 공개 목록·상세의 기존 `01/02` 조회 조건을 유지하고 가입 관리·계원 추방·설정 조회의 `03` 상태 활동을 차단했다. 프론트에는 실제 목록 API, 서버 페이징·필터, 사유 입력 모달, 중복 제출 방지, 성공 후 재조회를 연결했다.
- DB·의존성·외부 서비스 변경: 없음. 기존 `saving_group.status`와 `restriction_history`를 재사용했다.
- 검증: 백엔드 `compileJava`, 프론트 `npm run lint`, `npm run build`, `git diff --check` 통과. 린트의 기존 경고 8건은 이번 작업과 관계없다.
- 개발자 검토: 실제 DB에서 제한·해제 이력과 일반 일시정지 해제 거부, 제한 상태의 일반 사용자 접근·활동 차단을 확인해야 한다.

## 2026-10-29 — 계모임 삭제 심사

- 목적: 계주가 사유와 함께 삭제 심사를 신청하고, 관리자가 목록·상세에서 승인 또는 반려하도록 연결한다.
- 사용자 확정: 새 테이블·컬럼 없이 기존 `meeting_approval(type='DELETE')`, `saving_group.status`, 거래·출금 원장, `groups.deleted_at`을 재사용한다.
- AI 제안·작성: 계주 권한 및 중복 신청 검증, 관리자 유형 필터, 승인 시 진행 상태·공동자금·미처리 출금 재검사, 트랜잭션 기반 심사 갱신·소프트 삭제, 프론트 신청 모달과 관리자 심사 화면을 구현했다.
- AI 제안·작성: 계주 탈퇴 후에도 DELETE 심사를 유지하되 신청자 이름을 `탈퇴한 회원`으로 마스킹하고, 중복 승인·반려 및 상태 변경·소프트 삭제 실패 경로를 관리자 서비스 테스트로 보강했다.
- DB·의존성·외부 서비스 변경: 없음.
- 검증: 백엔드 `compileJava`, `processResources`, `git diff --check` 통과, 프론트 `npm run build` 및 `npm run lint` 통과. 백엔드 테스트는 기존 `SmallGroupSecurityIntegrationTest` 생성자 인자 불일치로 테스트 소스 컴파일 단계에서 중단된다.
- 개발자 검토: 실제 DB에서 0원/잔액 존재, 상태 01/02/03/04, PENDING·APPROVED 출금 요청, 승인 후 일반 조회 제외를 확인해야 한다.

## 2026-10-30 — 회원 기능별 제한 및 신고 승인 범위 선택

- 목적: 계정 정지와 커뮤니티 기능 제한을 분리하고, 관리자 직접 제한과 신고 승인에서 CONTENT·CHAT·REPORT 범위를 선택해 독립적으로 제한한다.
- 사용자 확정: 현재 제한은 `member_feature_restriction`, 과거 이력은 `restriction_history`에 저장하며 로그인과 조회 기능은 허용한다.
- AI 제안·작성: 기능 범위·액션 Enum, 범위별 제한 UPSERT·해제·만료 정리, 공통 기간 누적 정책, 신고 승인 범위 선택과 조합 검증, 관리자 상세의 제한 목록, 프론트 신고 처리 영역 선택을 구현했다.
- DB 변경 필요: `member_feature_restriction` 테이블과 `restriction_history.restriction_scope`, `report_case.restriction_scope` 컬럼이 필요하지만 DB 담당자 반영 전이므로 `schema.sql`은 수정하지 않았다.
- 검증: 관리자 관련 백엔드 테스트 통과, Mapper XML 파싱 및 프론트 `npm run lint`, `npm run build` 통과. 린트의 기존 경고와 Vite 청크 크기 경고는 이번 변경과 관계없는 비차단 경고이다.
- 개발자 검토: DB 마이그레이션 후 신고 승인 범위별 제한과 관리자 직접 제한·해제, 만료 정리를 실제 MySQL 및 브라우저에서 확인해야 한다.

## 2026-10-30 — 회원 기능별 제한 실제 적용 및 관리자 목록 표시

- 목적: 계정 로그인을 차단하지 않고 `CONTENT`, `CHAT`, `REPORT` 영역별 제한을 독립적으로 관리하며, 구현된 콘텐츠 기능에 실제 제한을 적용한다.
- 사용자 확정: `CONTENT` 제한 회원은 게시글·댓글 조회는 가능하지만 작성·수정·삭제는 할 수 없다. 채팅과 일반 회원 신고 접수는 해당 기능 구현 브랜치에서 각각 제한 검사를 연결한다.
- AI 제안·작성: `MemberFeatureRestrictionService`의 공통 제한 검사를 게시글·댓글 Service에서 재사용하고, 각 Service 내부의 반복 호출은 `validateContentAllowed` 메서드로 정리했다. 현재 제한은 `member_feature_restriction`의 회원·영역 행 존재 여부와 `restricted_until`로 판정한다.
- AI 제안·작성: 관리자 회원 목록에 활성 제한 영역 필터와 현재 제한 영역 표시를 추가했다. 목록 SQL은 `EXISTS`로 필터링하고, 표시 데이터는 상관 서브쿼리의 `GROUP_CONCAT`으로 함께 조회해 중복 목록 행과 회원별 추가 조회를 방지했다.
- DB 변경 필요: `member_feature_restriction` 테이블과 관련 제한 범위 컬럼은 DB 담당자 반영이 필요하며, 이번 작업에서는 `schema.sql`을 수정하지 않았다.
- 검증: 백엔드 `compileJava`와 기능 제한 Service 단위 테스트가 통과했다. 프론트 변경 파일은 Vite 변환을 통과했으나 전체 빌드는 기존 `VerificationPage.jsx`의 `@portone/browser-sdk/v2` 의존성 누락으로 중단됐다.
- 개발자 검토: DB 반영 후 `CONTENT` 기간·영구 제한 상태에서 게시글·댓글 작성·수정·삭제 차단과 조회 허용, 만료 후 재허용, 관리자 목록의 영역별 필터·표시를 실제 MySQL 및 브라우저에서 확인해야 한다.

## 2026-10-30 — 게시글·댓글 서비스 테스트 복구 및 댓글 공백 정규화

- 목적: 기능별 제한 서비스 추가 후 실패하던 게시글·댓글 단위 테스트를 복구하고, 댓글 저장값의 앞뒤 공백 처리 규칙을 서버에서 통일한다.
- AI 제안·작성: 게시글·댓글 서비스 테스트에 `MemberFeatureRestrictionService` Mock을 추가하고, 일반 댓글·대댓글 등록과 댓글 수정 시 `trim()`을 적용했다.
- AI 제안·작성: 일반 댓글·대댓글·수정 댓글에 공백이 포함된 입력을 사용해 Mapper 전달 전에 정규화되는지 검증하는 테스트를 추가했다.
- DB·의존성·외부 서비스 변경: 없음.
- 검증: `PostServiceImplTest`, `CommentServiceImplTest` 통과. 전체 백엔드 테스트 실행은 후속 단계로 분리했다.
- 개발자 검토: 최종 확인 대기. 댓글 본문의 의도적인 앞뒤 공백도 제거되는 정책이 서비스 요구사항과 일치하는지 확인해야 한다.

## 2026-10-30 — 게시글·댓글 입력·집계·가독성 개선

- 목적: 게시글 입력 제한을 작성 단계에서 안내하고, 댓글 API의 잘못된 경로 번호와 삭제 댓글 집계 문제를 보완하며 상세 화면 가독성을 높인다.
- AI 제안·작성: 게시글 제목과 본문 입력에 백엔드와 동일한 100자·5,000자 `maxLength`를 적용하고, 댓글 Controller의 게시글·댓글·부모 댓글 번호에 `@Positive`를 추가했다.
- AI 제안·작성: 댓글 총 개수 SQL에서 삭제된 댓글을 제외하고, Mapper XML 파싱 및 `deleted_at IS NULL` 조건을 검증하는 테스트를 추가했다.
- AI 제안·작성: 50~60세대 사용자를 고려해 게시글 상세의 메타·본문·댓글·입력창 글자를 14~18px 범위로 확대했다.
- AI 제안·작성: `@Validated` 프록시와 Spring MVC 기본 메서드 검증에서 발생하는 `ConstraintViolationException`, `HandlerMethodValidationException`을 공통 400 응답으로 처리하고 댓글 경로 번호 검증 테스트를 추가했다.
- DB·의존성·외부 서비스 변경: 없음.
- 검증: 프론트 `npm run build`와 변경 파일 lint, 백엔드 컴파일, 댓글 Service 테스트, `CommentMapperXmlTest`, `CommentControllerTest`가 통과했다. 기존에 실패했던 소모임 설정·투표 Controller 경로 검증 테스트 4건도 통과했다.
- 개발자 검토: 실제 서버에서 0·음수 경로 요청의 400 응답과 메시지 표시를 Postman으로 최종 확인해야 한다.

## 2026-11-02 — Valkey 기반 인증 조회 성능 개선

- 목적: 원격 Aiven MySQL을 사용하는 보호 API의 반복 인증 조회를 줄이고, 공개 목록 API의 불필요한 인증 DB 조회를 제거한다.
- AI 제안·작성: 공개 목록 API JWT 필터 제외, 세션·회원 상태·폐기 토큰 MySQL 단일 조회,
  Aiven Valkey 연결 의존성, 인증 캐시 계약·구현, 로그인·재발급 캐시 저장, 로그아웃·전체 세션 폐기 캐시 무효화,
  Valkey 우선 인증과 MySQL 대체, 단위 테스트 및 성능 개선 기록 문서.
- 보안 정책: MySQL은 원본 저장소로 유지하고 Refresh Token 원문·비밀번호는 Valkey에 저장하지 않는다.
  Valkey 읽기 장애는 MySQL 단일 인증 조회로 대체한다. 로그아웃·전체 세션 폐기에서 캐시 무효화에 실패하면
  오래된 캐시 허용을 막기 위해 요청을 실패 처리한다.
- 의존성 변경: `spring-boot-starter-data-redis` 추가. Aiven Valkey의 Host·Port·User·Password는
  IntelliJ 환경변수로만 설정하며 비밀번호는 코드·Git·문서에 기록하지 않는다.
- 검증: `git diff --check` 통과. `AuthSessionServiceImplTest`에 캐시 적중·캐시 미스·폐기 토큰·장애 대체를 추가했다.
  Gradle 테스트는 Wrapper 잠금 파일 접근 거부로 실행 결과를 확보하지 못했다.
- 개발자 확인 필요: 백엔드 재시작 후 Aiven Valkey TLS 연결, 로그인·로그아웃·공개 목록·장애 대체 동작과
  실제 API 응답 시간 및 MyBatis 인증 SQL 호출 여부를 확인한다.
- 상세 기록: [인증 조회 성능 개선 기록](auth-performance-improvement.md)

## 2026-11-02 — 관리자 페이지 공통 레이아웃 정리

- 목적: 관리자 화면마다 반복되던 페이지 너비, 제목, 사이드바, 본문 그리드 구조를 공통화한다.
- AI 제안·작성: `AdminPageLayout`을 추가하고 관리자 회원·계모임·소모임·게시글·댓글·신고 화면에서 재사용하도록 변경했다. 화면별 조회·처리 로직과 모달은 유지했다.
- DB·API·의존성 변경: 없음.
- 검증: 변경 파일 `oxlint` 통과, `git diff --check` 통과. 별도 기능 테스트는 실행하지 않았다.
- 개발자 검토: 데스크톱·태블릿·모바일에서 관리자 메뉴와 제목, 표 너비, 모달 표시가 기존과 동일한지 확인해야 한다.

## 2026-11-02 — 관리자 신고 유형·상태 선택지 공통화

- 목적: 신고 유형과 처리 상태 코드가 API 변환, 필터, 백엔드 정규식에 반복되어 서로 달라질 수 있는 문제를 줄인다.
- AI 제안·작성: 프론트 공통 옵션을 추가해 API 표시 변환과 필터가 재사용하게 하고, 백엔드 검색 조건은 문자열 정규식 대신 `AdminReportType`, `AdminReportStatus` Enum으로 제한했다.
- 기존 정책 유지: 미완성 채팅 신고의 `CHAT_MESSAGE`는 응답 표시와 백엔드 조회를 지원하지만 관리자 필터에는 노출하지 않는다.
- DB·API·의존성 변경: 없음. 요청 파라미터의 기존 문자열 코드는 그대로 유지된다.
- 검증: 프론트 변경 파일 `oxlint`, 백엔드 `compileJava`, `git diff --check`를 확인했다. 기능 테스트는 실행하지 않았다.

<!-- AI작성: 사용자 요청에 따라 후기 목록 작업 이력을 마지막 문서화 단계에서 일괄 기록한다. -->
## 2026-11-02 — 소모임 후기 목록 조회 1~10단계 및 Mapper 누락 복구

- 목적: 모든 사용자가 소모임 후기를 최신순으로 읽고 8개 단위로 스크롤 조회하도록 기존 등록 기능에 목록을 연결한다.
- 사용자 확정: 비로그인·비회원·미승인도 공개 조회, MANAGER 차단, 닉네임·내용·작성일·별점 표시, 검색·필터 없음, 삭제 후기 제외 및 관리자 숨김 정책은 별도 확정. 문서는 마지막 단계에 모아서 작성한다.
- 1단계 AI작성: SmallGroupReviewSearchCondition(page 기본 1, NotNull·Min), ListCondition(대상 소모임·offset·고정 limit 8), ListItemResponse(공개 항목·BigDecimal 별점), ListResponse(items·PageMeta), Mapper 건수·목록 계약을 추가했다.
- 2단계 AI작성: SmallGroupMapper.xml에서 동일 소모임 조건의 건수·목록 SQL, 닉네임 LEFT JOIN, 작성일·후기 ID 내림차순 및 LIMIT을 구현하고 기존 XML 파싱 테스트에 statement 확인을 추가했다.
- 3단계 AI작성: Service 인터페이스·구현체에 읽기 트랜잭션의 getSmallGroupReviews를 추가했다. 기존 상세·회원 관계 조회로 소모임 존재와 MANAGER·중복·미정의 역할을 검증한다. 승인·작성 가능 날짜로 조회를 제한하지 않으며 범위 초과는 offset 계산 전 빈 목록을 반환한다.
- 4단계 AI작성: 목록 Service 테스트에 페이지 조건·메타·공개 조회·MANAGER 차단·관계 오류·잘못된 입력을 검증하는 사례를 추가했다. 삭제된 대상은 Mapper가 null을 반환하는 경우로 검증하며 실제 DB SQL 검증은 아니다.
- 5~6단계 AI작성: GET 목록 Controller·정확한 숫자 경로의 공개 Security 설정·Controller 테스트·비로그인 Security 테스트를 추가했다. 기존 POST 인증 필수 테스트를 유지한다. 목록 ID의 Positive는 공통 예외 처리와의 호환 문제로 제거하고 Service 범위 검증·업무 예외 응답을 사용하도록 조정했다.
- 7단계 AI작성: smallGroups.js에 getSmallGroupReviews·오류 클래스·공개 항목 변환을 추가하고 기존 API 테스트에 목록 테스트 6개를 추가했다. 페이지·signal만 전달하며 서버 오류·요청 취소를 보존하고 작성자 ID를 제외한다.
- 8단계 AI작성: SmallGroupReviewsPanel을 추가하고 비회원 후기 탭을 공개했다. 하단 감지 추가 조회·중복 요청 및 ID 방지·취소·빈 상태·오류 재시도·마지막 안내·미지원 환경 더 보기 버튼을 처리했다. 기존 작성 버튼·모달 조건은 유지했다.
- 9단계 AI작성: 기존 등록 모달 콜백에 created 여부를 전달했다. 실제 저장 성공 시에만 목록 key를 변경해 첫 페이지부터 재조회하고, 중복 등록은 완료 안내만 표시하도록 연결했다.
- 후속 복구 AI작성: DTO 4개는 존재하지만 Mapper import·조회 선언 2개가 누락된 것을 재확인해 SmallGroupMapper.java 한 파일에서 복구했다. Service 호출·XML 이름과 일치함을 확인했다.
- 10단계 AI작성: docs/small-group-public-detail-api.md의 공개 탭·보안 경로·기존 등록 범위 설명을 갱신하고 목록 요청·응답·권한·처리 구조·화면 흐름·검증·TODO를 추가했다. 본 기록을 포함해 문서 2개만 수정했다.
- 최종 실행 검증: front의 node --test src/api/smallGroups.test.js 51개 통과, npm run build 통과(번들 크기 경고). git diff --check 통과. 이전 화면 단계의 신규 패널·등록 연동 파일 린트 통과, 기존 메뉴 파일의 Fast Refresh 경고는 별도 남아 있다.
- 백엔드 실행 제한: 후기 등록·목록 Service/Controller 및 Security·Mapper XML 테스트 총 6개 클래스를 지정해 gradlew test --offline을 시도했으나 Java 17 toolchain 부재로 컴파일 전에 중단됐다. 통과 여부는 확인하지 못했다.
- 미실행 검증: 실제 DB·발급 JWT·Postman·브라우저 동작은 확인하지 않았다. 상세 수동 절차는 API 문서의 후기 목록 절에 기록했다.
- 개발자 검토·수정: 단계별 커밋은 사용자 수행. 최종 정책·동작 검토 대기. DB 담당자 테이블 적용 및 삭제 정책 확정, Java 17 환경 테스트, 스크롤·등록 후 갱신을 검토해야 한다.
- 남은 사항: 전달 DDL에는 삭제 컬럼이 없어 SQL 삭제 제외 조건은 미구현이며 물리 삭제만 자연스럽게 제외된다. 신고·비공개 숨김과 계정 삭제 작성자 닉네임 처리는 별도 정책이 필요하다. offset 방식의 동시 등록·삭제 시 페이지 구성 변화는 남아 있다.
- DB·의존성·외부 서비스 변경: 없음. 후기 테이블 schema.sql·ERD·실제 DB 반영은 DB 담당자 작업이다. 기존 컴파일 환경과 공통 설정을 임의 변경하지 않았다.

<!-- AI작성: 계모임·소모임 검색 지역 필터 1단계 구조 조사 결과입니다. -->
## 2026-11-03 — 모임 검색 시·구·동 필터 1단계 구조 확인

- 목적: 구현 전 기존 지역 선택·검색 구조와 재사용 범위, 후속 변경 대상을 확인한다.
- 작업 범위: 관련 소스 정적 확인과 본 기록만 작성했다. 구현 코드·DB·API·의존성·외부 서비스는 변경하지 않았다. 커밋은 사용자가 수행한다.
- 회원 지역 선택: front/src/components/member/JoinForm.jsx와 front/src/pages/VerificationPage.jsx는 getMemberRegions(front/src/api/member.js)의 /auth/regions 응답을 사용한다. regionId, provinceName, districtName, townName으로 시·구·동을 구성하며 상위 선택 변경 시 하위를 초기화한다. 회원용 폼 전체를 재사용하기보다는 기존 조회 함수와 선택지 구성 방식을 활용할 수 있다.
- 현재 공개 모임 검색: SavingGroupsPage.jsx·SmallGroupsPage.jsx와 각 FilterBar는 단일 지역 선택 UI다. savingGroups.js·smallGroups.js는 고정 지역 코드 01~10을 전달하고 실제 백엔드 API로 조회한다. 두 SearchCondition은 해당 코드만 허용하고, ServiceImpl에서 고정 지역명으로 변환하며 ListCondition과 Mapper XML은 r.region_name 완전 일치로 검색한다. UI만 변경해서는 시·구·동 검색이 동작하지 않는다.
- 재사용 참고: 관리자 계모임·소모임 검색은 같은 회원 지역 조회와 provinceName·districtName·townName 조건을 이미 사용한다. AdminSavingGroupMapper.xml·AdminSmallGroupMapper.xml의 선택된 단계별 컬럼 일치 조건을 참고할 수 있다. 관리자용 UI 스타일·권한은 공개 검색에 그대로 적용하지 않는다.
- DB 영향: schema.sql의 region에는 province_name·district_name·town_name이 있고 groups.region_id를 통해 모임과 연결된다. 현재 소스상 컬럼 추가는 불필요해 보인다. 실제 DB의 동 데이터와 기존 광역 지역 행의 컬럼 채움 여부는 미확인이다. region-data.sql은 시·군·구 중심이므로 이 파일만으로 동 데이터 준비를 보장하지 않는다. 세종의 district_name 빈 문자열 처리도 확인해야 한다.
- 후속 계모임 변경 대상: front/src/pages/SavingGroupsPage.jsx, front/src/components/savingGroup/SavingGroupFilterBar.jsx, front/src/api/savingGroups.js, back/src/main/java/com/nightstudy/moa/savingGroup/dto/SavingGroupSearchCondition.java·SavingGroupListCondition.java, back/src/main/java/com/nightstudy/moa/savingGroup/service/SavingGroupServiceImpl.java, back/src/main/resources/mappers/SavingGroupMapper.xml. 기존 API 테스트·관련 명세도 변경 내용에 맞게 갱신한다.
- 후속 소모임 변경 대상: front/src/pages/SmallGroupsPage.jsx, front/src/components/smallGroup/SmallGroupFilterBar.jsx, front/src/api/smallGroups.js, back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupSearchCondition.java·SmallGroupListCondition.java, back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java, back/src/main/resources/mappers/SmallGroupMapper.xml. 기존 API·Service·Mapper 테스트와 docs/small-group-public-detail-api.md를 변경 내용에 맞게 갱신한다.
- 구현 전 승인 사항: 공개 검색에 provinceName·districtName·townName 선택 조건을 추가하는 방안은 관리자 검색과 일관되지만 API 계약 변경이다. 기존 regionCode 유지 시 하위 호환성이 있으나 두 조건 동시 입력 우선순위가 필요하고, 교체 시 단순하지만 기존 호출자 변경이 필요하다. 사용자 승인 전 확정하거나 구현하지 않는다. 여러 파일 수정도 대상 목록과 이유를 공유하고 승인 후 진행한다.
- 후속 검증: 전체·시·시/구·시/구/동 검색, 잘못된 상하위 조합, 상위 변경 시 초기화, 첫 페이지 재조회, 기존 검색어·관심사 조합, 로딩·오류·빈 결과, 비로그인 지역 조회, 세종 선택을 확인한다. 현재 지역 변경 즉시 조회되는 동작은 유지 여부를 확인하고 임의로 검색 버튼 방식으로 바꾸지 않는다.
- 검증 범위: 정적 소스 확인만 수행했다. 브라우저·Postman·실제 DB·자동 테스트는 실행하지 않았다. 개발자 검토·수정 내용: 아직 검토 대기이며 본 설계와 실제 데이터 상태를 확인해야 한다.

<!-- AI작성: 승인된 계모임 검색 지역 선택 UI 2단계 구현 기록입니다. -->
## 2026-11-03 — 계모임 검색 지역 필터 2단계 UI 적용

- 목적: 회원가입·지역 인증과 같은 지역 목록으로 계모임 검색에 시·구·동 선택 UI를 구성한다.
- 사용자 승인: SavingGroupFilterBar.jsx, SavingGroupsPage.jsx, 본 기록 총 3개 파일 수정. 시만 또는 시·구까지만 선택해도 검색할 수 있으며 하위 지역을 포함하는 실제 검색 연동은 3단계로 분리한다.
- AI 제안·작성: getMemberRegions 재사용, 시·도/시·군·구/읍·면·동 선택지, 상위 미선택 시 하위 비활성화, 시 변경 시 구·동 초기화, 구 변경 시 동 초기화, 지역 조회 로딩·실패·재시도를 추가했다. 구 ID를 선택값으로 사용해 세종의 빈 구 이름도 구분한다. 화면 너비에 따라 선택란을 줄바꿈한다.
- 단계 제한: 기존 고정 regionCode 선택 UI와 전송을 제거했으며 이번 단계의 지역 선택값은 목록 API에 전달하지 않는다. 현재 목록은 검색어·페이지로 조회되므로 지역별 검색 결과 검증은 3단계 연동 후 수행해야 한다. 구·동 미선택은 검색 제출을 막지 않는다.
- 검증: 변경 JSX 두 파일 oxlint 통과, npm run build 통과(500kB 초과 번들 경고), git diff --check 통과. 브라우저·실제 지역 API·DB 검증은 실행하지 않았다.
- 브라우저 검증 방법: 계모임 목록에서 지역 로딩 확인 → 시만 선택하고 검색 → 구까지 선택하고 검색 → 동 선택 후 시·구 변경 시 하위 초기화 확인 → 세종의 구 선택 뒤 동 목록 확인 → 지역 조회 실패 시 재시도 및 모바일 줄바꿈 확인. 실제 지역 범위별 결과는 3단계에서 확인한다.
- 개발자 검토·수정: 사용자 검토 대기. 커밋은 사용자 수행. DB·API 계약·의존성·외부 서비스 변경 없음. 실제 DB 동 데이터 준비 여부는 별도 확인이 필요하다.

<!-- AI작성: 승인된 계모임 지역 검색 API 연동 3단계 구현 기록입니다. -->
## 2026-11-03 — 계모임 시·구·동 검색 조건 연동

- 목적·승인: 기존 regionCode 호환을 유지하면서 provinceName·districtName·townName을 추가하는 추천안으로 9개 파일을 수정했다. 커밋은 사용자가 수행한다.
- AI 제안·작성: SavingGroupsPage.jsx에서 선택한 정규 지역명을 전달하고 지역 변경 시 첫 페이지를 조회한다. savingGroups.js는 미선택 지역 파라미터를 생략하고 세종의 빈 구 이름을 보존한다. SavingGroupSearchCondition에 길이·상위 필수·기존 코드와 동시 사용 금지 검증을 추가했다. SavingGroupListCondition·SavingGroupServiceImpl·SavingGroupMapper.xml에 선택 단계까지만 AND 적용하는 조건을 추가했다. 목록·건수는 기존 공통 listWhere를 재사용한다. 기존 regionCode의 지역명 일치 동작과 응답 형식은 유지한다.
- 검색 범위: 미선택은 전체, 시만 선택하면 해당 시의 모든 구·동 모임, 시·구 선택이면 해당 구의 모든 동 모임, 동 선택 시 해당 동이다. region 테이블에 상위 지역 컬럼이 채워진 행을 기준으로 조회한다. 존재하지 않거나 서로 맞지 않는 지역명 조합은 결과 0건이다. 실제 지역 존재 조회·DB 데이터 보정은 추가하지 않았다.
- 테스트·명세: savingGroups.test.js에 전체·시·시/구·시/구/동·세종 빈 구 이름과 취소 신호 전달 검증을 추가했다. docs/saving-groups-api.md에 신규 파라미터·검증·범위·화면 동작을 갱신했다.
- 실행 검증: node --test src/api/savingGroups.test.js 29개 통과, 변경 프론트 파일 oxlint 통과, npm run build 통과(기존 번들 크기 경고), git diff --check 통과.
- 백엔드 검증 제한: gradlew.bat compileJava --offline은 기본 실행에서 Gradle 캐시 잠금 접근이 거부됐고, 권한 재시도 후에는 Java 17 toolchain 부재로 컴파일 전에 중단됐다. Java 21은 있으나 프로젝트 설정은 변경하지 않았다. 백엔드 컴파일·Validation·실제 SQL 실행 통과를 확인하지 못했다.
- 수동 검증: 브라우저에서 전체→시→구→동 선택별 결과 및 2페이지 이후 지역 변경 시 첫 페이지 확인. Postman GET /api/saving-groups에 provinceName만, provinceName+districtName, 세 항목을 각각 보내 범위·건수를 비교한다. regionCode=01 단독은 기존 동작, regionCode+provinceName 및 districtName 단독·townName 단독은 400이어야 한다. 존재하지 않는 조합은 200 빈 목록이어야 한다. 세종은 provinceName+districtName=(빈 문자열)+townName으로 확인한다. 실제 DB region 상위 컬럼과 동 데이터 준비 여부도 확인한다.
- 개발자 검토·수정: 검토 대기. 실제 브라우저·Postman·DB 검증은 미실행이다. DB·의존성·외부 서비스 변경 없음. 소모임 검색은 다음 단계 범위다.

<!-- AI작성: 승인된 소모임 지역 필터 4단계 구현 기록입니다. -->
## 2026-11-03 — 소모임 시·구·동 선택 및 검색 연동

- 목적·사용자 승인: 4단계로 승인된 10개 파일에서 소모임 검색에도 계모임과 동일한 시·구·동 선택·검색 규칙을 적용한다. 커밋은 사용자 수행.
- AI 제안·작성: SmallGroupFilterBar.jsx·SmallGroupsPage.jsx에 회원 지역 API getMemberRegions 재사용, 시·구·동 선택지, 상위 미선택 시 하위 비활성화, 시 변경 시 구·동 초기화, 구 변경 시 동 초기화, 지역 변경 시 첫 페이지 재조회, 지역 로딩·실패·재시도를 적용했다. 구 ID로 세종 빈 구 이름과 미선택을 구분한다. 기존 검색어·관심사·요청 취소·오래된 응답 무시는 유지한다.
- API·SQL: smallGroups.js에 provinceName·districtName·townName을 추가하고 SmallGroupSearchCondition에 길이·상위 조건 필수·regionCode와 동시 사용 금지 검증을 추가했다. SmallGroupListCondition·SmallGroupServiceImpl·SmallGroupMapper.xml은 선택한 단계까지만 AND 조건을 적용한다. 시만 선택하면 해당 시의 모든 구·동, 시·구까지만 선택하면 해당 구의 모든 동을 포함한다. 목록·건수는 동일한 smallGroupListWhere를 재사용한다. 기존 regionCode는 호환 유지, 존재하지 않거나 맞지 않는 지역명 조합은 빈 결과다.
- 작업 파일: front/src/components/smallGroup/SmallGroupFilterBar.jsx, front/src/pages/SmallGroupsPage.jsx, front/src/api/smallGroups.js, front/src/api/smallGroups.test.js, back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupSearchCondition.java, back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupListCondition.java, back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java, back/src/main/resources/mappers/SmallGroupMapper.xml, docs/small-group-public-detail-api.md, 본 기록.
- 실행 검증: 계모임·소모임 API 테스트 총 83개 통과(기존 코드, 전체·시·구·동·세종 빈 구 이름, 검색어·관심사 결합, 실패·취소 포함). 변경 프론트 4개 파일 oxlint 통과, npm run build 통과(기존 500kB 초과 번들 경고), git diff --check 통과. .NET XML 파서로 Mapper XML 파싱과 목록·건수의 공통 조건 참조를 확인했다. Python 실행 별칭은 접근 불가여서 XML 확인은 .NET으로 수행했다.
- 백엔드 제한: 앞 단계에서 확인된 Java 17 toolchain 부재로 이번에는 Gradle을 반복 실행하지 않았다. 백엔드 컴파일·Validation 실제 실행·MyBatis 바인딩·DB SQL 동작은 검증하지 못했다. 설정·의존성을 변경하지 않았다.
- 브라우저·Postman 검증 방법: 전체·시·시/구·시/구/동 범위와 건수, 기존 검색어·관심사 결합, 페이지 이동 후 지역 변경 시 1페이지, 상위 변경 시 하위 초기화, 지역 목록 실패·재시도, 세종 동 선택과 모바일 줄바꿈을 확인한다. GET /api/small-groups에서 기존 regionCode 단독은 기존 결과, regionCode와 새 조건 동시 사용·구/동 단독은 400, 존재하지 않는 지역 조합은 200 빈 목록인지 확인한다. 계모임에도 동일 검색을 수행해 회귀 점검한다.
- 개발자 검토·수정: 검토 대기. 브라우저·실제 서버·Postman·DB 검증은 미실행. DB region의 상위 컬럼과 동 데이터가 준비돼 있어야 하며 데이터 보정은 하지 않았다. DB·의존성·외부 서비스 변경 없음. 5단계 예외 상태 최종 점검은 별도 요청 범위다.

<!-- AI작성: 사용자 요청에 맞춘 5단계 검색 초기화·결과 건수 표시 작업 기록입니다. -->
## 2026-11-03 — 검색 초기화 및 결과 건수 표시

- 목적·범위: 사용자가 이전 5단계를 되돌린 4단계 커밋 b56c3bb에서 검색 초기화와 결과 건수 표시만 적용하도록 요청했다.
- AI 제안·작성: SavingGroupsPage.jsx·SmallGroupsPage.jsx의 정상 빈 결과에 검색 조건 초기화 버튼을 추가했다. 입력·제출 검색어, 시·구·동, 검색어 오류, 페이지를 초기화하며 소모임은 관심사도 전체로 되돌린다. 전체 조건에서도 재조회하도록 retry를 증가시킨다. 범위 초과의 첫 페이지 이동은 유지한다.
- 결과 건수: 소모임 목록의 로딩·실패 중 결과 건수와 최신순 헤더를 숨긴다. 계모임은 기존 성공 시에만 표시하는 동작을 유지한다.
- 작업 파일: front/src/pages/SavingGroupsPage.jsx, front/src/pages/SmallGroupsPage.jsx, docs/saving-groups-api.md, docs/small-group-public-detail-api.md, 본 기록. 문서는 이번 두 기능만 기록했다.
- 검증: 변경 페이지 2개 oxlint 통과, npm run build 통과(번들 크기 경고), git diff --check 통과. 실제 브라우저 클릭 검증은 미실행이다. API 변경이 없어 기존 API 테스트는 재실행하지 않았다.
- 개발자 검토·수정: 검토 대기. 빈 결과에서 초기화 후 전체 1페이지 조회, 소모임 관심사 초기화, 로딩·실패 중 건수 숨김을 확인해야 한다. 커밋은 사용자 수행.
