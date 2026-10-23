# AI작성: 네트워크 설치 없이 실제 ASGI API와 합성 계좌 10013의 14건 점수 동등성을 검증한다.
import asyncio
import json
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.main import app
from app.inference import scorer

async def request(method, route, payload=None):
    messages = []
    raw = json.dumps(payload).encode() if payload is not None else b''
    async def receive():
        return {'type': 'http.request', 'body': raw, 'more_body': False}
    async def send(message):
        messages.append(message)
    await app({'type':'http', 'asgi':{'version':'3.0'}, 'http_version':'1.1',
               'method':method, 'scheme':'http', 'path':route, 'raw_path':route.encode(),
               'query_string':b'', 'root_path':'', 'headers':[(b'content-type',b'application/json')],
               'server':('127.0.0.1',8001), 'client':('127.0.0.1',12345)}, receive, send)
    status = next(m['status'] for m in messages if m['type']=='http.response.start')
    body = b''.join(m.get('body',b'') for m in messages if m['type']=='http.response.body')
    return status, json.loads(body)

async def main():
    assert scorer.ready, scorer.error
    status, health = await request('GET','/health')
    assert status == 200 and health['ready']
    cases = json.loads((Path(__file__).parent/'fixtures/synthetic-score-cases.json').read_text(encoding='utf-8'))
    for case in cases:
        status, result = await request('POST','/v1/anomalies/score',case['request'])
        assert status == 200, result
        assert abs(result['anomaly_score']-case['score']) < 1e-12
        assert result['is_anomaly'] == case['is_anomaly']
    status, _ = await request('POST','/v1/anomalies/score',{})
    assert status == 422
    ready = scorer.ready
    try:
        scorer.ready = False
        status, _ = await request('POST','/v1/anomalies/score',cases[0]['request'])
        assert status == 503
    finally:
        scorer.ready = ready
    print(json.dumps({'passed':True,'cases':len(cases),'model_version':health['model_version']},ensure_ascii=False))

if __name__ == '__main__':
    asyncio.run(main())
