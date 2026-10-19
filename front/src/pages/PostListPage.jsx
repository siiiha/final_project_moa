import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import PostFilterBar from '../components/post/PostFilterBar';
import PostTable from '../components/post/PostTable';
import { postCategories, postItems } from '../mocks/posts';

const POSTS_PER_PAGE = 10;
const PAGE_BUTTON_COUNT = 5;

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

function PostListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordDraft, setKeywordDraft] = useState(searchParams.get('keyword') || '');
  const [currentPage, setCurrentPage] = useState(1);

  const category = searchParams.get('category') || '전체';
  const condition = searchParams.get('condition') || '제목+내용';
  const sort = searchParams.get('sort') || '최신순';
  const keyword = searchParams.get('keyword') || '';
  const order = sort === '최신순' ? 'desc' : 'asc';

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

  const filteredPosts = useMemo(() => {
    // TODO: Spring Boot 게시글 목록 조회 API에 category, condition, keyword, sort, order, page 정보를 전달합니다.
    const normalizedKeyword = keyword.toLowerCase();
    const visiblePosts = postItems.filter((post) => {
      if (category !== '전체' && post.category !== category) return false;
      if (!normalizedKeyword) return true;

      const searchableText =
        condition === '제목' ? post.title : condition === '내용' ? post.content : `${post.title} ${post.content}`;
      return searchableText.toLowerCase().includes(normalizedKeyword);
    });

    return [...visiblePosts].sort((a, b) => (order === 'desc' ? b.id - a.id : a.id - b.id));
  }, [category, condition, keyword, order]);

  const totalPages = Math.max(Math.ceil(filteredPosts.length / POSTS_PER_PAGE), 1);
  const visiblePosts = filteredPosts.slice((currentPage - 1) * POSTS_PER_PAGE, currentPage * POSTS_PER_PAGE);
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
    navigate('/post/create');
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
          categories={postCategories}
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
        <PostTable
          posts={visiblePosts}
          totalCount={filteredPosts.length}
          currentPage={currentPage}
          pageNumbers={pageNumbers}
          onPageChange={setCurrentPage}
          onPostClick={handlePostClick}
        />
      </Content>
    </Page>
  );
}

export default PostListPage;
