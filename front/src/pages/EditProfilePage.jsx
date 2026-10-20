import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import EditProfileForm from '../components/member/EditProfileForm';
import { memberMenuItems } from '../mocks/mypage';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;
const Heading = styled.div`
  margin-left: 216px;
  h1 {
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
function EditProfilePage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('edit');

  return (
    <Page>
      <Heading>
        <h1>내 정보 수정</h1>
        <p>변경할 정보를 입력하고 저장해주세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          <EditProfileForm />
        </main>
      </Grid>
    </Page>
  );
}
export default EditProfilePage;
