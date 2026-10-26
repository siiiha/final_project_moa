# AI작성: 일별 내부 API의 고정 예측·품질/범위·상한·주간 후보·기존 단건 독립성을 검사함.
import asyncio
from copy import deepcopy
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from app.main import app
from app import daily_history as route
from app.daily_inference import DailyEngine
from app.daily_schemas import DailyRequest,KST

async def request(method,path,payload=None,raw=None):
    raw=raw if raw is not None else json.dumps(payload).encode() if payload is not None else b''
    sent=False; messages=[]
    async def receive():
        nonlocal sent
        if sent: return {'type':'http.disconnect'}
        sent=True
        return {'type':'http.request','body':raw,'more_body':False}
    async def send(message): messages.append(message)
    await app({'type':'http','asgi':{'version':'3.0'},'http_version':'1.1','method':method,
               'scheme':'http','path':path,'raw_path':path.encode(),'query_string':b'','root_path':'',
               'headers':[(b'content-type',b'application/json')],'server':('127.0.0.1',8001),'client':('127.0.0.1',12345)},receive,send)
    start=next(m for m in messages if m['type']=='http.response.start')
    assert (b'cache-control',b'no-store') in start['headers']
    body=b''.join(m.get('body',b'') for m in messages if m['type']=='http.response.body')
    return start['status'],json.loads(body)

