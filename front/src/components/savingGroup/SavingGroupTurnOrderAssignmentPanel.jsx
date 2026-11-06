import { useCallback, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { getSavingGroupTurnOrders, updateSavingGroupTurnOrders } from '../../api/savingGroups';

// AI작성: 고령 사용자에게 실제 적용월과 회차를 명확히 안내하고 회원별 입력을 큰 글씨로 표시한다.
const Panel = styled.section`
  display: grid; gap: 20px; padding: 20px; border: 1px solid #ccc; border-radius: 12px;
  font-size: 18px; line-height: 1.6;
  ul { list-style: none; padding: 0; display: grid; gap: 16px; }
  li { display: flex; gap: 16px; flex-wrap: wrap; align-items: center; padding-bottom: 12px; border-bottom: 1px solid #ddd; }
  select { min-height: 48px; padding: 8px; font: inherit; }
  button { min-height: 48px; font-size: 18px; }
`;

// AI작성: 표시 컴포넌트를 분리해 로딩·오류·빈 상태·지급 완료 제외를 검증한다.
export function TurnOrderAssignmentView({ data, drafts = {}, loading, saving, error, notice, onChange, onSave, onReload, disabled }) {
  if (loading) return <Panel role="status">순번을 불러오고 있습니다.</Panel>;
  if (!data) return <Panel><p role="alert">{error || '순번 정보를 불러오지 못했습니다.'}</p><Button onClick={onReload}>다시 조회</Button></Panel>;
  const eligible = data.members.filter(member => !member.paid);
  const values = eligible.map(member => Number(drafts[member.memberId]));
  const valid = values.length > 0 && new Set(values).size === values.length
    && values.every(value => Number.isInteger(value) && value >= data.startRoundNumber && value < data.startRoundNumber + eligible.length);
  return <Panel aria-label="순번 지정">
    <h3>순번 지정</h3>
    <p>{data.scheduled ? `${data.effectiveMonth.slice(0, 7).replace('-', '년 ')}월부터 적용됩니다. 이번 달 순번은 유지됩니다.` : '저장하면 순번이 바로 적용됩니다.'}</p>
    <p>지급 완료 회원을 제외한 모든 승인 회원에게 남은 회차를 한 번씩 배정해 주세요.</p>
    {data.needsReassignment && <p role="status">다음 달 예약이 없거나 모임원 구성이 변경되었습니다. 전체 순번을 확인해 주세요.</p>}
    <ul>{data.members.map(member => <li key={member.memberId}>
      <span>{member.nickname}{member.role === 'OWNER' ? ' · 계주' : ''}</span>
      <span>현재: {member.currentSequenceNumber ? `${member.currentSequenceNumber}회차` : '미지정'}</span>
      {member.paid ? <strong>지급 완료 · 재배정 제외</strong> : <label>
        {member.nickname} 배정 회차{' '}
        <select value={drafts[member.memberId] || ''} onChange={event => onChange(member.memberId, event.target.value)} disabled={saving || disabled}>
          <option value="">선택해 주세요</option>
          {Array.from({ length: eligible.length }, (_, index) => data.startRoundNumber + index).map(round => <option key={round} value={round}>{round}회차</option>)}
        </select>
      </label>}
    </li>)}</ul>
    {!eligible.length && <p>순번을 지정할 미지급 승인 회원이 없습니다.</p>}
    {eligible.length > 0 && !valid && <p>모든 대상의 회차를 중복 없이 선택해 주세요.</p>}
    {error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
    <Button disabled={!valid || saving || disabled} onClick={onSave}>{saving ? '저장 중…' : data.scheduled ? '다음 달 순번 저장' : '순번 저장'}</Button>
    <Button disabled={saving || disabled} onClick={onReload}>최신 순번 다시 조회</Button>
  </Panel>;
}

export default function SavingGroupTurnOrderAssignmentPanel({ savingGroupId, disabled, onBusyChange }) {
  const [snapshot, setSnapshot] = useState(null);
  const [drafts, setDrafts] = useState({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [revision, setRevision] = useState(0);
  // AI작성: 현재 조회 키에 맞는 결과만 표시해 이전 응답이 새 화면에 남지 않도록 한다.
  const loading = snapshot?.key !== revision;
  const data = loading ? null : snapshot.data;
  const controller = useRef(null);
  const submitting = useRef(false);
  const apply = useCallback(result => {
    setSnapshot({ key: revision, data: result });
    setDrafts(Object.fromEntries(result.members.filter(member => !member.paid).map(member => [member.memberId,
      result.scheduled ? member.plannedSequenceNumber ?? '' : member.currentSequenceNumber ?? ''])));
  }, [revision]);
  useEffect(() => {
    const request = new AbortController(); controller.current = request;
    getSavingGroupTurnOrders(savingGroupId, { signal: request.signal }).then(result => {
      if (!request.signal.aborted) apply(result);
    }).catch(failure => { if (!request.signal.aborted) { setError(failure.message); setSnapshot({ key: revision, data: null }); } });
    return () => request.abort();
  }, [savingGroupId, revision, apply]);
  const save = async () => {
    if (submitting.current || disabled || !data) return;
    if (!window.confirm(data.scheduled ? `${data.effectiveMonth.slice(0,7)}부터 적용할 순번을 저장할까요?` : '지정한 순번을 지금 적용할까요?')) return;
    submitting.current = true; setSaving(true); onBusyChange?.(true); setError(''); setNotice('');
    const request = controller.current;
    try {
      const result = await updateSavingGroupTurnOrders(savingGroupId, data.members.filter(member => !member.paid)
        .map(member => ({ memberId: member.memberId, sequenceNumber: Number(drafts[member.memberId]) })), { signal: request.signal });
      if (!request.signal.aborted) { apply(result); setNotice(result.scheduled ? '다음 달 적용할 순번을 저장했습니다.' : '순번을 저장했습니다.'); }
    } catch (failure) { if (!request.signal.aborted) setError(failure.message); }
    finally { submitting.current = false; if (!request.signal.aborted) { setSaving(false); onBusyChange?.(false); } }
  };
  return <TurnOrderAssignmentView data={data} drafts={drafts} loading={loading} saving={saving} error={error} notice={notice}
    disabled={disabled} onChange={(id, value) => setDrafts(current => ({ ...current, [id]: value }))} onSave={save}
    onReload={() => { setError(''); setNotice(''); setRevision(value => value + 1); }} />;
}
