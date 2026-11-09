import { Client } from '@stomp/stompjs';
import client, { refreshAccessToken } from './client.js';
import useAuthStore from '../store/authStore.js';

export async function findChatRoom(savingGroupId, signal) {
  const response = await client.get(`/saving-groups/${savingGroupId}/chat-room`, { signal });
  return response.data.data;
}

export async function fetchChatHistory(roomId, signal) {
  const response = await client.get(`/v1/chat/rooms/${roomId}/messages`, { signal });
  return response.data.data;
}

// AI작성: 토큰 재발급을 공통 HTTP 클라이언트와 공유하고 재연결마다 현재 방을 구독한다.
export function connectSavingGroupChat(roomId, authVersion, callbacks) {
  const url = new URL('/ws', window.location.href);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  let refresh = false;
  let stopped = false;
  const connection = new Client({
    brokerURL: url.href,
    reconnectDelay: 3000,
    connectionTimeout: 10000,
    beforeConnect: async () => {
      try {
        if (refresh) await refreshAccessToken(authVersion);
        refresh = false;
        const state = useAuthStore.getState();
        if (stopped || state.authVersion !== authVersion || !state.token) {
          await connection.deactivate();
          return;
        }
        connection.connectHeaders = { Authorization: `Bearer ${state.token}` };
      } catch {
        callbacks.onError('로그인 상태를 확인해 주세요.');
        await connection.deactivate();
      }
    },
    onConnect: () => {
      connection.subscribe(`/topic/chat/rooms/${roomId}`, (frame) =>
        callbacks.onMessage(JSON.parse(frame.body)));
      connection.subscribe('/user/queue/errors', (frame) =>
        callbacks.onError(JSON.parse(frame.body).error?.message || '메시지를 보내지 못했습니다.'));
      callbacks.onConnect();
    },
    onStompError: (frame) => {
      refresh = frame.headers.message === 'TOKEN_EXPIRED';
      if (refresh) return;
      callbacks.onError(frame.headers.message === 'CHAT_FORBIDDEN'
        ? '채팅방에 참여할 권한이 없습니다.' : '채팅 연결을 시작하지 못했습니다.');
      void connection.deactivate();
    },
    onWebSocketClose: () => callbacks.onDisconnect(connection.active),
  });
  connection.activate();
  return {
    send(content) {
      if (!connection.connected) return false;
      connection.publish({
        destination: `/app/chat/rooms/${roomId}`,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ content }),
      });
      return true;
    },
    close() {
      stopped = true;
      return connection.deactivate();
    },
  };
}
