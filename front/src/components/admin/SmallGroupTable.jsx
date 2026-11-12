import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  margin-top: 14px;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Header = styled.div`
  padding: 17px 18px 0;
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const Description = styled.p`
  margin-top: 3px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const FilterArea = styled.div`
  padding: 14px 18px 0;
`;

const TableArea = styled.div`
  padding: 14px 18px 18px;
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;

  th {
    padding: 10px 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    border-top: 1px solid ${({ theme }) => theme.colors.border};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 11px;
    font-weight: 700;
    /* AI작성: 소모임 관리 표의 열 제목을 한 기준으로 중앙 정렬한다. */
    text-align: center;
  }

  td {
    padding: 11px 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 11px;
    /* AI작성: 소모임 목록의 각 표시값을 열 제목과 같은 기준으로 중앙 정렬한다. */
    text-align: center;
    vertical-align: middle;
  }

  th:nth-child(1) {
    width: 18%;
  }
  th:nth-child(2) {
    width: 10%;
  }
  th:nth-child(3) {
    width: 15%;
  }
  th:nth-child(4) {
    width: 13%;
  }
  th:nth-child(5) {
    width: 10%;
  }
  th:nth-child(6) {
    width: 12%;
  }
  th:nth-child(7) {
    width: 10%;
  }
  th:nth-child(8) {
    width: 12%;
  }

  /* AI작성: 활동 지역은 길이가 달라 비교하기 쉽도록 제목과 값을 왼쪽 정렬한다. */
  th:nth-child(4),
  td:nth-child(4) {
    text-align: left;
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

    td:last-child {
      justify-content: flex-end;
    }

    td:last-child::before {
      margin-right: auto;
    }
  }
`;

const GroupName = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const Status = styled.span`
  display: inline-flex;
  min-width: 43px;
  justify-content: center;
  padding: 3px 6px;
  color: ${({ $status, theme }) => ($status === '정지' ? theme.colors.danger : theme.colors.primary)};
  background: ${({ $status, theme }) => ($status === '정지' ? theme.colors.dangerSoft : theme.colors.primarySoft)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
`;

const ActionButton = styled(Button)`
  min-height: 28px;
  padding: 5px 8px;
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

const Empty = styled.td`
  padding: 42px 12px !important;
  color: ${({ theme }) => theme.colors.muted} !important;
  text-align: center !important;
`;

function getPageNumbers(currentPage, totalPages) {
  // AI작성: 많은 소모임 데이터에서도 페이지 버튼이 과도하게 늘어나지 않도록 다섯 개만 표시한다.
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const length = Math.min(5, totalPages - start + 1);
  return Array.from({ length }, (_, index) => start + index);
}

function SmallGroupTable({
  groups,
  filter,
  currentPage,
  totalPages,
  onPageChange,
  onStatusAction,
  loading,
  errorMessage,
  submitting,
}) {
  return (
    <Card>
      <Header>
        <Title>소모임 관리</Title>
        <Description>서비스에서 운영 중인 소모임의 활동 상태를 확인하고 관리할 수 있습니다.</Description>
      </Header>
      <FilterArea>{filter}</FilterArea>
      <TableArea>
        <Table>
          <thead>
            <tr>
              <th scope="col">소모임명</th>
              <th scope="col">카테고리</th>
              <th scope="col">모임장 정보</th>
              <th scope="col">활동 지역</th>
              <th scope="col">참여자 수</th>
              <th scope="col">개설일</th>
              <th scope="col">상태</th>
              <th scope="col">관리</th>
            </tr>
          </thead>
          <tbody>
            {loading || errorMessage || groups.length === 0 ? (
              <tr>
                <Empty colSpan="8">
                  {loading ? '소모임 목록을 불러오는 중입니다.' : errorMessage || '검색 조건에 맞는 소모임이 없습니다.'}
                </Empty>
              </tr>
            ) : (
              groups.map((group) => {
                const canRestrict = group.statusCode === 'ACTIVE';
                const canRelease = group.statusCode === 'RESTRICTED';
                const actionLabel = canRelease ? '제한 해제' : '이용 제한';

                return (
                  <tr key={group.id}>
                    <td data-label="소모임명">
                      <GroupName>{group.name}</GroupName>
                    </td>
                    <td data-label="카테고리">{group.category}</td>
                    <td data-label="모임장 정보">{group.leader}</td>
                    <td data-label="활동 지역">{group.region}</td>
                    <td data-label="참여자 수">{group.participants}명</td>
                    <td data-label="개설일">{group.openedAt}</td>
                    <td data-label="상태">
                      <Status $status={group.status}>{group.status}</Status>
                    </td>
                    <td data-label="관리">
                      {canRestrict || canRelease ? (
                        <ActionButton
                          type="button"
                          $variant={canRestrict ? 'danger' : 'primary'}
                          onClick={() => onStatusAction(group)}
                          disabled={submitting}
                        >
                          {actionLabel}
                        </ActionButton>
                      ) : '-'}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </Table>
        {!loading && !errorMessage && totalPages > 1 && (
          <Pagination aria-label="소모임 목록 페이지">
            <PageButton
              type="button"
              $active={false}
              disabled={currentPage <= 5}
              onClick={() => onPageChange(Math.floor((currentPage - 1) / 5) * 5)}
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
              disabled={Math.ceil(currentPage / 5) * 5 >= totalPages}
              onClick={() => onPageChange(Math.min(totalPages, (Math.floor((currentPage - 1) / 5) + 1) * 5 + 1))}
              aria-label="다음 페이지"
            >
              다음
            </PageButton>
          </Pagination>
        )}
      </TableArea>
    </Card>
  );
}

export default SmallGroupTable;
