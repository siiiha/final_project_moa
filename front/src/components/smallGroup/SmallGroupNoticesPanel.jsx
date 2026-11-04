import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { BsPin, BsPinFill } from 'react-icons/bs';
import Button from '../common/Button';
import ConfirmModal from '../common/ConfirmModal';
import SmallGroupNoticeCreateForm from './SmallGroupNoticeCreateForm';
import { getSmallGroupNoticePagination, updateSmallGroupNoticePinned } from '../../api/smallGroups';

// AI작성: 계모임과 같은 요약 열·고정 배지·44px 핀 버튼을 PC 공지 영역에 제공한다.
const Table = styled.div`
  margin-top: 12px; border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm}; overflow: hidden;
`;
const Row = styled.div`
  display: grid; align-items: center; gap: 12px; padding: 12px;
  grid-template-columns: ${({ $withPin }) => $withPin ? 'minmax(0, 1fr) 64px 90px 44px' : 'minmax(0, 1fr) 64px 90px'};
  font-size: 14px; line-height: 1.6; border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  background: ${({ $pinned, theme }) => $pinned ? theme.colors.primarySoft : theme.colors.white};
  &[role='row']:first-child { background: ${({ theme }) => theme.colors.background}; font-weight: 700; }
  &:last-child { border-bottom: 0; }
  span { min-width: 0; overflow-wrap: anywhere; }
`;
const TitleButton = styled.button`
  padding: 0; border: 0; background: transparent; text-align: left; font: inherit;
  color: ${({ theme }) => theme.colors.primary}; cursor: pointer; text-decoration: underline;
  overflow-wrap: anywhere;
  &:focus-visible { outline: 2px solid currentColor; outline-offset: 3px; }
`;
const PinButton = styled(Button)`
  width: 44px; min-width: 44px; min-height: 44px; padding: 0;
  background: transparent; border-color: transparent;
  color: ${({ $pinned, theme }) => $pinned ? theme.colors.primary : theme.colors.muted};
`;
const Controls = styled.div`display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px;`;
const Message = styled.p`margin-top: 12px; font-size: 16px; line-height: 1.7;`;

function noticeDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '날짜 확인 필요' : new Intl.DateTimeFormat('ko-KR').format(date);
}

