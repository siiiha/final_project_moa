import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportDetail from '../components/admin/AdminReportDetail';
import AdminPageLayout from '../components/layout/AdminPageLayout';
import { getAdminReport, processAdminReport } from '../api/adminReports';

const Message = styled.p`
  padding: 24px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
`;

function getRestrictionScopeForReportType(reportType) {
  if (['POST', 'COMMENT'].includes(reportType)) return 'CONTENT';
  if (reportType === 'CHAT_MESSAGE') return 'CHAT';
  return null;
}

// AI작성: 신고 상세를 서버에서 재조회해 중복 처리와 오래된 Mock 상태를 방지한다.
function AdminReportDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [action, setAction] = useState('CONFIRMED');
  const [restrictionScope, setRestrictionScope] = useState('CONTENT');
  const [restrictionType, setRestrictionType] = useState('7_DAYS');
  const [deleteTargetContent, setDeleteTargetContent] = useState(true);
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
        setRestrictionScope(getRestrictionScopeForReportType(result.report.typeCode) || '');
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
        // AI작성: 신고 유형과 무관한 기능 제한을 보내지 않도록 허용 범위가 있을 때만 제한값을 전송한다.
        restrictionScope: action === 'CONFIRMED' && restrictionScope ? restrictionScope : undefined,
        restrictionType: action === 'CONFIRMED' && restrictionScope ? restrictionType : undefined,
        // AI작성: 게시글·댓글 신고 승인일 때만 관리자가 선택한 콘텐츠 삭제 여부를 전송한다.
        deleteTargetContent: action === 'CONFIRMED'
          && ['POST', 'COMMENT'].includes(report.typeCode)
          ? deleteTargetContent
          : false,
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
      <AdminReportDetail report={report} action={action} restrictionScope={restrictionScope} restrictionType={restrictionType} deleteTargetContent={deleteTargetContent} processReason={processReason} submitting={submitting} onActionChange={setAction} onRestrictionTypeChange={setRestrictionType} onDeleteTargetContentChange={setDeleteTargetContent} onProcessReasonChange={setProcessReason} onSubmit={handleSubmit} onBack={() => navigate('/admin/reposts')} />
    </>;
  }

  return (
    <AdminPageLayout
      title="회원 신고 상세"
      description="신고 내용을 검토하고 처리 결과를 기록하세요."
      activeItem="reports"
    >
      {content}
    </AdminPageLayout>
  );
}
export default AdminReportDetailPage;
