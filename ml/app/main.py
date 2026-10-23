# AI작성: 기존 추론 API 계약을 변경하지 않고 프로젝트로 통합한다.
from __future__ import annotations

import json
import math
from datetime import datetime
from typing import Any

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .inference import PreparationError, SCHEMA_VERSION, SCORE_DEFINITION, scorer

MAX_BODY_BYTES = 64 * 1024
ALLOWED_QUALITY_FLAGS = {
    "bank_balance_missing", "source_sequence_missing", "same_timestamp_order_unknown",
    "ledger_balance_mismatch", "bank_balance_arithmetic_mismatch", "same_direction_history_under_5",
    "balance_before_nonpositive", "withdrawal_balance_unavailable", "withdrawal_exceeds_balance_before",
}

app = FastAPI(title="MOA anomaly inference", version="stage5-local-inference-v1")

# AI작성: DB 비의존 내부 이력 분석 API를 기존 피처 입력 API와 별도로 제공한다.
from .history import router as history_router
app.include_router(history_router)


def _json_no_constants(raw: bytes) -> Any:
    def reject(value: str) -> None:
        raise ValueError(f"JSON 비유한 숫자 {value}는 허용되지 않습니다.")
    return json.loads(raw, parse_constant=reject)


def _validate(body: Any) -> dict[str, Any]:
    if not isinstance(body, dict):
        raise ValueError("요청 본문은 JSON 객체여야 합니다.")
    for key in ("account_id", "transacted_at", "transaction_type", "features", "context"):
        if key not in body:
            raise ValueError(f"필수 필드가 없습니다: {key}")
    if not isinstance(body["account_id"], (str, int)) or isinstance(body["account_id"], bool) or not str(body["account_id"]).strip():
        raise ValueError("account_id는 비어 있지 않은 문자열 또는 정수여야 합니다.")
    if not ("source_transaction_id" in body) ^ ("transaction_id" in body):
        raise ValueError("source_transaction_id 또는 transaction_id 중 정확히 하나가 필요합니다.")
    tx_key = "source_transaction_id" if "source_transaction_id" in body else "transaction_id"
    if not isinstance(body[tx_key], (str, int)) or isinstance(body[tx_key], bool) or not str(body[tx_key]).strip():
        raise ValueError(f"{tx_key}는 비어 있지 않은 문자열 또는 정수여야 합니다.")
    if not isinstance(body["transacted_at"], str):
        raise ValueError("transacted_at은 offset이 포함된 ISO 8601 문자열이어야 합니다.")
    try:
        stamp = datetime.fromisoformat(body["transacted_at"].replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError("transacted_at이 유효한 ISO 8601 시각이 아닙니다.") from exc
    if stamp.tzinfo is None or stamp.utcoffset() is None:
        raise ValueError("transacted_at에는 UTC offset이 필요합니다.")
    if body["transaction_type"] not in ("DEPOSIT", "WITHDRAWAL"):
        raise ValueError("transaction_type은 DEPOSIT 또는 WITHDRAWAL이어야 합니다.")
    features = body["features"]
    if not isinstance(features, dict):
        raise ValueError("features는 객체여야 합니다.")
    names = scorer.metadata["feature_allowlist"] if scorer.metadata else []
    missing = [name for name in names if name not in features]
    unknown = sorted(set(features) - set(names))
    if missing:
        raise ValueError("feature key 누락: " + ", ".join(missing))
    if unknown:
        raise ValueError("허용하지 않는 feature key: " + ", ".join(unknown))
    for name, value in features.items():
        if value is None:
            continue
        if isinstance(value, bool) or not isinstance(value, (str, int, float)):
            raise ValueError(f"features.{name}은 숫자, 숫자 문자열 또는 null이어야 합니다.")
        try:
            number = float(value)
        except (TypeError, ValueError, OverflowError) as exc:
            raise ValueError(f"features.{name} 숫자로 변환할 수 없습니다.") from exc
        if not math.isfinite(number):
            raise ValueError(f"features.{name}은 유한한 숫자여야 합니다.")
    context = body["context"]
    if not isinstance(context, dict) or "quality_flags" not in context:
        raise ValueError("context.quality_flags가 필요합니다. 잔액 비율 마스킹을 위해 생략할 수 없습니다.")
    flags = context["quality_flags"]
    if not isinstance(flags, list) or any(not isinstance(f, str) for f in flags):
        raise ValueError("context.quality_flags는 문자열 배열이어야 합니다.")
    invalid_flags = sorted(set(flags) - ALLOWED_QUALITY_FLAGS)
    if invalid_flags:
        raise ValueError("알 수 없는 quality flag: " + ", ".join(invalid_flags))
    cold_start = context.get("cold_start")
    if cold_start is not None and not isinstance(cold_start, bool):
        raise ValueError("context.cold_start는 boolean 또는 null이어야 합니다.")
    return {"tx_key": tx_key, "features": features, "flags": flags, "cold_start": cold_start}


@app.get("/health")
async def health() -> dict[str, Any]:
    return {
        "ready": scorer.ready,
        "model_version": scorer.manifest.get("model_version") if scorer.manifest else None,
        "feature_schema_version": SCHEMA_VERSION,
        "error": scorer.error,
    }


@app.post("/v1/anomalies/score")
async def score(request: Request) -> JSONResponse:
    raw = await request.body()
    if len(raw) > MAX_BODY_BYTES:
        return JSONResponse(status_code=413, content={"detail": f"요청 본문은 {MAX_BODY_BYTES} bytes 이하여야 합니다."})
    if not scorer.ready:
        return JSONResponse(status_code=503, content={"detail": "모델 추론 서비스가 준비되지 않았습니다.", "error": scorer.error})
    try:
        body = _json_no_constants(raw)
        validated = _validate(body)
        anomaly_score = scorer.score(validated["features"], validated["flags"])
    except (json.JSONDecodeError, ValueError) as exc:
        return JSONResponse(status_code=422, content={"detail": str(exc)})
    except PreparationError as exc:
        return JSONResponse(status_code=503, content={"detail": str(exc)})
    threshold = scorer.threshold
    context = body["context"]
    return JSONResponse(content={
        "account_id": body["account_id"],
        validated["tx_key"]: body[validated["tx_key"]],
        "transacted_at": body["transacted_at"],
        "transaction_type": body["transaction_type"],
        "anomaly_score": anomaly_score,
        "threshold": threshold,
        "is_anomaly": anomaly_score > threshold,
        "score_definition": SCORE_DEFINITION,
        "model_version": scorer.manifest["model_version"],
        "feature_schema_version": SCHEMA_VERSION,
        "cold_start": validated["cold_start"],
        "quality_flags": validated["flags"],
        "requires_review": bool(validated["cold_start"] or validated["flags"]),
        "review_definition": "cold_start 또는 quality_flags가 있으면 사람 검토 문맥으로 표시; 이 값은 ML 이상 경고를 뜻하지 않음",
    })
