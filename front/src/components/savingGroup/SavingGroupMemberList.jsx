import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 14px;
  }
`;

const Count = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const CardHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const RoleEditButton = styled(Button)`
  min-height: 34px;
  padding: 7px 12px;
  font-size: 12px;
`;

const TableWrap = styled.div`
  margin-top: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: none;
  }
`;

const Table = styled.table`
  width: 100%;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-collapse: separate;
  border-spacing: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  table-layout: fixed;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    text-align: center;
    vertical-align: middle;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(1),
  td:nth-child(2) {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Name = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const Email = styled.span`
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const RoleSelect = styled.select`
  width: 100%;
  max-width: 96px;
  min-height: 32px;
  padding: 5px 26px 5px 9px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const MobileList = styled.div`
  margin-top: 14px;
  display: none;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: grid;
  }
`;

const MobileItem = styled.article`
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const MobileTop = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const MobileEmail = styled.p`
  margin-top: 5px;
  overflow-wrap: anywhere;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const MobileInfo = styled.dl`
  margin-top: 12px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 7px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-weight: 700;
  }
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Pagination = styled.nav`
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 4px;
`;

const PageButton = styled(Button)`
  min-width: 30px;
`;

function SavingGroupMemberList({
  members,
  currentPage,
  totalPages,
  totalCount,
  onPageChange,
  isRoleEditing,
  onStartRoleEdit,
  onRoleChange,
  onSaveRoles,
}) {
  return (
    <>
      <Card>
        <CardHeader>
          <Count>계원 {totalCount}명</Count>
          <RoleEditButton type="button" onClick={isRoleEditing ? onSaveRoles : onStartRoleEdit}>
            {isRoleEditing ? '저장' : '권한 수정'}
          </RoleEditButton>
        </CardHeader>
        {members.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="15%" />
                  <col width="37%" />
                  <col width="22%" />
                  <col width="26%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">이름</th>
                    <th scope="col">이메일</th>
                    <th scope="col">역할</th>
                    <th scope="col">가입일</th>
                  </tr>
                </thead>
                <tbody>
                  {members.map((member) => (
                    <tr key={member.id}>
                      <td><Name>{member.name}</Name></td>
                      <td><Email>{member.email}</Email></td>
                      <td>
                        {isRoleEditing && member.role !== '계주' ? (
                          <RoleSelect
                            aria-label={`${member.name} 역할`}
                            value={member.role}
                            onChange={(event) => onRoleChange(member.id, event.target.value)}
                          >
                            <option value="회계">회계</option>
                            <option value="계원">계원</option>
                          </RoleSelect>
                        ) : member.role}
                      </td>
                      <td>{member.joinedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {members.map((member) => (
                <MobileItem key={member.id}>
                  <MobileTop>
                    <Name>{member.name}</Name>
                  </MobileTop>
                  <MobileEmail>{member.email}</MobileEmail>
                  <MobileInfo>
                    <dt>역할</dt>
                    <dd>
                      {isRoleEditing && member.role !== '계주' ? (
                        <RoleSelect
                          aria-label={`${member.name} 역할`}
                          value={member.role}
                          onChange={(event) => onRoleChange(member.id, event.target.value)}
                        >
                          <option value="회계">회계</option>
                          <option value="계원">계원</option>
                        </RoleSelect>
                      ) : member.role}
                    </dd>
                    <dt>가입일</dt>
                    <dd>{member.joinedAt}</dd>
                  </MobileInfo>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>검색 조건에 맞는 계원이 없습니다.</Empty>
        )}
      </Card>

      {totalPages > 1 && (
        <Pagination aria-label="계원 목록 페이지 이동">
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>
            이전
          </PageButton>
          {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
            <PageButton
              key={page}
              type="button"
              $variant={page === currentPage ? 'primary' : 'secondary'}
              $size="small"
              aria-current={page === currentPage ? 'page' : undefined}
              onClick={() => onPageChange(page)}
            >
              {page}
            </PageButton>
          ))}
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => onPageChange(currentPage + 1)}>
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default SavingGroupMemberList;
