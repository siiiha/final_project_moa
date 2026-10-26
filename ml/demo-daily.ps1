# AI작성: 합성 회귀 자료만 내부 API에 보내 정상/확인 후보/주간 후보 억제를 시연함.
param([ValidateSet('normal','warning','suppressed')][string]$Case='warning',
      [ValidateRange(1,65535)][int]$Port=8001)
$ErrorActionPreference='Stop'
$dailyFixturePath=Join-Path $PSScriptRoot "tests/fixtures/daily-$Case.json"
$dailyFixture=Get-Content -LiteralPath $dailyFixturePath -Raw -Encoding UTF8 | ConvertFrom-Json
$dailyBody=$dailyFixture.request | ConvertTo-Json -Depth 12
$dailyResult=Invoke-RestMethod -Uri "http://127.0.0.1:$Port/v1/anomalies/daily-history" -Method Post -ContentType 'application/json' -Body $dailyBody -TimeoutSec 15
$dailyResult | Select-Object status,anomaly_score,daily_anomaly,weekly_review_candidate,candidate_date,notification_week,notification_enabled,quality_flags | ConvertTo-Json -Depth 5
