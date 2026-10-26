# AI작성: 내부 일별 분석 요청을 엄격하게 검사함. 금융 값/내부 오류를 응답에 반사하지 않음.
from datetime import date,datetime,timedelta,timezone
from decimal import Decimal
import re
from typing import Literal
from pydantic import BaseModel,ConfigDict,Field,StrictBool,field_validator,model_validator

KST=timezone(timedelta(hours=9))
MAX_ROWS=10000

class Transaction(BaseModel):
    model_config=ConfigDict(extra='forbid')
    account_id:str=Field(strict=True,min_length=1,max_length=64)
    transaction_id:str=Field(strict=True,min_length=1,max_length=128)
    transacted_at:str=Field(strict=True,max_length=40)
    transaction_type:Literal['DEPOSIT','WITHDRAWAL']
    amount:str=Field(strict=True,pattern=r'^\d{1,13}\.\d{2}$')
    balance_after:str|None=Field(default=None,strict=True,pattern=r'^-?\d{1,13}\.\d{2}$')

    @field_validator('account_id','transaction_id')
    @classmethod
    def identifier(cls,value):
        if value!=value.strip() or not value.strip(): raise ValueError('invalid ID')
        return value

    @field_validator('amount')
    @classmethod
    def positive_money(cls,value):
        if Decimal(value)<=0: raise ValueError('positive amount required')
        return value

    @field_validator('transacted_at')
    @classmethod
    def timestamp(cls,value):
        dt=datetime.fromisoformat(value.replace('Z','+00:00'))
        if dt.tzinfo is None or dt.utcoffset() is None or dt.microsecond: raise ValueError('offset seconds required')
        return dt.astimezone(KST).isoformat(timespec='seconds')

class DailyRequest(BaseModel):
    model_config=ConfigDict(extra='forbid')
    account_id:str=Field(strict=True,min_length=1,max_length=64)
    target_date:str=Field(strict=True,pattern=r'^\d{4}-\d{2}-\d{2}$')
    history_start:str=Field(strict=True,pattern=r'^\d{4}-\d{2}-\d{2}$')
    history_end:str=Field(strict=True,pattern=r'^\d{4}-\d{2}-\d{2}$')
    history_complete:StrictBool
    transactions:list[Transaction]=Field(max_length=MAX_ROWS)

    @field_validator('account_id')
    @classmethod
    def identifier(cls,value): return Transaction.identifier(value)

    @field_validator('target_date','history_start','history_end')
    @classmethod
    def calendar_date(cls,value):
        date.fromisoformat(value)
        return value

    @model_validator(mode='after')
    def consistency(self):
        target=date.fromisoformat(self.target_date); start=date.fromisoformat(self.history_start); end=date.fromisoformat(self.history_end)
        if end!=target+timedelta(days=1) or start>=end or (end-start).days>90:
            raise ValueError('invalid coverage range')
        seen=set()
        for row in self.transactions:
            if row.account_id!=self.account_id or row.transaction_id in seen: raise ValueError('mixed account or duplicate ID')
            seen.add(row.transaction_id)
            if not start<=datetime.fromisoformat(row.transacted_at).date()<end: raise ValueError('out-of-range transaction')
        return self
