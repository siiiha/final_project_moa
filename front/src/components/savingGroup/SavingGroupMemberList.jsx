import { useState } from 'react';
import ConfirmModal from '../common/ConfirmModal';
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

// AI작성: 역할 선택과 해당 회원 저장 버튼을 함께 배치하고 모바일에서도 줄바꿈을 허용한다.
const RoleControls = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
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

// AI작성: 추방 버튼은 충분한 클릭 영역과 읽기 쉬운 크기를 적용한다.
const ExpelButton = styled(Button)`
  min-height: 44px;
  font-size: 16px;
`;

// AI작성: 역할은 서버 코드로 비교하고 표시할 때만 한국어로 변환한다.
function SavingGroupMemberList({
  members,
  currentPage,
  totalPages,
  totalCount,
  hasSearch = false,
  onPageChange,
  isRoleEditing,
  onStartRoleEdit,
  onRoleChange,
  onCancelRoleEdit,
  drafts,
  pendingId,
  onSaveRole,
  canExpel = false,
  onExpel,
  onExpelOpen,
  expulsionError = '',
  expellingId = null,
}) {
  const [selectedMember, setSelectedMember] = useState(null);
  const busy = pendingId != null || expellingId != null;
  // AI작성: 승인 OWNER·모집중 여부는 상위 화면에서 전달하며 대상 OWNER는 여기서도 차단한다.
  const renderExpel = (member) => canExpel && ['MEMBER', 'MANAGER'].includes(member.role) ? (
    <ExpelButton type="button" $variant="danger" disabled={busy || isRoleEditing}
      aria-label={`${member.name} 계원 추방`} onClick={() => { onExpelOpen?.(); setSelectedMember(member); }}>추방</ExpelButton>
  ) : null;
  const confirmExpel = async () => {
    if (busy || !canExpel || !selectedMember || !onExpel) return;
    if (await onExpel(selectedMember.id)) setSelectedMember(null);
  };
  const renderRole = (member) => isRoleEditing ? (
    <RoleControls>
      <RoleSelect aria-label={`${member.name} 역할`} value={drafts[member.id] || member.role}
        disabled={busy} onChange={(event) => onRoleChange(member.id, event.target.value)}>
        <option value="MANAGER">회계</option>
        <option value="MEMBER">계원</option>
      </RoleSelect>
      <Button type="button" $size="small" disabled={busy || !drafts[member.id] || drafts[member.id] === member.role}
        aria-label={`${member.name} 역할 저장`} onClick={() => onSaveRole(member.id)}>
        {pendingId === member.id ? '저장 중…' : '저장'}
      </Button>
    </RoleControls>
  ) : member.role === 'MANAGER' ? '회계' : '계원';
  return (
    <>
      <Card>
        <CardHeader>
          <Count>{hasSearch ? '검색 결과' : '계원'} {totalCount}명</Count>
          <RoleEditButton type="button" disabled={busy || !members.length} onClick={isRoleEditing ? onCancelRoleEdit : onStartRoleEdit}>
            {isRoleEditing ? '편집 종료' : '역할 수정'}
          </RoleEditButton>
        </CardHeader>
        {members.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="15%" />
                  <col width={canExpel ? "27%" : "37%"} />
                  <col width="22%" />
                  <col width={canExpel ? "21%" : "26%"} />
                  {canExpel && <col width="15%" />}
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">이름</th>
                    <th scope="col">이메일</th>
                    <th scope="col">역할</th>
                    <th scope="col">가입일</th>
                    {canExpel && <th scope="col">관리</th>}
                  </tr>
                </thead>
                <tbody>
                  {members.map((member) => (
                    <tr key={member.id}>
                      <td><Name>{member.name}</Name></td>
                      <td><Email>{member.email}</Email></td>
                      <td>
                        {renderRole(member)}
                      </td>
                      <td>{member.joinedAt}</td>
                      {canExpel && <td>{renderExpel(member)}</td>}
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
                      {renderRole(member)}
                    </dd>
                    <dt>가입일</dt>
                    <dd>{member.joinedAt}</dd>
                  </MobileInfo>
                  {renderExpel(member)}
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>{hasSearch ? '검색 조건에 맞는 계원이 없습니다.' : '승인된 계원이 없습니다.'}</Empty>
        )}
      </Card>

      {/* AI작성: 기존 공통 확인창을 재사용하며 취소는 요청 없이 닫는다. */}
      {canExpel && selectedMember && <ConfirmModal title="계원을 추방하시겠습니까?"
        description="추방된 계원은 이 계모임에 다시 가입할 수 없습니다."
        confirmLabel={expellingId != null ? '추방 중…' : '계원 추방'} confirmVariant="danger"
        isBusy={busy} onConfirm={confirmExpel} onCancel={() => { if (!busy) setSelectedMember(null); }}>
        <p>선택한 계원: {selectedMember.name}</p>
        {expulsionError && <p role="alert">{expulsionError}</p>}
      </ConfirmModal>}
      {totalPages > 1 && (
        <Pagination aria-label="계원 목록 페이지 이동">
          <PageButton type="button" $variant="secondary" $size="small" disabled={busy || currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>
            이전
          </PageButton>
          {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
            <PageButton
              key={page}
              type="button"
              disabled={busy}
              $variant={page === currentPage ? 'primary' : 'secondary'}
              $size="small"
              aria-current={page === currentPage ? 'page' : undefined}
              onClick={() => onPageChange(page)}
            >
              {page}
            </PageButton>
          ))}
          <PageButton type="button" $variant="secondary" $size="small" disabled={busy || currentPage === totalPages} onClick={() => onPageChange(currentPage + 1)}>
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default SavingGroupMemberList;
