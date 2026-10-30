import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { getSmallGroupVoteDetail } from '../api/smallGroups';
import Button from '../components/common/Button';
import SmallGroupVoteDetail from '../components/smallGroup/SmallGroupVoteDetail';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 24px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Content = styled.main`
  max-width: 1120px;
  margin-top: 12px;
`;

const EmptyCard = styled.section`
  padding: 42px 20px;
  text-align: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};

  p {
    margin-bottom: 16px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 13px;
  }
`;

const Actions = styled.div`
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
`;

// AI작성: URL의 소모임·투표 ID가 함께 바뀔 때마다 이전 요청을 취소해 다른 투표 결과가 표시되지 않게 한다.
function SmallGroupVoteDetailPage() {
  const navigate = useNavigate();
  const { smallGroupId, voteId } = useParams();
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [voteView, setVoteView] = useState({
    voteId: null,
    selectedOptionIds: [],
    isResultOpen: false,
  });

  useEffect(() => {
    const controller = new AbortController();
    const requestKey = `${smallGroupId}:${voteId}:${retry}`;
    let active = true;

    getSmallGroupVoteDetail(smallGroupId, voteId, { signal: controller.signal })
      .then((vote) => {
        if (active) setResult({ requestKey, vote });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ requestKey, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [smallGroupId, voteId, retry]);

  const requestKey = `${smallGroupId}:${voteId}:${retry}`;
  const current = result?.requestKey === requestKey ? result : null;
  const errorCode = current?.error?.code;
  const errorMessage = errorCode === 'SMALL_GROUP_NOT_FOUND'
    || errorCode === 'SMALL_GROUP_VOTE_NOT_FOUND'
    ? '조회할 수 있는 투표가 없습니다.'
    : errorCode === 'SMALL_GROUP_VOTE_DETAIL_FORBIDDEN'
      ? '승인된 소모임 회원만 투표 상세를 조회할 수 있습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(errorCode)
        ? '로그인이 필요합니다.'
        : current?.error?.message || '투표 상세를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  const canRetry = ![
    'SMALL_GROUP_NOT_FOUND',
    'SMALL_GROUP_VOTE_NOT_FOUND',
    'SMALL_GROUP_VOTE_DETAIL_FORBIDDEN',
    'UNAUTHORIZED',
    'LOGIN_REQUIRED',
  ].includes(errorCode);
  // AI작성: 투표 상세에서 돌아갈 때는 소모임 기본 공지가 아닌 기존 투표 탭을 다시 연다.
  const goToVoteList = () => navigate(`/small-groups/${smallGroupId}`, {
    state: { activeSmallGroupMenu: 'poll' },
  });
  const currentVote = current?.vote;
  const selectedOptionIds = voteView.voteId === voteId
    ? voteView.selectedOptionIds
    : currentVote?.myParticipation.selectedOptionIds || [];
  const isResultOpen = voteView.voteId === voteId && voteView.isResultOpen;

  // AI작성: 참여 저장 API가 추가되기 전에는 선택 UI만 갱신하고 서버의 참여 정보는 바꾸지 않는다.
  const handleSelectOption = (optionId) => {
    if (!currentVote || currentVote.status === 'CLOSED' || currentVote.myParticipation.participated) return;

    setVoteView((previous) => {
      const previousIds = previous.voteId === voteId
        ? previous.selectedOptionIds
        : currentVote.myParticipation.selectedOptionIds;
      const hasSelected = previousIds.includes(optionId);
      const nextIds = currentVote.multipleChoice
        ? (hasSelected ? previousIds.filter((id) => id !== optionId) : [...previousIds, optionId])
        : [optionId];

      return {
        voteId,
        selectedOptionIds: nextIds,
        isResultOpen: false,
      };
    });
  };

  // AI작성: 결과 공개 여부는 상세 응답의 참여 여부만 기준으로 하며 별도 요청을 보내지 않는다.
  const handleShowResults = () => {
    if (!currentVote?.myParticipation.participated) return;
    setVoteView((previous) => ({
      voteId,
      selectedOptionIds: previous.voteId === voteId
        ? previous.selectedOptionIds
        : currentVote.myParticipation.selectedOptionIds,
      isResultOpen: true,
    }));
  };

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 메인 &gt; 투표 상세</Breadcrumb>
      <Content>
        {!current ? (
          <EmptyCard role="status">투표 상세를 불러오는 중입니다.</EmptyCard>
        ) : current.error ? (
          <EmptyCard role="alert">
            <p>{errorMessage}</p>
            <Actions>
              {canRetry && <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>}
              <Button type="button" $variant="secondary" onClick={goToVoteList}>투표 목록으로</Button>
            </Actions>
          </EmptyCard>
        ) : (
          <SmallGroupVoteDetail
            vote={currentVote}
            onBack={goToVoteList}
            selectedOptionIds={selectedOptionIds}
            onSelectOption={handleSelectOption}
            isResultOpen={isResultOpen}
            onShowResults={handleShowResults}
          />
        )}
      </Content>
    </Page>
  );
}

export default SmallGroupVoteDetailPage;
