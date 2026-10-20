import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
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
    text-align: left;
  }

  td {
    padding: 11px 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 11px;
    vertical-align: middle;
  }

  th:nth-child(1) {
    width: 27%;
  }
  th:nth-child(2) {
    width: 14%;
  }
  th:nth-child(3) {
    width: 17%;
  }
  th:nth-child(4) {
    width: 16%;
  }
  th:nth-child(5) {
    width: 12%;
  }
  th:nth-child(6) {
    width: 14%;
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
  min-width: 36px;
  justify-content: center;
  padding: 3px 6px;
  color: ${({ $status, theme }) => {
    if ($status === '반려') return theme.colors.danger;
    if ($status === '대기') return theme.colors.warning;
    return theme.colors.primary;
  }};
  background: ${({ $status, theme }) => {
    if ($status === '반려') return theme.colors.dangerSoft;
    if ($status === '대기') return theme.colors.warningSoft;
    return theme.colors.primarySoft;
  }};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
`;

const ReviewButton = styled(Button)`
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

function SavingGroupApplicationTable({ applications, filter, currentPage, totalPages, onPageChange, onReview }) {
  return (
    <Card>
      <Header>
        <Title>계모임 심사 목록</Title>
        <Description>계모임 개설 신청을 검토하고 처리 상태를 확인할 수 있습니다.</Description>
      </Header>
      <FilterArea>{filter}</FilterArea>
      <TableArea>
        <Table>
          <thead>
            <tr>
              <th scope="col">계모임명</th>
              <th scope="col">신청자</th>
              <th scope="col">희망 지역</th>
              <th scope="col">신청일</th>
              <th scope="col">상태</th>
              <th scope="col">심사</th>
            </tr>
          </thead>
          <tbody>
            {applications.length === 0 ? (
              <tr>
                <Empty colSpan="6">검색 조건에 맞는 신청 내역이 없습니다.</Empty>
              </tr>
            ) : (
              applications.map((application) => (
                <tr key={application.id}>
                  <td data-label="계모임명">
                    <GroupName>{application.name}</GroupName>
                  </td>
                  <td data-label="신청자">{application.applicant}</td>
                  <td data-label="희망 지역">{application.region}</td>
                  <td data-label="신청일">{application.appliedAt}</td>
                  <td data-label="상태">
                    <Status $status={application.status}>{application.status}</Status>
                  </td>
                  <td data-label="심사">
                    <ReviewButton type="button" $variant="secondary" onClick={() => onReview(application.id)}>
                      심사 보기
                    </ReviewButton>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </Table>
        {totalPages > 1 && (
          <Pagination aria-label="계모임 심사 목록 페이지">
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
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
          </Pagination>
        )}
      </TableArea>
    </Card>
  );
}

export default SavingGroupApplicationTable;
