import { useEffect, useId, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { createSavingGroupNotice } from '../../api/savingGroups.js';

// AI작성: 공지 입력은 큰 글씨와 충분한 간격을 적용하고 기존 테마·버튼을 재사용한다.
const Form = styled.form`
  margin-top: 16px;
  display: grid;
  gap: 16px;
  label { display: grid; gap: 8px; font-size: 16px; font-weight: 700; }
  input, textarea {
    width: 100%; min-width: 0; box-sizing: border-box; padding: 12px;
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
    font: inherit; font-size: 16px; line-height: 1.6;
  }
  textarea { min-height: 200px; resize: vertical; }
  input:focus, textarea:focus { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px; }
  small { color: ${({ theme }) => theme.colors.secondaryText}; font-size: 14px; font-weight: 400; }
`;
const Actions = styled.div`
  margin-top: 12px; display: flex; flex-wrap: wrap; gap: 10px;
  button { min-height: 48px; font-size: 16px; }
`;
const Message = styled.p`
  margin-top: 12px; font-size: 16px; line-height: 1.6;
  color: ${({ $error, theme }) => $error ? theme.colors.danger : theme.colors.primary};
`;

// AI작성: 서버가 확인한 현재 관계와 상태가 모두 있어야 작성 UI를 제공한다.
function canWriteNotice(group) {
  return group.membershipStatus === 'APPROVED'
    && ['OWNER', 'MANAGER'].includes(group.myRole)
    && ['01', '02'].includes(group.status);
}

// AI작성: 등록 실패 시 입력을 유지하고 성공 후 조회 실패는 재등록 없이 조회만 재시도한다.
function SavingGroupNoticeCreateForm({ group, onRefresh }) {
  const fieldId = useId();
  const scopeRef = useRef(null);
  const lockRef = useRef(false);
  const titleRef = useRef(null);
  const openButtonRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [phase, setPhase] = useState('idle');
  const [error, setError] = useState('');
  const [blocked, setBlocked] = useState(false);
  const allowed = canWriteNotice(group) && !blocked;
  const busy = phase === 'submitting' || phase === 'refreshing';

  useEffect(() => {
    const scope = { active: true };
    scopeRef.current = scope;
    return () => { scope.active = false; };
  }, []);
  useEffect(() => { if (open) titleRef.current?.focus(); }, [open]);

  const refresh = async (scope, saved) => {
    setPhase('refreshing');
    try {
      await onRefresh();
      if (!scope.active) return;
      setPhase(saved ? 'saved' : 'idle');
      setBlocked(false);
      setError('');
    } catch {
      if (!scope.active) return;
      setPhase(saved ? 'refreshError' : 'idle');
      setError(saved ? '공지는 등록되었습니다. 최근 공지를 불러오지 못했습니다. 새로고침해 주세요.'
        : '현재 작성 권한을 확인하지 못했습니다. 새로고침해 주세요.');
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    const scope = scopeRef.current;
    if (!scope?.active || lockRef.current || !allowed || busy) return;
    if (!title.trim() || title.length > 100 || !content.trim() || content.length > 5000) {
      setError('제목은 1~100자, 내용은 1~5,000자로 입력해 주세요. 공백만 입력할 수 없습니다.');
      return;
    }
    lockRef.current = true;
    setPhase('submitting');
    setError('');
    try {
      await createSavingGroupNotice(group.savingGroupId, { title, content });
      if (!scope.active) return;
      setOpen(false);
      setTitle('');
      setContent('');
      openButtonRef.current?.focus();
      await refresh(scope, true);
    } catch (failure) {
      if (!scope.active) return;
      setPhase('idle');
      setError(failure.message || '공지 등록에 실패했습니다. 입력 내용을 확인해 주세요.');
      // AI작성: 권한·모임 상태가 바뀐 오류는 이전 권한으로 재요청하지 않도록 조회 전까지 작성 UI를 숨긴다.
      if ([401, 403, 404, 409].includes(failure.status)) setBlocked(true);
    } finally {
      lockRef.current = false;
    }
  };

  return (
    <>
      {allowed && !open && (
        <Actions><Button ref={openButtonRef} type="button" disabled={busy || phase === 'refreshError'}
          onClick={() => { setOpen(true); setError(''); setPhase('idle'); }}>공지 작성</Button></Actions>
      )}
      {allowed && open && (
        <Form onSubmit={submit} aria-label="계모임 공지 작성" aria-busy={busy}>
          <label htmlFor={`${fieldId}-title`}>제목 (필수)
            <input ref={titleRef} id={`${fieldId}-title`} name="title" value={title} required maxLength={100}
              disabled={busy} aria-describedby={`${fieldId}-title-count`}
              onChange={(event) => setTitle(event.target.value)} />
            <small id={`${fieldId}-title-count`}>{title.length} / 100자</small>
          </label>
          <label htmlFor={`${fieldId}-content`}>내용 (필수)
            <textarea id={`${fieldId}-content`} name="content" value={content} required maxLength={5000}
              disabled={busy} aria-describedby={`${fieldId}-content-count`}
              onChange={(event) => setContent(event.target.value)} />
            <small id={`${fieldId}-content-count`}>{content.length.toLocaleString('ko-KR')} / 5,000자</small>
          </label>
          <Actions>
            <Button type="submit" disabled={busy}>{phase === 'submitting' ? '등록 중…' : '공지 등록'}</Button>
            <Button type="button" $variant="secondary" disabled={busy}
              onClick={() => { setOpen(false); setTitle(''); setContent(''); setError('');
                requestAnimationFrame(() => openButtonRef.current?.focus()); }}>취소</Button>
          </Actions>
        </Form>
      )}
      {phase === 'saved' && <Message role="status">공지가 등록되었습니다.</Message>}
      {phase === 'refreshing' && <Message role="status">최근 공지를 새로고침하고 있습니다.</Message>}
      {error && <Message role="alert" $error>{error}</Message>}
      {(phase === 'refreshError' || blocked) && (
        <Actions><Button type="button" $variant="secondary" disabled={busy}
          onClick={() => refresh(scopeRef.current, phase === 'refreshError')}>최근 공지 새로고침</Button></Actions>
      )}
    </>
  );
}

export default SavingGroupNoticeCreateForm;
