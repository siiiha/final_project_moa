import axios from 'axios';
import useAuthStore from '../store/authStore.js';

// AI작성: 기존 공통 Axios 인스턴스에서 쿠키·재발급·재시도를 함께 처리한다.
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'X-Moa-Request': '1',
  },
});

let refreshPromise = null;

function getPath(config) {
  return String(config.url ?? '')
    .split('?')[0]
    .replace(/^\/api(?=\/)/, '');
}

// AI작성: 로그인 실패나 재발급 실패를 다시 재발급하지 않는다.
function isAuthRequest(config) {
  const path = getPath(config);
  return path.startsWith('/auth/') && path !== '/auth/me';
}

// AI작성: 계정·세션 변경 여부 비교에만 사용하며 JWT 인증 판단에는 사용하지 않는다.
function readIdentity(token) {
  try {
    const payload = token.split('.')[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    return JSON.parse(window.atob(payload));
  } catch {
    return null;
  }
}

function requireCurrentLogin(authVersion) {
  if (useAuthStore.getState().authVersion !== authVersion) {
    throw new axios.CanceledError('로그인 상태가 변경되었습니다.');
  }
}

async function requestRefresh(authVersion) {
  requireCurrentLogin(authVersion);

  const previousToken = useAuthStore.getState().token;

  if (!previousToken) {
    throw new axios.CanceledError('로그인이 필요합니다.');
  }

  const response = await client.post('/auth/refresh', null);
  requireCurrentLogin(authVersion);

  const token = response.data?.data;

  if (response.data?.success !== true
      || typeof token !== 'string'
      || !token.trim()) {
    throw new Error('토큰 재발급 응답을 확인할 수 없습니다.');
  }

  const previous = readIdentity(previousToken);
  const renewed = readIdentity(token);

  // AI작성: 다른 탭에서 계정이 바뀌어도 현재 화면의 계정을 자동 전환하지 않는다.
  if (!previous || !renewed
      || previous.sub !== renewed.sub
      || (previous.sid && previous.sid !== renewed.sid)) {
    useAuthStore.getState().clearToken();
    throw new axios.CanceledError(
      '로그인 계정이 변경되었습니다. 다시 로그인해주세요.',
    );
  }

  useAuthStore.getState().replaceToken(token);
  return token;
}

function refreshAccessToken(authVersion) {
  // AI작성: 같은 화면의 동시 요청은 하나의 재발급 작업을 기다린다.
  if (!refreshPromise) {
    const refresh = () => requestRefresh(authVersion);

    // AI작성: 지원하는 브라우저에서는 다른 탭의 재발급과도 순서를 맞춘다.
    refreshPromise = (
      navigator.locks
        ? navigator.locks.request('moa-token-refresh', refresh)
        : refresh()
    ).finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

client.interceptors.request.use((config) => {
  const state = useAuthStore.getState();
  config._authVersion = state.authVersion;

  if (isAuthRequest(config)) {
    // AI작성: 로그아웃을 제외한 인증 API에는 만료된 JWT를 보내지 않는다.
    if (getPath(config) !== '/auth/logout') {
      config.headers.delete('Authorization');
    }
  } else if (state.token) {
    config.headers.set('Authorization', `Bearer ${state.token}`);
  }

  return config;
});

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    const state = useAuthStore.getState();

    if (!request
        || error.response?.status !== 401
        || isAuthRequest(request)
        || request._retry
        || request._authVersion !== state.authVersion) {
      throw error;
    }

    const code = error.response?.data?.error?.code;
    const sentAuthorization = request.headers.get('Authorization');

    // AI작성: 만료 이외의 인증 실패는 자동 재발급하지 않는다.
    if (code !== 'ACCESS_TOKEN_EXPIRED') {
      if (code === 'INVALID_TOKEN'
          && state.token
          && sentAuthorization === `Bearer ${state.token}`) {
        state.clearToken();
      }

      throw error;
    }

    if (!state.token) {
      throw error;
    }

    request._retry = true;
    const authVersion = state.authVersion;

    // AI작성: 다른 요청에서 이미 교체했다면 현재 토큰으로 바로 재시도한다.
    if (sentAuthorization !== `Bearer ${state.token}`) {
      return client(request);
    }

    try {
      await refreshAccessToken(authVersion);
      requireCurrentLogin(authVersion);
    } catch (refreshError) {
      const status = refreshError.response?.status;

      if ((status === 401 || status === 403)
          && useAuthStore.getState().authVersion === authVersion) {
        useAuthStore.getState().clearToken();
      }

      throw refreshError;
    }

    // AI작성: 원래 요청은 새 토큰으로 한 번만 재시도한다.
    return client(request);
  },
);

export default client;
