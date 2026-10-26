import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { getPosts } from '../api/postApi';
import Button from '../components/common/Button';
import PostFilterBar from '../components/post/PostFilterBar';
import PostTable from '../components/post/PostTable';
import useAuthStore from '../store/authStore';

const POSTS_PER_PAGE = 10;
const PAGE_BUTTON_COUNT = 5;

// AI작성: 화면의 카테고리 이름을 DB의 post_category ID와 연결합니다.
const POST_CATEGORIES = [
  { name: '전체', id: null },
  { name: '행사·축제', id: 1 },
  { name: '일자리', id: 2 },
  { name: '거래·나눔', id: 3 },
];

const SEARCH_TYPE_BY_CONDITION = {
  '제목': 'title',
  '내용': 'content',
};

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 20px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Heading = styled.div`
  margin-top: 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -1.2px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const WriteButton = styled(Button)`
  min-width: 102px;
  min-height: 38px;
  font-size: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const Content = styled.main`
  margin-top: 14px;
`;

const StatusMessage = styled.p`
  padding: 32px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
`;

function PostListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuthStore((state) => state.token);
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordDraft, setKeywordDraft] = useState(searchParams.get('keyword') || '');
  const [currentPage, setCurrentPage] = useState(1);
  const [posts, setPosts] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const category = searchParams.get('category') || '전체';
  const condition = searchParams.get('condition') || '제목+내용';
  const sort = searchParams.get('sort') || '최신순';
  const keyword = searchParams.get('keyword') || '';
  const order = sort === '최신순' ? 'desc' : 'asc';

  useEffect(() => {
    let isActive = true;

    async function loadPosts() {
      setIsLoading(true);
      setErrorMessage('');

      const selectedCategory = POST_CATEGORIES.find((item) => item.name === category);
      const params = {
        page: currentPage,
        size: POSTS_PER_PAGE,
        order,
      };

      if (selectedCategory?.id) params.categoryId = selectedCategory.id;
      if (keyword) params.keyword = keyword;
      if (SEARCH_TYPE_BY_CONDITION[condition]) {
        params.searchType = SEARCH_TYPE_BY_CONDITION[condition];
      }

      try {
        // AI작성: Spring Boot 게시글 목록 API의 데이터와 페이징 정보를 화면 형식으로 변환합니다.
        const response = await getPosts(params);
        if (!response.success) {
          throw new Error(response.error?.message || '게시글 목록을 불러오지 못했습니다.');
        }

        if (!isActive) return;

        setPosts(
          (response.data || []).map((post) => ({
            id: post.postId,
            category: post.categoryName || '기타',
            title: post.title,
            author: post.writerNickname || '알 수 없음',
            date: post.createdAtStr || '',
            views: post.viewCount,
          }))
        );
        setTotalCount(response.meta?.total || 0);
        setTotalPages(response.meta?.totalPages || 0);
      } catch (error) {
        if (!isActive) return;
        setPosts([]);
        setTotalCount(0);
        setTotalPages(0);
        setErrorMessage(error.message || '게시글 목록을 불러오지 못했습니다.');
      } finally {
        if (isActive) setIsLoading(false);
      }
    }

    loadPosts();

    return () => {
      isActive = false;
    };
  }, [category, condition, currentPage, keyword, order]);

  const updateFilters = (nextFilters) => {
    const nextCategory = nextFilters.category ?? category;
    const nextCondition = nextFilters.condition ?? condition;
    const nextSort = nextFilters.sort ?? sort;
    const nextKeyword = nextFilters.keyword ?? keyword;
    const nextOrder = nextSort === '최신순' ? 'desc' : 'asc';
    const nextParams = new URLSearchParams({
      condition: nextCondition,
      sort: nextSort,
      order: nextOrder,
      category: nextCategory,
    });

    if (nextKeyword.trim()) {
      nextParams.set('keyword', nextKeyword.trim());
    }

    setCurrentPage(1);
    setSearchParams(nextParams);
  };

  const pageStart = Math.floor((currentPage - 1) / PAGE_BUTTON_COUNT) * PAGE_BUTTON_COUNT + 1;
  const pageNumbers = Array.from(
    { length: Math.min(PAGE_BUTTON_COUNT, totalPages - pageStart + 1) },
    (_, index) => pageStart + index
  );

  const handleSearch = (event) => {
    event.preventDefault();
    updateFilters({ keyword: keywordDraft });
  };

  const handlePostClick = (postId) => {
    navigate(`/post/${postId}`);
  };

  const handleWrite = () => {
    // AI작성: 작성은 JWT 인증이 필요하므로 비로그인 사용자는 로그인 화면으로 안내한다.
    navigate(token ? '/post/create' : `/member/login?redirect=${encodeURIComponent(location.pathname)}`);
  };

  return (
    <Page>
      <Breadcrumb>게시판 &gt; 게시판 목록</Breadcrumb>
      <Heading>
        <div>
          <h1>우리 동네 게시판</h1>
          <p>이웃들과 생활 정보와 경험을 나눠보세요.</p>
        </div>
        <WriteButton type="button" onClick={handleWrite}>
          + 게시글 작성
        </WriteButton>
      </Heading>
      <Content>
        <PostFilterBar
          categories={POST_CATEGORIES.map((item) => item.name)}
          category={category}
          condition={condition}
          keyword={keywordDraft}
          sort={sort}
          onCategoryChange={(value) => updateFilters({ category: value })}
          onConditionChange={(value) => updateFilters({ condition: value })}
          onKeywordChange={setKeywordDraft}
          onSortChange={(value) => updateFilters({ sort: value })}
          onSearch={handleSearch}
        />
        {isLoading ? (
          <StatusMessage>게시글을 불러오는 중입니다.</StatusMessage>
        ) : errorMessage ? (
          <StatusMessage role="alert">{errorMessage}</StatusMessage>
        ) : (
          <PostTable
            posts={posts}
            totalCount={totalCount}
            currentPage={currentPage}
            pageNumbers={pageNumbers}
            onPageChange={setCurrentPage}
            onPostClick={handlePostClick}
          />
        )}
      </Content>
    </Page>
  );
}

export default PostListPage;
