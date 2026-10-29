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
// AI작성: 관리자 처리 결과를 성공·실패에 따라 구분하되 기존 테마 색상과 여백을 재사용한다.
const FeedbackMessage = styled.p`
  margin-bottom: 12px;
  padding: 11px 13px;
  color: ${({ $error, theme }) => ($error ? theme.colors.danger : theme.colors.primary)};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ $error, theme }) => ($error ? theme.colors.danger : theme.colors.primary)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

function AdminSavingGroupApplicationDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [application, setApplication] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
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
    setSuccessMessage('');
    setIsProcessing(true);
    try {
      if (type === 'approve') await approveSavingGroupCreationApplication(id);
      else await rejectSavingGroupCreationApplication(id, rejectReason);
      // AI작성: 처리 결과를 프론트에서 추정하지 않고 상세 API를 다시 호출해 DB의 최종 상태를 표시한다.
      try {
        const refreshedApplication = await getSavingGroupCreationApplication(id);
        setApplication(refreshedApplication);
        // AI작성: 서버 최종 상태 재조회까지 성공한 경우에만 관리자에게 처리 완료를 안내한다.
        setSuccessMessage(type === 'approve' ? '심사 신청을 승인했습니다.' : '심사 신청을 반려했습니다.');
      } catch {
        // AI작성: 처리는 성공했지만 재조회가 실패하면 이전 PENDING 화면에서 중복 처리하지 못하도록 숨긴다.
        setApplication(null);
        throw new Error('심사 처리는 완료되었지만 최신 정보를 불러오지 못했습니다. 목록에서 다시 확인해 주세요.');
      }
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
        <h1>계모임 심사 상세</h1>
        <p>개설 또는 삭제 신청 내용을 확인하고 심사 처리하세요.</p>
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
              {error && (
                <FeedbackMessage $error role="alert">
                  {error}
                </FeedbackMessage>
              )}
              {successMessage && <FeedbackMessage role="status">{successMessage}</FeedbackMessage>}
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
