import { FiChevronRight } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const TYPE_LABELS = {
  SAVING_GROUP_JOIN_REQUEST: '계모임 가입 신청',
  SAVING_GROUP_JOIN_APPROVED: '계모임 가입 승인',
  SAVING_GROUP_JOIN_REJECTED: '계모임 가입 거절',
  SMALL_GROUP_JOIN_REQUEST: '소모임 가입 신청',
  SMALL_GROUP_JOIN_APPROVED: '소모임 가입 승인',
  SMALL_GROUP_JOIN_REJECTED: '소모임 가입 거절',
  SCHEDULE_CREATED: '새 일정',
  NOTICE_CREATED: '새 공지',
  VOTE_CREATED: '새 투표',
  DUES_PAYMENT_DUE: '회비 납부 안내',
  REPORT_PROCESSED: '신고 처리 결과',
  MEMBER_RESTRICTED: '이용 제한 안내',
  MEMBER_RESTRICTION_RELEASED: '이용 제한 해제',
  TEST: '테스트 알림',
};

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
`;

const List = styled.ul`
  margin-top: 14px;
  display: grid;
  gap: 10px;
`;

const NotificationButton = styled.button`
  width: 100%;
  min-height: 88px;
  padding: 14px 12px;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $read, theme }) => ($read ? theme.colors.white : theme.colors.primarySoft)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  text-align: left;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

const Dot = styled.span`
  width: 7px;
  height: 7px;
  align-self: start;
  margin-top: 7px;
  background: ${({ $read, theme }) => ($read ? theme.colors.border : theme.colors.primary)};
  border-radius: ${({ theme }) => theme.radius.pill};
`;

const NotificationTitle = styled.strong`
  display: block;
  font-size: 13px;
  font-weight: 800;
`;

const Message = styled.span`
  margin-top: 6px;
  display: block;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.55;
`;

const DateText = styled.time`
  margin-top: 8px;
  display: block;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Actions = styled.div`
  margin-top: 14px;
  display: flex;
  justify-content: center;
`;

function formatCreatedAt(createdAt) {
  if (!createdAt) return '';

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(createdAt));
}

function NotificationList({
  notifications,
  loading,
  loadFailed,
  hasMore,
  onNotificationClick,
  onLoadMore,
  onRetry,
}) {
  return (
    <Panel>
      <Title>내 알림</Title>

      {notifications.length > 0 && (
        <List>
          {notifications.map((notification) => (
            <li key={notification.notificationId}>
              <NotificationButton
                type="button"
                $read={notification.read}
                onClick={() => onNotificationClick(notification)}
              >
                <Dot $read={notification.read} aria-hidden="true" />
                <span>
                  <NotificationTitle>
                    {TYPE_LABELS[notification.type] ?? '알림'}
                  </NotificationTitle>
                  <Message>{notification.message}</Message>
                  <DateText dateTime={notification.createdAt}>
                    {formatCreatedAt(notification.createdAt)}
                    {!notification.read && ' · 새 알림'}
                  </DateText>
                </span>
                <FiChevronRight size={20} aria-hidden="true" />
              </NotificationButton>
            </li>
          ))}
        </List>
      )}

      {loading && notifications.length === 0 && <Empty>알림을 불러오는 중입니다.</Empty>}
      {!loading && loadFailed && notifications.length === 0 && <Empty>알림을 불러오지 못했습니다.</Empty>}
      {!loading && !loadFailed && notifications.length === 0 && <Empty>새로운 알림이 없습니다.</Empty>}

      <Actions>
        {loadFailed && (
          <Button type="button" $variant="secondary" $size="small" onClick={onRetry}>
            다시 시도
          </Button>
        )}
        {!loadFailed && hasMore && (
          <Button type="button" $variant="secondary" $size="small" onClick={onLoadMore} disabled={loading}>
            {loading ? '불러오는 중…' : '알림 더 보기'}
          </Button>
        )}
      </Actions>
    </Panel>
  );
}

export default NotificationList;
