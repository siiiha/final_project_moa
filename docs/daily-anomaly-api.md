# MOA 일별 분석 — 로컬 읽기 전용 API

2026-10-26 작성함. 일별 IF 후보를 실제 호출 가능한 내부 API로 연결함. 기존 단건 API·모델·DB·인증·프론트는 교체하지 않음.

## 바로 실행

ml 폴더에서 아래 명령을 실행함. 설치된 .venv와 기존 고정 패키지를 사용하며 신규 패키지는 설치하지 않음.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-daily.ps1 -Port 8001
```

실행 정책 옵션은 해당 프로세스에만 적용함. Windows 사용자/시스템 정책을 변경하지 않음. 서버는127.0.0.1에만 바인딩하며 접근 로그를 끔. 종료는 해당 터미널에서 Ctrl+C를 누름.

- Swagger: http://127.0.0.1:8001/docs
- 일별 준비 상태: GET http://127.0.0.1:8001/v1/anomalies/daily-health
- 일별 분석: POST http://127.0.0.1:8001/v1/anomalies/daily-history

다른 터미널에서 다음 합성 예제를 호출함.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\demo-daily.ps1 -Case normal -Port 8001
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\demo-daily.ps1 -Case warning -Port 8001
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\demo-daily.ps1 -Case suppressed -Port 8001
```

normal은 정상 점수, warning은 일별 이상 및 주간 확인 후보, suppressed는 일별 점수는 높지만 해당 주에 앞선 후보가 있어 주간 후보만 false가 되는 사례임. 어떤 경우에도 실제 알림은 발송하지 않음.

## 계약·안전 범위

- 이 API는 Spring 내부 호출 또는 로컬 합성 시연용임. 사용자에게 직접 공개하지 않음. 사용자 인증/OWNER·계좌 소속 검증은 아직 Spring 일별 경로에 연결하지 않았음.
- 기본 비활성화이며 MOA_DAILY_ENABLED=true일 때만 분석함. 실행 스크립트는 자기 프로세스에만 활성화하고 종료 시 복구함.
- 입력은 단일 account_id, target_date, history_start/end, history_complete, transactions임. 거래의 account_id와 transaction_id는 문자열, 금액은 소수2자리 문자열, 시각은 offset 포함 초 단위임.
- history_end는 종료일의 다음날을 나타내는 배타적 날짜임. 오늘/미래 일자, 범위 밖 거래, 계좌 혼합, ID 중복, 임의 필드는422로 거부함.
- 완전한60일 이력이 필요함. 알림 주의 첫 마감일 일요일부터 재계산하므로 최대 약67일의 자료가 필요함. 최대90일·10000건·2MiB로 제한함. 예제 요청에서 정확한 형태를 확인할 수 있음.
- history_complete는 신뢰하는 수집/조회 어댑터의 보장값임. API 자체가 은행 누락을 확인해 주는 증명이 아님. 실제 연동 시 수집 완전성이 확인되지 않으면 false로 보내야 함.
- 불완전 이력·잔액 누락·잔액 불일치·동시각 순서 미상은 NOT_SCORED로 반환함. 정상 점수0으로 대체하지 않음. 완전한 범위에서 당일 거래가 없으면 NO_ACTIVITY로 반환함.
- 상태가 SCORED일 때만 anomaly_score/daily_anomaly를 해석함. 점수는 확률이 아님.
- 마감일 D의 결과는 D+1일00:00 KST부터 이용 가능함. ISO 알림 주는 실제 이용 가능 시점으로 계산함. 일요일 마감은 월요일 알림 주에 속함.
- weekly_review_candidate는 조회 스냅샷에 대한 시뮬레이션임. 영구 저장·발송 이력이 없어 실제 발송 주1회/동시성 중복 방지를 보장하지 않음. notification_enabled는 항상false임.
- 입력 순서가 달라도 같은 거래·버전·정책이면 fingerprint와 점수가 같음. 늦은 거래/정정/모델 변경이 있으면 달라질 수 있음.
- 모델·피처 해시와 Python/sklearn/numpy/joblib 버전을 확인함. 불일치 시503으로 닫히며 경로·거래 원문 오류는 응답하지 않음. 모델은 프로세스에서1회 로드함.
- 요청2MiB·거래10000건·동시 계산2개로 제한함. 바쁘면503을 반환함. Uvicorn 제한은 실행 스크립트에서8개로 둠. 운영 부하 인증을 의미하지 않음.

## 파일

- app/daily_features.py: 연구에서 검증한 동일 일별 피처 계산임.
- app/daily_schemas.py: 엄격한 거래/조회 범위 계약임.
- app/daily_inference.py: 모델 준비 검사·점수·품질·주간 후보 계산임.
- app/daily_history.py: 스트리밍 본문 상한·오류 비노출·스레드 계산을 적용한 신규 router임.
- artifacts/daily-v1/: 기존 단건 모델과 분리한 model.joblib/manifest.json임.
- tests/verify_daily.py, tests/fixtures/daily-*.json: 고정 점수 및 오류·주간 경계 회귀 검사임.

## 검증

```powershell
.\.venv\Scripts\python.exe -B -E -s tests\verify_daily.py
.\.venv\Scripts\python.exe -B -E -s tests\verify_service.py
.\.venv\Scripts\python.exe -B -E -s tests\verify_history.py
```

일별 정상/확인/주간 후보 억제 사례는 연구 예측과1e-12 이내로 일치해야 함. 일부 합성 거래 소수초는 DB DATETIME 계약에 맞춰 테스트 요청에서만 초 단위로 내렸으며 원래 연구 자료는 변경하지 않음.

현재 후보는 합성 데이터의 운영 효용을 검토한 모델임. 실제 금융 성능은 미검증이며 자동 출금 차단·회원 불이익·사기/연체 판정에 사용하지 않음.

다음 단계는 Spring 권한·계좌 소속·기간 조회·이력 완전성 검증을 붙이고 로컬 테스트 DB에서 확인하는 것임. 이후 모임장의 검토 결과를 수집하는 비공개 시험 운영을 별도로 검토해야 함.
