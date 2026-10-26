import axios from 'axios';
import useAuthStore from '../store/authStore.js';

// 모든 API 호출이 이 인스턴스를 거친다.
// baseURL 이 상대 경로('/api/v1')라서 요청은 Vite 개발 서버(5173)로 가고,
// vite.config.js 의 proxy 가 실제 백엔드로 전달한다.
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

// AI작성: 관리자 API를 포함한 인증 요청은 저장된 JWT를 자동으로 전달한다.
// 이미 호출부에서 Authorization 헤더를 지정한 경우에는 그 값을 그대로 사용한다.
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token && !config.headers?.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// AI작성: 만료·무효 토큰은 즉시 제거해 이후 요청에 재사용하지 않는다.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      useAuthStore.getState().clearAuth();
      if (window.location.pathname !== '/member/login') window.location.assign('/member/login');
    }
    return Promise.reject(error);
  }
);

export default client;
