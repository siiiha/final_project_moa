# AI작성: 내부 읽기 전용 일별 API. 요청 크기·동시 계산 수를 제한하고 계산을 이벤트 루프 밖에서 실행함.
from datetime import datetime
import json
import os
from threading import BoundedSemaphore
from fastapi import APIRouter,Request
from fastapi.responses import JSONResponse
from pydantic import ValidationError
from starlette.concurrency import run_in_threadpool
from .daily_inference import DailyEngine,DailyFailure
from .daily_schemas import DailyRequest,KST

router=APIRouter()
engine=DailyEngine()
MAX_BODY_BYTES=2*1024*1024
slots=BoundedSemaphore(2)
# AI작성: 스트리밍 크기 검사를 유지하면서 /docs에도 요청 구조를 표시함. 정의 참조는 이 API 내부에서 펼침.
REQUEST_SCHEMA=DailyRequest.model_json_schema()
definitions=REQUEST_SCHEMA.pop('$defs',{})
REQUEST_SCHEMA['properties']['transactions']['items']=definitions['Transaction']

def now(): return datetime.now(KST)
def enabled(): return os.environ.get('MOA_DAILY_ENABLED','false').lower()=='true'

def response(status,body):
    return JSONResponse(status_code=status,content=body,headers={'Cache-Control':'no-store'})

def reject_constant(value): raise ValueError('invalid number')
def unique_object(pairs):
    value={}
    for key,item in pairs:
        if key in value: raise ValueError('duplicate key')
        value[key]=item
    return value

def calculate(request):
    if not slots.acquire(blocking=False): raise DailyFailure('DAILY_BUSY')
    try: return engine.analyze(request,now())
    finally: slots.release()

@router.get('/v1/anomalies/daily-health')
def daily_health():
    body=engine.health(); body['enabled']=enabled()
    return response(200 if enabled() and engine.ready else 503,body)

@router.post('/v1/anomalies/daily-history',openapi_extra={'requestBody':{'required':True,'content':{'application/json':{
    'schema':REQUEST_SCHEMA,'example':{'account_id':'demo','target_date':'2026-10-25','history_start':'2026-08-20',
    'history_end':'2026-10-26','history_complete':False,'transactions':[]}}}}})
async def daily_history(request:Request):
    if not enabled(): return response(503,{'error_code':'DAILY_DISABLED'})
    if not engine.ready: return response(503,{'error_code':engine.error_code})
    raw=bytearray()
    async for chunk in request.stream():
        if len(raw)+len(chunk)>MAX_BODY_BYTES: return response(413,{'error_code':'DAILY_BODY_TOO_LARGE'})
        raw.extend(chunk)
    try:
        body=json.loads(raw,parse_constant=reject_constant,object_pairs_hook=unique_object)
        data=DailyRequest.model_validate(body)
    except (ValueError,ValidationError,UnicodeDecodeError,RecursionError):
        return response(422,{'error_code':'DAILY_INVALID_INPUT'})
    try:
        result=await run_in_threadpool(calculate,data)
        return response(200,result)
    except DailyFailure as exc: return response(exc.status,{'error_code':exc.code})
    except Exception: return response(503,{'error_code':'DAILY_CALCULATION_FAILED'})
