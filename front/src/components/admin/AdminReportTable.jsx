import styled from 'styled-components';
import Badge from '../common/Badge';

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
    width: 19%;
  }
  th:nth-child(2) {
    width: 34%;
  }
  th:nth-child(3) {
    width: 17%;
  }
  th:nth-child(4) {
    width: 14%;
  }
  th:nth-child(5) {
    width: 16%;
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
  }
`;

const Reason = styled.span`
  display: inline-flex;
  padding: 4px 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
`;

const Target = styled.strong`
  overflow: hidden;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const TargetButton = styled.button`
  width: 100%;
  padding: 0;
  overflow: hidden;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    text-decoration: underline;
  }

  &:focus-visible {
    border-radius: ${({ theme }) => theme.radius.sm};
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

const StatusBadge = styled(Badge)`
  color: ${({ $pending, theme }) => ($pending ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $pending, theme }) => ($pending ? theme.colors.warningSoft : theme.colors.primarySoft)};
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
  // AI작성: 많은 신고 데이터에서도 페이지 버튼이 과도하게 늘어나지 않도록 다섯 개만 표시한다.
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const length = Math.min(5, totalPages - start + 1);
  return Array.from({ length }, (_, index) => start + index);
}

function AdminReportTable({ reports, totalCount, currentPage, totalPages, onPageChange, onViewReport, loading }) {
  return (
    <Card>
      <Title>조회 결과 {totalCount}건</Title>
      {loading ? <Empty role="status">신고 목록을 불러오는 중입니다.</Empty> : reports.length ? (
        <>
          <TableWrap>
            <Table>
              <thead>
                <tr>
                  <th scope="col">신고 유형</th>
                  <th scope="col">신고 대상</th>
                  <th scope="col">신고 수</th>
                  <th scope="col">신고일</th>
                  <th scope="col">처리 상태</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id}>
                    <td data-label="신고 유형">
                      <Reason>{report.type}</Reason>
                    </td>
                    <td data-label="신고 대상">
                      <Target>
                        <TargetButton type="button" onClick={() => onViewReport(report.id)}>
                          {report.targetMember}
                        </TargetButton>
                      </Target>
                    </td>
                    <td data-label="신고 수">{report.reporterCount}건</td>
                    <td data-label="신고일">
                      {/* AI작성: 신고 시점의 연도와 날짜 안내 문구가 잘리지 않도록 전체 값을 표시한다. */}
                      {report.createdAt}
                    </td>
                    <td data-label="처리 상태">
                      <StatusBadge $pending={report.statusCode === 'PENDING'}>{report.status}</StatusBadge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableWrap>
          {totalPages > 1 && (
            <Pagination aria-label="관리자 회원 신고 목록 페이지">
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
        <Empty>검색 조건에 맞는 신고 내역이 없습니다.</Empty>
      )}
    </Card>
  );
}

export default AdminReportTable;
