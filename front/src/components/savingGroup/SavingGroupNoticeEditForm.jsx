import { useEffect, useId, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { updateSavingGroupNotice } from '../../api/savingGroups.js';

// AI작성: 기존 테마와 공통 버튼을 사용해 상세 화면 안에서 제목·내용만 편집한다.
const Form = styled.form`
  display: grid; gap: 20px; margin-top: 24px;
  label { display: grid; gap: 8px; font-size: 18px; }
  input, textarea {
    width: 100%; box-sizing: border-box; padding: 12px; font: inherit; line-height: 1.7;
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
  }
  textarea { min-height: 240px; resize: vertical; }
  input:focus, textarea:focus { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px; }
`;
const Actions = styled.div`display: flex; flex-wrap: wrap; gap: 12px;`;

// AI작성: 부모의 계정·URL 키 변경 시 폼을 해제하며 중복 제출은 동기 잠금으로 차단한다.
export default function SavingGroupNoticeEditForm({ groupId, notice, onSaved, onCancel, onBlocked }) {
  const fieldId = useId();
  const lock = useRef(false);
  const active = useRef(false);
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);
  const [title, setTitle] = useState(notice.title);
  const [content, setContent] = useState(notice.content);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event) => {
    event.preventDefault();
    if (lock.current) return;
    if (!title.trim() || title.length > 100 || !content.trim() || content.length > 5000) {
      setError('제목은 1~100자, 내용은 1~5,000자로 입력해 주세요. 공백만 입력할 수 없습니다.');
      return;
    }
    lock.current = true;
    setBusy(true);
    setError('');
    try {
      await updateSavingGroupNotice(groupId, notice.noticeId, { title, content });
      if (!active.current) return;
      onSaved({ ...notice, title: title.trim(), content: content.trim() });
    } catch (failure) {
      if (!active.current) return;
      setError(failure.message);
      if ([401, 403, 404, 409].includes(failure.status)) onBlocked();
    } finally {
      lock.current = false;
      if (active.current) setBusy(false);
    }
  };
  return (
    <Form onSubmit={submit} aria-label="계모임 공지 수정" aria-busy={busy}>
      <label htmlFor={`${fieldId}-title`}>제목 (필수)
        <input autoFocus id={`${fieldId}-title`} value={title} required maxLength={100} disabled={busy}
          aria-describedby={`${fieldId}-title-count`}
          onChange={(event) => setTitle(event.target.value)} />
        <small id={`${fieldId}-title-count`}>{title.length} / 100자</small>
      </label>
      <label htmlFor={`${fieldId}-content`}>내용 (필수)
        <textarea id={`${fieldId}-content`} value={content} required maxLength={5000} disabled={busy}
          aria-describedby={`${fieldId}-content-count`}
          onChange={(event) => setContent(event.target.value)} />
        <small id={`${fieldId}-content-count`}>{content.length.toLocaleString('ko-KR')} / 5,000자</small>
      </label>
      {error && <p role="alert">{error}</p>}
      <Actions>
        <Button type="submit" disabled={busy}>{busy ? '저장 중…' : '수정 저장'}</Button>
        <Button type="button" $variant="secondary" disabled={busy} onClick={onCancel}>취소</Button>
      </Actions>
    </Form>
  );
}
