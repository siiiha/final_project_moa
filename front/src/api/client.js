import axios from 'axios';
import useAuthStore from '../store/authStore.js';

// 모든 API 호출이 이 인스턴스를 거친다.
// baseURL 이 상대 경로('/api/v1')라서 요청은 Vite 개발 서버(5173)로 가고,
// vite.config.js 의 proxy 가 실제 백엔드로 전달한다.
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

// AI작성: 로그인한 사용자의 JWT를 인증이 필요한 API 요청 헤더에 자동으로 포함한다.
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default client;
