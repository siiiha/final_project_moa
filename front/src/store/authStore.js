import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

// AI작성: JWT는 서명 검증을 서버가 담당하며, 이 함수는 화면 메뉴 노출용 역할 정보만 읽는다.
export function getSystemRole(token) {
  try {
    const payload = String(token || '').split('.')[1];

    if (!payload) return null;

    const normalizedPayload = payload
      .replace(/-/g, '+')
      .replace(/_/g, '/');
    const decodedPayload = window.atob(
      normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '='),
    );

    return JSON.parse(decodedPayload).role ?? null;
  } catch {
    return null;
  }
}

// AI작성: 관리자 화면의 메뉴 노출과 프론트 라우트 보호에 같은 판별 기준을 사용한다.
export function isAdminToken(token) {
  return getSystemRole(token) === 'ADMIN';
}

// AI작성: access token만 저장하고 refresh token은 HttpOnly 쿠키로 관리한다.
const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      authVersion: 0,

      setToken: (token) =>
        set((state) => ({
          token,
          authVersion: state.authVersion + 1,
        })),

      // AI작성: 재발급은 현재 로그인 상태를 유지하며 토큰만 교체한다.
      replaceToken: (token) => set({ token }),

      clearToken: () =>
        set((state) => ({
          token: null,
          authVersion: state.authVersion + 1,
        })),
    }),
    {
      name: 'moa-auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({ token: state.token }),
    },
  ),
);

export default useAuthStore;
