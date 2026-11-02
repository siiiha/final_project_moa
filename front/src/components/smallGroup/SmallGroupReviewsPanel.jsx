import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { getSmallGroupReviews } from '../../api/smallGroups';
import Button from '../common/Button';

// AI작성: 후기 본문을 목록에서 모두 읽을 수 있도록 줄임 처리 없이 충분한 글씨 크기와 간격을 적용한다.
const ReviewList = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

const Review = styled.li`
  padding: 24px 0;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  font-size: 16px;
  line-height: 1.6;
`;

const ReviewHeading = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;

  strong { overflow-wrap: anywhere; }
  time { color: ${({ theme }) => theme.colors.secondaryText}; }
`;

const Rating = styled.p`
  margin: 8px 0;
  color: ${({ theme }) => theme.colors.primaryDark};
  font-weight: 700;
`;

const Content = styled.p`
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
`;

const Status = styled.div`
  padding: 24px 0;
  text-align: center;
  font-size: 16px;
  line-height: 1.6;
  color: ${({ theme }) => theme.colors.secondaryText};

  p { margin: 0 0 12px; }
`;

// AI작성: 서버의 한국 현지 작성일 문자열을 날짜로 표시하며 브라우저 시간대로 변환하지 않는다.
function formatReviewDate(value) {
  return value.slice(0, 10).replaceAll('-', '.');
}

// AI작성: 부모가 소모임·조회자 역할별 key를 지정해 목록 상태와 이전 요청을 함께 초기화한다.
function SmallGroupReviewsPanel({ smallGroupId }) {
  const [page, setPage] = useState(1);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState({ items: [], page: 0, totalPages: 0, requestPage: 0, retry: 0, error: '' });
  const sentinel = useRef(null);
  const supportsObserver = typeof IntersectionObserver !== 'undefined';
  // AI작성: 요청한 페이지·재시도 번호와 완료 결과를 비교해 추가 렌더 없이 로딩 상태를 판단한다.
  const isLoading = result.requestPage !== page || result.retry !== retry;

  useEffect(() => {
    const controller = new AbortController();
    getSmallGroupReviews(smallGroupId, { page, signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted) return;
        setResult((previous) => ({
          // AI작성: 페이지 사이에 새 후기가 생겨 중복 행이 내려와도 같은 후기 ID는 한 번만 표시한다.
          items: [...new Map([...previous.items, ...data.items]
            .map((item) => [String(item.smallGroupReviewId), item])).values()],
          page: data.currentPage,
          totalPages: data.totalPages,
          requestPage: page,
          retry,
          error: '',
        }));
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setResult((previous) => ({
          ...previous, requestPage: page, retry,
          error: error.message || '후기 목록을 불러오지 못했습니다. 다시 시도해 주세요.',
        }));
      });
    // AI작성: 탭·소모임 전환이나 언마운트 후 도착한 응답은 목록에 반영하지 않는다.
    return () => controller.abort();
  }, [smallGroupId, page, retry]);

  const hasNext = result.page < result.totalPages;
  const canLoadMore = !isLoading && !result.error && result.page === page && hasNext;

  useEffect(() => {
    if (!supportsObserver || !canLoadMore || !sentinel.current) return undefined;
    let triggered = false;
    const observer = new IntersectionObserver((entries) => {
      if (triggered || !entries.some((entry) => entry.isIntersecting)) return;
      // AI작성: 한 번 감지하면 해제해 같은 페이지를 중복 요청하지 않고 다음 응답 후 다시 관찰한다.
      observer.disconnect();
      triggered = true;
      setPage((previous) => previous + 1);
    });
    observer.observe(sentinel.current);
    return () => observer.disconnect();
  }, [supportsObserver, canLoadMore, page]);

  return (
    <section aria-label="소모임 후기 목록" aria-busy={isLoading}>
      <ReviewList>
        {result.items.map((item) => (
          <Review key={item.smallGroupReviewId}>
            <ReviewHeading>
              <strong>{item.writerNickname}</strong>
              <time dateTime={item.createdAt}>{formatReviewDate(item.createdAt)}</time>
            </ReviewHeading>
            <Rating aria-label={`별점 5점 만점에 ${item.rating}점`}>
              <span aria-hidden="true">★ </span>{item.rating.toFixed(1)} / 5점
            </Rating>
            <Content>{item.content}</Content>
          </Review>
        ))}
      </ReviewList>
      {isLoading ? (
        <Status role="status">{result.items.length ? '후기를 더 불러오는 중입니다.' : '후기를 불러오는 중입니다.'}</Status>
      ) : result.error ? (
        <Status role="alert">
          <p>{result.error}</p>
          <Button type="button" onClick={() => setRetry((previous) => previous + 1)}>다시 시도</Button>
        </Status>
      ) : result.items.length === 0 ? (
        <Status role="status">아직 등록된 후기가 없습니다.</Status>
      ) : !hasNext ? (
        <Status role="status">모든 후기를 확인했습니다.</Status>
      ) : !supportsObserver ? (
        <Status><Button type="button" disabled={!canLoadMore} onClick={() => setPage((previous) => previous + 1)}>후기 더 보기</Button></Status>
      ) : null}
      <div ref={sentinel} aria-hidden="true" style={{ height: 1 }} />
    </section>
  );
}

export default SmallGroupReviewsPanel;
