import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import {
  closeSmallGroupVote,
  deleteSmallGroupVote,
  getSmallGroupDetail,
  getSmallGroupVoteDetail,
  getSmallGroupVoteStatus,
  participateSmallGroupVote,
} from '../api/smallGroups';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
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
  });
  const [isVoteSubmitting, setIsVoteSubmitting] = useState(false);
  const [voteActionError, setVoteActionError] = useState(null);
  const [statusView, setStatusView] = useState(null);
  const [groupRole, setGroupRole] = useState(null);
  const [isVoteDeleteModalOpen, setIsVoteDeleteModalOpen] = useState(false);
  const [isVoteDeleting, setIsVoteDeleting] = useState(false);
  const [voteDeleteError, setVoteDeleteError] = useState(null);
  // AI작성: 종료 상태는 소모임·투표별로 구분하고 ref로 같은 틱의 중복 요청과 늦은 응답을 차단한다.
  const routeKey = `${smallGroupId}:${voteId}`;
  const [closeAction, setCloseAction] = useState(null);
  const closeRequestRef = useRef(null);
  const scopeRef = useRef(null);
  const resultSequenceRef = useRef(0);
  const currentClose = closeAction?.key === routeKey && closeAction.scope?.active ? closeAction : null;
  const isVoteClosing = Boolean(currentClose?.busy);

  useEffect(() => {
    const scope = { active: true };
    scopeRef.current = scope;
    return () => { scope.active = false; };
  }, [smallGroupId, voteId]);

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

  // AI작성: 직접 상세 URL로 진입한 OWNER도 삭제 버튼을 확인할 수 있도록 소모임 관계 정보를 별도로 조회한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    getSmallGroupDetail(smallGroupId, { signal: controller.signal })
      .then((group) => {
        if (active) setGroupRole({ smallGroupId, myRole: group.myRole });
      })
      .catch(() => {
        if (active && !controller.signal.aborted) setGroupRole({ smallGroupId, myRole: null });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [smallGroupId]);

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
  const canDeleteVote = groupRole?.smallGroupId === smallGroupId && groupRole.myRole === 'OWNER';
  const currentStatus = statusView?.requestKey === requestKey ? statusView : null;
  const selectedOptionIds = voteView.voteId === voteId
    ? voteView.selectedOptionIds
    : currentVote?.myParticipation.selectedOptionIds || [];

  // AI작성: 진행 중 투표는 기존 참여 회원도 선택을 바꿀 수 있으며, 저장 전 결과 패널은 닫아 오래된 현황을 숨긴다.
  const handleSelectOption = (optionId) => {
    if (!currentVote || currentVote.status === 'CLOSED' || isVoteSubmitting || isVoteDeleting
      || closeRequestRef.current?.active || isVoteClosing) return;

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
      };
    });
    setStatusView(null);
  };

  // AI작성: 선택 변경을 포함한 참여 요청 후 상세를 다시 읽어 서버가 확정한 내 선택·참여 여부를 반영한다.
  const handleVoteSubmit = async () => {
    if (!currentVote || currentVote.status === 'CLOSED' || selectedOptionIds.length === 0
      || isVoteSubmitting || isVoteDeleting || closeRequestRef.current?.active || isVoteClosing) return;

    setIsVoteSubmitting(true);
    setVoteActionError(null);
    try {
      await participateSmallGroupVote(smallGroupId, voteId, selectedOptionIds);
      setVoteView({ voteId, selectedOptionIds });
      setStatusView(null);
      setRetry((value) => value + 1);
    } catch (error) {
      setVoteActionError(error.message || '투표 참여 처리에 실패했습니다.');
    } finally {
      setIsVoteSubmitting(false);
    }
  };

  // AI작성: 현황은 참여 완료 회원 전용 API에서 다시 읽어 마감 전에도 최신 득표 수와 닉네임을 표시한다.
  const handleShowResults = async () => {
    if (!currentVote?.myParticipation.participated || isVoteClosing || closeRequestRef.current?.active) return;
    const sequence = ++resultSequenceRef.current;
    const scope = scopeRef.current;
    setStatusView({ requestKey, loading: true });
    try {
      const vote = await getSmallGroupVoteStatus(smallGroupId, voteId);
      if (scope?.active && sequence === resultSequenceRef.current) setStatusView({ requestKey, vote });
    } catch (error) {
      if (scope?.active && sequence === resultSequenceRef.current) setStatusView({ requestKey, error });
    }
  };

  // AI작성: 확인 뒤에만 삭제 API를 호출하고, 성공 시 논리 삭제된 투표가 제외된 목록으로 돌아간다.
  const handleVoteDeleteConfirm = async () => {
    if (!currentVote || !canDeleteVote || isVoteDeleting || isVoteSubmitting
      || isVoteClosing || closeRequestRef.current?.active) return;

    setIsVoteDeleting(true);
    try {
      await deleteSmallGroupVote(smallGroupId, voteId);
      setIsVoteDeleteModalOpen(false);
      goToVoteList();
    } catch (error) {
      setIsVoteDeleteModalOpen(false);
      setVoteDeleteError(error.message || '투표를 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsVoteDeleting(false);
    }
  };

  // AI작성: 확인 후 종료하며 오래된 현황·미저장 선택을 초기화하고 서버 상세를 다시 조회한다.
  const handleVoteCloseConfirm = async () => {
    if (!currentVote?.canClose || currentVote.status !== 'OPEN' || isVoteSubmitting
      || isVoteDeleting || closeRequestRef.current?.active || isVoteClosing) return;
    const scope = scopeRef.current;
    if (!scope?.active) return;
    closeRequestRef.current = scope;
    setCloseAction({ key: routeKey, scope, open: true, busy: true });
    resultSequenceRef.current += 1;
    setStatusView(null);
    try {
      await closeSmallGroupVote(smallGroupId, voteId);
      if (!scope.active) return;
      setCloseAction(null);
      setVoteView({ voteId: null, selectedOptionIds: [] });
      setVoteActionError(null);
      setRetry((value) => value + 1);
    } catch (error) {
      if (!scope.active) return;
      setCloseAction({ key: routeKey, scope, error: error.message || '투표를 종료하지 못했습니다. 잠시 후 다시 시도해 주세요.' });
      if (['SMALL_GROUP_VOTE_CLOSE_FORBIDDEN', 'SMALL_GROUP_VOTE_NOT_FOUND', 'SMALL_GROUP_NOT_FOUND'].includes(error.code)) {
        setRetry((value) => value + 1);
      }
    } finally {
      if (closeRequestRef.current === scope) closeRequestRef.current = null;
    }
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
            onSubmit={handleVoteSubmit}
            isSubmitting={isVoteSubmitting}
            isResultOpen={Boolean(currentStatus?.vote)}
            resultVote={currentStatus?.vote}
            isStatusLoading={Boolean(currentStatus?.loading)}
            statusError={currentStatus?.error?.message}
            onShowResults={handleShowResults}
            actionError={voteActionError}
            canDelete={canDeleteVote}
            onDelete={() => setIsVoteDeleteModalOpen(true)}
            isDeleting={isVoteDeleting}
            canClose={currentVote.canClose && currentVote.status === 'OPEN'}
            onClose={() => setCloseAction({ key: routeKey, scope: scopeRef.current, open: true })}
            isClosing={isVoteClosing}
          />
        )}
      </Content>
      {currentClose?.open && currentVote && (
        <ConfirmModal
          title="투표 닫기"
          description={`'${currentVote.title}' 투표를 닫으시겠어요? 닫은 뒤에는 참여하거나 다시 열 수 없습니다.`}
          confirmLabel={isVoteClosing ? '종료 중...' : '투표 닫기'}
          cancelFirst
          isBusy={isVoteClosing}
          onConfirm={handleVoteCloseConfirm}
          onCancel={() => { if (!isVoteClosing) setCloseAction(null); }}
        />
      )}
      {currentClose?.error && (
        <ConfirmModal
          title="투표 종료 실패"
          description={currentClose.error}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setCloseAction(null)}
          onCancel={() => setCloseAction(null)}
        />
      )}
      {isVoteDeleteModalOpen && currentVote && (
        <ConfirmModal
          title="투표 삭제"
          description={`'${currentVote.title}' 투표를 삭제하시겠어요? 삭제된 투표는 다시 되돌릴 수 없습니다.`}
          confirmLabel="투표 삭제"
          confirmVariant="danger"
          cancelFirst
          isBusy={isVoteDeleting}
          onConfirm={handleVoteDeleteConfirm}
          onCancel={() => setIsVoteDeleteModalOpen(false)}
        />
      )}
      {voteDeleteError && (
        <ConfirmModal
          title="투표 삭제 실패"
          description={voteDeleteError}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setVoteDeleteError(null)}
          onCancel={() => setVoteDeleteError(null)}
        />
      )}
    </Page>
  );
}

export default SmallGroupVoteDetailPage;
