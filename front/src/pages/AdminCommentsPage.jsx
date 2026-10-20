import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminCommentFilterBar from '../components/admin/AdminCommentFilterBar';
import AdminCommentTable from '../components/admin/AdminCommentTable';
import Badge from '../components/common/Badge';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminComments } from '../mocks/adminComments';

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

function AdminCommentsPage() {
  const navigate = useNavigate();
  const [comments, setComments] = useState(adminComments);
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [region, setRegion] = useState('전체');
  const [status, setStatus] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredComments = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return comments.filter((comment) => {
      const matchesQuery =
        !normalizedQuery ||
        comment.content.toLowerCase().includes(normalizedQuery) ||
        comment.author.toLowerCase().includes(normalizedQuery) ||
        comment.postTitle.toLowerCase().includes(normalizedQuery);
      const matchesRegion = region === '전체' || comment.region.startsWith(region);
      const matchesStatus = status === '전체' || comment.status === status;

      return matchesQuery && matchesRegion && matchesStatus;
    });
  }, [comments, region, status, submittedQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredComments.length / PAGE_SIZE));
  const pageComments = filteredComments.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleToggleHidden = (commentId) => {
    // TODO: Spring Boot 관리자 댓글 숨김·공개 API 요청 후 댓글 목록을 갱신합니다.
    setComments((currentComments) =>
      currentComments.map((comment) =>
        comment.id === commentId ? { ...comment, status: comment.status === '공개' ? '숨김' : '공개' } : comment
      )
    );
  };

  const handleDelete = (commentId) => {
    // TODO: Spring Boot 관리자 댓글 삭제 API 요청 후 댓글 목록을 갱신합니다.
    setComments((currentComments) => currentComments.filter((comment) => comment.id !== commentId));
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
          <AdminCommentTable
            comments={pageComments}
            totalCount={filteredComments.length}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
            onToggleHidden={handleToggleHidden}
            onDelete={handleDelete}
          />
        </Content>
      </Grid>
    </Page>
  );
}

export default AdminCommentsPage;
