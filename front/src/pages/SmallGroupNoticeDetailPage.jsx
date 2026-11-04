import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { getSmallGroupNoticeDetail, readSmallGroupNoticeQuery, toSmallGroupNoticeSearch } from '../api/smallGroups.js';
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

// AI작성: URL·계정·재시도 회차가 다른 응답은 숨기고 이탈한 요청은 취소해 이전 본문 노출을 막는다.
export default function SmallGroupNoticeDetailPage() {
  const { id, noticeId } = useParams();
  const navigate = useNavigate();
  // AI작성: 목록에서 전달한 페이지·정렬만 검증해 복귀 URL을 만든다.
  const [searchParams] = useSearchParams();
  const { page, sort } = readSmallGroupNoticeQuery(searchParams);
  const authVersion = useAuthStore((state) => state.authVersion);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSmallGroupNoticeDetail(id, noticeId, { signal: controller.signal })
      .then((notice) => {
        if (active) setResult({ id, noticeId, authVersion, retry, notice });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ id, noticeId, authVersion, retry, error });
      });
    return () => { active = false; controller.abort(); };
  }, [id, noticeId, authVersion, retry]);

  const current = result?.id === id && result?.noticeId === noticeId
    && result?.authVersion === authVersion && result?.retry === retry ? result : null;
  const error = current?.error;
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
        ) : <SmallGroupNoticeDetail notice={current.notice} />}
      <Actions>
        {/* AI작성: 직접 URL로 진입해도 같은 소모임의 기본 공지 탭으로 돌아간다. */}
        <Button type="button" $variant="secondary" onClick={() => navigate(`/small-groups/${encodeURIComponent(id)}?${toSmallGroupNoticeSearch(page, sort)}`)}>
          소모임 공지로 돌아가기
        </Button>
      </Actions>
    </Page>
  );
}
