import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import client from '../api/client';
import MemberSidebar from '../components/layout/MemberSidebar';
import NotificationList from '../components/member/NotificationList';
import { memberMenuItems } from '../mocks/mypage';

const NOTIFICATIONS_PER_REQUEST = 10;
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
  const [markingAllRead, setMarkingAllRead] = useState(false);
  const [deletingRead, setDeletingRead] = useState(false);

  // AI작성: 마지막 알림 번호를 커서로 전달해 중복 없이 다음 알림 목록을 이어 붙입니다.
  const loadNotifications = useCallback(async (lastNotificationId = null) => {
    setLoading(true);

    try {
      const response = await client.get('/v1/notifications', {
        params: {
          size: NOTIFICATIONS_PER_REQUEST,
          ...(lastNotificationId ? { lastNotificationId } : {}),
        },
      });
      const received = Array.isArray(response.data?.data) ? response.data.data : [];

      setNotifications((current) => {
        if (!lastNotificationId) return received;

        const existingIds = new Set(current.map((item) => item.notificationId));
        return [...current, ...received.filter((item) => !existingIds.has(item.notificationId))];
      });
      setHasMore(received.length === NOTIFICATIONS_PER_REQUEST);
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
    pageStateRef.current = { notifications, hasMore };
  }, [notifications, hasMore]);

  useEffect(() => () => {
    if (!pageStateRef.current) return;

    // AI작성: 페이지를 떠나기 직전 목록과 위치를 잠시 저장해 뒤로가기 사용성을 유지합니다.
    notificationPageCache = {
      ...pageStateRef.current,
      scrollY: window.scrollY,
      savedAt: Date.now(),
    };
  }, []);

  const handleLoadMore = () => {
    const lastNotification = notifications.at(-1);
    if (lastNotification) loadNotifications(lastNotification.notificationId);
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      try {
        await client.patch(`/v1/notifications/${notification.notificationId}/read`);
        setNotifications((current) => current.map((item) => (
          item.notificationId === notification.notificationId
            ? { ...item, read: true }
            : item
        )));
      } catch {
        setLoadFailed(true);
        return;
      }
    }

    // AI작성: 알림에 저장된 모아 내부 주소만 이동 대상으로 사용합니다.
    if (notification.targetUrl?.startsWith('/')) {
      navigate(notification.targetUrl);
    }
  };

  const handleDeleteAllRead = async () => {
    setDeletingRead(true);

    try {
      const response = await client.delete('/v1/notifications/read');
      const deletedCount = Number(response.data?.data ?? 0);

      // AI작성: 삭제로 현재 커서가 사라질 수 있으므로 첫 페이지부터 다시 조회합니다.
      setNotifications([]);
      setHasMore(false);
      await loadNotifications();
      window.alert(`읽은 알림 ${deletedCount}개를 삭제했습니다.`);
    } catch {
      window.alert('읽은 알림을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setDeletingRead(false);
    }
  };

  const handleReadAll = async () => {
    setMarkingAllRead(true);

    try {
      await client.patch('/v1/notifications/read-all');

      // AI작성: 서버의 전체 읽음 처리가 끝나면 현재 화면의 모든 알림도 읽음 상태로 맞춥니다.
      setNotifications((current) => current.map((notification) => (
        notification.read ? notification : { ...notification, read: true }
      )));
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
          items={memberMenuItems}
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
            hasUnreadNotifications={notifications.some((notification) => !notification.read)}
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
