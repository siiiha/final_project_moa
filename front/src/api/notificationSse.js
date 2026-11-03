import { fetchEventSource } from '@microsoft/fetch-event-source';
import { getUnreadNotificationCount } from './notifications';
import useAuthStore from '../store/authStore';

// AI작성: access token이 만료되어 재발급 후 다시 연결해야 하는 경우입니다.
class RenewTokenError extends Error {}

// AI작성: 재연결해도 해결되지 않는 인증 오류이므로 연결을 중단하는 경우입니다.
class StopRetryError extends Error {}

const SSE_URL = '/api/v1/notifications/subscribe';
const SSE_RETRY_DELAY_MS = 3_000;

/**
 * 실시간 알림 SSE 연결을 시작합니다.
 *
 * @param {(notification: object) => void} onNotification
 * 새 알림을 받았을 때 실행할 함수
 *
 * @param {(error: Error) => void} [onError]
 * 연결을 복구할 수 없는 오류가 발생했을 때 실행할 함수
 *
 * @returns {() => void}
 * SSE 연결을 종료하는 함수
 */
export function subscribeNotifications(onNotification, onError) {
  const controller = new AbortController();

  const start = () => {
    // 이미 화면을 벗어났거나 로그아웃했다면 다시 연결하지 않습니다.
    if (controller.signal.aborted) return;

    // AI작성: 재연결할 때마다 Zustand에서 가장 최근 access token을 가져옵니다.
    const token = useAuthStore.getState().token;

    if (!token) return;

    fetchEventSource(SSE_URL, {
      method: 'GET',

      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
        'X-Moa-Request': '1',
      },

      // refresh token 쿠키를 사용할 수 있도록 쿠키를 함께 전송합니다.
      credentials: 'include',

      signal: controller.signal,

      // 다른 브라우저 탭을 보고 있어도 SSE 연결을 유지합니다.
      openWhenHidden: true,

      // AI작성: 정상적인 SSE 응답인지 확인하고 인증 오류를 구분합니다.
      async onopen(response) {
        const contentType = response.headers.get('content-type') ?? '';

        if (
          response.ok
          && contentType.includes('text/event-stream')
        ) {
          return;
        }

        const responseBody = await response
          .json()
          .catch(() => null);

        const errorCode = responseBody?.error?.code;

        // 현재 백엔드 JwtAuthenticationFilter가 반환하는 만료 코드입니다.
        if (
          response.status === 401
          && errorCode === 'ACCESS_TOKEN_EXPIRED'
        ) {
          throw new RenewTokenError(
            'SSE access token이 만료되었습니다.',
          );
        }

        // INVALID_TOKEN, LOGIN_REQUIRED, 접근 거부 등은 반복 연결하지 않습니다.
        throw new StopRetryError(
          responseBody?.error?.message
            ?? `SSE 연결에 실패했습니다. status=${response.status}`,
        );
      },

      // AI작성: 백엔드가 보낸 notification 이벤트만 JSON으로 변환합니다.
      onmessage(event) {
        // 연결 확인용 connected 이벤트는 화면에 표시하지 않습니다.
        if (event.event === 'connected') return;

        // notification 이외의 이벤트와 ping 주석은 무시합니다.
        if (event.event !== 'notification') return;
        if (!event.data) return;

        try {
          const notification = JSON.parse(event.data);
          onNotification(notification);
        } catch (error) {
          onError?.(
            error instanceof Error
              ? error
              : new Error('실시간 알림 데이터를 확인할 수 없습니다.'),
          );
        }
      },

      // 30분 제한 등으로 서버가 연결을 종료하면 다시 연결합니다.
      onclose() {
        throw new Error('SSE 연결이 종료되었습니다.');
      },

      onerror(error) {
        // 토큰 재발급과 재시도 중단은 아래 catch에서 직접 처리합니다.
        if (
          error instanceof RenewTokenError
          || error instanceof StopRetryError
        ) {
          throw error;
        }

        // 네트워크 단절이나 서버 재시작은 3초 후 같은 연결을 재시도합니다.
        return SSE_RETRY_DELAY_MS;
      },
    }).catch(async (error) => {
      if (controller.signal.aborted) return;

      if (error instanceof RenewTokenError) {
        try {
          /*
           * AI작성:
           * 일반 알림 API를 호출하여 client.js의 Axios 인터셉터를 실행합니다.
           * 만료 응답을 받으면 인터셉터가 refresh token으로 access token을
           * 재발급하고 요청을 다시 처리합니다.
           */
          await getUnreadNotificationCount();

          // 재발급 도중 화면을 벗어났다면 다시 연결하지 않습니다.
          if (controller.signal.aborted) return;

          // 새 access token은 authStore에 저장되어 있으므로 다시 읽어 연결합니다.
          start();
        } catch (refreshError) {
          onError?.(
            refreshError instanceof Error
              ? refreshError
              : new Error('로그인 토큰을 재발급하지 못했습니다.'),
          );
        }

        return;
      }

      if (error instanceof StopRetryError) {
        onError?.(error);
        return;
      }

      onError?.(
        error instanceof Error
          ? error
          : new Error('실시간 알림 연결 중 오류가 발생했습니다.'),
      );
    });
  };

  start();

  // React effect cleanup 또는 로그아웃 시 호출합니다.
  return () => {
    controller.abort();
  };
}