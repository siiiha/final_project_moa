import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportFilterBar from '../components/admin/AdminReportFilterBar';
import AdminReportTable from '../components/admin/AdminReportTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { getAdminReports } from '../api/adminReports';
import { adminMenuItems } from '../mocks/admin';

const PAGE_SIZE = 10;
const Page = styled.div`width: min(100% - 32px, ${({ theme }) => theme.maxWidth}); margin: 0 auto; padding: 26px 0 48px;`;
const Heading = styled.div`margin-left: 218px; h1 { font-size: 26px; } p { font-size: 12px; } @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { margin-left: 0; }`;
const Grid = styled.div`margin-top: 22px; display: grid; grid-template-columns: 202px minmax(0, 1fr); gap: 16px; @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { grid-template-columns: 1fr; }`;

// AI작성: 관리자 신고 목록은 서버의 검색·필터·페이징 결과만 표시한다.
function AdminReportsPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [searchType, setSearchType] = useState('reportedMember');
  const [reportType, setReportType] = useState('');
  const [status, setStatus] = useState('');
  const [reports, setReports] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    async function loadReports() {
      setLoading(true); setErrorMessage('');
      try {
        const result = await getAdminReports({ status, reportType, searchType, keyword: submittedQuery, page: currentPage, size: PAGE_SIZE, signal: controller.signal });
        setReports(result.items); setPageMeta(result.meta);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') { setReports([]); setPageMeta({ total: 0, totalPages: 0 }); setErrorMessage(error.message); }
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    loadReports();
    return () => controller.abort();
  }, [currentPage, reportType, searchType, status, submittedQuery]);

  const handleSearch = (event) => { event.preventDefault(); setSubmittedQuery(query); setCurrentPage(1); };
  return <Page><Heading><h1>회원 신고</h1><p>접수된 신고 사건을 조회하고 처리 현황을 확인하세요.</p></Heading><Grid>
    <AdminSidebar items={adminMenuItems} activeItem="reports" />
    <main>
    <AdminReportFilterBar query={query} searchType={searchType} reportType={reportType} status={status} onQueryChange={setQuery} onSearchTypeChange={(value) => { setSearchType(value); setCurrentPage(1); }} onReportTypeChange={(value) => { setReportType(value); setCurrentPage(1); }} onStatusChange={(value) => { setStatus(value); setCurrentPage(1); }} onSearch={handleSearch} />
    {errorMessage && <p role="alert">{errorMessage}</p>}
    <AdminReportTable reports={reports} totalCount={pageMeta.total} currentPage={currentPage} totalPages={pageMeta.totalPages} onPageChange={setCurrentPage} onViewReport={(caseId) => navigate(`/admin/reposts/${caseId}`)} loading={loading} />
    </main></Grid></Page>;
}

export default AdminReportsPage;
