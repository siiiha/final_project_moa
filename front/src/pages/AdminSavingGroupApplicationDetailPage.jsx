import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SavingGroupApplicationDetail from '../components/admin/SavingGroupApplicationDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminSavingGroupApplications, adminSavingGroupDeletionDetails } from '../mocks/adminSavingGroupApplications';

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

function AdminSavingGroupApplicationDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const application = adminSavingGroupApplications.find((item) => item.id === Number(id));
  const detail = adminSavingGroupDeletionDetails[id];
  const [status, setStatus] = useState(application?.status || '대기');

  const handleMenuSelect = (menuId) => {
    if (menuId === 'members') {
      navigate('/admin/members');
      return;
    }

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

  const handleDeleteApprove = () => {
    // TODO: Spring Boot 계모임 삭제 승인 API 요청 후 신청 상태를 갱신합니다.
    setStatus('승인');
  };

  const handleReject = () => {
    // TODO: Spring Boot 계모임 삭제 반려 API 요청 후 신청 상태를 갱신합니다.
    setStatus('반려');
  };

  if (!application || !detail) {
    return (
      <Page>
        <EmptyCard>
          <p>존재하지 않는 계모임 삭제 신청입니다.</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/admin/saving-groups/applications')}>
            심사 목록으로
          </Button>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Heading>
        <h1>계모임 삭제 심사 상세</h1>
        <p>{application.name} 삭제 신청의 상세 내용을 확인하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gathering-review" onSelect={handleMenuSelect} />
        <main>
          <SavingGroupApplicationDetail
            application={application}
            detail={detail}
            status={status}
            onDeleteApprove={handleDeleteApprove}
            onReject={handleReject}
            onBack={() => navigate('/admin/saving-groups/applications')}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminSavingGroupApplicationDetailPage;
