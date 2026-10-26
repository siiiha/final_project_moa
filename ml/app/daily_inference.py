# AI작성: 별도 일별 모델·manifest를 확인하고 읽기 전용 점수/주간 후보를 계산함. 발송/DB 저장은 하지 않음.
from datetime import date,datetime,timedelta
from decimal import Decimal
import hashlib
import json
import math
from pathlib import Path
import platform
import joblib
import numpy as np
import sklearn
from .daily_features import FEATURES,build_daily
from .daily_schemas import DailyRequest,KST

class DailyFailure(Exception):
    def __init__(self,code,status=503): self.code=code; self.status=status

class DailyEngine:
    def __init__(self,folder=None):
        self.folder=Path(folder) if folder else Path(__file__).resolve().parents[1]/'artifacts/daily-v1'
        self.ready=False; self.error_code='DAILY_NOT_READY'; self.manifest={}; self.model=None
        self.load()

    def load(self):
        self.ready=False; self.model=None
        try:
            meta=json.loads((self.folder/'manifest.json').read_text(encoding='utf-8'))
            expected={'python':platform.python_version(),'sklearn':sklearn.__version__,'numpy':np.__version__,'joblib':joblib.__version__}
            if meta['versions']!=expected: raise DailyFailure('DAILY_RUNTIME_MISMATCH')
            model_path=self.folder/'model.joblib'
            if hashlib.sha256(model_path.read_bytes()).hexdigest()!=meta['model_sha256']: raise DailyFailure('DAILY_MODEL_HASH_MISMATCH')
            if hashlib.sha256(Path(__file__).with_name('daily_features.py').read_bytes()).hexdigest()!=meta['feature_source_sha256']:
                raise DailyFailure('DAILY_FEATURE_HASH_MISMATCH')
            if meta['feature_names']!=FEATURES or meta['score_definition']!='negative_score_samples' or not 0<float(meta['threshold'])<1:
                raise DailyFailure('DAILY_MANIFEST_INVALID')
            # AI작성: 자체 생성하고 해시가 고정된 신뢰 모델만 로드함. 요청으로 파일 경로를 받지 않음.
            bundle=joblib.load(model_path)
            if bundle['feature_names']!=FEATURES or bundle['unit']!='closed_account_day': raise DailyFailure('DAILY_MODEL_SCHEMA_MISMATCH')
            self.model=bundle['model']; self.manifest=meta; self.ready=True; self.error_code=None
        except DailyFailure as exc: self.error_code=exc.code
        except Exception: self.error_code='DAILY_MODEL_LOAD_FAILED'

    def health(self):
        return {'ready':self.ready,'error_code':self.error_code,
                'model_version':self.manifest.get('model_version'),'feature_schema_version':self.manifest.get('feature_schema_version'),
                'policy_mode':'READ_ONLY_SIMULATION','notification_enabled':False}

    def analyze(self,request:DailyRequest,now:datetime|None=None):
        if not self.ready: raise DailyFailure(self.error_code or 'DAILY_NOT_READY')
        now=(now or datetime.now(KST)).astimezone(KST)
        target=date.fromisoformat(request.target_date); end=target+timedelta(days=1)
        if target>=now.date(): raise DailyFailure('DAILY_NOT_CLOSED',422)
        notify=end; week_start=notify-timedelta(days=notify.weekday()); first_day=week_start-timedelta(days=1)
        required_start=first_day-timedelta(days=60)
        tx=sorted(request.transactions,key=lambda row:(row.transacted_at,row.transaction_id))
        canonical={'request':request.model_dump(),'model_version':self.manifest['model_version'],
                   'feature_schema_version':self.manifest['feature_schema_version'],'policy_version':self.manifest['policy_version']}
        canonical['request']['transactions']=[r.model_dump() for r in tx]
        fingerprint=hashlib.sha256(json.dumps(canonical,sort_keys=True,separators=(',',':')).encode()).hexdigest()
        iso=notify.isocalendar()
        result={'account_id':request.account_id,'target_date':target.isoformat(),
                'period_start':target.isoformat()+'T00:00:00+09:00','period_end':end.isoformat()+'T00:00:00+09:00',
                'available_at':end.isoformat()+'T00:00:00+09:00','snapshot_at':now.isoformat(timespec='seconds'),
                'status':'NOT_SCORED','anomaly_score':None,'threshold':self.manifest['threshold'],'daily_anomaly':None,
                'weekly_review_candidate':None,'candidate_date':None,'notification_week':f'{iso.year}-W{iso.week:02d}',
                'policy_mode':'READ_ONLY_SIMULATION','notification_enabled':False,'cold_start':False,'quality_flags':[],
                'model_version':self.manifest['model_version'],'feature_schema_version':self.manifest['feature_schema_version'],
                'policy_version':self.manifest['policy_version'],'input_fingerprint':fingerprint,
                'notice':'평소와 다른 흐름의 확인 후보이며 사기·연체 판정 또는 확률이 아닙니다.'}
        if not request.history_complete:
            result['quality_flags']=['history_coverage_unverified']; return result
        if date.fromisoformat(request.history_start)>required_start:
            result.update(cold_start=True,quality_flags=['insufficient_history']); return result
        flags=set(); previous=None; previous_time=None
        for row in tx:
            if previous_time==row.transacted_at: flags.add('same_timestamp_order_unknown')
            previous_time=row.transacted_at
            if row.balance_after is None:
                flags.add('bank_balance_missing'); previous=None; continue
            amount=Decimal(row.amount); after=Decimal(row.balance_after)
            before=after+(amount if row.transaction_type=='WITHDRAWAL' else -amount)
            if after<0 or before<0: flags.add('balance_nonpositive')
            if previous is not None and previous!=before: flags.add('ledger_balance_mismatch')
            previous=after
        if flags:
            result['quality_flags']=sorted(flags); return result
        history=[{'account_id':r.account_id,'source_transaction_id':r.transaction_id,'transacted_at':r.transacted_at,
                  'transaction_type':r.transaction_type,'amount':r.amount,'balance_after':r.balance_after} for r in tx]
        rows=build_daily(history,request.history_start,first_day.isoformat(),target.isoformat())
        warnings=[]; target_score=None
        if rows:
            x=np.array([[r['features'][n] for n in FEATURES] for r in rows],dtype=float)
            values=-self.model.score_samples(x)
            if not np.isfinite(values).all(): raise DailyFailure('DAILY_SCORE_INVALID')
            for row,value in zip(rows,values):
                if value>self.manifest['threshold']: warnings.append(row['date'])
                if row['date']==target.isoformat(): target_score=float(value)
        candidate=min(warnings) if warnings else None
        result['candidate_date']=candidate
        result['weekly_review_candidate']=candidate==target.isoformat()
        if target_score is None:
            result['status']='NO_ACTIVITY'
            return result
        result.update(status='SCORED',anomaly_score=target_score,daily_anomaly=target_score>self.manifest['threshold'])
        return result
