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

const TableArea = styled.div`
  padding: 14px 18px 18px;
`;

const FilterArea = styled.div`
  padding: 14px 18px 0;
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
    width: 11%;
  }
  th:nth-child(2) {
    width: 20%;
  }
  th:nth-child(3) {
    width: 13%;
  }
  th:nth-child(4) {
    width: 12%;
  }
  th:nth-child(5) {
    width: 11%;
  }
  th:nth-child(6) {
    width: 16%;
  }
  th:nth-child(7) {
    width: 8%;
  }
  th:nth-child(8) {
    width: 9%;
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

const MemberRow = styled.tr`
  cursor: pointer;

  &:hover td {
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  &:focus-visible {
    outline: 3px solid rgba(23, 107, 91, 0.38);
    outline-offset: -3px;
  }
`;

const MemberName = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const Status = styled.span`
  display: inline-flex;
  min-width: 37px;
  justify-content: center;
  padding: 3px 6px;
  color: ${({ $status, theme }) => ($status === '제재' ? theme.colors.danger : theme.colors.primary)};
  background: ${({ $status, theme }) => ($status === '제재' ? theme.colors.dangerSoft : theme.colors.primarySoft)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
`;

const RestrictionList = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
`;

const RestrictionBadge = styled.span`
  padding: 3px 6px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
`;

const DetailButton = styled(Button)`
  min-height: 28px;
  padding: 5px 8px;
  font-size: 10px;
`;

const Empty = styled.td`
  padding: 42px 12px !important;
  color: ${({ theme }) => theme.colors.muted} !important;
  text-align: center !important;
`;

const Pagination = styled.nav`
  padding: 0 18px 18px;
  display: flex;
  justify-content: center;
  gap: 6px;
`;

const PageButton = styled(Button)`
  min-width: 32px;
  min-height: 30px;
  padding: 5px 8px;
  font-size: 11px;
`;

function getPageNumbers(currentPage, totalPages) {
  // AI작성: 많은 회원 데이터에서도 페이지 버튼이 과도하게 늘어나지 않도록 다섯 개만 표시한다.
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  const length = Math.min(5, totalPages - start + 1);
  return Array.from({ length }, (_, index) => start + index);
}

function AdminMemberTable({
  members,
  filter,
  onViewMember,
  loading,
  errorMessage,
  currentPage,
  totalPages,
  onPageChange,
}) {
  return (
    <Card>
      <Header>
        <Title>회원 관리</Title>
        <Description>회원 정보와 인증·계정 상태를 확인하고 관리할 수 있습니다.</Description>
      </Header>
      <FilterArea>{filter}</FilterArea>
      <TableArea>
        <Table>
          <thead>
            <tr>
              <th scope="col">이름</th>
              <th scope="col">이메일</th>
              <th scope="col">활동 지역</th>
              <th scope="col">인증 상태</th>
              <th scope="col">가입일</th>
              <th scope="col">제한 영역</th>
              <th scope="col">상태</th>
              <th scope="col">관리</th>
            </tr>
          </thead>
          <tbody>
            {loading || errorMessage || members.length === 0 ? (
              <tr>
                <Empty colSpan="8">
                  {loading ? '회원 목록을 불러오는 중입니다.' : errorMessage || '검색 조건에 맞는 회원이 없습니다.'}
                </Empty>
              </tr>
            ) : (
              members.map((member) => (
                <MemberRow
                  key={member.id}
                  tabIndex="0"
                  onClick={() => onViewMember(member.id)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      onViewMember(member.id);
                    }
                  }}
                >
                  <td data-label="이름">
                    <MemberName>{member.name}</MemberName>
                  </td>
                  <td data-label="이메일">{member.email}</td>
                  <td data-label="활동 지역">{member.region}</td>
                  <td data-label="인증 상태">{member.verified}</td>
                  <td data-label="가입일">{member.joinedAt}</td>
                  <td data-label="제한 영역">
                    {member.restrictionScopes.length > 0 ? (
                      <RestrictionList>
                        {member.restrictionScopes.map((scope) => (
                          <RestrictionBadge key={scope}>{scope}</RestrictionBadge>
                        ))}
                      </RestrictionList>
                    ) : '-'}
                  </td>
                  <td data-label="상태">
                    <Status $status={member.status}>{member.status}</Status>
                  </td>
                  <td data-label="관리">
                    <DetailButton
                      type="button"
                      $variant="secondary"
                      onClick={(event) => {
                        event.stopPropagation();
                        onViewMember(member.id);
                      }}
                    >
                      상세
                    </DetailButton>
                  </td>
                </MemberRow>
              ))
            )}
          </tbody>
        </Table>
      </TableArea>
      {!loading && !errorMessage && totalPages > 1 && (
        <Pagination aria-label="관리자 회원 목록 페이지">
          <PageButton
            type="button"
            $variant="secondary"
            disabled={currentPage <= 1}
            onClick={() => onPageChange(currentPage - 1)}
          >
            이전
          </PageButton>
          {getPageNumbers(currentPage, totalPages).map((page) => (
            <PageButton
              key={page}
              type="button"
              $variant={page === currentPage ? 'primary' : 'secondary'}
              onClick={() => onPageChange(page)}
              aria-current={page === currentPage ? 'page' : undefined}
            >
              {page}
            </PageButton>
          ))}
          <PageButton
            type="button"
            $variant="secondary"
            disabled={currentPage >= totalPages}
            onClick={() => onPageChange(currentPage + 1)}
          >
            다음
          </PageButton>
        </Pagination>
      )}
    </Card>
  );
}

export default AdminMemberTable;
