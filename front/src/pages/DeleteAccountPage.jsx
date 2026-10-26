import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Checkbox from '../components/common/Checkbox';
import ConfirmModal from '../components/common/ConfirmModal';
import PasswordInput from '../components/common/PasswordInput';
import MemberSidebar from '../components/layout/MemberSidebar';
import WithdrawalPanel from '../components/member/WithdrawalPanel';
import { memberMenuItems, withdrawalStatus } from '../mocks/mypage';

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

function DeleteAccountPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isAgreed, setIsAgreed] = useState(false);

  const closeModal = () => {
    setIsModalOpen(false);
    setPassword('');
    setIsPasswordVisible(false);
    setIsAgreed(false);
  };

  const handleWithdrawalConfirm = () => {
    // TODO: Spring Boot 회원 탈퇴 API 요청이 성공하면 인증 상태를 정리한 뒤 로그인 전 메인 화면으로 이동합니다.
    navigate('/');
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
          confirmLabel="회원탈퇴"
          cancelLabel="취소"
          confirmVariant="danger"
          onConfirm={handleWithdrawalConfirm}
          onCancel={closeModal}
        >
          <ModalNotice>현재 계모임 0개 · 미정산 금액 0원 · 탈퇴 가능</ModalNotice>
          <WarningTitle>탈퇴하기 전에 아래 내용을 확인해주세요.</WarningTitle>
          <WarningList>
            <li>탈퇴 중인 계모임과 종료되어야 탈퇴할 수 있습니다.</li>
            <li>탈퇴 시 참여 중인 모임 및 관련 서비스 이용 정보가 삭제됩니다.</li>
            <li>작성한 게시글과 댓글은 운영 정책에 따라 보관될 수 있습니다.</li>
            <li>탈퇴 후 30일 이내에는 계정을 복구할 수 있습니다.</li>
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
