import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  margin-top: 24px;
  padding: 18px;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;

const TableWrap = styled.div`
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;

  th {
    padding: 13px 12px;
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 12px;
    font-weight: 700;
    text-align: left;
  }

  td {
    padding: 12px;
    color: ${({ theme }) => theme.colors.secondaryText};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 12px;
    vertical-align: middle;
  }

  tr:last-child td {
    border-bottom: 0;
  }

  th:nth-child(1) {
    width: 18%;
  }
  th:nth-child(2) {
    width: 47%;
  }
  th:nth-child(3) {
    width: 15%;
  }
  th:nth-child(4) {
    width: 12%;
  }
  th:nth-child(5) {
    width: 8%;
    text-align: right;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    thead {
      display: none;
    }

    tbody,
    tr,
    td {
      display: block;
      width: 100%;
    }

    tr {
      padding: 8px 0;
      border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    }

    tr:last-child {
      border-bottom: 0;
    }

    td {
      min-height: 27px;
      padding: 5px 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      border: 0;
      text-align: right;
    }

    td::before {
      flex: 0 0 auto;
      color: ${({ theme }) => theme.colors.muted};
      content: attr(data-label);
      font-weight: 700;
      text-align: left;
    }

    td:last-child::before {
      margin-right: auto;
    }
  }
`;

const Category = styled.span`
  display: inline-flex;
  padding: 4px 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
`;

const PostTitle = styled.strong`
  overflow: hidden;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const ActionArea = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 5px;
`;

const ActionButton = styled(Button)`
  min-height: 30px;
  padding: 6px 8px;
  font-size: 10px;
`;

const Pagination = styled.nav`
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 5px;
`;

const PageButton = styled.button`
  min-width: 28px;
  height: 28px;
  padding: 0 7px;
  color: ${({ $active, theme }) => ($active ? theme.colors.white : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
  font-weight: 700;
`;

const Empty = styled.p`
  padding: 46px 16px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 13px;
  text-align: center;
`;

function getPageNumbers(currentPage, totalPages) {
  // AI작성: 많은 게시글 데이터에서도 페이지 버튼이 과도하게 늘어나지 않도록 다섯 개만 표시한다.
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const length = Math.min(5, totalPages - start + 1);
  return Array.from({ length }, (_, index) => start + index);
}

function AdminPostTable({ posts, totalCount, currentPage, totalPages, onPageChange, onDelete, loading }) {
  return (
    <Card>
      <Title>조회 결과 {totalCount}건</Title>
      {loading ? (
        <Empty role="status">게시글 목록을 불러오는 중입니다.</Empty>
      ) : posts.length ? (
        <>
          <TableWrap>
            <Table>
              <thead>
                <tr>
                  <th scope="col">분류</th>
                  <th scope="col">제목</th>
                  <th scope="col">작성자</th>
                  <th scope="col">작성일</th>
                  <th scope="col">관리</th>
                </tr>
              </thead>
              <tbody>
                {posts.map((post) => (
                  <tr key={post.id}>
                    <td data-label="분류">
                      <Category>{post.category}</Category>
                    </td>
                    <td data-label="제목">
                      <PostTitle>{post.title}</PostTitle>
                    </td>
                    <td data-label="작성자">{post.author}</td>
                    <td data-label="작성일">
                      {/* AI작성: 게시글 작성 시점의 연도와 날짜 안내 문구가 잘리지 않도록 전체 값을 표시한다. */}
                      {post.date}
                    </td>
                    <td data-label="관리">
                      <ActionArea>
                        <ActionButton type="button" $variant="danger" onClick={() => onDelete(post.id)}>
                          삭제
                        </ActionButton>
                      </ActionArea>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableWrap>
          {totalPages > 1 && (
            <Pagination aria-label="관리자 게시글 목록 페이지">
              <PageButton
                type="button"
                $active={false}
                disabled={currentPage === 1}
                onClick={() => onPageChange(currentPage - 1)}
                aria-label="이전 페이지"
              >
                이전
              </PageButton>
              {getPageNumbers(currentPage, totalPages).map((page) => (
                <PageButton
                  key={page}
                  type="button"
                  $active={page === currentPage}
                  onClick={() => onPageChange(page)}
                  aria-current={page === currentPage ? 'page' : undefined}
                >
                  {page}
                </PageButton>
              ))}
              <PageButton
                type="button"
                $active={false}
                disabled={currentPage === totalPages}
                onClick={() => onPageChange(currentPage + 1)}
                aria-label="다음 페이지"
              >
                다음
              </PageButton>
            </Pagination>
          )}
        </>
      ) : (
        <Empty>검색 조건에 맞는 게시글이 없습니다.</Empty>
      )}
    </Card>
  );
}

export default AdminPostTable;
