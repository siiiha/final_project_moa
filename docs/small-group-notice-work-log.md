<!-- AI작성: 사용자 승인 범위의 공지 구현·실제 Git 커밋·검증 결과를 정리한 기록이다. -->
# 소모임 공지 구현 작업 기록

- 작업일: 2026-11-04 (한국 시간)
- 기준 브랜치: feature/smallgroup (28575d8)
- 작업 브랜치: feature/smallgroup-notice
- 별도 워크트리: C:/Users/USER/.codex/worktrees/smallgroup-notice/final_project_moa
- 범위: PC 메인 배치·전체 보기 버튼 삭제 → 작성 → 메인 목록·정렬·페이징 → 상세 → 수정 → 논리 삭제 → 고정·해제.
- 모바일 전용 개선·공지 첨부·일정·길찾기·DB 마이그레이션은 포함하지 않는다.
- 아래 커밋 제목은 단계별 추천명으로 사용했고 실제 Git에도 같은 제목으로 기록했다. 최종 문서 정리 커밋 자체는 이 목록 이후에 기록한다.

## 최종 동작

| 구분 | 적용 내용 |
| --- | --- |
| 메인 | 왼쪽 소개·공지, 오른쪽 다음 일정·장소. 전체 보기 버튼 제거 |
| 작성 | 해당 소모임 개설자·APPROVED OWNER만 가능. 제목 1~100자, 본문 1~5,000자, 공백만 입력 불가 |
| 목록 | 일반 공지 페이지당 4건, 최신순·오래된순, 3페이지 번호 묶음, URL 상태 복원 |
| 공개 범위 | 일반·고정 요약은 공개, 상세 본문은 APPROVED OWNER·MEMBER만 조회 |
| 수정·삭제 | 현재 해당 소모임장이면서 작성자 본인만 가능. 새 소모임장에게 타 작성자 공지 수정·삭제 권한 없음 |
| 고정 | 해당 소모임장만 지정·해제, 최대 2개. 일반 목록·건수와 분리, 행 우측 핀 버튼과 제한 안내 모달 |
| 삭제 | deleted_at만 기록. 모든 조회·고정 개수에서 제외. 마지막 페이지 삭제 시 유효 페이지로 복귀 |
| 실패 처리 | 중복 전송 방지, 입력 보존, 권한 변경 차단, 저장 후 조회 실패 시 재저장 대신 조회 재시도 |

## 주요 작업 파일

| 영역 | 파일 | 작업 |
| --- | --- | --- |
| 프론트 상세 | front/src/components/smallGroup/SmallGroupDetail.jsx | 카드 배치·전체 보기 제거·공지 패널 연결 |
| 메인 상태 | front/src/pages/SmallGroupDetailPage.jsx | 페이지·정렬·작성 후 초기화·요청 충돌·삭제 페이지 보정·재진입 갱신 |
| 공지 패널 | front/src/components/smallGroup/SmallGroupNoticesPanel.jsx | 고정·일반 목록·핀 버튼·조회 재시도·정렬·페이징 |
| 공지 폼·본문 | front/src/components/smallGroup/SmallGroupNoticeCreateForm.jsx, SmallGroupNoticeEditForm.jsx, SmallGroupNoticeDetail.jsx, SmallGroupNoticeDeleteButton.jsx | 작성·수정·본문·삭제 확인 |
| 상세 페이지 | front/src/pages/SmallGroupNoticeDetailPage.jsx, front/src/App.jsx | 상세 경로·작성자 버튼·수정·삭제·목록 복귀 |
| 프론트 API | front/src/api/smallGroups.js | 공지 CRUD·고정 요청·URL·요약 변환·페이지 메타 |
| 백엔드 | back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java, SmallGroupServiceImpl.java | 비즈니스·권한·행 잠금·페이지 보정 |
| Controller | back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java | 요청 검증·인증 회원 전달·공통 응답 |
| DTO | back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNotice*.java, SmallGroupDetailResponse.java | 입력·저장·상세·목록·고정 계약 |
| Mapper | back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java, back/src/main/resources/mappers/SmallGroupMapper.xml | 저장·조회·정렬·집계·수정·논리 삭제·고정 SQL |
| 테스트 | back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNotice*.java, SmallGroupSecurityIntegrationTest.java; front/src/api/smallGroupNotice*.test.js, smallGroups.test.js | API·권한·SQL·보안 필터·실제 JSX 검증 |
| 문서 | docs/small-group-public-detail-api.md, docs/ai-usage-log.md | 최종 계약·수동 검증·AI 작업 기록 |

