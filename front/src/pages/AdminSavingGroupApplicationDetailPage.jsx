import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SavingGroupApplicationDetail from '../components/admin/SavingGroupApplicationDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import {
  approveSavingGroupCreationApplication,
  getSavingGroupCreationApplication,
  rejectSavingGroupCreationApplication,
} from '../api/savingGroups';

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
const MessageCard = styled.section`
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
  const [application, setApplication] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getSavingGroupCreationApplication(id, { signal: controller.signal })
      .then(setApplication)
      .catch((failure) => {
        if (failure.code !== 'ERR_CANCELED') setError(failure.message);
      })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [id]);

  const handleProcess = async (type, rejectReason) => {
    setError('');
    setIsProcessing(true);
    try {
      if (type === 'approve') await approveSavingGroupCreationApplication(id);
      else await rejectSavingGroupCreationApplication(id, rejectReason);
      setApplication((current) => ({
        ...current,
        status: type === 'approve' ? 'APPROVED' : 'REJECTED',
        rejectReason: rejectReason || null,
      }));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleMenuSelect = (menuId) => {
    const paths = {
      members: '/admin/members',
      gatherings: '/admin/saving-groups',
      'gathering-review': '/admin/saving-groups/applications',
      groups: '/admin/small-groups',
      posts: '/admin/posts',
      comments: '/admin/comments',
      reports: '/admin/reposts',
    };
    if (paths[menuId]) navigate(paths[menuId]);
  };

  return (
    <Page>
      <Heading>
        <h1>계모임 개설 심사 상세</h1>
        <p>개설 신청 내용을 확인하고 심사 처리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gathering-review" onSelect={handleMenuSelect} />
        <main>
          {loading ? (
            <p role="status">심사 상세를 불러오는 중입니다.</p>
          ) : error && !application ? (
            <MessageCard>
              <p>{error}</p>
              <Button type="button" $variant="secondary" onClick={() => navigate('/admin/saving-groups/applications')}>
                심사 목록으로
              </Button>
            </MessageCard>
          ) : application ? (
            <>
              {error && <p role="alert">{error}</p>}
              <SavingGroupApplicationDetail
                application={application}
                isProcessing={isProcessing}
                onApprove={() => handleProcess('approve')}
                onReject={(reason) => handleProcess('reject', reason)}
                onBack={() => navigate('/admin/saving-groups/applications')}
              />
            </>
          ) : null}
        </main>
      </Grid>
    </Page>
  );
}

export default AdminSavingGroupApplicationDetailPage;
