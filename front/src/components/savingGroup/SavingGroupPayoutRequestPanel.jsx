import { useEffect, useId, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import useAuthStore from '../../store/authStore.js';
import { createSavingGroupPayout, getSavingGroupPayoutOptions } from '../../api/savingGroups.js';

// AI작성: 기존 테마·버튼을 재사용하고 큰 글씨와 명확한 입력 문구로 계금 요청만 처리한다.
const Panel = styled.section`
  margin-top: 22px;
  padding: 24px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 16px;
  line-height: 1.7;
  h2 { font-size: 22px; margin-bottom: 12px; }
  p { margin: 12px 0; }
  form { display: grid; gap: 20px; margin-top: 20px; }
  label { display: grid; gap: 8px; font-weight: 700; }
  input {
    width: 100%; min-height: 48px; padding: 10px;
    font: inherit; border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
  }
  button { min-height: 48px; font-size: 16px; }
`;

// AI작성: 모임·로그인 세션 변경 시 입력·요청 상태를 새로 만들고 이전 계정 결과를 표시하지 않는다.
function SavingGroupPayoutRequestPanel({ savingGroupId }) {
  const authVersion = useAuthStore((state) => state.authVersion);
  return <PayoutForm key={`${savingGroupId}:${authVersion}`} savingGroupId={savingGroupId} />;
}

function PayoutForm({ savingGroupId }) {
  const fieldId = useId();
  const lock = useRef(false);
  const alive = useRef(false);
  const submitController = useRef(null);
  const [options, setOptions] = useState(null);
  const [phase, setPhase] = useState('loading');
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [amount, setAmount] = useState('');
  const [title, setTitle] = useState('');
  const [receipt, setReceipt] = useState(null);

  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; submitController.current?.abort(); };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let current = true;
    getSavingGroupPayoutOptions(savingGroupId, { signal: controller.signal }).then((value) => {
      if (!current) return;
      if (!['순번계', '친목계'].includes(value?.groupType)
          || (value.groupType === '순번계' && (value.amount == null || value.roundNumber == null))) {
        throw new Error('요청 정보를 확인할 수 없습니다.');
      }
      setOptions(value); setPhase('ready');
    }).catch((failure) => {
      if (!current || failure.code === 'ERR_CANCELED') return;
      setError(failure.message); setPhase('blocked');
    });
    return () => { current = false; controller.abort(); };
  }, [savingGroupId, revision]);

  const isRotation = options?.groupType === '순번계';
  const formattedAmount = options?.amount == null ? '' : `${Number(options.amount).toLocaleString('ko-KR')}원`;
  const retry = () => { setError(''); setOptions(null); setPhase('loading'); setRevision((value) => value + 1); };

  const submit = async (event) => {
    event.preventDefault();
    if (lock.current || phase !== 'ready') return;
    if (!isRotation && (!/^[1-9]\d*$/.test(amount) || amount.length > 13 || !title.trim() || !receipt)) {
      setError('사용 금액은 1원 이상의 정수로 입력하고 제목과 영수증을 확인해주세요.'); return;
    }
    lock.current = true; setPhase('submitting'); setError('');
    const controller = new AbortController(); submitController.current = controller;
    try {
      await createSavingGroupPayout(savingGroupId, isRotation ? {} : { amount, title: title.trim(), receipt }, { signal: controller.signal });
      if (alive.current) { setPhase('success'); setReceipt(null); }
    } catch (failure) {
      if (!alive.current || failure.code === 'ERR_CANCELED') return;
      setError(failure.message); setPhase('blocked');
    } finally { lock.current = false; }
  };

  return <Panel aria-labelledby={`${fieldId}-heading`}>
    <h2 id={`${fieldId}-heading`}>계금 요청</h2>
    {phase === 'loading' && <p role="status">요청 가능 정보를 확인하고 있습니다.</p>}
    {phase === 'success' ? <p role="status">계금 요청이 접수되었습니다. 승인 결과를 기다려주세요.</p> : <>
      {error && <p role="alert">{error}</p>}
      {phase === 'blocked' && <Button type="button" onClick={retry}>요청 가능 정보 다시 확인</Button>}
      {options && phase !== 'blocked' && <>
        {isRotation ? <p>현재 {options.roundNumber}회차 · 확정 요청 금액 <strong>{formattedAmount}</strong></p>
          : <p>사용 금액과 제목을 입력하고 영수증을 첨부해주세요. 저장된 영수증은 계주만 열람할 수 있습니다.</p>}
        <form onSubmit={submit} aria-busy={phase === 'submitting'}>
          {!isRotation && <>
            <label htmlFor={`${fieldId}-amount`}>사용 금액 (원)
              <input id={`${fieldId}-amount`} inputMode="numeric" pattern="[1-9][0-9]*" maxLength={13} required
                value={amount} onChange={(event) => setAmount(event.target.value)} disabled={phase === 'submitting'} />
            </label>
            <label htmlFor={`${fieldId}-title`}>사용 내역 제목
              <input id={`${fieldId}-title`} maxLength={100} required value={title}
                onChange={(event) => setTitle(event.target.value)} disabled={phase === 'submitting'} />
            </label>
            <label htmlFor={`${fieldId}-receipt`}>영수증 이미지 (필수)
              <input id={`${fieldId}-receipt`} type="file" accept="image/jpeg,image/png" required disabled={phase === 'submitting'}
                onChange={(event) => {
                  const selected = event.target.files?.[0];
                  if (selected && (!['image/jpeg', 'image/png'].includes(selected.type) || selected.size > 5 * 1024 * 1024)) {
                    event.target.value = ''; setReceipt(null); setError('JPG 또는 PNG 이미지를 5MB 이하로 첨부해주세요.');
                  } else { setReceipt(selected || null); setError(''); }
                }} />
            </label>
            <p>가로·세로 각각 4096픽셀 이하의 JPG·PNG 파일을 첨부해주세요. 개인정보는 가린 후 첨부해주세요.</p>
          </>}
          <Button type="submit" disabled={phase !== 'ready'}>{phase === 'submitting' ? '요청 중입니다…' : '계금 요청하기'}</Button>
        </form>
      </>}
    </>}
  </Panel>;
}

export default SavingGroupPayoutRequestPanel;