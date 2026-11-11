import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import useAuthStore from '../store/authStore';
import SmallGroupDetail from '../components/smallGroup/SmallGroupDetail';
import {
  createSmallGroupVote,
  fetchSmallGroupCategories,
  getSmallGroupDetail,
  readSmallGroupNoticeQuery,
  toSmallGroupNoticeSearch,
  getSmallGroupSettings,
  getSmallGroupVotes,
  requestSmallGroupJoin,
  updateSmallGroupSettings,
} from '../api/smallGroups';

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

// AI작성: 공개 상세 API 응답을 기존 상세 컴포넌트의 표시 필드로만 변환한다.
function toSmallGroupDetailView(data) {
  const memberCount = Number.isFinite(Number(data.memberCount)) ? Number(data.memberCount) : 0;
  return {
    name: data.groupName,
    region: data.regionName,
    interest: data.likeCategory?.trim() || '관심사 미정',
    participants: data.maxMembers == null
      ? `${memberCount}명 / 정원 미정`
      : `${memberCount} / ${data.maxMembers}명`,
    description: data.description?.trim() || '소개글이 없습니다.',
    creatorNickname: data.creatorNickname,
    createdAt: data.createdAt,
    meetDate: data.meetDate,
    location: data.location,
    notices: data.notices,
    pinnedNotices: data.pinnedNotices,
    // AI작성: 표시용 변환에서 서버의 가입 신청 상태·판단 필드를 누락하지 않는다.
    membershipStatus: data.membershipStatus,
    canApply: data.canApply,
    joinRestrictionCode: data.joinRestrictionCode,
    joinRestrictionMessage: data.joinRestrictionMessage,
  };
}

function SmallGroupDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // AI작성: 상세에서 목록으로 이동할 때 적용된 검색 조건을 복원한다.
  const goToList = () => navigate({ pathname: '/small-groups',
    search: new URLSearchParams(location.search).get('listSearch') ?? '' });
  const { id } = useParams();
  const noticeQuery = readSmallGroupNoticeQuery(location.search);
  // AI작성: 로그인 계정이 바뀌면 이전 계정의 신청 상태와 응답을 재사용하지 않는다.
  const authVersion = useAuthStore((state) => state.authVersion);
  const [result, setResult] = useState(null);
  // AI작성: 상세 조회·작성 후 갱신 요청은 같은 순번으로 이전 응답을 폐기한다.
  const detailRequest = useRef(0);
  const [retry, setRetry] = useState(0);
  const [menu, setMenu] = useState({
    id,
    // AI작성: 투표 상세의 목록 복귀 상태만 허용해 일반 소모임 진입은 기존 공지 탭을 유지한다.
    value: location.state?.activeSmallGroupMenu === 'poll' ? 'poll' : 'notice',
  });
  const [settingsResult, setSettingsResult] = useState(null);
  const [settingsRetry, setSettingsRetry] = useState(0);
  const [isSettingsSaving, setIsSettingsSaving] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isVoteCreating, setIsVoteCreating] = useState(false);
  const [isVoteCreateSuccessModalOpen, setIsVoteCreateSuccessModalOpen] = useState(false);
  const [voteResult, setVoteResult] = useState(null);
  const [voteRetry, setVoteRetry] = useState(0);

  // AI작성: 동일 렌더의 연속 클릭은 ref로 차단하고 안내는 모임·계정별로 구분한다.
  const joinLock = useRef(null);
  const joinScope = useRef(null);
  const [joinState, setJoinState] = useState(null);
  const currentJoin = joinState?.id === id && joinState?.authVersion === authVersion ? joinState : null;
  const isJoinSubmitting = currentJoin?.status === 'submitting';
  const isJoinModalOpen = currentJoin?.status === 'success';
  const isIdentityVerificationModalOpen = currentJoin?.status === 'verification';
  const joinErrorMessage = currentJoin?.status === 'error' ? currentJoin.message : null;

  // AI작성: 이미 전송된 POST는 취소로 되돌릴 수 없으므로 화면 이탈 후의 안내·재조회만 차단한다.
  useEffect(() => {
    const scope = { active: true };
    joinScope.current = scope;
    return () => {
      scope.active = false;
      // AI작성: 다른 모임으로 갔다가 돌아와도 이전 완료·실패 안내가 다시 열리지 않게 한다.
      setJoinState(null);
    };
  }, [id, authVersion]);

  // AI작성: URL 변경·화면 이탈 시 이전 요청을 취소해 다른 소모임의 결과가 표시되지 않게 한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    const requestVersion = ++detailRequest.current;
    getSmallGroupDetail(id, { signal: controller.signal, noticePage: noticeQuery.page, noticeSort: noticeQuery.sort })
      .then((group) => {
        // AI작성: 마지막 공지 삭제 등으로 서버가 페이지를 보정하면 URL도 같은 페이지로 맞춘다.
        if (active && requestVersion === detailRequest.current && group.noticePageMeta?.page && group.noticePageMeta.page !== noticeQuery.page) {
          navigate({ pathname: location.pathname, search: toSmallGroupNoticeSearch(group.noticePageMeta.page, noticeQuery.sort, location.search) }, { replace: true });
          return;
        }
        if (active && requestVersion === detailRequest.current) setResult({ id, retry, authVersion, search: location.search, group });
      })
      .catch((error) => {
        if (active && requestVersion === detailRequest.current && !controller.signal.aborted) setResult({ id, retry, authVersion, search: location.search, error });
      });

    return () => {
      active = false;
      controller.abort();
      detailRequest.current += 1;
    };
  }, [id, retry, authVersion, location.search, location.pathname, navigate, noticeQuery.page, noticeQuery.sort]);

  const current = result?.id === id && result?.retry === retry && result?.authVersion === authVersion && result?.search === location.search ? result : null;
  const isSettingsSelected = current?.group?.myRole === 'OWNER'
    && menu.id === id && menu.value === 'settings';
  const isVoteSelected = ['OWNER', 'MEMBER'].includes(current?.group?.myRole)
    && menu.id === id && menu.value === 'poll';

  // AI작성: OWNER가 설정 탭을 열 때만 설정값과 서버 기준 관심사 목록을 함께 조회한다.
  useEffect(() => {
    if (!isSettingsSelected) return undefined;

    const controller = new AbortController();
    let active = true;
    setSettingsResult({ id, retry: settingsRetry, loading: true });

    Promise.all([
      getSmallGroupSettings(id, { signal: controller.signal }),
      fetchSmallGroupCategories({ signal: controller.signal }),
    ])
      .then(([settings, categories]) => {
        if (active) setSettingsResult({ id, retry: settingsRetry, settings, categories });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setSettingsResult({ id, retry: settingsRetry, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [id, isSettingsSelected, settingsRetry]);

  // AI작성: 승인 회원이 투표 탭을 열 때만 목록을 조회하고, 화면 전환 시 이전 요청은 취소한다.
  useEffect(() => {
    if (!isVoteSelected) return undefined;

    const controller = new AbortController();
    let active = true;
    setVoteResult({ id, retry: voteRetry, loading: true });

    getSmallGroupVotes(id, { signal: controller.signal })
      .then((votes) => {
        if (active) setVoteResult({ id, retry: voteRetry, votes });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setVoteResult({ id, retry: voteRetry, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [id, isVoteSelected, voteRetry]);

  // AI작성: 폼이 비교·검증한 변경 필드만 저장하고 성공 후 공개 상세와 설정값을 모두 최신 상태로 다시 읽는다.
  const handleSettingsSubmit = async (changes) => {
    setIsSettingsSaving(true);
    try {
      await updateSmallGroupSettings(id, changes);
      setRetry((value) => value + 1);
      setSettingsRetry((value) => value + 1);
      // AI작성: 저장 성공한 경우에만 최신 상세 정보를 보여줄 수 있도록 공지 탭으로 돌아간다.
      setMenu({ id, value: 'notice' });
    } finally {
      setIsSettingsSaving(false);
    }
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 소모임 삭제 API에 현재 소모임 ID를 전송하고 목록 화면으로 이동합니다.
    setIsDeleteModalOpen(false);
  };

  // AI작성: 가입 신청은 상세 조회 ID를 그대로 사용하며, 본인 인증 오류만 별도 안내 모달로 구분한다.
  const handleJoinRequest = async () => {
    const scope = joinScope.current;
    if (!scope?.active || joinLock.current?.scope === scope || current?.group?.canApply !== true) return;

    const request = { scope };
    joinLock.current = request;
    setJoinState({ id, authVersion, status: 'submitting' });
    try {
      await requestSmallGroupJoin(id);
      if (!scope.active) return;
      setJoinState({ id, authVersion, status: 'success' });
      // AI작성: 신청 성공은 가입 완료가 아니므로 서버의 최신 심사 상태를 다시 조회한다.
      setRetry((value) => value + 1);
    } catch (error) {
      if (!scope.active) return;
      setJoinState({
        id, authVersion,
        status: error.code === 'IDENTITY_VERIFICATION_REQUIRED' ? 'verification' : 'error',
        message: error.message || '가입 신청 처리에 실패했습니다.',
      });
      // AI작성: 정원·가입 상태 변경 또는 응답 유실 후에는 POST를 반복하지 않고 상세부터 확인한다.
      setRetry((value) => value + 1);
    } finally {
      if (joinLock.current === request) joinLock.current = null;
    }
  };

  // AI작성: 투표 생성 성공 뒤 목록을 다시 조회해 새 투표를 현재 탭에 즉시 반영한다.
  const handleVoteCreate = async (request) => {
    if (isVoteCreating) return;

    setIsVoteCreating(true);
    try {
      await createSmallGroupVote(id, request);
      setVoteRetry((value) => value + 1);
      setIsVoteCreateSuccessModalOpen(true);
    } catch (error) {
      // AI작성: 생성 한도 오류 시 서버 기준 목록을 다시 읽고, 입력값과 오류 안내는 생성 모달에 유지한다.
      if (error.code === 'SMALL_GROUP_OPEN_VOTE_LIMIT_EXCEEDED') {
        setVoteRetry((value) => value + 1);
      }
      throw error;
    } finally {
      setIsVoteCreating(false);
    }
  };

  const errorCode = current?.error?.code;
  const errorMessage = errorCode === 'NOT_FOUND' ? '조회할 수 있는 소모임이 없습니다.'
    : errorCode === 'ACCESS_DENIED' ? '이 소모임에 접근할 수 없습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(errorCode) ? '로그인이 필요합니다.'
        : current?.error?.message || '소모임 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  const group = current?.group ? toSmallGroupDetailView(current.group) : null;
  const currentSettings = settingsResult?.id === id && settingsResult?.retry === settingsRetry
    ? settingsResult : null;
  const settingsErrorCode = currentSettings?.error?.code;
  const settingsErrorMessage = settingsErrorCode === 'SMALL_GROUP_SETTINGS_FORBIDDEN'
    ? '소모임 개설자만 설정을 조회할 수 있습니다.'
    : settingsErrorCode === 'SMALL_GROUP_NOT_FOUND'
      ? '조회할 수 있는 소모임이 없습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(settingsErrorCode)
        ? '로그인이 필요합니다.'
        : currentSettings?.error?.message || '소모임 설정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  const currentVotes = voteResult?.id === id && voteResult?.retry === voteRetry ? voteResult : null;
  const voteErrorCode = currentVotes?.error?.code;
  const voteErrorMessage = voteErrorCode === 'SMALL_GROUP_VOTE_LIST_FORBIDDEN'
    ? '승인된 소모임 회원만 투표 목록을 조회할 수 있습니다.'
    : voteErrorCode === 'SMALL_GROUP_NOT_FOUND'
      ? '조회할 수 있는 소모임이 없습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(voteErrorCode)
        ? '로그인이 필요합니다.'
        : currentVotes?.error?.message || '투표 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록 &gt; 소모임 메인</Breadcrumb>
      <Content>
        {!current ? <EmptyCard role="status">소모임 정보를 불러오는 중입니다.</EmptyCard>
          : current.error ? (
            <EmptyCard role="alert">
              <p>{errorMessage}</p>
              {!['NOT_FOUND', 'ACCESS_DENIED', 'UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(errorCode) && (
                <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
              )}
              <Button type="button" $variant="secondary" onClick={goToList}>
                소모임 목록으로
              </Button>
            </EmptyCard>
          ) : (
            <SmallGroupDetail
              key={`${id}:${authVersion}`}
              group={group}
              groupId={current.group.groupId}
              smallGroupId={current.group.smallGroupId}
              // AI작성: 장소 카드 아래 목록 버튼에서도 기존 검색·페이지 복원 함수를 재사용한다.
              onBack={goToList}
              myRole={current.group.myRole}
              activeMenu={menu.id === id ? menu.value : 'notice'}
              settings={currentSettings?.settings}
              settingsCategories={currentSettings?.categories || []}
              isSettingsLoading={isSettingsSelected && (!currentSettings || currentSettings.loading)}
              isSettingsSaving={isSettingsSaving}
              settingsErrorMessage={currentSettings?.error ? settingsErrorMessage : null}
              votes={currentVotes?.votes || []}
              isVoteLoading={isVoteSelected && (!currentVotes || currentVotes.loading)}
              voteErrorMessage={currentVotes?.error ? voteErrorMessage : null}
              onMenuChange={(value) => {
                // AI작성: 변경 도중 다른 탭으로 이탈했어도 공지 재진입 시 서버 상태를 다시 확인한다.
                setMenu({ id, value });
                if (value === 'notice') setRetry((previous) => previous + 1);
              }}
              onSettingsSubmit={handleSettingsSubmit}
              onSettingsCancel={() => setMenu({ id, value: 'notice' })}
              onRetrySettings={() => setSettingsRetry((value) => value + 1)}
              onDelete={() => setIsDeleteModalOpen(true)}
              onJoinRequest={handleJoinRequest}
              isJoinSubmitting={isJoinSubmitting}
              onCreateVote={handleVoteCreate}
              isVoteCreating={isVoteCreating}
              onRetryVotes={() => setVoteRetry((value) => value + 1)}
              onNoticeDetail={(noticeId) => navigate('/small-groups/' + encodeURIComponent(id) + '/notices/' + encodeURIComponent(noticeId) + '?' + toSmallGroupNoticeSearch(current.group.noticePageMeta?.page || noticeQuery.page, noticeQuery.sort))}
              noticePageMeta={current.group.noticePageMeta}
              noticeSort={noticeQuery.sort}
              onNoticeQueryChange={(page, sort) => navigate({ pathname: location.pathname, search: toSmallGroupNoticeSearch(page, sort, location.search) })}
              onNoticeRefresh={async ({ resetNoticeQuery = false } = {}) => {
                // AI작성: 작성 성공은 최신순 첫 페이지로 이동하고 재조회 실패 시 등록을 반복하지 않는다.
                const query = resetNoticeQuery ? { page: 1, sort: 'LATEST' } : noticeQuery;
                const version = ++detailRequest.current;
                const data = await getSmallGroupDetail(id, { noticePage: query.page, noticeSort: query.sort });
                if (version !== detailRequest.current) return;
                const search = '?' + toSmallGroupNoticeSearch(data.noticePageMeta?.page || query.page, query.sort, location.search);
                if (search !== location.search) navigate({ pathname: location.pathname, search }, { replace: true });
                else setResult({ id, retry, authVersion, search: location.search, group: data });
              }}
              onVoteDetail={(smallGroupVoteId) => navigate(`/small-groups/${id}/votes/${smallGroupVoteId}`)}
            />
          )}
      </Content>
      {isDeleteModalOpen && group && (
        <ConfirmModal
          title="소모임 삭제"
          description={`${group.name} 소모임을 삭제하시겠어요?`}
          confirmLabel="소모임 삭제"
          confirmVariant="danger"
          cancelFirst
          onConfirm={handleDeleteConfirm}
          onCancel={() => setIsDeleteModalOpen(false)}
        />
      )}
      {isJoinModalOpen && (
        <ConfirmModal
          title="가입 신청 완료"
          description="가입 신청이 접수되었습니다. 모임 운영자의 심사를 기다려 주세요."
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setJoinState(null)}
          onCancel={() => setJoinState(null)}
        />
      )}
      {isVoteCreateSuccessModalOpen && (
        <ConfirmModal
          title="투표 생성 완료"
          description="투표가 생성되었습니다."
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setIsVoteCreateSuccessModalOpen(false)}
          onCancel={() => setIsVoteCreateSuccessModalOpen(false)}
        />
      )}
      {isIdentityVerificationModalOpen && (
        <ConfirmModal
          title="본인 인증 필요"
          description="본인 인증 완료 후 신청이 가능합니다."
          confirmLabel="인증하러 가기"
          cancelLabel="닫기"
          cancelFirst
          onConfirm={() => navigate('/member/verify')}
          onCancel={() => setJoinState(null)}
        />
      )}
      {joinErrorMessage && (
        <ConfirmModal
          title="가입 신청 실패"
          description={joinErrorMessage}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setJoinState(null)}
          onCancel={() => setJoinState(null)}
        />
      )}
    </Page>
  );
}

export default SmallGroupDetailPage;
