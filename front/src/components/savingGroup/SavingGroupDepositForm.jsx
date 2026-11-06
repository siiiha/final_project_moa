import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import ConfirmModal from '../common/ConfirmModal';
import { requestSavingGroupDeposit, confirmSavingGroupDeposit } from '../../api/savingGroups.js';

// AI작성: 모임·계정별 부모 key로 폼을 분리하고 계좌 원문·확인값을 영구 저장하지 않는다.
const FormArea = styled.section`
  margin-top: 24px; padding-top: 20px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
  label { display: grid; gap: 10px; margin: 16px 0; font-size: 18px; }
  input, select { width: 100%; min-height: 48px; padding: 12px; font-size: 18px;
    color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: 8px; }
  button { min-height: 48px; font-size: 18px; }
  [role='dialog'] p { font-size: 18px; }
  [role='dialog'] button[aria-label] { min-width: 44px; }
`;

// AI작성: 상태별 실제 출력은 순수 View로 분리해 기존 SSR 테스트 방식으로 검증한다.
export function SavingGroupDepositView({ account, amount, busy, pending, uncertain, message, error,
  onAmountChange, onSubmit, onConfirm, onCancel }) {
  return <FormArea aria-label="테스트 입금" aria-busy={busy}>
    <h3>테스트 입금</h3>
    <p>실제 돈은 이동하지 않습니다. 서비스 내부 거래내역과 장부 잔액만 변경됩니다.</p>
    <form onSubmit={onSubmit}>
      <label>입금받을 모임 공용 계좌
        <select aria-label="입금받을 모임 공용 계좌" value="group" disabled={busy || uncertain || Boolean(pending)} onChange={() => {}}>
          <option value="group">모임 공용 계좌 · {account.maskedAccountNumber}</option>
        </select>
      </label>
      <label>입금 금액 (원)
        <input type="text" inputMode="numeric" autoComplete="off" placeholder="예: 10,000"
          value={amount ? Number(amount).toLocaleString('ko-KR') : ''}
          disabled={busy || uncertain || Boolean(pending)} onChange={event => onAmountChange(event.target.value)} />
      </label>
      <Button type="submit" disabled={busy || Boolean(pending)}>
        {busy ? '처리중입니다' : uncertain ? '같은 입금 요청 결과 다시 확인' : '테스트 입금 등록'}
      </Button>
    </form>
    {message && <p role="status">{message}</p>}
    {error && <p role="alert">{error}</p>}
    {uncertain && <p>입금 결과가 확인되지 않았습니다. 금액을 바꾸거나 새 입금을 등록하지 말고 같은 요청 결과를 다시 확인해주세요.</p>}
    {pending && <ConfirmModal title="동일한 금액의 입금 확인" description={pending.message}
      confirmLabel={uncertain ? '같은 확인 요청 결과 다시 확인' : '한 번 더 등록'} cancelFirst
      showCancel={!uncertain} isBusy={busy} onConfirm={onConfirm} onCancel={uncertain ? () => {} : onCancel}>
      <p>{pending.amount.toLocaleString('ko-KR')}원 · {account.maskedAccountNumber}</p>
      <p>확인 유효시간은 안내 발급 후 5분입니다. 실제 돈은 이동하지 않습니다.</p>
      {error && <p role="alert">{error}</p>}
    </ConfirmModal>}
  </FormArea>;
}

export default function SavingGroupDepositForm({ groupId, account, onCompleted }) {
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(null);
  const [uncertain, setUncertain] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const attempt = useRef(null);
  const lock = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);

  const changeAmount = value => {
    if (lock.current || uncertain || pending) return;
    const normalized = value.replaceAll(',', '');
    if (!/^\d{0,13}$/.test(normalized)) return;
    setAmount(normalized.replace(/^0+(?=\d)/, '')); attempt.current = null; setMessage(''); setError('');
  };
  const run = async confirming => {
    if (lock.current) return;
    if (!confirming && (!Number.isSafeInteger(Number(amount)) || Number(amount) < 1)) {
      setError('금액은 1원 이상의 원 단위 정수로 입력해주세요.'); return;
    }
    lock.current = true; setBusy(true); setError(''); setMessage('');
    try {
      // AI작성: 통신 결과가 불확실하면 동일 키·금액을 보존해 재전송에 의한 이중 등록을 막는다.
      if (!attempt.current) attempt.current = { amount: Number(amount), requestKey: crypto.randomUUID() };
      const data = confirming
        ? await confirmSavingGroupDeposit(groupId, { requestKey: attempt.current.requestKey, confirmationToken: pending.confirmationToken })
        : await requestSavingGroupDeposit(groupId, attempt.current);
      if (!alive.current) return;
      setUncertain(false);
      if (data.status === 'PENDING_CONFIRMATION') setPending(data);
      else {
        setPending(null); setAmount(''); attempt.current = null; setMessage(data.message);
        // AI작성: 저장 성공과 잔액 재조회 실패를 분리해 성공한 입금을 다시 제출하게 하지 않는다.
        onCompleted?.(data);
      }
    } catch (failure) {
      if (!alive.current) return;
      setError(failure.message || '입금 결과를 확인하지 못했습니다.');
      const unknown = !failure.status || failure.status >= 500 || failure.code === 'INVALID_RESPONSE';
      setUncertain(unknown);
      if (failure.code === 'DEPOSIT_CONFIRMATION_EXPIRED' || failure.code === 'DEPOSIT_CONFIRMATION_INVALID') {
        setPending(null); // AI작성: 같은 요청 키로 안내를 재발급받아 만료·교체된 확인값을 갱신한다.
      }
    } finally {
      lock.current = false;
      if (alive.current) setBusy(false);
    }
  };
  return <SavingGroupDepositView account={account} amount={amount} busy={busy} pending={pending}
    uncertain={uncertain} message={message} error={error} onAmountChange={changeAmount}
    onSubmit={event => { event.preventDefault(); run(false); }} onConfirm={() => run(true)}
    onCancel={() => { if (lock.current || uncertain) return; setPending(null); attempt.current = null; setError(''); }} />;
}
