import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { formatSavingGroupAmount, getSavingGroupTurnOrderRecommendations } from '../../api/savingGroups.js';
import useAuthStore from '../../store/authStore.js';

// AI작성: 기존 테마를 재사용하고 모의 결과를 큰 글씨·반응형 목록으로 표시한다.
const Panel = styled.section`
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  display: grid;
  gap: 16px;
  font-size: 16px;
  line-height: 1.6;
  h3 { font-size: 20px; }
  ol { display: grid; gap: 12px; padding: 0; list-style: none; }
  li { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px 20px;
    padding: 14px; background: ${({ theme }) => theme.colors.background}; overflow-wrap: anywhere; }
  button { min-height: 44px; font-size: 16px; justify-self: start; }
`;

// AI작성: 표시 전용 부분을 분리해 성공·로딩·오류·빈 결과를 실제 JSX로 검증할 수 있게 한다.
export function TurnOrderRecommendationView({ phase = 'idle', items = [], error = '', onRecommend, disabled = false }) {
  return <Panel aria-label="순번 추천" aria-busy={phase === 'loading'}>
    <h3>순번 추천</h3>
    {/* AI작성: 진행 중인 순번계는 현재 지급 회차 이후만 추천한다. */}
    <p>순번을 무작위로 제안합니다. 진행 중에는 지급 완료 계원과 현재 회차 계원을 제외합니다. 추천 결과는 참고용입니다.</p>
    <p>예상 지급 금액은 현재 인원과 회비가 유지되고 앞 순번이 모두 지급받는다는 가정으로 계산합니다. 실제 지급 금액과 다를 수 있습니다.</p>
    <Button type="button" disabled={disabled || phase === 'loading'} onClick={onRecommend}>
      {phase === 'loading' ? '추천 중…' : phase === 'success' ? '다시 추천받기' : '추천받기'}
    </Button>
    {phase === 'idle' && <p>추천받기를 누르면 모의 순번과 예상 지급 금액을 확인할 수 있습니다.</p>}
    {phase === 'loading' && <p role="status">순번을 추천하고 있습니다.</p>}
    {phase === 'error' && <p role="alert">{error || '순번 추천을 불러오지 못했습니다.'}</p>}
    {phase === 'success' && (items.length === 0 ? <p role="status">추천할 수 있는 순번이 없습니다.</p>
      : <div><p role="status">{items.length}명의 모의 추천 결과입니다.</p>
        <ol aria-label="추천 순번 목록">{items.map((item) => <li key={item.memberId}>
          <span><strong>{item.recommendedTurnOrder}회차</strong> · {item.nickname}</span>
          <span>예상 지급 금액 <strong>{formatSavingGroupAmount(item.expectedPayoutAmount)}</strong></span>
        </li>)}</ol></div>)}
  </Panel>;
}

// AI작성: 모임·로그인 변경은 key로 초기화하고 닫힘·이동 시 요청을 취소한다. 중복 클릭은 ref로 즉시 차단한다.
function RecommendationSession({ savingGroupId, disabled }) {
  const [state, setState] = useState({ phase: 'idle', items: [] });
  const request = useRef(null);
  useEffect(() => () => { request.current?.abort(); }, []);
  const recommend = async () => {
    if (disabled || request.current) return;
    const controller = new AbortController(); request.current = controller;
    setState({ phase: 'loading', items: [] });
    try {
      const items = await getSavingGroupTurnOrderRecommendations(savingGroupId, { signal: controller.signal });
      if (!controller.signal.aborted) setState({ phase: 'success', items });
    } catch (failure) {
      if (!controller.signal.aborted) setState({ phase: 'error', items: [], error: failure.message });
    } finally { if (request.current === controller) request.current = null; }
  };
  return <TurnOrderRecommendationView {...state} disabled={disabled} onRecommend={recommend} />;
}

export default function SavingGroupTurnOrderRecommendationPanel({ savingGroupId, disabled = false }) {
  const authVersion = useAuthStore((state) => state.authVersion);
  return <RecommendationSession key={JSON.stringify([savingGroupId, authVersion])} savingGroupId={savingGroupId} disabled={disabled} />;
}
