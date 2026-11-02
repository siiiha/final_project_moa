import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Checkbox from '../components/common/Checkbox';
import ConfirmModal from '../components/common/ConfirmModal';
import PasswordInput from '../components/common/PasswordInput';
import MemberSidebar from '../components/layout/MemberSidebar';
import WithdrawalPanel from '../components/member/WithdrawalPanel';
import { memberMenuItems, withdrawalStatus } from '../mocks/mypage';
import { withdrawMyAccount } from '../api/member';
import useAuthStore from '../store/authStore';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 216px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const ModalNotice = styled.div`
  padding: 10px 12px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

const WarningTitle = styled.p`
  margin-top: 16px;
  color: ${({ theme }) => theme.colors.danger};
  font-size: 12px;
  font-weight: 700;
`;

const WarningList = styled.ul`
  margin: 9px 0 18px;
  padding-left: 17px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  line-height: 1.8;
`;

const Agreement = styled.div`
  padding-top: 14px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

// AI작성: 탈퇴 요청 오류를 확인창 안에 표시합니다.
const ErrorNotice = styled.p`
  margin: 12px 0;
  padding: 12px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
  line-height: 1.6;
`;

function DeleteAccountPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isAgreed, setIsAgreed] = useState(false);

  // AI작성: 로그인 토큰을 사용하고, 탈퇴 성공 시 로그인 정보를 정리합니다.
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);

  // AI작성: 요청 중복을 방지하고 처리 상태와 오류를 화면에 표시합니다.
  const submittingRef = useRef(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  // AI작성: 처리 중에는 확인창을 유지하고, 닫을 때 입력값을 초기화합니다.
  const closeModal = () => {
    if (submittingRef.current) return;

    setIsModalOpen(false);
    setPassword('');
    setIsPasswordVisible(false);
    setIsAgreed(false);
    setErrorMessage('');
  };

  const handleWithdrawalConfirm = async () => {
    if (submittingRef.current) return;

    setErrorMessage('');

    // AI작성: 서버 요청 전에 로그인 상태와 필수 입력값을 확인합니다.
    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    if (!password.trim()) {
      setErrorMessage('현재 비밀번호를 입력해주세요.');
      return;
    }

    if (!isAgreed) {
      setErrorMessage('회원탈퇴 안내사항에 동의해주세요.');
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);

    try {
      await withdrawMyAccount(token, {
        currentPassword: password,
        agreed: isAgreed,
      });

      // AI작성: 탈퇴 성공 후 로그인 정보를 지우고 메인 화면으로 이동합니다.
      clearToken();
      navigate('/', { replace: true });
    } catch (error) {
      // AI작성: 만료되거나 사용할 수 없는 토큰은 정리하고 로그인을 안내합니다.
      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      const errorCode = error.response?.data?.error?.code;

      if (errorCode === 'INVALID_CURRENT_PASSWORD') {
        setErrorMessage('비밀번호가 맞지 않아요. 다시 확인해주세요.');
      } else if (errorCode === 'WITHDRAWAL_BLOCKED_BY_GROUP') {
        setErrorMessage('진행 중이거나 정지된 계모임이 있어요. 해당 모임이 종료된 후 탈퇴할 수 있습니다.');
      } else {
        setErrorMessage(
          error.response?.data?.error?.message ?? '회원탈퇴를 처리하지 못했습니다. 잠시 후 다시 시도해주세요.'
        );
      }
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  };

  return (
    <Page>
      <Heading>
        <h1>회원탈퇴</h1>
        <p>탈퇴 전 확인사항과 계정 상태를 확인해주세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
          isWithdrawActive
        />
        <main>
          <WithdrawalPanel status={withdrawalStatus} onRequestWithdrawal={() => setIsModalOpen(true)} />
        </main>
      </Grid>
      {isModalOpen && (
        <ConfirmModal
          title="회원탈퇴 확인"
          confirmLabel={isSubmitting ? '처리 중...' : '회원탈퇴'}
          isBusy={isSubmitting}
          cancelLabel="취소"
          confirmVariant="danger"
          onConfirm={handleWithdrawalConfirm}
          onCancel={closeModal}
        >
          <ModalNotice>진행 중이거나 정지된 계모임이 있으면 탈퇴할 수 없습니다.</ModalNotice>
          <WarningTitle>탈퇴하기 전에 아래 내용을 확인해주세요.</WarningTitle>
          <WarningList>
            <li>탈퇴하면 현재 계정으로 로그인하거나 서비스를 이용할 수 없습니다.</li>
            <li>작성한 게시글과 댓글은 탈퇴만으로 삭제되지 않습니다.</li>
            <li>아래 안내사항에 동의하고 현재 비밀번호를 입력해주세요.</li>
          </WarningList>
          <PasswordInput
            id="withdrawal-password"
            label="현재 비밀번호"
            placeholder="비밀번호를 입력해주세요"
            autoComplete="current-password"
            compact
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            isVisible={isPasswordVisible}
            onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
          />
          {errorMessage && <ErrorNotice role="alert">{errorMessage} </ErrorNotice>}
          <Agreement>
            <Checkbox
              id="withdrawal-agreement"
              label="안내사항을 확인했으며 회원탈퇴에 동의합니다."
              checked={isAgreed}
              onChange={(event) => setIsAgreed(event.target.checked)}
            />
          </Agreement>
        </ConfirmModal>
      )}
    </Page>
  );
}

export default DeleteAccountPage;
