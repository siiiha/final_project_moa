import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import {
  deleteReadNotifications,
  getNotifications,
  getUnreadNotificationCount,
  readAllNotifications,
  readNotification,
} from '../api/notification/notifications';
import MemberSidebar from '../components/layout/MemberSidebar';
import NotificationList from '../components/member/NotificationList';
import { MEMBER_MENU_ITEMS } from '../constants/memberMenu';
import {
  hasNavigableNotificationTarget,
  NOTIFICATION_RECEIVED_EVENT,
  NOTIFICATION_UPDATED_EVENT,
} from '../constants/notification';

const NOTIFICATIONS_PER_REQUEST = 10;
const NOTIFICATIONS_LOOKAHEAD_SIZE = NOTIFICATIONS_PER_REQUEST + 1;
const NOTIFICATION_CACHE_TTL = 5 * 60 * 1000;

let notificationPageCache = null;

function getNotificationPageCache() {
  if (!notificationPageCache) return null;

  const cacheExpired = Date.now() - notificationPageCache.savedAt > NOTIFICATION_CACHE_TTL;
  if (cacheExpired) {
    notificationPageCache = null;
    return null;
  }

  return notificationPageCache;
}

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 216px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

function NotificationPage() {
  const navigate = useNavigate();
  const restoredCache = useRef(getNotificationPageCache()).current;
  const pageStateRef = useRef(null);
  const [activeMenu, setActiveMenu] = useState('alerts');
  const [notifications, setNotifications] = useState(restoredCache?.notifications ?? []);
  const [loading, setLoading] = useState(!restoredCache);
  const [loadFailed, setLoadFailed] = useState(false);
  const [hasMore, setHasMore] = useState(restoredCache?.hasMore ?? false);
  const [unreadCount, setUnreadCount] = useState(restoredCache?.unreadCount ?? 0);
  const [markingAllRead, setMarkingAllRead] = useState(false);
  const [deletingRead, setDeletingRead] = useState(false);

  // AI작성: 마지막 알림 번호를 커서로 전달해 중복 없이 다음 알림 목록을 이어 붙입니다.
  const loadNotifications = useCallback(async (lastNotificationId = null) => {
    setLoading(true);

    try {
      const [listResult, unreadCountResult] = await Promise.allSettled([
        getNotifications({
          // AI작성: 표시 개수보다 한 건 더 조회해 다음 알림 존재 여부를 정확히 판단합니다.
          size: NOTIFICATIONS_LOOKAHEAD_SIZE,
          lastNotificationId,
        }),
        lastNotificationId ? Promise.resolve(null) : getUnreadNotificationCount(),
      ]);

      if (listResult.status === 'rejected') throw listResult.reason;

      const received = listResult.value;
      const visibleNotifications = received.slice(0, NOTIFICATIONS_PER_REQUEST);

      setNotifications((current) => {
        if (!lastNotificationId) return visibleNotifications;

        const existingIds = new Set(current.map((item) => item.notificationId));
        return [
          ...current,
          ...visibleNotifications.filter((item) => !existingIds.has(item.notificationId)),
        ];
      });
      setHasMore(received.length > NOTIFICATIONS_PER_REQUEST);
      if (unreadCountResult.status === 'fulfilled' && unreadCountResult.value !== null) {
        setUnreadCount(unreadCountResult.value);
      } else if (!lastNotificationId) {
        // AI작성: 전체 개수 조회만 실패하면 현재 목록의 미확인 개수로 버튼 기능을 유지합니다.
        setUnreadCount(visibleNotifications.filter((notification) => !notification.read).length);
      }
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!restoredCache) loadNotifications();
  }, [loadNotifications, restoredCache]);

  useEffect(() => {
    if (!restoredCache) return undefined;

    // AI작성: 상세 화면에서 뒤로 돌아오면 더보기 목록과 기존 스크롤 위치를 복원합니다.
    const frameId = window.requestAnimationFrame(() => {
      window.scrollTo({ top: restoredCache.scrollY, behavior: 'auto' });
    });

    return () => window.cancelAnimationFrame(frameId);
  }, [restoredCache]);

  useEffect(() => {
    pageStateRef.current = { notifications, hasMore, unreadCount };
  }, [notifications, hasMore, unreadCount]);

  useEffect(() => () => {
    if (!pageStateRef.current) return;

    // AI작성: 페이지를 떠나기 직전 목록과 위치를 잠시 저장해 뒤로가기 사용성을 유지합니다.
    notificationPageCache = {
      ...pageStateRef.current,
      scrollY: window.scrollY,
      savedAt: Date.now(),
    };
  }, []);

  useEffect(() => {
    // AI작성: 알림 페이지를 열어둔 동안 SSE로 받은 알림을 목록 맨 위에 즉시 추가합니다.
    const handleNotificationReceived = (event) => {
      const notification = event.detail;
      if (!notification?.notificationId) return;

      setNotifications((current) => {
        const alreadyExists = current.some(
          (item) => item.notificationId === notification.notificationId,
        );

        return alreadyExists ? current : [notification, ...current];
      });

      if (!notification.read) {
        setUnreadCount((current) => current + 1);
      }
    };

    window.addEventListener(
      NOTIFICATION_RECEIVED_EVENT,
      handleNotificationReceived,
    );

    return () => {
      window.removeEventListener(
        NOTIFICATION_RECEIVED_EVENT,
        handleNotificationReceived,
      );
    };
  }, []);

  const handleLoadMore = () => {
    const lastNotification = notifications.at(-1);
    if (lastNotification) loadNotifications(lastNotification.notificationId);
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      try {
        await readNotification(notification.notificationId);
        setNotifications((current) => current.map((item) => (
          item.notificationId === notification.notificationId
            ? { ...item, read: true }
            : item
        )));
        setUnreadCount((current) => Math.max(current - 1, 0));
        // AI작성: 헤더 알림 배지가 즉시 최신 개수를 조회하도록 변경 사실을 알립니다.
        window.dispatchEvent(new Event(NOTIFICATION_UPDATED_EVENT));
      } catch {
        // AI작성: 읽음 처리 실패를 목록 조회 실패 상태와 분리해 잘못된 재조회 버튼이 표시되지 않게 합니다.
        window.alert('알림을 읽음 처리하지 못했습니다. 잠시 후 다시 시도해주세요.');
        return;
      }
    }

    // AI작성: 알림에 저장된 모아 내부 주소만 이동 대상으로 사용합니다.
    if (hasNavigableNotificationTarget(notification)) {
      navigate(notification.targetUrl);
    }
  };

  const handleDeleteAllRead = async () => {
    setDeletingRead(true);

    try {
      await deleteReadNotifications();

      // AI작성: 삭제로 현재 커서가 사라질 수 있으므로 첫 페이지부터 다시 조회합니다.
      setNotifications([]);
      setHasMore(false);
      await loadNotifications();
      window.dispatchEvent(new Event(NOTIFICATION_UPDATED_EVENT));
    } catch {
      window.alert('읽은 알림을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setDeletingRead(false);
    }
  };

  const handleReadAll = async () => {
    setMarkingAllRead(true);

    try {
      await readAllNotifications();

      // AI작성: 서버의 전체 읽음 처리가 끝나면 현재 화면의 모든 알림도 읽음 상태로 맞춥니다.
      setNotifications((current) => current.map((notification) => (
        notification.read ? notification : { ...notification, read: true }
      )));
      setUnreadCount(0);
      window.dispatchEvent(new Event(NOTIFICATION_UPDATED_EVENT));
    } catch {
      window.alert('알림을 읽음 처리하지 못했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setMarkingAllRead(false);
    }
  };

  return (
    <Page>
      <Heading>
        <h1>알림 목록</h1>
        <p>모임 소식과 내 활동의 새로운 소식을 확인하세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={MEMBER_MENU_ITEMS}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          <NotificationList
            notifications={notifications}
            loading={loading}
            loadFailed={loadFailed}
            hasMore={hasMore}
            hasUnreadNotifications={unreadCount > 0}
            markingAllRead={markingAllRead}
            deletingRead={deletingRead}
            onNotificationClick={handleNotificationClick}
            onReadAll={handleReadAll}
            onDeleteAllRead={handleDeleteAllRead}
            onLoadMore={handleLoadMore}
            onRetry={() => loadNotifications()}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default NotificationPage;
