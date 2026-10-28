import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminCommentFilterBar from '../components/admin/AdminCommentFilterBar';
import AdminCommentTable from '../components/admin/AdminCommentTable';
import Badge from '../components/common/Badge';
import AdminSidebar from '../components/layout/AdminSidebar';
import { getAdminComments } from '../api/adminComments';
import { adminMenuItems } from '../mocks/admin';

const PAGE_SIZE = 10;

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

const Content = styled.main`
  padding-top: 4px;
`;

const ErrorMessage = styled.p`
  margin-top: 16px;
  padding: 14px 16px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
`;

function AdminCommentsPage() {
  const navigate = useNavigate();
  const [comments, setComments] = useState([]);
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [region, setRegion] = useState('');
  const [status, setStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    async function loadComments() {
      setLoading(true);
      setErrorMessage('');
      try {
        const result = await getAdminComments({
          keyword: submittedQuery,
          regionName: region,
          status,
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setComments(result.items);
        setPageMeta(result.meta);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setComments([]);
          setPageMeta({ total: 0, totalPages: 0 });
          setErrorMessage(error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadComments();
    return () => controller.abort();
  }, [currentPage, region, status, submittedQuery]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

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

    if (menuId === 'reports') {
      navigate('/admin/reposts');
      return;
    }

    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  return (
    <Page>
      <Heading>
        <div>
          <h1>댓글 관리</h1>
          <p>검색과 상세 조회를 통해 서비스 운영 현황을 확인하세요.</p>
        </div>
        <AdminBadge>관리자 전용</AdminBadge>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="comments" onSelect={handleMenuSelect} />
        <Content>
          <AdminCommentFilterBar
            query={query}
            region={region}
            status={status}
            onQueryChange={setQuery}
            onRegionChange={(value) => {
              setRegion(value);
              setCurrentPage(1);
            }}
            onStatusChange={(value) => {
              setStatus(value);
              setCurrentPage(1);
            }}
            onSearch={handleSearch}
          />
          {errorMessage && <ErrorMessage role="alert">{errorMessage}</ErrorMessage>}
          <AdminCommentTable
            comments={comments}
            totalCount={pageMeta.total}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            loading={loading}
          />
        </Content>
      </Grid>
    </Page>
  );
}

export default AdminCommentsPage;