// AI작성: 고정 변경과 작성은 동시에 실행하지 않으며 변경 후 조회 실패 시 PATCH를 재전송하지 않는다.
export default function SmallGroupNoticesPanel({ group, heading, pageMeta, sort, onRefresh, onQueryChange, onDetail }) {
  const [state, setState] = useState({ phase: 'idle' });
  const lock = useRef(false);
  const scope = useRef(null);
  useEffect(() => { const current = { active: true }; scope.current = current; return () => { current.active = false; }; }, []);
  const canManage = group.myRole === 'OWNER' && group.membershipStatus === 'APPROVED';
  const canRead = ['OWNER', 'MEMBER'].includes(group.myRole) && group.membershipStatus === 'APPROVED';
  const busy = state.phase === 'pending';
  const blocked = busy || state.needsRefresh;
  const pages = getSmallGroupNoticePagination(pageMeta?.page || 1, pageMeta?.totalPages || 0);
  const pinned = group.pinnedNotices || [];
  const refresh = async () => {
    if (lock.current) return;
    const current = scope.current; lock.current = true; setState({ phase: 'pending' });
    try { await onRefresh(); if (current.active) setState({ phase: 'idle' }); }
    catch { if (current.active) setState({ phase: 'idle', needsRefresh: true, message: '공지 목록을 불러오지 못했습니다. 새로고침해 주세요.' }); }
    finally { lock.current = false; }
  };
  const changePin = async (noticeId, nextPinned) => {
    if (lock.current || blocked || !canManage) return;
    const current = scope.current; lock.current = true; setState({ phase: 'pending' });
    let saved = false;
    try {
      await updateSmallGroupNoticePinned(group.smallGroupId, noticeId, nextPinned);
      saved = true;
      if (!current.active) return;
      await onRefresh();
      if (current.active) setState({ phase: 'idle', message: nextPinned ? '공지를 고정했습니다.' : '공지 고정을 해제했습니다.' });
    } catch (error) {
      if (!current.active) return;
      const limit = error.code === 'SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED';
      setState({ phase: 'idle', limit, needsRefresh: !limit,
        message: saved ? '고정 변경은 완료되었습니다. 목록을 새로고침해 주세요.' : error.message });
    } finally { lock.current = false; }
  };
  const table = (items, isPinned) => <Table role="table" aria-label={isPinned ? '고정 공지 목록' : '일반 공지 목록'}>
    <Row role="row" $withPin={canManage}><span role="columnheader">제목</span><span role="columnheader">작성자</span><span role="columnheader">작성일</span>{canManage && <span role="columnheader">고정</span>}</Row>
    {items.map((notice) => <Row role="row" key={notice.noticeId} $pinned={isPinned} $withPin={canManage}>
      <span role="cell">{isPinned && <strong>고정 </strong>}{canRead ? <TitleButton type="button" disabled={busy} onClick={() => onDetail(notice.noticeId)}>{notice.title}</TitleButton> : notice.title}</span>
      <span role="cell">{notice.writerNickname || '작성자 정보 없음'}</span>
      <span role="cell"><time dateTime={notice.createdAt}>{noticeDate(notice.createdAt)}</time></span>
      {canManage && <span role="cell"><PinButton type="button" $pinned={isPinned} disabled={blocked}
        aria-label={notice.title + (isPinned ? ' 고정 해제' : ' 고정')} aria-pressed={isPinned}
        onClick={() => changePin(notice.noticeId, !isPinned)}>{isPinned ? <BsPinFill aria-hidden="true" /> : <BsPin aria-hidden="true" />}</PinButton></span>}
    </Row>)}
  </Table>;
  return <SmallGroupNoticeCreateForm group={group} heading={heading} onRefresh={onRefresh} disabled={blocked}>
    <Controls role="group" aria-label="공지 정렬">
      {['LATEST', 'OLDEST'].map((value) => <Button key={value} type="button" disabled={blocked}
        $variant={sort === value ? 'primary' : 'secondary'} aria-pressed={sort === value} onClick={() => onQueryChange(1, value)}>{value === 'LATEST' ? '최신순' : '오래된순'}</Button>)}
    </Controls>
    {busy && <Message role="status">공지를 새로고침하고 있습니다.</Message>}
    {state.message && !state.limit && <Message role={state.needsRefresh ? 'alert' : 'status'}>{state.message}</Message>}
    {state.needsRefresh && <Button type="button" disabled={busy} onClick={refresh}>공지 새로고침</Button>}
    {pinned.length > 0 && <section aria-label="고정 공지"><h3 style={{ marginTop: 16 }}>고정 공지</h3>{table(pinned, true)}</section>}
    {group.notices.length ? table(group.notices, false) : <Message>{pinned.length ? '등록된 일반 공지가 없습니다.' : '등록된 공지가 없습니다.'}</Message>}
    <Controls as="nav" aria-label="공지 페이지 이동">
      <Button type="button" $variant="secondary" disabled={blocked || pages.previous === null} onClick={() => onQueryChange(pages.previous, sort)}>이전</Button>
      {pages.pages.map((page) => <Button key={page} type="button" disabled={blocked} $variant={pageMeta?.page === page ? 'primary' : 'secondary'}
        aria-current={pageMeta?.page === page ? 'page' : undefined} onClick={() => onQueryChange(page, sort)}>{page}</Button>)}
      <Button type="button" $variant="secondary" disabled={blocked || pages.next === null} onClick={() => onQueryChange(pages.next, sort)}>다음</Button>
    </Controls>
    {state.limit && <ConfirmModal title="공지 고정 안내" description={state.message} showCancel={false}
      onConfirm={() => setState({ phase: 'idle' })} onCancel={() => setState({ phase: 'idle' })} />}
  </SmallGroupNoticeCreateForm>;
}
