import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportDetail from '../components/admin/AdminReportDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { getAdminReport, processAdminReport } from '../api/adminReports';
import { adminMenuItems } from '../mocks/admin';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;
  h1 { color: ${({ theme }) => theme.colors.text}; font-size: 26px; font-weight: 800; letter-spacing: -1.35px; }
  p { margin-top: 5px; color: ${({ theme }) => theme.colors.muted}; font-size: 12px; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { margin-left: 0; }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { grid-template-columns: 1fr; }
`;

const Message = styled.p`
  padding: 24px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
`;

// AI작성: 신고 상세를 서버에서 재조회해 중복 처리와 오래된 Mock 상태를 방지한다.
function AdminReportDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [action, setAction] = useState('CONFIRMED');
  const [restrictionType, setRestrictionType] = useState('7_DAYS');
  const [processReason, setProcessReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadReport() {
      setLoading(true);
      setError('');
      try {
        const result = await getAdminReport(id, controller.signal);
        setReport(result.report);
      } catch (failure) {
        if (failure.code !== 'ERR_CANCELED') setError(failure.message);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadReport();
    return () => controller.abort();
  }, [id, reloadKey]);

  // AI작성: 처리 완료 뒤 서버 데이터를 다시 조회해 처리 상태와 제한 기간을 최신 값으로 표시한다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!report?.processId || submitting || !processReason.trim()) return;

    setSubmitting(true);
    setError('');
    try {
      await processAdminReport(report.processId, {
        status: action,
        restrictionType: action === 'CONFIRMED' ? restrictionType : undefined,
        processReason: processReason.trim(),
      });
      setProcessReason('');
      setReloadKey((value) => value + 1);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSubmitting(false);
    }
  };

  let content = <Message role="status">신고 상세를 불러오는 중입니다.</Message>;
  if (!loading && !report) content = <Message role="alert">{error || '신고 사건을 찾을 수 없습니다.'}</Message>;
  if (!loading && report) {
    content = <>
      {error && <Message role="alert">{error}</Message>}
      <AdminReportDetail report={report} action={action} restrictionType={restrictionType} processReason={processReason} submitting={submitting} onActionChange={setAction} onRestrictionTypeChange={setRestrictionType} onProcessReasonChange={setProcessReason} onSubmit={handleSubmit} onBack={() => navigate('/admin/reposts')} />
    </>;
  }

  return (
    <Page>
      <Heading><h1>회원 신고 상세</h1><p>신고 내용을 검토하고 처리 결과를 기록하세요.</p></Heading>
      <Grid><AdminSidebar items={adminMenuItems} activeItem="reports" /><main>{content}</main></Grid>
    </Page>
  );
}
export default AdminReportDetailPage;
