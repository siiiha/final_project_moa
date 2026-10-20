import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const Intro = styled.div`
  margin-top: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  h2 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 20px;
    font-weight: 800;
    letter-spacing: -0.9px;
  }

  p {
    margin-top: 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const AdminBadge = styled(Badge)`
  min-height: 28px;
  padding: 4px 9px;
  flex: 0 0 auto;
`;

const Card = styled.section`
  margin-top: 16px;
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 14px;
  }
`;

const CardTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
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

  td:first-child {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Applicant = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const TypeBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $type, theme }) => ($type === '지출' ? theme.colors.secondaryText : theme.colors.primary)};
  background: ${({ $type, theme }) => ($type === '지출' ? theme.colors.background : theme.colors.primarySoft)};
  font-size: 10px;
`;

const StatusBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $status, theme }) => ($status === '반려' ? theme.colors.danger : $status === '대기' ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $status, theme }) => ($status === '반려' ? theme.colors.dangerSoft : $status === '대기' ? theme.colors.warningSoft : theme.colors.primarySoft)};
  font-size: 10px;
`;

const ReviewButton = styled(Button)`
  min-height: 29px;
  padding: 5px 8px;
  font-size: 10px;
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
  gap: 10px;
`;

const MobileInfo = styled.dl`
  margin-top: 10px;
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

const MobileAction = styled.div`
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
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

const ReviewNotice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

function SavingGroupPayoutApprovalList({ requests, currentPage, totalPages, onPageChange, totalCount, onReview, reviewApplicant }) {
  return (
    <>
      <Intro>
        <div>
          <h2>계금·지출 승인</h2>
          <p>요청 내역과 증빙을 확인한 뒤 처리해주세요.</p>
        </div>
        <AdminBadge>운영진 전용</AdminBadge>
      </Intro>

      <Card>
        <CardTitle>승인 요청 {totalCount}건</CardTitle>
        {requests.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="22%" />
                  <col width="18%" />
                  <col width="20%" />
                  <col width="18%" />
                  <col width="22%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">신청자</th>
                    <th scope="col">유형</th>
                    <th scope="col">금액</th>
                    <th scope="col">상태</th>
                    <th scope="col">처리</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td><Applicant>{request.applicant}</Applicant></td>
                      <td><TypeBadge $type={request.type}>{request.type}</TypeBadge></td>
                      <td>{request.amount}</td>
                      <td><StatusBadge $status={request.status}>{request.status}</StatusBadge></td>
                      <td>
                        <ReviewButton type="button" $variant="secondary" onClick={() => onReview(request.applicant)}>
                          내역 확인·처리
                        </ReviewButton>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {requests.map((request) => (
                <MobileItem key={request.id}>
                  <MobileTop>
                    <Applicant>{request.applicant}</Applicant>
                    <StatusBadge $status={request.status}>{request.status}</StatusBadge>
                  </MobileTop>
                  <MobileInfo>
                    <dt>유형</dt>
                    <dd><TypeBadge $type={request.type}>{request.type}</TypeBadge></dd>
                    <dt>금액</dt>
                    <dd>{request.amount}</dd>
                  </MobileInfo>
                  <MobileAction>
                    <ReviewButton type="button" $variant="secondary" onClick={() => onReview(request.applicant)}>
                      내역 확인·처리
                    </ReviewButton>
                  </MobileAction>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>처리할 계금 또는 지출 요청이 없습니다.</Empty>
        )}
        {reviewApplicant && <ReviewNotice role="status">{reviewApplicant}님의 요청 내역을 확인할 예정입니다.</ReviewNotice>}
      </Card>

      {totalPages > 1 && (
        <Pagination aria-label="계금 요청 목록 페이지 이동">
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

export default SavingGroupPayoutApprovalList;
