import { useEffect, useId, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import ConfirmModal from '../common/ConfirmModal';
import { requestSavingGroupDeposit, confirmSavingGroupDeposit } from '../../api/savingGroups.js';

// AI작성: 모임·계정별 부모 key로 폼을 분리하고 계좌 원문·확인값을 영구 저장하지 않는다.
const FormArea = styled.section`
  width: min(100%, 540px); max-height: calc(100dvh - 48px); overflow-y: auto;
  padding: 28px; border-radius: 16px; background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.text};
  p { font-size: 18px; line-height: 1.7; }
  label { display: grid; gap: 10px; margin: 16px 0; font-size: 18px; }
  input, select { width: 100%; min-height: 48px; padding: 12px; font-size: 18px;
    color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: 8px; }
  button { min-height: 48px; font-size: 18px; }
  [role='dialog'] p { font-size: 18px; }
  [role='dialog'] button[aria-label] { min-width: 44px; }
`;
// AI작성: 기존 컴포넌트만으로 입력폼을 모달로 감싸며 추가 라이브러리는 사용하지 않는다.
const Overlay = styled.div`
  position: fixed; inset: 0; z-index: 30; padding: 24px 16px;
  display: grid; place-items: center; background: rgba(26, 41, 36, 0.38);
`;
const Actions = styled.div`display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 12px; margin-top: 20px;`;

// AI작성: 상태별 실제 출력은 순수 View로 분리해 기존 SSR 테스트 방식으로 검증한다.
export function SavingGroupDepositView({ account, amount, busy, pending, uncertain, message, error,
  onAmountChange, onSubmit, onConfirm, onCancel, onClose, dialogRef, onDialogKeyDown }) {
  const titleId = useId();
  return <Overlay><FormArea role="dialog" aria-modal="true" aria-labelledby={titleId} aria-busy={busy}
    tabIndex={-1} ref={dialogRef} onKeyDown={onDialogKeyDown}>
    <h2 id={titleId}>{pending ? '동일한 금액의 입금 확인' : uncertain ? '입금 결과 확인 필요' : error ? '입금 처리 실패' : '회비 납부'}</h2>
    <p>실제 돈은 이동하지 않습니다. 서비스 내부 거래내역과 장부 잔액만 변경됩니다.</p>
    {/* AI작성: 중첩 모달 없이 하나의 모달 안에서 입력과 중복 확인을 전환한다. */}
    {pending ? <>
      <p>{pending.message}</p>
      <p>{pending.amount.toLocaleString('ko-KR')}원 · {account.maskedAccountNumber}</p>
      <p>확인 유효시간은 안내 발급 후 5분입니다.</p>
      <Actions>
        {!uncertain && <Button type="button" $variant="secondary" onClick={onCancel} disabled={busy}>취소</Button>}
        <Button type="button" onClick={onConfirm} disabled={busy}>
          {busy ? '처리중입니다' : uncertain ? '같은 확인 요청 결과 다시 확인' : '한 번 더 등록'}
        </Button>
      </Actions>
    </> : <form onSubmit={onSubmit}>
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
      <Actions>
        <Button type="button" $variant="secondary" disabled={busy || uncertain} onClick={onClose}>취소</Button>
        <Button type="submit" disabled={busy || Boolean(pending)}>
          {busy ? '처리중입니다' : uncertain ? '같은 입금 요청 결과 다시 확인' : '입금'}
        </Button>
      </Actions>
    </form>}
    {message && <p role="status">{message}</p>}
    {error && <p role="alert">{error}</p>}
    {uncertain && <p>입금 결과가 확인되지 않았습니다. 금액을 바꾸거나 새 입금을 등록하지 말고 같은 요청 결과를 다시 확인해주세요.</p>}
  </FormArea></Overlay>;
}

const ResultArea = styled.div`
  [role='dialog'] p { font-size: 18px; line-height: 1.7; }
  [role='dialog'] button { min-height: 44px; font-size: 18px; }
`;

// AI작성: 입력폼과 독립된 완료 모달이므로 잔액 재조회가 진행돼도 사용자가 확인할 때까지 남는다.
export function SavingGroupDepositResultModal({ result, onClose }) {
  const root = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    root.current?.querySelector('button')?.focus();
    return () => { if (previous?.isConnected) previous.focus(); };
  }, []);
  return <ResultArea ref={root} onKeyDown={event => {
    if (event.key === 'Escape') { event.preventDefault(); onClose(); }
    trapTab(event, root.current);
  }}>
    <ConfirmModal title="테스트 입금 완료" description={result.message} showCancel={false}
      onConfirm={onClose} onCancel={onClose}>
      <p>입금 금액: {result.amount.toLocaleString('ko-KR')}원</p>
      <p>현재 내부 장부 잔액: {result.balance.toLocaleString('ko-KR')}원</p>
      <p>실제 돈은 이동하지 않았습니다.</p>
    </ConfirmModal>
  </ResultArea>;
}

// AI작성: 입력·확인·완료 화면 모두 Tab 초점이 모달 밖으로 빠져나가지 않도록 한다.
function trapTab(event, root) {
  if (event.key !== 'Tab' || !root) return;
  const focusable = [...root.querySelectorAll('input:not(:disabled), select:not(:disabled), button:not(:disabled)')];
  const first = focusable[0], last = focusable.at(-1);
  if (!first) { event.preventDefault(); root.focus(); }
  else if (event.shiftKey && (!root.contains(document.activeElement) || document.activeElement === first || document.activeElement === root)) {
    event.preventDefault(); last.focus();
  } else if (!event.shiftKey && (!root.contains(document.activeElement) || document.activeElement === last || document.activeElement === root)) {
    event.preventDefault(); first.focus();
  }
}

export default function SavingGroupDepositForm({ savingGroupId, account, onCompleted, onClose }) {
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(null);
  const [uncertain, setUncertain] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const attempt = useRef(null);
  const lock = useRef(false);
  const alive = useRef(true);
  const dialog = useRef(null);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  // AI작성: 키보드 초점을 모달 안에 유지하고 종료 시 납부 버튼으로 복원한다.
  useEffect(() => {
    const previous = document.activeElement;
    dialog.current?.querySelector('input')?.focus();
    return () => { if (previous?.isConnected) previous.focus(); };
  }, []);
  // AI작성: 중복 확인을 취소하면 입력 단계로 돌아가며, 처리중·결과 불명확 상태는 요청 정보를 보존한다.
  const cancelPending = () => {
    if (lock.current || uncertain) return;
    setPending(null); attempt.current = null; setError('');
  };
  const close = () => {
    if (lock.current || uncertain) return;
    if (pending) cancelPending(); else onClose?.();
  };
  useEffect(() => { if (pending || uncertain || error) dialog.current?.focus(); }, [pending, uncertain, error]);
  const keyDown = event => {
    if (event.key === 'Escape') { event.preventDefault(); close(); }
    trapTab(event, dialog.current);
  };

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
        ? await confirmSavingGroupDeposit(savingGroupId, { requestKey: attempt.current.requestKey, confirmationToken: pending.confirmationToken })
        : await requestSavingGroupDeposit(savingGroupId, attempt.current);
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
    onClose={close} dialogRef={dialog} onDialogKeyDown={keyDown}
    uncertain={uncertain} message={message} error={error} onAmountChange={changeAmount}
    onSubmit={event => { event.preventDefault(); run(false); }} onConfirm={() => run(true)}
    onCancel={cancelPending} />;
}
