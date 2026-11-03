import client from '../client';

const NOTIFICATION_API_PATH = '/v1/notifications';

// AI작성: 알림 API 응답 구조를 컴포넌트가 직접 해석하지 않도록 요청 함수를 모아 관리합니다.
export async function getNotifications({ size, lastNotificationId } = {}) {
  const response = await client.get(NOTIFICATION_API_PATH, {
    params: {
      ...(size ? { size } : {}),
      ...(lastNotificationId ? { lastNotificationId } : {}),
    },
  });

  return Array.isArray(response.data?.data) ? response.data.data : [];
}

export async function getUnreadNotificationCount() {
  const response = await client.get(`${NOTIFICATION_API_PATH}/unread-count`);
  return Number.isInteger(response.data?.data) ? response.data.data : 0;
}

export function readNotification(notificationId) {
  return client.patch(`${NOTIFICATION_API_PATH}/${notificationId}/read`);
}

export function readAllNotifications() {
  return client.patch(`${NOTIFICATION_API_PATH}/read-all`);
}

export async function deleteReadNotifications() {
  const response = await client.delete(`${NOTIFICATION_API_PATH}/read`);
  return Number(response.data?.data ?? 0);
}
