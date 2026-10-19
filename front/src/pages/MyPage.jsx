import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import ConfirmModal from '../components/common/ConfirmModal';
import MemberSidebar from '../components/layout/MemberSidebar';
import MeetingList from '../components/member/MeetingList';
import ProfileSummary from '../components/member/ProfileSummary';
import QuickLinks from '../components/member/QuickLinks';
import PasswordInput from '../components/common/PasswordInput';
import { memberMenuItems, memberSummary, participatingMeetings, quickLinks } from '../mocks/mypage';

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
  grid-template-columns: 200px minmax(0, 1fr) 210px;
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

function MyPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('overview');
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [password, setPassword] = useState('');

  const handlePendingAction = () => {
    // TODO: Spring Boot API 및 화면 연결 후 선택한 마이페이지 기능으로 이동합니다.
  };

  const handlePasswordConfirm = () => {
    // TODO: 확인 버튼 클릭 시 Spring Boot 비밀번호 확인 API 요청 후 성공하면 내 정보 수정 화면으로 이동합니다.
  };

  const handlePasswordCancel = () => {
    setPassword('');
    setIsPasswordVisible(false);
    setIsPasswordModalOpen(false);
  };

  return (
    <Page>
      <Heading>
        <h1>마이페이지</h1>
        <p>내 정보와 참여 중인 모임을 한눈에 확인하세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          <ProfileSummary profile={memberSummary} onEdit={() => setIsPasswordModalOpen(true)} />
          <MeetingList meetings={participatingMeetings} onView={handlePendingAction} />
        </main>
        <QuickLinks links={quickLinks} onSelect={handlePendingAction} />
      </Grid>
      {isPasswordModalOpen && (
        <ConfirmModal
          title="비밀번호 확인"
          description="회원 정보를 수정하려면 비밀번호를 입력해주세요."
          onConfirm={handlePasswordConfirm}
          onCancel={handlePasswordCancel}
        >
          <PasswordInput
            id="mypage-password-confirm"
            label="비밀번호"
            placeholder="비밀번호를 입력해주세요"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            isVisible={isPasswordVisible}
            onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
          />
        </ConfirmModal>
      )}
    </Page>
  );
}

export default MyPage;