## 검증 결과

- 최종 백엔드: 공지 48건 + 보안 필터·기존 Mapper 16건 = 64건 모두 통과.
- 프론트: API 59건 + 실제 JSX 렌더링 3건 = 62건 모두 통과.
- 프로덕션 빌드: 통과. 번들 크기 경고만 남음.
- 정적 검사: 이번 변경 오류 없음. 기존 상세의 set-state-in-effect 경고 2건은 유지.
- 확대 소모임 회귀: 350건 중 328건 통과, 후기 작성 8건 실패, DB 동시성 등 14건 건너뜀. 실패 8건은 변경 전 feature/smallgroup에서도 동일하게 재현하여 이번 변경과 분리했다.
- 전체 저장소 테스트: 기존 테스트의 누락 클래스 import 때문에 컴파일 불가. 저장소 설정은 변경하지 않고 임시 Gradle init 스크립트로 범위를 제한했다. 확대 실행에서 기존 참조 오류가 있는 장소 테스트 2개를 제외하고 상속하는 계모임 동시성 기반 클래스는 포함했다.
- 실제 MySQL CRUD·동시 요청·브라우저 클릭 검증은 미실시. 역할별 UI는 SSR로 실제 JSX를 렌더링해 확인했으며 브라우저 미리보기는 개발 환경 모듈 로딩 문제로 확인하지 못했다.

## 브라우저·Postman 수동 검증 순서

개발용 서버·테스트 DB에서 실제 계정·모임의 역할을 확인한 후 진행한다. DB나 회원 정보를 코드에 넣지 않는다. 아래 {smallGroupId}는 small_group ID이며 groups ID와 구분한다.

1. PC에서 /small-groups/{smallGroupId} 진입: 카드 배치·전체 보기 제거·빈 목록을 확인한다.
2. 모임장으로 POST /api/small-groups/{smallGroupId}/notices: title·content를 전송하고 공통 성공 응답을 확인한다. 빈 값·공백·100/101자 제목·5,000/5,001자 본문, 일반 회원 요청을 확인한다.
3. GET /api/small-groups/{smallGroupId}?noticePage=1&noticeSort=LATEST: meta.size=4, 일반 목록과 pinnedNotices 분리, 최신·오래된순, 페이지 URL 복원과 과도한 페이지 보정을 확인한다.
4. GET /api/small-groups/{smallGroupId}/notices/{noticeId}: 승인 모임원 성공, 비로그인 401·비회원 403, 타 모임·삭제 공지 404를 확인한다.
5. PUT 같은 상세 경로: 본인 공지 수정·취소, 작성자·작성일 보존, 일반 회원·다른 작성자 403을 확인한다.
6. PATCH 같은 상세 경로 + /pin: pinned=true·false, 2개까지 성공·세 번째 409, 동일 상태 재요청 성공, 일반 목록 중복 제외를 확인한다. 동시 지정은 별도 테스트 DB에서 검증한다.
7. DELETE 같은 상세 경로: 취소·성공·중복 클릭 방지, 삭제 후 상세 404, 마지막 페이지 보정과 고정 공지 삭제 후 고정 자리 확보를 확인한다.
8. 저장·고정 변경은 성공하고 재조회만 실패하는 경우 재등록·재변경 없이 조회만 재시도하는지 확인한다. 계정·모임·탭 전환 시 이전 응답·본문이 나타나지 않는지 확인한다.

