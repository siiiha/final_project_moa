# AI작성: 신뢰된 내부 서버용 거래 이력 분석. 저장·은행 호출·권한 대행은 수행하지 않는다.
from datetime import datetime
from decimal import Decimal, InvalidOperation
import math
import json
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from .features import build_features, parse_time
from .inference import scorer, SCORE_DEFINITION, SCHEMA_VERSION

router = APIRouter()
MAX_HISTORY_BYTES = 1024 * 1024
MAX_TRANSACTIONS = 2000

def identifier(value):
    if isinstance(value, bool) or not isinstance(value, (str, int)) or not str(value).strip():
        raise ValueError('거래·계좌 ID가 유효하지 않습니다.')
    return str(value)

def amount(value, positive=False):
    if isinstance(value, bool) or not isinstance(value, (str, int, float)):
        raise ValueError('금액은 유한한 숫자 또는 숫자 문자열이어야 합니다.')
    try:
        number = Decimal(str(value))
        if not number.is_finite() or not math.isfinite(float(number)) or abs(number) > Decimal('9999999999999.99'):
            raise ValueError('금액 범위를 초과했습니다.')
        if number.as_tuple().exponent < -2 or (positive and number <= 0):
            raise ValueError('금액은 소수 둘째 자리까지이며 거래금액은 양수여야 합니다.')
    except InvalidOperation:
        raise ValueError('유효하지 않은 금액입니다.') from None

def prepare(body):
    if not isinstance(body, dict) or set(body) != {'account_id','target_transaction_id','transactions'}:
        raise ValueError('account_id, target_transaction_id, transactions만 필요합니다.')
    account = identifier(body['account_id'])
    target = identifier(body['target_transaction_id'])
    txs = body['transactions']
    if not isinstance(txs, list) or not 1 <= len(txs) <= MAX_TRANSACTIONS:
        raise ValueError('거래 이력은 1~2000건이어야 합니다. 잘라 보내지 말고 한도 초과는 별도 처리해야 합니다.')
    seen = set()
    stamps = {}
    for tx in txs:
        if not isinstance(tx, dict) or set(tx) - {'transaction_id','account_id','transaction_type','amount','transacted_at','balance_after','source_sequence'}:
            raise ValueError('허용하지 않는 거래 필드가 있습니다.')
        if identifier(tx.get('account_id')) != account:
            raise ValueError('서로 다른 계좌의 이력을 섞을 수 없습니다.')
        key = identifier(tx.get('transaction_id'))
        if key in seen:
            raise ValueError('거래 ID가 중복되었습니다.')
        seen.add(key)
        if tx.get('transaction_type') not in ('DEPOSIT','WITHDRAWAL'):
            raise ValueError('거래 종류가 유효하지 않습니다.')
        amount(tx.get('amount'), positive=True)
        if tx.get('balance_after') is not None:
            amount(tx['balance_after'])
        seq = tx.get('source_sequence')
        if seq is not None and (isinstance(seq,bool) or not isinstance(seq,int) or seq < 0):
            raise ValueError('source_sequence는 0 이상의 정수 또는 null이어야 합니다.')
        if not isinstance(tx.get('transacted_at'), str):
            raise ValueError('거래시각은 offset 포함 ISO 문자열이어야 합니다.')
        stamps[key] = parse_time(tx['transacted_at'])
    if target not in seen:
        raise ValueError('분석 대상 거래가 이력에 없습니다.')
    # AI작성: 대상 이후 시각 거래는 피처 계산에서 제외한다. 동일 시각은 기존 순번·불명확성 정책을 유지한다.
    history = []
    for tx in txs:
        if stamps[str(tx['transaction_id'])] <= stamps[target]:
            history.append({**tx, 'source_transaction_id':str(tx['transaction_id'])})
    rows = build_features(history)
    return next(r for r in rows if r['source_transaction_id'] == target)

@router.post('/v1/anomalies/score-history')
async def score_history(request: Request):
    if not scorer.ready:
        return JSONResponse(status_code=503, content={'detail':'모델이 준비되지 않았습니다.'})
    raw = bytearray()
    async for chunk in request.stream():
        raw.extend(chunk)
        if len(raw) > MAX_HISTORY_BYTES:
            return JSONResponse(status_code=413, content={'detail':'요청은 1MiB 이하여야 합니다.'})
    try:
        row = prepare(json.loads(raw))
    except (ValueError, TypeError, OverflowError):
        # AI작성: 입력 원문·금액·계좌를 오류 응답이나 로그에 노출하지 않는다.
        return JSONResponse(status_code=422, content={'detail':'거래 이력 입력 형식 또는 값이 올바르지 않습니다.'})
    try:
        score = scorer.score(row['features'], row['context']['quality_flags'])
    except Exception:
        return JSONResponse(status_code=503, content={'detail':'거래 분석에 실패했습니다.'})
    context = row['context']
    return {'account_id':row['account_id'], 'transaction_id':row['source_transaction_id'],
            'transacted_at':row['transacted_at'], 'transaction_type':row['transaction_type'],
            'anomaly_score':score, 'threshold':scorer.threshold, 'is_anomaly':score > scorer.threshold,
            'score_definition':SCORE_DEFINITION, 'model_version':scorer.manifest['model_version'],
            'feature_schema_version':SCHEMA_VERSION, 'cold_start':context['cold_start'],
            'quality_flags':context['quality_flags'],
            'requires_review':bool(context['cold_start'] or context['quality_flags'])}
