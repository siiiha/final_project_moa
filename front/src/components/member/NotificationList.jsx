import { FiChevronRight, FiTrash2 } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const Panel = styled.section`
  padding: 22px 14px 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 19px 12px 12px;
  }
`;

const Title = styled.h2`
  padding: 0 6px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;

const List = styled.div`
  margin-top: 14px;
  display: grid;
  gap: 10px;
`;

const NotificationCard = styled.article`
  overflow: hidden;
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid #d8ebe4;
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const NotificationSummary = styled.div`
  min-height: 84px;
  padding: 14px 12px;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;

  @media (max-width: 520px) {
    grid-template-columns: 8px minmax(0, 1fr);
    align-items: start;
  }
`;

const Dot = styled.span`
  width: 6px;
  height: 6px;
  align-self: start;
  margin-top: 7px;
  background: ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.pill};
`;

const NotificationTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 13px;
  font-weight: 800;
  letter-spacing: -0.4px;
`;

const Summary = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const DateText = styled.time`
  display: block;
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Actions = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  @media (max-width: 520px) {
    grid-column: 2;
    justify-content: flex-end;
  }
`;

const IconButton = styled.button`
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.primary};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  transform: ${({ $expanded }) => ($expanded ? 'rotate(90deg)' : 'rotate(0)')};
  transition: transform 160ms ease;

  &:hover {
    background: rgba(23, 107, 91, 0.1);
  }
`;

const DeleteButton = styled.button`
  padding: 5px 3px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.colors.danger};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;

  &:hover {
    background: rgba(201, 70, 63, 0.1);
  }
`;

const Detail = styled.div`
  padding: 14px 16px 16px 30px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border-top: 1px solid #d8ebe4;
  font-size: 12px;
  line-height: 1.7;

  strong {
    display: block;
    margin-bottom: 4px;
    color: ${({ theme }) => theme.colors.text};
    font-size: 12px;
  }

  @media (max-width: 520px) {
    padding-left: 16px;
  }
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Pagination = styled.nav`
  margin-top: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
`;

const PageButton = styled(Button)`
  min-width: 29px;
`;

function NotificationList({ notifications, expandedIds, currentPage, totalPages, onToggle, onDelete, onPageChange }) {
  return (
    <>
      <Panel>
        <Title>내 알림</Title>
        {notifications.length ? (
          <List>
            {notifications.map((notification) => {
              const isExpanded = expandedIds.includes(notification.id);

              return (
                <NotificationCard key={notification.id}>
                  <NotificationSummary>
                    <Dot aria-hidden="true" />
                    <div>
                      <NotificationTitle>{notification.title}</NotificationTitle>
                      <Summary>{notification.summary}</Summary>
                      <DateText>{notification.date}</DateText>
                    </div>
                    <Actions>
                      <IconButton
                        type="button"
                        $expanded={isExpanded}
                        aria-label={`${notification.title} 상세 ${isExpanded ? '닫기' : '보기'}`}
                        aria-expanded={isExpanded}
                        onClick={() => onToggle(notification.id)}
                      >
                        <FiChevronRight size={17} aria-hidden="true" />
                      </IconButton>
                      <DeleteButton type="button" onClick={() => onDelete(notification.id)}>
                        <FiTrash2 size={13} aria-hidden="true" />
                        삭제
                      </DeleteButton>
                    </Actions>
                  </NotificationSummary>
                  {isExpanded && (
                    <Detail>
                      <strong>알림 상세</strong>
                      {notification.detail}
                    </Detail>
                  )}
                </NotificationCard>
              );
            })}
          </List>
        ) : (
          <Empty>새로운 알림이 없습니다.</Empty>
        )}
      </Panel>
      {notifications.length > 0 && (
        <Pagination aria-label="알림 페이지 이동">
          <PageButton
            type="button"
            $variant="secondary"
            $size="small"
            disabled={currentPage === 1}
            onClick={() => onPageChange(currentPage - 1)}
          >
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
          <PageButton
            type="button"
            $variant="secondary"
            $size="small"
            disabled={currentPage === totalPages}
            onClick={() => onPageChange(currentPage + 1)}
          >
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default NotificationList;
