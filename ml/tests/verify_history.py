# AI작성: 기존 모델 점수, 미래 거래 차단, 입력 경계, 서비스 미준비를 검증한다.
import asyncio
import copy
import json
from verify_service import request
from app.history import prepare
from app.inference import scorer

def tx(key, time, value='50000.00'):
    return {'transaction_id':key,'account_id':'10013','transaction_type':'WITHDRAWAL',
            'amount':value,'transacted_at':time,'balance_after':'1000000.00','source_sequence':int(key)}

async def main():
    body = {'account_id':'10013','target_transaction_id':'2','transactions':[
        tx('1','2026-09-18T20:00:00+09:00'), tx('2','2026-09-18T20:08:00+09:00')]}
    status, result = await request('POST','/v1/anomalies/score-history',body)
    assert status == 200, result
    row = prepare(body)
    assert result['anomaly_score'] == scorer.score(row['features'], row['context']['quality_flags'])
    future = copy.deepcopy(body)
    future['transactions'].append(tx('3','2026-09-19T20:00:00+09:00'))
    assert (await request('POST','/v1/anomalies/score-history',future))[1] == result
    invalid = []
    item = copy.deepcopy(body); item['transactions'][0]['account_id']='other'; invalid.append(item)
    item = copy.deepcopy(body); item['transactions'].append(item['transactions'][0]); invalid.append(item)
    item = copy.deepcopy(body); item['target_transaction_id']='missing'; invalid.append(item)
    for value in ['NaN','Infinity','-1','0','1.001',True]:
        item = copy.deepcopy(body); item['transactions'][0]['amount']=value; invalid.append(item)
    item = copy.deepcopy(body); item['transactions'][0]['transacted_at']='2026-09-18T20:00:00'; invalid.append(item)
    item = copy.deepcopy(body); item['transactions'][0]['source_sequence']=True; invalid.append(item)
    for item in invalid:
        assert (await request('POST','/v1/anomalies/score-history',item))[0] == 422
    oversized = copy.deepcopy(body); oversized['transactions'] *= 1001
    assert (await request('POST','/v1/anomalies/score-history',oversized))[0] == 422
    assert (await request('POST','/v1/anomalies/score-history',{'padding':'x'*1048576}))[0] == 413
    ready = scorer.ready
    try:
        scorer.ready = False
        assert (await request('POST','/v1/anomalies/score-history',body))[0] == 503
    finally:
        scorer.ready = ready
    print(json.dumps({'passed':True,'invalid_cases':len(invalid),'future_excluded':True}))

if __name__ == '__main__':
    asyncio.run(main())
