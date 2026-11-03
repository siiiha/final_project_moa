import { useCallback, useEffect, useRef, useState } from 'react';
import { FiBell, FiChevronRight } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import {
  getNotifications,
  getUnreadNotificationCount,
  readNotification,
} from '../api/notification/notifications';
import { subscribeNotifications } from '../api/notification/subscribeNotifications';
import {
  NOTIFICATION_RECEIVED_EVENT,
  NOTIFICATION_UPDATED_EVENT,
} from '../constants/notification';
import useAuthStore from '../store/authStore';
import formatNotificationDate from '../utils/formatNotificationDate';

// AI작성: 작은 알림창의 조회 범위와 자동 갱신 주기를 한 곳에서 관리합니다.
const NOTIFICATION_PANEL_SIZE = 50;
const NOTIFICATION_POLL_INTERVAL_MS = 60_000;

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

const NotificationItem = styled.div`
  width: 100%;
  display: grid;
  grid-template-columns: ${({ $hasTarget }) => ($hasTarget ? 'minmax(0, 1fr) 46px' : '1fr')};
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $read, theme }) => ($read ? theme.colors.white : theme.colors.primarySoft)};
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
`;

const NotificationReadButton = styled.button`
  min-width: 0;
  padding: 15px 18px;
  display: block;
  color: inherit;
  background: transparent;
  border: 0;
  text-align: left;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: ${({ theme }) => theme.colors.background};
  }
`;

const NotificationMoveButton = styled.button`
  width: 46px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-left: 1px solid ${({ theme }) => theme.colors.border};
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${({ theme }) => theme.colors.primary};
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

function NotificationBell() {
  const token = useAuthStore((state) => state.token);
  const navigate = useNavigate();
  const notificationIdsRef = useRef(new Set());
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
      notificationIdsRef.current.clear();
      return;
    }

    setLoading(true);

    try {
      const [listResult, countResult] = await Promise.allSettled([
        // AI작성: 작은 알림 패널에서도 백엔드 허용 최대치인 최근 50개까지 확인합니다.
        getNotifications({ size: NOTIFICATION_PANEL_SIZE }),
        getUnreadNotificationCount(),
      ]);

      if (listResult.status === 'rejected') throw listResult.reason;

      const notifications = listResult.value;
      notificationIdsRef.current = new Set(
        notifications.map((notification) => notification.notificationId),
      );
      setItems(notifications);
      // AI작성: 개수 조회만 실패해도 목록은 표시하고 현재 목록의 미확인 개수를 임시 사용합니다.
      setUnreadCount(
        countResult.status === 'fulfilled'
          ? countResult.value
          : notifications.filter((notification) => !notification.read).length,
      );
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

    // AI작성: SSE 연결 누락이나 일시적인 장애를 보완하기 위해 60초 주기 조회를 유지합니다.
    const intervalId = window.setInterval(loadNotifications, NOTIFICATION_POLL_INTERVAL_MS);
    // AI작성: 전체 알림 화면에서 읽음 상태가 바뀌면 헤더 배지를 즉시 다시 조회합니다.
    window.addEventListener(NOTIFICATION_UPDATED_EVENT, loadNotifications);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener(NOTIFICATION_UPDATED_EVENT, loadNotifications);
    };
  }, [loadNotifications]);

  useEffect(() => {
    if (!token) return undefined;

    // AI작성: SSE로 받은 새 알림을 헤더에 즉시 추가하고 전체 알림 화면에도 전달합니다.
    const unsubscribe = subscribeNotifications(
      (notification) => {
        if (notificationIdsRef.current.has(notification.notificationId)) return;

        notificationIdsRef.current.add(notification.notificationId);
        setItems((current) => (
          [notification, ...current].slice(0, NOTIFICATION_PANEL_SIZE)
        ));

        if (!notification.read) {
          setUnreadCount((current) => current + 1);
        }

        window.dispatchEvent(
          new CustomEvent(NOTIFICATION_RECEIVED_EVENT, {
            detail: notification,
          }),
        );
      },
      (error) => {
        // SSE가 실패해도 기존 60초 조회가 알림 누락을 보완합니다.
        console.warn('실시간 알림 연결 오류:', error.message);
      },
    );

    return unsubscribe;
  }, [token]);

  const handleBellClick = () => {
    setOpen((current) => !current);
    if (!open) loadNotifications();
  };

  const handleNotificationRead = async (notification) => {
    if (!notification.read) {
      try {
        await readNotification(notification.notificationId);
        setItems((current) => current.map((item) => (
          item.notificationId === notification.notificationId
            ? { ...item, read: true }
            : item
        )));
        setUnreadCount((current) => Math.max(current - 1, 0));
      } catch {
        setLoadFailed(true);
        return false;
      }
    }

    return true;
  };

  const handleNotificationMove = async (notification) => {
    const readSucceeded = await handleNotificationRead(notification);
    if (!readSucceeded) return;

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
                  <NotificationItem
                    $read={notification.read}
                    $hasTarget={notification.targetUrl?.startsWith('/')}
                  >
                    <NotificationReadButton
                      type="button"
                      onClick={() => handleNotificationRead(notification)}
                      aria-label="알림 읽음 처리"
                    >
                      <Message $read={notification.read}>{notification.message}</Message>
                      <DateText dateTime={notification.createdAt}>
                        {formatNotificationDate(notification.createdAt)}
                      </DateText>
                    </NotificationReadButton>
                    {notification.targetUrl?.startsWith('/') && (
                      <NotificationMoveButton
                        type="button"
                        onClick={() => handleNotificationMove(notification)}
                        aria-label="알림 연결 화면으로 이동"
                        title="연결 화면으로 이동"
                      >
                        <FiChevronRight size={20} aria-hidden="true" />
                      </NotificationMoveButton>
                    )}
                  </NotificationItem>
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
