import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportDetail from '../components/admin/AdminReportDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { getAdminReport, processAdminReport } from '../api/adminReports';
import { adminMenuItems } from '../mocks/admin';

const Page = styled.div`width: min(100% - 32px, ${({ theme }) => theme.maxWidth}); margin: 0 auto; padding: 26px 0 48px;`;
const Grid = styled.div`margin-top: 22px; display: grid; grid-template-columns: 202px minmax(0, 1fr); gap: 16px;`;

// AI작성: 신고 상세를 서버에서 재조회해 중복 처리와 오래된 Mock 상태를 방지한다.
function AdminReportDetailPage() {
  const navigate = useNavigate(); const { id } = useParams();
  const [report, setReport] = useState(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const [action, setAction] = useState('CONFIRMED'); const [restrictionType, setRestrictionType] = useState('7_DAYS'); const [processReason, setProcessReason] = useState(''); const [submitting, setSubmitting] = useState(false); const [reloadKey, setReloadKey] = useState(0);
  useEffect(() => { const controller = new AbortController(); getAdminReport(id, controller.signal).then((result) => setReport(result.report)).catch((failure) => { if (failure.code !== 'ERR_CANCELED') setError(failure.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); }); return () => controller.abort(); }, [id, reloadKey]);
  const handleSubmit = async (event) => { event.preventDefault(); if (!report?.processId || submitting || !processReason.trim()) return; setSubmitting(true); setError(''); try { await processAdminReport(report.processId, { status: action, restrictionType: action === 'CONFIRMED' ? restrictionType : undefined, processReason: processReason.trim() }); setProcessReason(''); setLoading(true); setReloadKey((value) => value + 1); } catch (failure) { setError(failure.message); } finally { setSubmitting(false); } };
  return <Page><h1>회원 신고 상세</h1><Grid><AdminSidebar items={adminMenuItems} activeItem="reports" /><main>{loading ? <p role="status">신고 상세를 불러오는 중입니다.</p> : !report ? <p role="alert">{error || '신고 사건을 찾을 수 없습니다.'}</p> : <><>{error && <p role="alert">{error}</p>}</><AdminReportDetail report={report} action={action} restrictionType={restrictionType} processReason={processReason} submitting={submitting} onActionChange={setAction} onRestrictionTypeChange={setRestrictionType} onProcessReasonChange={setProcessReason} onSubmit={handleSubmit} onBack={() => navigate('/admin/reposts')} /></>}</main></Grid></Page>;
}
export default AdminReportDetailPage;
