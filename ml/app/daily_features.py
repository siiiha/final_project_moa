# AI작성: KST 마감일 단위 피처. 현재 일자 마감 후 이용 가능하며 과거 기준은 전일까지의 이력만 사용함.
from collections import defaultdict
from datetime import date,timedelta
from decimal import Decimal
import math
import statistics

FEATURES=['withdrawal_log','deposit_log','withdrawal_count_log','net_outflow_log','three_day_outflow_log',
          'relative_daily_outflow_log','relative_three_day_outflow_log','relative_count_log','day_balance_fraction','deposit_withdrawal_ratio_log']

def build_daily(transactions,history_start,score_start,score_end):
    by_account=defaultdict(lambda:defaultdict(list))
    for tx in transactions:
        assert tx['transacted_at'].endswith('+09:00')
        by_account[tx['account_id']][tx['transacted_at'][:10]].append(tx)
    output=[]
    for account,by_day in by_account.items():
        day=date.fromisoformat(history_start); end=date.fromisoformat(score_end); history=[]
        while day<=end:
            key=day.isoformat(); tx=sorted(by_day.get(key,[]),key=lambda t:t['transacted_at'])
            deposits=sum((Decimal(t['amount']) for t in tx if t['transaction_type']=='DEPOSIT'),Decimal(0))
            outflows=sum((Decimal(t['amount']) for t in tx if t['transaction_type']=='WITHDRAWAL'),Decimal(0))
            count=sum(t['transaction_type']=='WITHDRAWAL' for t in tx)
            prior=history[-60:]
            if tx and key>=score_start and len(prior)>=28:
                active=[x['outflow'] for x in prior if x['outflow']>0]
                typical=statistics.median(active) if active else 0.
                historical_three=[sum(z['outflow'] for z in prior[i-2:i+1]) for i in range(2,len(prior))]
                nonzero_three=[v for v in historical_three if v>0]
                typical_three=statistics.median(nonzero_three) if nonzero_three else 0.
                previous_counts=[x['count'] for x in prior if x['count']>0]
                typical_count=statistics.median(previous_counts) if previous_counts else 0.
                three=float(outflows)+sum(x['outflow'] for x in prior[-2:])
                first=tx[0]; before=Decimal(first['balance_after'])+Decimal(first['amount'])*(1 if first['transaction_type']=='WITHDRAWAL' else -1)
                values=[math.log1p(float(outflows)),math.log1p(float(deposits)),math.log1p(count),
                        math.log1p(max(0,float(outflows-deposits))),math.log1p(three),
                        math.log1p(float(outflows)/(typical+100)),math.log1p(three/(typical_three+100)),
                        math.log1p(count/(typical_count+1)),float(outflows)/(float(before)+100),
                        math.log1p(float(outflows)/(float(deposits)+100))]
                assert all(math.isfinite(v) for v in values)
                output.append({'key':f'{account}:{key}','account_id':account,'date':key,
                               'available_at':(day+timedelta(days=1)).isoformat()+'T00:00:00+09:00',
                               'features':dict(zip(FEATURES,values)),
                               'raw':{'withdrawal_sum':float(outflows),'deposit_sum':float(deposits),'withdrawal_count':count,
                                      'three_day_outflow':three,'relative_daily_outflow':float(outflows)/(typical+100)},
                               'transaction_ids':[t['source_transaction_id'] for t in tx]})
            # AI작성: 무거래일도 이력에 포함해3일 창을 달력 기준으로 유지함. 현재 일은 피처 계산 뒤에만 추가함.
            history.append({'date':key,'outflow':float(outflows),'count':count})
            day+=timedelta(days=1)
    return sorted(output,key=lambda r:(r['date'],r['account_id']))
