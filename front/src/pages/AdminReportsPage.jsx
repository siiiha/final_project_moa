import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminReportFilterBar from '../components/admin/AdminReportFilterBar';
import AdminReportTable from '../components/admin/AdminReportTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { getAdminReports } from '../api/adminReports';
import { adminMenuItems } from '../mocks/admin';

const PAGE_SIZE = 10;

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
  const routes = { members: '/admin/members', gatherings: '/admin/saving-groups', 'gathering-review': '/admin/saving-groups/applications', groups: '/admin/small-groups', posts: '/admin/posts', comments: '/admin/comments', reports: '/admin/reposts' };

  return <main>
    <h1>회원 신고</h1>
    <AdminSidebar items={adminMenuItems} activeItem="reports" onSelect={(menuId) => routes[menuId] && navigate(routes[menuId])} />
    <AdminReportFilterBar query={query} searchType={searchType} reportType={reportType} status={status} onQueryChange={setQuery} onSearchTypeChange={(value) => { setSearchType(value); setCurrentPage(1); }} onReportTypeChange={(value) => { setReportType(value); setCurrentPage(1); }} onStatusChange={(value) => { setStatus(value); setCurrentPage(1); }} onSearch={handleSearch} />
    {errorMessage && <p role="alert">{errorMessage}</p>}
    <AdminReportTable reports={reports} totalCount={pageMeta.total} currentPage={currentPage} totalPages={pageMeta.totalPages} onPageChange={setCurrentPage} onViewReport={(caseId) => navigate(`/admin/reposts/${caseId}`)} loading={loading} />
  </main>;
}

export default AdminReportsPage;
