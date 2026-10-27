import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import { getPostCategories, getPosts } from '../api/postApi';
import Button from '../components/common/Button';
import PostFilterBar from '../components/post/PostFilterBar';
import PostTable from '../components/post/PostTable';
import useAuthStore from '../store/authStore';

const POSTS_PER_PAGE = 10;
const PAGE_BUTTON_COUNT = 5;
const ALL_CATEGORY_NAME = '전체보기';

// AI작성: 화면의 카테고리 이름을 DB의 post_category ID와 연결합니다.
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
  const pageParam = Number(searchParams.get('page'));
  // AI작성: 새로고침과 뒤로 가기에도 페이지가 유지되도록 URL의 유효한 페이지 번호를 사용합니다.
  const currentPage = Number.isInteger(pageParam) && pageParam > 0 ? pageParam : 1;
  const [posts, setPosts] = useState([]);
  const [categories, setCategories] = useState([{ categoryName: ALL_CATEGORY_NAME, categoryId: null }]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const category = searchParams.get('category') || ALL_CATEGORY_NAME;
  const condition = searchParams.get('condition') || '제목+내용';
  const sort = searchParams.get('sort') || '최신순';
  const keyword = searchParams.get('keyword') || '';
  const order = sort === '최신순' ? 'desc' : 'asc';

  // AI작성: 검색 실행이나 뒤로 가기로 적용 검색어가 바뀌면 입력창도 URL 기준으로 맞춥니다.
  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- URL의 적용 검색어를 입력창에 동기화해야 합니다.
    setKeywordDraft(keyword);
  }, [keyword]);

  useEffect(() => {
    let isActive = true;
    getPostCategories()
      .then((response) => {
        if (!response.success) throw new Error(response.error?.message || '카테고리를 불러오지 못했습니다.');
        if (isActive) setCategories([{ categoryName: ALL_CATEGORY_NAME, categoryId: null }, ...(response.data || [])]);
      })
      .catch((error) => isActive && setErrorMessage(error.response?.data?.error?.message || error.message))
      .finally(() => isActive && setIsLoadingCategories(false));
    return () => { isActive = false; };
  }, []);

  useEffect(() => {
    let isActive = true;

    async function loadPosts() {
      if (isLoadingCategories) return;
      setIsLoading(true);
      setErrorMessage('');
      console.log("111")

      const selectedCategory = categories.find((item) => item.categoryName === category);
      const params = {
        page: currentPage,
        size: POSTS_PER_PAGE,
        order,
      };

      if (selectedCategory?.categoryId) params.categoryId = selectedCategory.categoryId;
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

        const responseTotalPages = response.meta?.totalPages || 0;

        // AI작성: 존재하지 않는 페이지 요청은 마지막 페이지 또는 기본 1페이지로 보정합니다.
        if (currentPage > 1 && currentPage > responseTotalPages) {
          setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams);

            if (responseTotalPages > 0) {
              nextParams.set('page', String(responseTotalPages));
            } else {
              nextParams.delete('page');
            }

            return nextParams;
          });
          return;
        }

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
        setTotalPages(responseTotalPages);
        console.log("2222")
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
  }, [categories, category, condition, currentPage, isLoadingCategories, keyword, order, setSearchParams]);

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

    setSearchParams(nextParams);
  };

  // AI작성: 페이지 이동 시 기존 검색 조건은 유지하고 page 값만 URL에 반영합니다.
  const handlePageChange = (page) => {
    const nextParams = new URLSearchParams(searchParams);

    if (page <= 1) nextParams.delete('page');
    else nextParams.set('page', String(page));

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
    const returnPath = `${location.pathname}${location.search}`;
    navigate(token ? '/post/create' : `/member/login?redirect=${encodeURIComponent(returnPath)}`);
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
          categories={categories.map((item) => item.categoryName)}
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
            onPageChange={handlePageChange}
            onPostClick={handlePostClick}
          />
        )}
      </Content>
    </Page>
  );
}

export default PostListPage;
