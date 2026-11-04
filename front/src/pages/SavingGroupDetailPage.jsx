import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupDetail from '../components/savingGroup/SavingGroupDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import useAuthStore from '../store/authStore';
import { getSavingGroupDetail, requestSavingGroupJoin, readSavingGroupNoticeQuery, toSavingGroupNoticeSearch, updateSavingGroupNoticePinned } from '../api/savingGroups.js';

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

// AI작성: URL 변경 시 이전 데이터를 숨기고 취소된 요청이 새 화면을 덮어쓰지 못하게 한다.
function SavingGroupDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  // AI작성: 로그인 계정이 바뀌면 이전 계정의 신청 상태와 응답을 재사용하지 않는다.
  const authVersion = useAuthStore((state) => state.authVersion);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [menu, setMenu] = useState({ id, value: 'notice' });
  // AI작성: URL을 조회 조건의 기준으로 삼아 상세 복귀·뒤로 가기·새로고침 때 같은 조건을 복원한다.
  const [searchParams, setSearchParams] = useSearchParams();
  const { page: noticePage, sort: noticeSort } = readSavingGroupNoticeQuery(searchParams);
  const noticeRequest = useRef(null);
  // AI작성: 고정 변경은 전송 후 취소하지 않으며 중복 클릭과 다른 계정·모임에서의 응답 반영을 차단한다.
  const pinLock = useRef(null);
  const [pinState, setPinState] = useState(null);
  const currentPin = pinState?.id === id && pinState?.authVersion === authVersion ? pinState : null;

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
      noticeRequest.current?.abort();
      // AI작성: 다른 모임으로 갔다가 돌아와도 이전 완료·실패 안내가 다시 열리지 않게 한다.
      setJoinState(null);
    };
  }, [id, authVersion]);

  useEffect(() => {
    // AI작성: 작성 후 갱신을 포함해 먼저 시작한 조회를 취소하고 마지막 요청만 반영한다.
    noticeRequest.current?.abort();
    const controller = new AbortController();
    noticeRequest.current = controller;
    let active = true;
    getSavingGroupDetail(id, { signal: controller.signal, noticePage, noticeSort })
      .then((group) => {
        if (!active || controller.signal.aborted || noticeRequest.current !== controller) return;
        setResult({ id, retry, authVersion, noticePage, noticeSort, group });
        // AI작성: 공지 수가 줄어 현재 페이지가 사라지면 마지막 유효 페이지로 URL을 보정한다.
        const lastPage = Math.max(1, group.noticePageMeta?.totalPages ?? 0);
        if (noticePage > lastPage) setSearchParams((previous) =>
          toSavingGroupNoticeSearch(lastPage, noticeSort, previous), { replace: true, preventScrollReset: true });
      })
      .catch((error) => {
        // AI작성: 통신·서버 오류 시 같은 계모임 정보는 유지하고 공지 영역에서 재조회한다.
        if (active && !controller.signal.aborted && noticeRequest.current === controller) setResult((previous) => ({
          id, retry, authVersion, noticePage, noticeSort, error,
          group: (!error.response?.status || error.response.status >= 500)
            && previous?.id === id && previous?.authVersion === authVersion ? previous.group : null,
        }));
      });
    return () => { active = false; controller.abort(); };
  }, [id, retry, authVersion, noticePage, noticeSort, setSearchParams]);

  const handleJoinRequest = async () => {
    const scope = joinScope.current;
    if (!scope?.active || joinLock.current?.scope === scope || current?.group?.canApply !== true) return;

    const request = { scope };
    joinLock.current = request;
    setJoinState({ id, authVersion, status: 'submitting' });
    try {
      await requestSavingGroupJoin(current.group.savingGroupId);
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

  const scoped = result?.id === id && result?.authVersion === authVersion ? result : null;
  const current = scoped?.retry === retry && scoped.noticePage === noticePage && scoped.noticeSort === noticeSort ? scoped : null;
  const visibleGroup = scoped?.group;
  // AI작성: 등록 후 조회만 갱신해 실패 시 폼 상태를 보존하고 모임·계정 변경 뒤 이전 응답은 반영하지 않는다.
  const handleNoticeRefresh = async ({ resetNoticeQuery = false } = {}) => {
    const scope = joinScope.current;
    const page = resetNoticeQuery ? 1 : noticePage;
    const sort = resetNoticeQuery ? 'LATEST' : noticeSort;
    noticeRequest.current?.abort();
    const controller = new AbortController();
    noticeRequest.current = controller;
    try {
      const group = await getSavingGroupDetail(id, { noticePage: page, noticeSort: sort, signal: controller.signal });
      if (!scope?.active || joinScope.current !== scope || controller.signal.aborted || noticeRequest.current !== controller) return;
      setResult({ id, retry, authVersion, noticePage: page, noticeSort: sort, group });
      // AI작성: 등록 성공 시 새 공지가 보이도록 최신순 1페이지로 전환하며 조회 실패 시 재등록하지 않는다.
      const lastPage = Math.max(1, group.noticePageMeta?.totalPages ?? 0);
      setSearchParams((previous) => toSavingGroupNoticeSearch(Math.min(page, lastPage), sort, previous),
        { replace: true, preventScrollReset: true });
      return group;
    } catch (error) {
      if (scope?.active && joinScope.current === scope && !controller.signal.aborted && noticeRequest.current === controller) {
        setResult((previous) => ({ id, retry, authVersion, noticePage, noticeSort, error,
          group: (!error.response?.status || error.response.status >= 500)
            && previous?.id === id && previous?.authVersion === authVersion ? previous.group : null }));
      }
      throw error;
    }
  };
  // AI작성: 고정 변경 성공 뒤 조회 실패는 성공한 PATCH를 반복하지 않고 목록 재조회만 허용한다.
  const handleNoticePinChange = async (noticeId, pinned) => {
    const scope = joinScope.current;
    const group = current?.group;
    if (!scope?.active || pinLock.current?.scope === scope || currentPin?.needsRefresh || !group
      || group.membershipStatus !== 'APPROVED' || !['OWNER', 'MANAGER'].includes(group.myRole)
      || !['01', '02'].includes(group.status)) return;
    const lock = { scope };
    pinLock.current = lock;
    setPinState({ id, authVersion, phase: 'pending' });
    let changed = false;
    try {
      await updateSavingGroupNoticePinned(id, noticeId, pinned);
      changed = true;
      if (!scope.active) return;
      const refreshed = await handleNoticeRefresh();
      if (!scope.active) return;
      setPinState({ id, authVersion, phase: refreshed ? 'success' : 'error', needsRefresh: !refreshed,
        message: refreshed ? (pinned ? '고정 공지로 지정했습니다.' : '공지 고정을 해제했습니다.')
          : '고정 변경은 완료되었습니다. 목록을 새로 고침해 주세요.' });
    } catch (error) {
      if (scope.active) setPinState({ id, authVersion, phase: 'error', needsRefresh: true,
        message: changed ? '고정 변경은 완료되었습니다. 목록을 불러오지 못했습니다. 새로 고침해 주세요.'
          : error.message || '공지 고정을 변경하지 못했습니다. 목록을 새로 고침해 주세요.' });
    } finally {
      if (pinLock.current === lock) pinLock.current = null;
    }
  };
  const handleNoticeRetry = async () => {
    try {
      const refreshed = await handleNoticeRefresh();
      if (refreshed) setPinState(null);
    } catch { /* AI작성: 조회 오류는 기존 목록 오류 영역과 고정 안내를 유지한다. */ }
  };
  const errorStatus = current?.error?.response?.status;
  const errorMessage = errorStatus === 404 ? '조회할 수 있는 계모임이 없습니다.'
    : errorStatus === 403 ? '이 계모임에 접근할 수 없습니다.'
      : errorStatus === 401 ? '인증 정보를 확인해 주세요.'
        : errorStatus === 400 ? '계모임 주소가 올바르지 않습니다.'
          : '계모임 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';

  return (
    <Page>
      <Breadcrumb>계모임 &gt; 계모임 목록 &gt; 계모임 메인</Breadcrumb>
      <Content>
        {!visibleGroup && !current ? <EmptyCard role="status">계모임 정보를 불러오는 중입니다.</EmptyCard>
          : !visibleGroup && current?.error ? (
            <EmptyCard role="alert">
              <p>{errorMessage}</p>
              {(!errorStatus || errorStatus >= 500) && (
                <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
              )}
              <Button type="button" $variant="secondary" onClick={() => navigate('/saving-groups')}>계모임 목록으로</Button>
            </EmptyCard>
          ) : (
            <SavingGroupDetail key={`${id}-${authVersion}`} group={visibleGroup}
              activeMenu={menu.id === id ? menu.value : 'notice'}
              onMenuChange={(value) => setMenu({ id, value })}
              onJoinRequest={handleJoinRequest}
              isJoinSubmitting={isJoinSubmitting}
              onNoticeRefresh={handleNoticeRefresh}
              noticePagination={{ page: noticePage, sort: noticeSort, totalPages: visibleGroup.noticePageMeta?.totalPages ?? 0 }}
              noticeLoading={!current} noticeError={Boolean(current?.error)}
              onNoticePageChange={(page) => setSearchParams((previous) =>
                toSavingGroupNoticeSearch(page, noticeSort, previous), { preventScrollReset: true })}
              onNoticeSortChange={(sort) => setSearchParams((previous) =>
                toSavingGroupNoticeSearch(1, sort, previous), { preventScrollReset: true })}
              onNoticeRetry={handleNoticeRetry}
              noticePinState={currentPin} onNoticePinChange={handleNoticePinChange}
              onBack={() => navigate('/saving-groups')} />
          )}
      </Content>
      {isJoinModalOpen && (
        <ConfirmModal title="가입 신청 완료"
          description="가입 신청이 접수되었습니다. 모임 운영자의 심사를 기다려 주세요."
          confirmLabel="확인" showCancel={false}
          onConfirm={() => setJoinState(null)}
          onCancel={() => setJoinState(null)}
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

export default SavingGroupDetailPage;
