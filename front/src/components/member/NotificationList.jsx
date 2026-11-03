import { FiChevronRight } from 'react-icons/fi';
import styled from 'styled-components';
import { NOTIFICATION_TYPE_LABELS } from '../../constants/notification';
import formatNotificationDate from '../../utils/formatNotificationDate';
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

const ListHeader = styled.div`
  padding: 0 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
`;

const HeaderActions = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
`;

const List = styled.ul`
  margin-top: 14px;
  display: grid;
  gap: 10px;
`;

const NotificationItem = styled.div`
  width: 100%;
  min-height: 88px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 48px;
  align-items: center;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $read, theme }) => ($read ? theme.colors.white : theme.colors.primarySoft)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  overflow: hidden;
`;

const ReadButton = styled.button`
  min-width: 0;
  min-height: 86px;
  padding: 14px 12px;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: inherit;
  background: transparent;
  border: 0;
  text-align: left;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: -2px;
  }
`;

const MoveButton = styled.button`
  width: 48px;
  min-height: 86px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-left: 1px solid ${({ theme }) => theme.colors.border};
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: -2px;
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

function NotificationList({
  notifications,
  loading,
  loadFailed,
  hasMore,
  hasUnreadNotifications,
  markingAllRead,
  deletingRead,
  onNotificationClick,
  onReadAll,
  onDeleteAllRead,
  onLoadMore,
  onRetry,
}) {
  return (
    <Panel>
      <ListHeader>
        <Title>내 알림</Title>
        <HeaderActions>
          {/* AI작성: 읽지 않은 알림이 있을 때 현재 회원의 알림을 한 번에 읽음 처리합니다. */}
          <Button
            type="button"
            $variant="primarySoft"
            onClick={onReadAll}
            disabled={!hasUnreadNotifications || markingAllRead || deletingRead}
          >
            {markingAllRead ? '처리 중…' : '전체 읽음'}
          </Button>
          {/* AI작성: 읽은 알림이 있을 때만 일괄 삭제를 실행할 수 있도록 상태를 구분합니다. */}
          <Button
            type="button"
            $variant="primary"
            onClick={onDeleteAllRead}
            disabled={notifications.length === 0 || deletingRead || markingAllRead}
          >
            {deletingRead ? '삭제 중…' : '읽은 알림 삭제'}
          </Button>
        </HeaderActions>
      </ListHeader>

      {notifications.length > 0 && (
        <List>
          {notifications.map((notification) => (
            <li key={notification.notificationId}>
              <NotificationItem $read={notification.read}>
                <ReadButton
                  type="button"
                  onClick={() => {
                    // AI작성: 알림 내용 영역은 페이지 이동 없이 읽음 처리만 수행합니다.
                    onNotificationClick({ ...notification, targetUrl: null });
                  }}
                  aria-label={`${NOTIFICATION_TYPE_LABELS[notification.type] ?? '알림'} 읽음 처리`}
                >
                  <Dot $read={notification.read} aria-hidden="true" />
                  <span>
                    <NotificationTitle>
                      {NOTIFICATION_TYPE_LABELS[notification.type] ?? '알림'}
                    </NotificationTitle>
                    <Message>{notification.message}</Message>
                    <DateText dateTime={notification.createdAt}>
                      {formatNotificationDate(notification.createdAt, { includeYear: true })}
                      {!notification.read && ' · 새 알림'}
                    </DateText>
                  </span>
                </ReadButton>
                {notification.targetUrl?.startsWith('/') && (
                  <MoveButton
                    type="button"
                    onClick={() => onNotificationClick(notification)}
                    aria-label={`${NOTIFICATION_TYPE_LABELS[notification.type] ?? '알림'} 연결 화면으로 이동`}
                    title="연결 화면으로 이동"
                  >
                    <FiChevronRight size={22} aria-hidden="true" />
                  </MoveButton>
                )}
              </NotificationItem>
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
        {!loadFailed && hasMore && notifications.length > 0 && (
          <Button type="button" $variant="secondary" $size="small" onClick={onLoadMore} disabled={loading}>
            {loading ? '불러오는 중…' : '알림 더 보기'}
          </Button>
        )}
      </Actions>
    </Panel>
  );
}

export default NotificationList;