## 세부 단계별 실제 커밋과 변경 파일


### 1단계: 메인 화면

**dcc8334 — style: 소모임 메인 공지와 장소 카드 배치 변경**

- `front/src/components/smallGroup/SmallGroupDetail.jsx`

**5c118f3 — docs: 소모임 메인 공지 배치 변경 검증 기록**

- `docs/ai-usage-log.md`


### 2단계: 공지 작성

**2622480 — feat: 소모임 공지 저장 DTO와 Mapper SQL 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeCreateRow.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**4558764 — feat: 소모임 공지 작성 서비스와 소모임장 권한 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeCreateServiceTest.java`

**6847982 — feat: 소모임 공지 작성 Controller와 입력 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeCreateRequest.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeCreationControllerTest.java`

**7408d61 — test: 소모임 공지 작성 요청 테스트를 기존 Controller 구조에 맞춤**

- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeCreationControllerTest.java`

**11dd443 — feat: 소모임 공지 작성 요청 API 추가**

- `front/src/api/smallGroups.js`

**149629b — feat: 소모임 공지 작성 폼과 메인 화면 연동 추가**

- `front/src/components/smallGroup/SmallGroupDetail.jsx`
- `front/src/components/smallGroup/SmallGroupNoticeCreateForm.jsx`
- `front/src/pages/SmallGroupDetailPage.jsx`

**b5b3dd9 — docs: 소모임 공지 작성 API와 검증 범위 기록**

- `docs/ai-usage-log.md`
- `docs/small-group-public-detail-api.md`


### 3단계: 목록·정렬·페이징

**52075d4 — feat: 소모임 공지 페이징 DTO와 Mapper SQL 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeListResponse.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeSearchCondition.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**86e0567 — feat: 소모임 공지 정렬과 페이징 서비스 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeListServiceTest.java`

**96f6671 — feat: 소모임 상세 API 공지 조회 조건과 페이지 메타 연결**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupDetailResponse.java`

**faed4a6 — feat: 소모임 상세 요청에 공지 조회 조건과 페이지 메타 추가**

- `front/src/api/smallGroups.js`

**0da9722 — feat: 소모임 공지 정렬과 세 페이지 묶음 목록 화면 구현**

- `front/src/components/smallGroup/SmallGroupDetail.jsx`
- `front/src/pages/SmallGroupDetailPage.jsx`

**98639cd — feat: 소모임 공지 작성 후 조회 상태 복원과 요청 충돌 방지**

- `front/src/pages/SmallGroupDetailPage.jsx`

**e6fb622 — docs: 소모임 공지 목록 페이징과 화면 정책 기록**

- `docs/ai-usage-log.md`
- `docs/small-group-public-detail-api.md`


### 4단계: 상세 조회

**cb6bd5e — feat: 소모임 공지 상세 DTO와 Mapper 조회 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeDetailResponse.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**f472941 — feat: 소모임 공지 상세 서비스와 승인 회원 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`

**613e472 — feat: 소모임 공지 상세 조회 Controller 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`

**075b45c — feat: 소모임 공지 상세 조회 요청 API 추가**

- `front/src/api/smallGroups.js`

**ee96a77 — feat: 소모임 공지 상세 화면과 목록 이동 연결**

- `front/src/App.jsx`
- `front/src/components/smallGroup/SmallGroupDetail.jsx`
- `front/src/components/smallGroup/SmallGroupNoticeDetail.jsx`
- `front/src/pages/SmallGroupDetailPage.jsx`
- `front/src/pages/SmallGroupNoticeDetailPage.jsx`

**55ac3a9 — fix: 소모임 공지 조회 조건 바인딩과 목록 복귀 상태 보완**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`
- `front/src/pages/SmallGroupNoticeDetailPage.jsx`

**7143adc — docs: 소모임 공지 상세 권한과 화면 명세 기록**

- `docs/ai-usage-log.md`
- `docs/small-group-public-detail-api.md`


