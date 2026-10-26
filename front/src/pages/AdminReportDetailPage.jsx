import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportDetail from '../components/admin/AdminReportDetail';
import Badge from '../components/common/Badge';
import Button from '../components/common/Button';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminReportDetails, adminReports } from '../mocks/adminReports';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

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

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

const AdminBadge = styled(Badge)`
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.border};
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

function AdminReportDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  // TODO: Spring Boot 관리자 회원 신고 상세 조회 API로 mock 데이터를 대체합니다.
  const report = adminReports.find((item) => item.id === Number(id));
  const detail = adminReportDetails[id];
  const [status, setStatus] = useState(report?.status || '대기');
  const [selectedStatus, setSelectedStatus] = useState(report?.status || '대기');

  const handleMenuSelect = (menuId) => {
    const routes = {
      members: '/admin/members',
      gatherings: '/admin/saving-groups',
      'gathering-review': '/admin/saving-groups/applications',
      groups: '/admin/small-groups',
      posts: '/admin/posts',
      comments: '/admin/comments',
      reports: '/admin/reposts',
    };

    if (routes[menuId]) {
      navigate(routes[menuId]);
    }
  };

  const handleSaveStatus = () => {
    // TODO: Spring Boot 관리자 회원 신고 처리 상태 변경 API 요청 후 최신 신고 상태를 반영합니다.
    setStatus(selectedStatus);
  };

  if (!report || !detail) {
    return (
      <Page>
        <EmptyCard>
          <p>존재하지 않는 회원 신고입니다.</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/admin/reposts')}>
            신고 목록으로
          </Button>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Heading>
        <div>
          <h1>회원 신고 상세</h1>
          <p>접수된 회원 신고의 상세 내용과 처리 상태를 확인하세요.</p>
        </div>
        <AdminBadge>관리자 전용</AdminBadge>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="reports" onSelect={handleMenuSelect} />
        <main>
          <AdminReportDetail
            report={report}
            detail={detail}
            status={status}
            selectedStatus={selectedStatus}
            onStatusSelect={setSelectedStatus}
            onSaveStatus={handleSaveStatus}
            onBack={() => navigate('/admin/reposts')}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminReportDetailPage;
