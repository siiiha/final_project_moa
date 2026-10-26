# AI작성: 내부 일별 분석 시연만 loopback에서 활성화함. DB·은행·알림에 접근하지 않음.
param([ValidateRange(1,65535)][int]$Port=8001)
$ErrorActionPreference='Stop'
$dailyPython=Join-Path $PSScriptRoot '.venv/Scripts/python.exe'
if (-not (Test-Path -LiteralPath $dailyPython)) { throw 'ml/.venv 환경을 먼저 준비해야 합니다.' }
$savedDailySetting=[Environment]::GetEnvironmentVariable('MOA_DAILY_ENABLED','Process')
Push-Location $PSScriptRoot
try {
    $env:MOA_DAILY_ENABLED='true'
    & $dailyPython -B -E -s -c "from app.daily_history import engine; import sys; print(engine.health()); sys.exit(0 if engine.ready else 1)"
    if ($LASTEXITCODE -ne 0) { throw '일별 모델 버전/해시 준비 검사를 통과하지 못했습니다.' }
    & $dailyPython -B -E -s -m uvicorn app.main:app --host 127.0.0.1 --port $Port --no-access-log --limit-concurrency 8
    if ($LASTEXITCODE -ne 0) { throw '일별 시연 서버가 오류로 종료되었습니다.' }
}
finally {
    [Environment]::SetEnvironmentVariable('MOA_DAILY_ENABLED',$savedDailySetting,'Process')
    Pop-Location
}
