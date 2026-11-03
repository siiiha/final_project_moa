import { useCallback, useEffect, useState } from 'react';
import { FiBell } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import client from '../api/client';
import useAuthStore from '../store/authStore';

const Wrap = styled.div`
  position: relative;
`;

const BellButton = styled.button`
  position: relative;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  padding: 0;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.pill};
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

const Count = styled.span`
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.colors.white};
  background: ${({ theme }) => theme.colors.danger};
  border: 2px solid ${({ theme }) => theme.colors.white};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 800;
`;

const Panel = styled.section`
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  right: 0;
  width: min(360px, calc(100vw - 32px));
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const PanelTitle = styled.h2`
  padding: 16px 18px;
  color: ${({ theme }) => theme.colors.text};
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  font-size: 17px;
  font-weight: 800;
`;

const NotificationList = styled.ul`
  max-height: 390px;
  overflow-y: auto;
`;

const NotificationButton = styled.button`
  width: 100%;
  padding: 15px 18px;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $read, theme }) => ($read ? theme.colors.white : theme.colors.primarySoft)};
  border: 0;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  text-align: left;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: ${({ theme }) => theme.colors.background};
  }
`;

const Message = styled.span`
  display: block;
  font-size: 14px;
  font-weight: ${({ $read }) => ($read ? 500 : 800)};
  line-height: 1.55;
`;

const DateText = styled.time`
  margin-top: 5px;
  display: block;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const StatusText = styled.p`
  padding: 28px 18px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 14px;
  text-align: center;
`;

const AllNotificationsButton = styled.button`
  width: 100%;
  min-height: 46px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.white};
  border: 0;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
`;

function formatCreatedAt(createdAt) {
  if (!createdAt) return '';

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(createdAt));
}

function NotificationBell() {
  const token = useAuthStore((state) => state.token);
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  // AI작성: 공통 Axios의 토큰 첨부·재발급 처리를 재사용해 최근 알림과 미확인 개수를 조회합니다.
  const loadNotifications = useCallback(async () => {
    if (!token) {
      setItems([]);
      setUnreadCount(0);
      return;
    }

    setLoading(true);

    try {
      const [listResponse, countResponse] = await Promise.all([
        client.get('/v1/notifications', { params: { size: 10 } }),
        client.get('/v1/notifications/unread-count'),
      ]);

      setItems(Array.isArray(listResponse.data?.data) ? listResponse.data.data : []);
      setUnreadCount(Number.isInteger(countResponse.data?.data) ? countResponse.data.data : 0);
      setLoadFailed(false);
    } catch {
      // AI작성: 헤더 보조 기능의 실패가 전체 화면을 중단시키지 않도록 패널 안에서만 안내합니다.
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadNotifications();

    // AI작성: SSE 인증 방식 확정 전까지 주기적으로 새 알림을 확인합니다.
    const intervalId = window.setInterval(loadNotifications, 60_000);
    return () => window.clearInterval(intervalId);
  }, [loadNotifications]);

  const handleBellClick = () => {
    setOpen((current) => !current);
    if (!open) loadNotifications();
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      try {
        await client.patch(`/v1/notifications/${notification.notificationId}/read`);
        setItems((current) => current.map((item) => (
          item.notificationId === notification.notificationId
            ? { ...item, read: true }
            : item
        )));
        setUnreadCount((current) => Math.max(current - 1, 0));
      } catch {
        setLoadFailed(true);
        return;
      }
    }

    setOpen(false);

    // AI작성: 외부 주소 실행을 막고 모아 내부 경로만 이동합니다.
    if (notification.targetUrl?.startsWith('/')) {
      navigate(notification.targetUrl);
    }
  };

  const countLabel = unreadCount > 99 ? '99+' : String(unreadCount);

  if (!token) return null;

  return (
    <Wrap>
      <BellButton
        type="button"
        onClick={handleBellClick}
        aria-label={unreadCount > 0 ? `읽지 않은 알림 ${unreadCount}개` : '알림'}
        aria-expanded={open}
        aria-haspopup="dialog"
      >
        <FiBell size={22} aria-hidden="true" />
        {unreadCount > 0 && <Count aria-hidden="true">{countLabel}</Count>}
      </BellButton>

      {open && (
        <Panel aria-label="최근 알림">
          <PanelTitle>최근 알림</PanelTitle>

          {loading && items.length === 0 && <StatusText>알림을 불러오는 중입니다.</StatusText>}
          {!loading && loadFailed && <StatusText>알림을 불러오지 못했습니다.</StatusText>}
          {!loading && !loadFailed && items.length === 0 && <StatusText>새로운 알림이 없습니다.</StatusText>}

          {items.length > 0 && (
            <NotificationList>
              {items.map((notification) => (
                <li key={notification.notificationId}>
                  <NotificationButton
                    type="button"
                    $read={notification.read}
                    onClick={() => handleNotificationClick(notification)}
                  >
                    <Message $read={notification.read}>{notification.message}</Message>
                    <DateText dateTime={notification.createdAt}>
                      {formatCreatedAt(notification.createdAt)}
                    </DateText>
                  </NotificationButton>
                </li>
              ))}
            </NotificationList>
          )}

          <AllNotificationsButton
            type="button"
            onClick={() => {
              setOpen(false);
              navigate('/member/notification');
            }}
          >
            알림 전체 보기
          </AllNotificationsButton>
        </Panel>
      )}
    </Wrap>
  );
}

export default NotificationBell;