async def main():
    base=Path(__file__).resolve().parent
    cases=[json.loads((base/'fixtures'/name).read_text(encoding='utf-8')) for name in ['daily-normal.json','daily-warning.json','daily-suppressed.json']]
    root=base.parent; before=hashlib.sha256((root/'artifacts/model.joblib').read_bytes()).hexdigest()
    old_enabled=os.environ.get('MOA_DAILY_ENABLED'); old_now=route.now
    checks=[]
    async def call(body): return await request('POST','/v1/anomalies/daily-history',body)
    def passed(name): checks.append(name)
    try:
        os.environ['MOA_DAILY_ENABLED']='false'
        status,body=await call(cases[0]['request']); assert status==503 and body['error_code']=='DAILY_DISABLED'; passed('disabled_fail_closed')
        os.environ['MOA_DAILY_ENABLED']='true'; route.now=lambda:datetime(2026,11,15,tzinfo=KST)
        assert route.engine.ready,route.engine.error_code
        status,body=await request('GET','/v1/anomalies/daily-health'); assert status==200 and body['ready'] and not body['notification_enabled']; passed('daily_readiness')
        for i,case in enumerate(cases):
            status,body=await call(case['request']); assert status==200 and body['status']=='SCORED',body
            assert abs(body['anomaly_score']-case['expected']['anomaly_score'])<1e-12
            for key in ('daily_anomaly','candidate_date','weekly_review_candidate'): assert body[key]==case['expected'][key],(key,body)
            assert body['notification_week']=='2026-W36' and body['policy_mode']=='READ_ONLY_SIMULATION'
            assert body['notification_enabled'] is False; passed('golden_parity_'+str(i))
        status,a=await call(cases[1]['request'])
        changed=deepcopy(cases[1]['request']); changed['transactions'].reverse()
        status,b=await call(changed); assert a['input_fingerprint']==b['input_fingerprint'] and a['anomaly_score']==b['anomaly_score']; passed('order_invariant_fingerprint')
        # AI작성: 같은 순간의 UTC/KST 표기는 같은 값·지문이 되어야 함.
        changed=deepcopy(cases[0]['request'])
        from datetime import timezone
        for row in changed['transactions']: row['transacted_at']=datetime.fromisoformat(row['transacted_at']).astimezone(timezone.utc).isoformat()
        _,b=await call(changed); _,a=await call(cases[0]['request']); assert a['input_fingerprint']==b['input_fingerprint']; passed('timezone_equivalence')
        for field,value,flag in [('history_complete',False,'history_coverage_unverified'),('history_start','2026-09-01','insufficient_history')]:
            changed=deepcopy(cases[1]['request']); changed[field]=value
            if field=='history_start': changed['transactions']=[t for t in changed['transactions'] if t['transacted_at'][:10]>=value]
            status,b=await call(changed); assert status==200 and b['status']=='NOT_SCORED' and flag in b['quality_flags'] and b['daily_anomaly'] is None
            passed(flag)
        for name,mutate,flag in [
            ('missing_balance',lambda q:q['transactions'][0].update(balance_after=None),'bank_balance_missing'),
            ('bad_ledger',lambda q:q['transactions'][0].update(balance_after='1.00'),'ledger_balance_mismatch'),
            ('timestamp_tie',lambda q:q['transactions'][1].update(transacted_at=q['transactions'][0]['transacted_at']),'same_timestamp_order_unknown')]:
            changed=deepcopy(cases[1]['request']); mutate(changed)
            status,b=await call(changed); assert status==200 and b['status']=='NOT_SCORED' and flag in b['quality_flags']; passed(name)
        changed=deepcopy(cases[0]['request']); changed['transactions']=[]
        status,b=await call(changed); assert status==200 and b['status']=='NO_ACTIVITY' and b['anomaly_score'] is None; passed('confirmed_no_activity')
        invalids={
            'mixed_account':lambda q:q['transactions'][0].update(account_id='other'),
            'duplicate_id':lambda q:q['transactions'].append(deepcopy(q['transactions'][0])),
            'bad_money':lambda q:q['transactions'][0].update(amount='0.00'),
            'float_money':lambda q:q['transactions'][0].update(amount=100.00),
            'no_offset':lambda q:q['transactions'][0].update(transacted_at='2026-08-01T12:00:00'),
            'subsecond':lambda q:q['transactions'][0].update(transacted_at=q['transactions'][0]['transacted_at'].replace('+09:00','.123+09:00')),
            'unknown_private_field':lambda q:q.update(account_number='SENSITIVE_DO_NOT_ECHO'),
            'future_row':lambda q:q['transactions'][0].update(transacted_at=q['history_end']+'T01:00:00+09:00'),
            'invalid_date':lambda q:q.update(target_date='2026-02-30'),
            'wrong_end':lambda q:q.update(history_end='2026-12-01'),
            'bool_coverage':lambda q:q.update(history_complete='true')}
        for name,mutate in invalids.items():
            changed=deepcopy(cases[0]['request']); mutate(changed)
            status,b=await call(changed); assert status==422 and b=={'error_code':'DAILY_INVALID_INPUT'}; passed(name)
        for name,raw in [('duplicate_json_key',b'{"account_id":"a","account_id":"b"}'),('nonfinite_json',b'{"amount":NaN}')]:
            status,b=await request('POST','/v1/anomalies/daily-history',raw=raw); assert status==422; passed(name)
        status,b=await request('POST','/v1/anomalies/daily-history',raw=b' '*(route.MAX_BODY_BYTES+1)); assert status==413; passed('body_limit')
        from pydantic import ValidationError
        changed=deepcopy(cases[0]['request']); changed['transactions']=[changed['transactions'][0]]*10001
        try: DailyRequest.model_validate(changed); raise AssertionError('row limit not enforced')
        except ValidationError: passed('row_limit')
        route.now=lambda:datetime.fromisoformat(cases[0]['request']['target_date']+'T23:59:59+09:00')
        status,b=await call(cases[0]['request']); assert status==422 and b['error_code']=='DAILY_NOT_CLOSED'; passed('not_closed')
        route.now=lambda:datetime(2026,11,15,tzinfo=KST)
        # AI작성: 일요일 마감은 월요일 알림 주로 리셋되고, 다음 월요일 마감은 같은 알림 주로 유지되어야 함.
        from decimal import Decimal
        import numpy as np
        engine=DailyEngine()
        engine.model=type('AlwaysHigh',(),{'score_samples':lambda self,x:np.full(len(x),-.9)})()
        boundary=deepcopy(cases[1]['request'])
        boundary.update(target_date='2026-09-06',history_start='2026-07-08',history_end='2026-09-07')
        boundary['transactions']=[t for t in boundary['transactions'] if t['transacted_at'][:10]>='2026-07-08']
        balance=Decimal(sorted(boundary['transactions'],key=lambda t:t['transacted_at'])[-1]['balance_after'])
        boundary['transactions'].append({'account_id':boundary['account_id'],'transaction_id':'boundary-sunday',
              'transacted_at':'2026-09-06T12:00:00+09:00','transaction_type':'WITHDRAWAL','amount':'100.00','balance_after':str(balance-100)})
        sunday=engine.analyze(DailyRequest.model_validate(boundary),route.now())
        assert sunday['notification_week']=='2026-W37' and sunday['candidate_date']=='2026-09-06' and sunday['weekly_review_candidate']; passed('sunday_notification_week_reset')
        boundary.update(target_date='2026-09-07',history_end='2026-09-08')
        boundary['transactions'].append({'account_id':boundary['account_id'],'transaction_id':'boundary-monday',
              'transacted_at':'2026-09-07T12:00:00+09:00','transaction_type':'WITHDRAWAL','amount':'100.00','balance_after':str(balance-200)})
        monday=engine.analyze(DailyRequest.model_validate(boundary),route.now())
        assert monday['notification_week']=='2026-W37' and monday['candidate_date']=='2026-09-06' and not monday['weekly_review_candidate']; passed('monday_stays_same_notification_week')
        route.slots.acquire(); route.slots.acquire()
        try:
            status,b=await call(cases[0]['request']); assert status==503 and b['error_code']=='DAILY_BUSY'; passed('concurrency_limit')
        finally: route.slots.release(); route.slots.release()
        for error in ('DAILY_RUNTIME_MISMATCH','DAILY_MODEL_HASH_MISMATCH','DAILY_FEATURE_HASH_MISMATCH'):
            meta=json.loads((root/'artifacts/daily-v1/manifest.json').read_text(encoding='utf-8'))
            if error=='DAILY_RUNTIME_MISMATCH': meta['versions']['python']='0.0.0'
            elif error=='DAILY_MODEL_HASH_MISMATCH': meta['model_sha256']='0'*64
            else: meta['feature_source_sha256']='0'*64
            with TemporaryDirectory(prefix='moa-daily-test-') as tmp:
                p=Path(tmp); (p/'manifest.json').write_text(json.dumps(meta),encoding='utf-8')
                # AI작성: 모델 해시 검사까지 가기 위한 임시 테스트 전용 복사임. 프로젝트 모델은 수정하지 않음.
                (p/'model.joblib').write_bytes((root/'artifacts/daily-v1/model.joblib').read_bytes())
                engine=DailyEngine(p); assert not engine.ready and engine.error_code==error,(engine.error_code,error)
                passed(error)
        health=route.engine.ready
        try:
            route.engine.ready=False
            status,b=await call(cases[0]['request']); assert status==503; passed('model_unavailable')
        finally: route.engine.ready=health
        assert hashlib.sha256((root/'artifacts/model.joblib').read_bytes()).hexdigest()==before; passed('legacy_model_unchanged')
        print(json.dumps({'passed':True,'checks':len(checks),'names':checks},ensure_ascii=False))
    finally:
        route.now=old_now
        if old_enabled is None: os.environ.pop('MOA_DAILY_ENABLED',None)
        else: os.environ['MOA_DAILY_ENABLED']=old_enabled

if __name__=='__main__': asyncio.run(main())