### 5단계: 수정

**3bd81e6 — feat: 소모임 공지 수정 Mapper와 잠금 조회 SQL 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**6c2e2ed — feat: 소모임 공지 수정 서비스와 작성자 권한 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeEditServiceTest.java`

**82f10ad — feat: 소모임 공지 수정 Controller와 요청 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeEditRequest.java`

**efe7113 — feat: 소모임 공지 수정 요청 API 추가**

- `front/src/api/smallGroups.js`

**f9582d8 — feat: 소모임 공지 수정 폼 추가**

- `front/src/components/smallGroup/SmallGroupNoticeEditForm.jsx`

**042c3bc — feat: 소모임 공지 상세 화면에 수정 흐름 연결**

- `front/src/pages/SmallGroupNoticeDetailPage.jsx`

**e93b422 — docs: 소모임 공지 수정 API와 테스트 결과 기록**

- `docs/ai-usage-log.md`
- `docs/small-group-public-detail-api.md`


### 6단계: 논리 삭제

**5b4c77b — feat: 소모임 공지 논리 삭제 Mapper와 조회 제외 조건 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**d2853bc — feat: 소모임 공지 삭제 서비스와 작성자 권한 검증 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeDeleteServiceTest.java`

**7527c39 — feat: 소모임 공지 삭제 Controller 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`

**5ddacac — feat: 소모임 공지 삭제 요청 API 추가**

- `front/src/api/smallGroups.js`

**f546d49 — feat: 소모임 공지 삭제 확인과 목록 복귀 및 페이지 보정 연결**

- `front/src/components/smallGroup/SmallGroupNoticeDeleteButton.jsx`
- `front/src/pages/SmallGroupDetailPage.jsx`
- `front/src/pages/SmallGroupNoticeDetailPage.jsx`

**7dbc2ab — docs: 소모임 공지 삭제 API와 페이지 복귀 정책 기록**

- `docs/ai-usage-log.md`
- `docs/small-group-public-detail-api.md`


### 7단계: 고정·해제 및 통합 검증

**c4b71ac — feat: 소모임 고정 공지 DTO와 Mapper 및 일반 목록 분리 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticePinRequest.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticePinRow.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/mapper/SmallGroupMapper.java`
- `back/src/main/resources/mappers/SmallGroupMapper.xml`

**206d816 — feat: 소모임 공지 고정 권한과 최대 두 개 제한 서비스 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupService.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/SmallGroupServiceImpl.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupDetailResponse.java`
- `back/src/main/java/com/nightstudy/moa/smallGroup/dto/SmallGroupNoticeListResponse.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticePinServiceTest.java`

**754cefd — feat: 소모임 공지 고정 Controller와 입력 검증 테스트 추가**

- `back/src/main/java/com/nightstudy/moa/smallGroup/controller/SmallGroupController.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeActionsControllerTest.java`

**2b8d5b1 — feat: 소모임 공지 고정 요청과 고정 목록 응답 처리 추가**

- `front/src/api/smallGroups.js`

**e5297be — feat: 소모임 고정 공지 영역과 핀 지정 해제 화면 연결**

- `front/src/components/smallGroup/SmallGroupDetail.jsx`
- `front/src/components/smallGroup/SmallGroupNoticesPanel.jsx`
- `front/src/pages/SmallGroupDetailPage.jsx`

**8e7c60a — fix: 소모임 공지 재진입 시 목록 최신 상태 확인**

- `front/src/pages/SmallGroupDetailPage.jsx`

**be70a1e — test: 소모임 공지 권한과 SQL 및 화면 회귀 검증 추가**

- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeActionsControllerTest.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeDetailServiceTest.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupNoticeMapperTest.java`
- `back/src/test/java/com/nightstudy/moa/smallGroup/SmallGroupSecurityIntegrationTest.java`
- `front/src/api/smallGroupNoticeViews.test.js`
- `front/src/api/smallGroupNotices.test.js`
- `front/src/api/smallGroups.test.js`
