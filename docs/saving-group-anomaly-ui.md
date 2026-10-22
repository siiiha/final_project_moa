<!-- AI작성: 승인된 7단계 화면의 저장 모델 결과 출처와 실제 연동 경계를 기록한다. -->
# 계모임 거래 이상 감지 화면 — 저장 모델 결과 재생

## 실행과 데이터
- front에서 npm run dev → /saving-groups/12 → 거래 내역.
- 합성 계좌 10013, 2026-09-01~09-30 거래 14건. 실제 stage4 Isolation Forest 결과에서 경고 4건/경고 없음 10건.
- 입력 출처: deliverables/moa-anomaly-stage4/predictions/test.jsonl
- 원본 전체 파일 SHA-256: 290ccfc169a651e12a8529d7ad3babd1bb4b49d954ef12fa8590a94c02966d92
- 모델: sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d
- 경고: score > 0.6180405340153708. 저장된 점수·기준·경고 여부를 변경하지 않음.
- 데이터는 기존 mocks/savingGroupAnomalies.js에 스냅샷으로 보관. 브라우저에서 Python을 실행하지 않음. getMockSavingGroupAnomalies는 450ms로 읽는 테스트 전송 계층이며 실제 서버 API가 아님.
- group12는 UI 시연 위치. 합성 account10013과 운영 group12의 실제 관계를 주장하지 않음. 다른 모임에는 복제하지 않음.
- 실제 거래 설명·시나리오명은 표시하지 않고 합성 계좌 입금/출금으로 대체. 금액·잔액·점수·경고·cold-start·quality는 원본을 유지.
- source ID는 행 매칭용이며 모델 입력이 아님. 평가 정답 라벨 파일은 읽지 않음.

## 상태와 표시
- 모두 완료된 저장 결과이므로 임의 PENDING/FAILED 행을 혼합하지 않음. 컴포넌트의 미분석·실패 방어 처리는 후속 서버 연결을 위해 유지.
- 실제 분석 시각과 DB revision은 원본에 없으므로 null로 두고 미기록으로 표시.
- requiresReview는 stage5와 동일하게 cold-start 또는 quality flag 존재 여부로 유도. isAnomaly와 별개. 실제 사고 정답이 아님.
- 금액 합계는 적용한 날짜/입출금 조건 기준이고 분석 상태 필터와 무관. 잔액은 전체 재생 목록 마지막 거래 기준.
- 빈 목록·조회 오류·권한 없음은 별도 조회 상태 시연. 저장 거래를 가짜 실패로 바꾸는 기능이 아님.
- 모델 점수는 확률이 아니며 실제 사고·안전 능력을 이 화면만으로 검증하지 않음.

## 수동 확인
1. group12 거래 메뉴: 로딩 후 14건, 2페이지.
2. 모델 경고 필터: 4건. 경고 없음: 10건.
3. 상세 원 점수/기준/모델 해시 확인. 표시 소수 6자리 반올림이며 판정은 원 정밀도 기준.
4. 이력·품질 확인은 모델 경고 없음과 동시에 나타날 수 있음.
5. 날짜 역전 오류, 빈 기간, Mock 조회 오류/403/빈 상태 확인.
6. 모바일 목록/상세 dialog Escape·닫기, 다른 모임에 데이터 노출 없음 확인.

## 실제 연결 전 남은 작업
현재 화면은 은행·인증·실제 MOA 거래 조회 서버와 연결되지 않음. 실제 목록 함수는 Mock과 별도로 작성하고 실패 시 자동 Mock 대체하지 않음.
stage6 계약에 맞춰 savingGroupId/groupId/accountId 매핑, 모임장·승인 운영진 서버 인가, 최신 revision 결과, 서버 필터·페이지·합계를 확정해야 함.
서버 snake_case는 API 경계에서 camelCase로 변환(status, revision_no, anomaly_score, threshold, is_anomaly, requires_review, cold_start, quality_flags, model_version, feature_schema_version, analyzed_at).
금융정보 조회·출금·재분석 요청·확인 완료 저장·다운로드·모델 재학습은 이번 변경 범위가 아님.
