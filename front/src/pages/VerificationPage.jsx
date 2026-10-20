import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import VerificationStatusList from '../components/member/VerificationStatusList';
import { memberMenuItems, verificationItems } from '../mocks/mypage';

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

function VerificationPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('verify');

  const handleVerificationAction = () => {
    // TODO: Spring Boot 본인 인증 및 지역 인증 상태 조회·인증 요청 API를 연결합니다.
  };

  return (
    <Page>
      <Heading>
        <h1>인증관리</h1>
        <p>인증 상태를 확인하고 필요한 인증을 진행하세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          <VerificationStatusList items={verificationItems} onAction={handleVerificationAction} />
        </main>
      </Grid>
    </Page>
  );
}

export default VerificationPage;
