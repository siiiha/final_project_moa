import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupDetail from '../components/savingGroup/SavingGroupDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import useAuthStore from '../store/authStore';
import { getSavingGroupDetail, requestSavingGroupJoin } from '../api/savingGroups.js';

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

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSavingGroupDetail(id, { signal: controller.signal })
      .then((group) => {
        if (active) setResult({ id, retry, authVersion, group });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ id, retry, authVersion, error });
      });
    return () => { active = false; controller.abort(); };
  }, [id, retry, authVersion]);

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

  const current = result?.id === id && result?.retry === retry && result?.authVersion === authVersion ? result : null;
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
        {!current ? <EmptyCard role="status">계모임 정보를 불러오는 중입니다.</EmptyCard>
          : current.error ? (
            <EmptyCard role="alert">
              <p>{errorMessage}</p>
              {(!errorStatus || errorStatus >= 500) && (
                <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
              )}
              <Button type="button" $variant="secondary" onClick={() => navigate('/saving-groups')}>계모임 목록으로</Button>
            </EmptyCard>
          ) : (
            <SavingGroupDetail key={id} group={current.group}
              activeMenu={menu.id === id ? menu.value : 'notice'}
              onMenuChange={(value) => setMenu({ id, value })}
              onJoinRequest={handleJoinRequest}
              isJoinSubmitting={isJoinSubmitting}
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
