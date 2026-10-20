import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminPostFilterBar from '../components/admin/AdminPostFilterBar';
import AdminPostTable from '../components/admin/AdminPostTable';
import Badge from '../components/common/Badge';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminPosts } from '../mocks/adminPosts';

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

function AdminPostsPage() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState(adminPosts);
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [region, setRegion] = useState('전체');
  const [status, setStatus] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredPosts = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return posts.filter((post) => {
      const matchesQuery =
        !normalizedQuery ||
        post.title.toLowerCase().includes(normalizedQuery) ||
        post.author.toLowerCase().includes(normalizedQuery);
      const matchesRegion = region === '전체' || post.region.startsWith(region);
      const matchesStatus = status === '전체' || post.status === status;

      return matchesQuery && matchesRegion && matchesStatus;
    });
  }, [posts, region, status, submittedQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const pagePosts = filteredPosts.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleToggleHidden = (postId) => {
    // TODO: Spring Boot 관리자 게시글 숨김·공개 API 요청 후 게시글 목록을 갱신합니다.
    setPosts((currentPosts) =>
      currentPosts.map((post) =>
        post.id === postId ? { ...post, status: post.status === '공개' ? '숨김' : '공개' } : post
      )
    );
  };

  const handleDelete = (postId) => {
    // TODO: Spring Boot 관리자 게시글 삭제 API 요청 후 게시글 목록을 갱신합니다.
    setPosts((currentPosts) => currentPosts.filter((post) => post.id !== postId));
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

    if (menuId === 'comments') {
      navigate('/admin/comments');
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
          <h1>게시글 관리</h1>
          <p>검색과 상세 조회를 통해 서비스 운영 현황을 확인하세요.</p>
        </div>
        <AdminBadge>관리자 전용</AdminBadge>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="posts" onSelect={handleMenuSelect} />
        <Content>
          <AdminPostFilterBar
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
          <AdminPostTable
            posts={pagePosts}
            totalCount={filteredPosts.length}
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

export default AdminPostsPage;
