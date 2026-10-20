import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import AdminMemberDetail from '../components/admin/AdminMemberDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMemberDetails, adminMembers, adminMenuItems } from '../mocks/admin';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;

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
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
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

function AdminMemberDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const member = adminMembers.find((item) => item.id === Number(id));
  const detail = adminMemberDetails[id];
  const [action, setAction] = useState(member?.status === '제재' ? '제한 해제' : '이용 제한');
  const [reason, setReason] = useState('');

  const handleMenuSelect = (menuId) => {
    if (menuId === 'gatherings') {
      navigate('/admin/saving-groups');
      return;
    }

    if (menuId === 'gathering-review') {
      navigate('/admin/saving-groups/applications');
      return;
    }

    if (menuId === 'groups') {
      navigate('/admin/small-groups');
      return;
    }

    if (menuId === 'posts') {
      navigate('/admin/posts');
      return;
    }

    if (menuId === 'comments') {
      navigate('/admin/comments');
      return;
    }

    if (menuId === 'reports') {
      navigate('/admin/reposts');
      return;
    }

    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  const handleStatusSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 회원 이용 제한·제한 해제 API 요청 후 회원 상세 정보를 갱신합니다.
  };

  if (!member || !detail) {
    return (
      <Page>
        <EmptyCard>
          <p>존재하지 않는 회원입니다.</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/admin/members')}>
            회원 목록으로
          </Button>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Heading>
        <h1>회원 상세</h1>
        <p>{member.name} 회원의 정보와 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="members" onSelect={handleMenuSelect} />
        <main>
          <AdminMemberDetail
            member={member}
            detail={detail}
            action={action}
            reason={reason}
            onActionChange={setAction}
            onReasonChange={setReason}
            onSubmit={handleStatusSubmit}
            onBack={() => navigate('/admin/members')}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminMemberDetailPage;
