import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { getSmallGroupDetail, getSmallGroupNoticeDetail, readSmallGroupNoticeQuery, toSmallGroupNoticeSearch } from '../api/smallGroups.js';
import SmallGroupNoticeEditForm from '../components/smallGroup/SmallGroupNoticeEditForm';
import SmallGroupNoticeDeleteButton from '../components/smallGroup/SmallGroupNoticeDeleteButton';
import Button from '../components/common/Button';
import SmallGroupNoticeDetail from '../components/smallGroup/SmallGroupNoticeDetail';
import useAuthStore from '../store/authStore';

// AI작성: 기존 소모임 상세의 페이지 너비와 공통 버튼·테마를 재사용한다.
const Page = styled.main`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 24px 0 48px;
`;
const Message = styled.section`
  padding: 32px 20px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 18px;
  line-height: 1.7;
  p { margin-bottom: 16px; }
`;
const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
`;

// AI작성: 기존 게시글 화면처럼 JWT subject는 버튼 노출 비교에만 사용하며 실제 인증·권한은 서버가 검증한다.
function noticeMemberId(token) {
  try {
    const payload = String(token || '').split('.')[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const subject = JSON.parse(window.atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))).sub;
    return subject ? String(subject) : null;
  } catch { return null; }
}

// AI작성: URL·계정·재시도 회차가 다른 응답은 숨기고 이탈한 요청은 취소해 이전 본문 노출을 막는다.
export default function SmallGroupNoticeDetailPage() {
  const { id, noticeId } = useParams();
  const navigate = useNavigate();
  // AI작성: 외부 복귀 주소를 받지 않고 같은 소모임의 검증된 공지 조건으로만 돌아간다.
  const [searchParams] = useSearchParams();
  const { page, sort } = readSmallGroupNoticeQuery(searchParams);
  const authVersion = useAuthStore((state) => state.authVersion);
  const token = useAuthStore((state) => state.token);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  // AI작성: 편집 상태를 URL·계정에 묶어 다른 회원이나 공지로 이동 시 이전 편집 내용을 숨긴다.
  const scope = `${id}:${noticeId}:${authVersion}`;
  const [editing, setEditing] = useState(null);
  const [saved, setSaved] = useState(null);
  const [blocked, setBlocked] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    Promise.all([
      getSmallGroupNoticeDetail(id, noticeId, { signal: controller.signal }),
      getSmallGroupDetail(id, { signal: controller.signal }),
    ])
      .then(([notice, group]) => {
        if (active) setResult({ id, noticeId, authVersion, retry, notice, group });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ id, noticeId, authVersion, retry, error });
      });
    return () => { active = false; controller.abort(); };
  }, [id, noticeId, authVersion, retry]);

  const current = result?.id === id && result?.noticeId === noticeId
    && result?.authVersion === authVersion && result?.retry === retry ? result : null;
  const error = current?.error;
  const canEdit = current?.group?.membershipStatus === 'APPROVED'
    && current.group.myRole === 'OWNER'
    && noticeMemberId(token) !== null && noticeMemberId(token) === String(current.notice?.writerId) && blocked !== scope;
  const status = error?.status;
  const message = status === 401 ? '로그인 후 공지를 확인해 주세요.'
    : status === 403 ? '해당 소모임의 승인된 회원만 공지를 볼 수 있습니다.'
      : status === 404 ? '조회할 수 있는 소모임 또는 공지가 없습니다.'
        : status === 400 || error?.code === 'BAD_REQUEST' ? '공지 주소가 올바르지 않습니다.'
          : '공지를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  const canRetry = error && error.code !== 'BAD_REQUEST' && (!status || status >= 500 || error.code === 'INVALID_RESPONSE');

  return (
    <Page>
      {!current ? <Message role="status">공지를 불러오는 중입니다.</Message>
        : error ? (
          <Message role="alert">
            <p>{message}</p>
            {canRetry && <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>}
          </Message>
        ) : (
          <>
            {saved === scope && <Message role="status">공지가 수정되었습니다.</Message>}
            {blocked === scope && <Message role="alert">현재 수정·삭제 권한 또는 공지 상태가 변경되었습니다. 새로고침 후 확인해 주세요.</Message>}
            {canEdit && editing === scope ? (
              <SmallGroupNoticeEditForm key={scope} groupId={id} notice={current.notice}
                onCancel={() => setEditing(null)} onBlocked={() => setBlocked(scope)}
                onSaved={(notice) => {
                  // AI작성: 재조회 실패로 성공한 저장을 실패로 안내하지 않고 서버와 동일한 정규화 값을 즉시 표시한다.
                  setResult((previous) => previous?.id === id && previous?.noticeId === noticeId
                    && previous?.authVersion === authVersion ? { ...previous, notice } : previous);
                  setEditing(null); setSaved(scope);
                }} />
            ) : <SmallGroupNoticeDetail notice={current.notice} actions={canEdit && (
              <>
              {/* AI작성: 기존 권한·동작을 유지한 수정·삭제 버튼을 상세 카드 내부에 전달한다. */}

              <Button type="button" onClick={() => { setEditing(scope); setSaved(null); }}>공지 수정</Button>
              {/* AI작성: 삭제는 동일한 작성자 조건에서만 표시하고 성공 후 기존 목록 재조회 흐름으로 복귀한다. */}
              <SmallGroupNoticeDeleteButton key={scope} groupId={id} noticeId={noticeId}
                onBlocked={() => setBlocked(scope)}
                onDeleted={() => navigate(`/small-groups/${encodeURIComponent(id)}?${toSmallGroupNoticeSearch(page, sort)}`,
                  { replace: true, state: { deletedNoticeId: noticeId, deletedNoticeGroupId: id } })} />
              </>
            )} />}
          </>
        )}
      {/* AI작성: 수정 폼이 표시되는 동안 목록 복귀 버튼을 숨기고 저장·취소 후 다시 표시한다. */}
      {!(canEdit && editing === scope) && (
      <Actions>
        {/* AI작성: 직접 URL 진입은 최신순 1페이지, 목록에서 진입한 경우 보던 페이지·정렬을 복원한다. */}
        <Button type="button" $variant="secondary" onClick={() => navigate(`/small-groups/${encodeURIComponent(id)}?${toSmallGroupNoticeSearch(page, sort)}`)}>
          소모임 공지로 돌아가기
        </Button>
      </Actions>
      )}
    </Page>
  );
}
